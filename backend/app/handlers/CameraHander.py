import math
import cv2
import numpy
from cscore import CameraServer
from app.classes.SettingsManager import SettingsManager
from ..handlers.SocketHandler import send_all_async
from multiprocessing import Process
import threading
import zmq
import asyncio
import time
from networktables import NetworkTables
import networktables
from .VisionHandler import VisionHandler


class CameraHandler:
    def __init__(self, cam_name, port):
        #settings vars up for vision loop
        self.cs = CameraServer.getInstance()
        self.settings_manager = SettingsManager()
        self.vision_handler = VisionHandler()
        self.port = port
        self.cam_name = cam_name
        self.image = None
        self.p_image = None
        self.table = None
        self.nt_data = {'valid': False}
        self.time_stamp = 0

    def run(self):
        #starting main thread
        threading.Thread(target=self.thread_proc).start()

    def thread_proc(self):
        self.settings_manager.cams_curr_pipeline[self.cam_name] = "pipeline0"
        pipeline = self.settings_manager.cams[self.cam_name]["pipelines"][self.settings_manager.cams_curr_pipeline[self.cam_name]]
        FOV = self.settings_manager.cams[self.cam_name]["FOV"]

        def change_camera_values(pipeline):
            self.settings_manager.usb_cameras[self.cam_name].setBrightness(pipeline['brightness'])
            self.settings_manager.usb_cameras[self.cam_name].setExposureManual(pipeline['exposure'])
            self.settings_manager.usb_cameras[self.cam_name].setWhiteBalanceAuto()

        def pipeline_listener(table, key, value, is_new):
            asyncio.set_event_loop(asyncio.new_event_loop())
            if value in self.settings_manager.cams[self.cam_name]['pipelines'].keys():
                self.settings_manager.cams_curr_pipeline[self.cam_name] = value
                change_camera_values(pipeline)
                if self.cam_name == self.settings_manager.general_settings['curr_camera']:
                    self.settings_manager.general_settings['curr_pipeline'] = value
                    update_settings = self.settings_manager.get_curr_pipeline()
                    update_settings['curr_pipeline'] = self.settings_manager.general_settings["curr_pipeline"]
                    send_all_async(update_settings)
            else:
                self.table.putString('Pipeline', self.settings_manager.cams_curr_pipeline[self.cam_name])

        def mode_listener(table, key, value, is_new):
            if value:
                change_camera_values({
                    'brightness': 25,
                    'exposure': 15
                })
            else:
                change_camera_values(pipeline)
        #setting up network table
        self.table = NetworkTables.getTable("/Chameleon-Vision/" + self.cam_name)
        #init values for pipeline and driver mode
        self.table.putString('Pipeline', self.settings_manager.cams_curr_pipeline[self.cam_name])
        self.table.putBoolean('Driver_Mode', False)
        self.table.addEntryListenerEx(pipeline_listener, key="Pipeline",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        self.table.addEntryListenerEx(mode_listener, key="Driver_Mode",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)

        # getting video from current camera
        cv_sink = self.cs.getVideo(camera=self.settings_manager.usb_cameras[self.cam_name])

        width = self.settings_manager.cams[self.cam_name]["video_mode"]["width"]
        height = self.settings_manager.cams[self.cam_name]["video_mode"]["height"]

        # setting up a video server for camera
        cv_publish = self.cs.putVideo(name=self.cam_name, width=width, height=height)
        # saving camera port in cam name dict for usage in client
        self.settings_manager.cams_port[self.cam_name] = self.cs._sinks['serve_' + self.cam_name].getPort()

        # setting up a zmq connection to the opencv subprocess
        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.bind('tcp://*:%s' % str(self.port))

        # starting the process with initial values
        p = Process(target=self.camera_process, args=(self.cam_name, self.port, FOV))
        p.start()

        change_camera_values(pipeline)

        def _publish_thread():
            #getting image values and publishing process image and data
            self.image = numpy.zeros(shape=(width, height, 3), dtype=numpy.uint8)
            self.p_image = self.image
            while True:
                try:
                    self.time_stamp, self.image = cv_sink.grabFrame(self.image)
                    cv_publish.putFrame(self.p_image)
                    self.table.putBoolean('valid', self.nt_data['valid'])
                    # check if point is valid
                    if self.nt_data['valid']:
                        # send the point using network tables
                        self.table.putNumber('pitch', self.nt_data['pitch'])
                        self.table.putNumber('yaw', self.nt_data['yaw'])
                    self.table.putNumber('time_stamp', self.nt_data['time_stamp'])
                    self.table.putNumber('fps', self.nt_data['fps'])
                        # if the selected camera in ui is this cam send the point to the ui
                except:
                    pass

        def _socket_thread():
            #publishing to websocket at slower interval
            asyncio.set_event_loop(asyncio.new_event_loop())
            while True:
                time.sleep(0.1)
                if self.settings_manager.general_settings['curr_camera'] == self.cam_name:
                    try:
                        send_all_async({
                            'raw_point': self.nt_data['raw_point'],
                            'point': {
                                'pitch': self.nt_data['pitch'],
                                'yaw': self.nt_data['yaw'],
                                'fps': self.nt_data['fps']
                            }
                        })
                    except:
                        pass

        threading.Thread(target=_publish_thread).start()
        threading.Thread(target=_socket_thread).start()

        while True:
            #sending and reciving data from opencv sub process
            pipeline = self.settings_manager.cams[self.cam_name]["pipelines"][
                self.settings_manager.cams_curr_pipeline[self.cam_name]]

            socket.send_json(dict(
                pipeline=pipeline,
                driver_mode=self.table.getBoolean('Driver_Mode', False)
            ), zmq.SNDMORE)

            socket.send_pyobj((self.time_stamp,self.image))
            self.p_image = socket.recv_pyobj()
            self.nt_data = socket.recv_json()

    def camera_process(self, cam_name, port, FOV):
        from fractions import Fraction
        #calc fov
        diagonalView = math.radians(FOV)

        width = self.settings_manager.cams[cam_name]["video_mode"]["width"]
        height = self.settings_manager.cams[cam_name]["video_mode"]["height"]
        centerX = (width / 2) - .5
        centerY = (height / 2) - .5
        cam_area = width * height

        aspect_fraction = Fraction(width, height)
        horizontal_ratio = aspect_fraction.numerator
        vertical_ratio = aspect_fraction.denominator

        horizontalView = math.atan(math.tan(diagonalView / 2) * (horizontal_ratio / diagonalView)) * 2
        verticalView = math.atan(math.tan(diagonalView / 2) * (vertical_ratio / diagonalView)) * 2

        H_FOCAL_LENGTH = width / (2 * math.tan((horizontalView / 2)))
        V_FOCAL_LENGTH = height / (2 * math.tan((verticalView / 2)))
        #setting up zmq socket
        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.connect('tcp://localhost:%s' % str(port))
        #setting up filter countours class
        filter_contours = self.vision_handler.Filter_Contours(center_x=centerX, center_y=centerY)

        x = 1
        counter = 0
        start_time = time.time()
        fps = 0

        while True:
            obj = socket.recv_json()
            curr_pipeline = obj['pipeline']
            driver_mode = obj['driver_mode']
            time_stamp, image = socket.recv_pyobj()
            if curr_pipeline['orientation'] == "Inverted":
                M = cv2.getRotationMatrix2D((width / 2, height / 2), 180, 1)
                image = cv2.warpAffine(image, M, (width, height))
            if not driver_mode:
                hsv_image = self.vision_handler._hsv_threshold(curr_pipeline["hue"],
                                                curr_pipeline["saturation"], curr_pipeline["value"],
                                                image, curr_pipeline["erode"], curr_pipeline["dilate"])
                # if table.getBoolean("Driver_Mode", False):
                contours = self.vision_handler.find_contours(hsv_image)
                filtered_contours = filter_contours.filter_contours(input_contours=contours, area=curr_pipeline['area'],
                                                                    ratio=curr_pipeline['ratio'],
                                                                    extent=curr_pipeline['extent'],
                                                                    sort_mode=curr_pipeline['sort_mode'], cam_area=cam_area,
                                                                    target_grouping=curr_pipeline['target_group'],
                                                                    target_intersection=
                                                                    curr_pipeline['target_intersection'])

                final_contour = self.vision_handler.output_contour(filtered_contours)

                try:
                    center = final_contour[0]
                    center_x = (center[1] - curr_pipeline['B']) / curr_pipeline["M"]
                    center_y = (center[0] * curr_pipeline["M"]) + curr_pipeline["B"]
                    pitch = self.vision_handler.calculate_pitch(pixel_y=center[1], center_y=center_y, v_focal_length=V_FOCAL_LENGTH)
                    yaw = self.vision_handler.calculate_yaw(pixel_x=center[0], center_x=center_x, h_focal_length=H_FOCAL_LENGTH)
                    valid = True
                except IndexError:
                    center = None
                    pitch = None
                    yaw = None
                    valid = False

                if curr_pipeline['is_binary']:
                    draw_image = hsv_image
                else:
                    draw_image = image

                res = self.vision_handler.draw_image(input_image=draw_image, contour=final_contour)
            else:
                res = image
                center = None
                pitch = None
                yaw = None
                valid = False

            socket.send_pyobj(res)
            socket.send_json(dict(
                pitch=pitch,
                yaw=yaw,
                valid=valid,
                raw_point=center,
                fps=fps,
                time_stamp=time_stamp
            ))
            counter += 1
            if (time.time() - start_time) > x:
                fps = (counter / (time.time() - start_time))
                counter = 0
                start_time = time.time()
