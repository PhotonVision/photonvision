import math

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
        self.cs = CameraServer.getInstance()
        self.settings_manager = SettingsManager()
        self.vision_handler = VisionHandler()
        self.port = port
        self.cam_name = cam_name

    def run(self):
        threading.Thread(target=self.thread_proc).start()

    def thread_proc(self):
        cam_name = self.cam_name
        port = self.port
        global p_image
        global nt_data
        global table
        nt_data = {'valid': False}
        asyncio.set_event_loop(asyncio.new_event_loop())
        self.settings_manager.cams_curr_pipeline[cam_name] = "pipeline0"
        pipeline = self.settings_manager.cams[cam_name]["pipelines"][self.settings_manager.cams_curr_pipeline[cam_name]]
        FOV = self.settings_manager.cams[cam_name]["FOV"]

        def change_camera_values(pipline):
            self.settings_manager.usb_cameras[cam_name].setBrightness(pipeline['brightness'])
            self.settings_manager.usb_cameras[cam_name].setExposureManual(pipeline['exposure'])
            self.settings_manager.usb_cameras[cam_name].setWhiteBalanceAuto()

        def pipeline_listener(table, key, value, is_new):
            asyncio.set_event_loop(asyncio.new_event_loop())
            self.settings_manager.cams_curr_pipeline[cam_name] = value
            change_camera_values(pipeline)
            if cam_name == self.settings_manager.general_settings['curr_camera']:
                self.settings_manager.general_settings['curr_pipeline'] = value
                update_settings = self.settings_manager.get_curr_pipeline()
                update_settings['curr_pipeline'] = self.settings_manager.general_settings["curr_pipeline"]
                send_all_async(update_settings)

        def mode_listener(table, key, value, is_new):
            change_camera_values({
                'brightness': 25,
                'exposure': 15
            })

        table = NetworkTables.getTable("/Chameleon-Vision/" + cam_name)
        table.putString('Pipeline', self.settings_manager.cams_curr_pipeline[cam_name])
        table.addEntryListenerEx(pipeline_listener, key="Pipeline",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        table.addEntryListenerEx(mode_listener, key="Driver_Mode",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        # gettings video from curent camera
        cv_sink = self.cs.getVideo(camera=self.settings_manager.usb_cameras[cam_name])

        width = self.settings_manager.cams[cam_name]["video_mode"]["width"]
        height = self.settings_manager.cams[cam_name]["video_mode"]["height"]

        # setting up a video server for camera
        cv_publish = self.cs.putVideo(name=cam_name, width=width, height=height)
        # saving camera port in cam name dict for usage in client
        self.settings_manager.cams_port[cam_name] = self.cs._sinks['serve_' + cam_name].getPort()

        # setting up a zmq connection to the opencv subprocess
        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.bind('tcp://*:%s' % str(port))

        # starting the process with inital values
        p = Process(target=self.camera_process, args=(cam_name, port, FOV))
        p.start()

        change_camera_values(pipeline)

        def _image_thread():
            global image
            global p_image
            global time_stamp
            image = numpy.zeros(shape=(width, height, 3), dtype=numpy.uint8)
            p_image = image
            while True:
                time_stamp, image = cv_sink.grabFrame(image)

        def _publish_thread():
            # asyncio.set_event_loop(asyncio.new_event_loop())
            while True:
                try:
                    cv_publish.putFrame(p_image)
                    table.putBoolean('valid', nt_data['valid'])
                    # check if point is valid
                    if nt_data['valid']:
                        # send the point using network tables
                        table.putNumber('pitch', nt_data['pitch'])
                        table.putNumber('yaw', nt_data['yaw'])
                        table.putNumber('fps', nt_data['fps'])
                        table.putNumber('time_stamp', time_stamp)
                        # if the selected camera in ui is this cam send the point to the ui
                except:
                    pass

        threading.Thread(target=_image_thread).start()
        threading.Thread(target=_publish_thread).start()

        while True:
            pipeline = self.settings_manager.cams[cam_name]["pipelines"][
                self.settings_manager.cams_curr_pipeline[cam_name]]
            socket.send_json(dict(
                pipeline=pipeline
            ), zmq.SNDMORE)

            socket.send_pyobj(image)
            p_image = socket.recv_pyobj()
            nt_data = socket.recv_json()
            if self.settings_manager.general_settings['curr_camera'] == cam_name:
                try:
                    send_all_async({
                        'raw_point': nt_data['raw_point'],
                        'point': {
                            'pitch': nt_data['pitch'],
                            'yaw': nt_data['yaw'],
                            'fps': nt_data['fps']
                        }
                    })
                except:
                    pass

    def camera_process(self, cam_name, port, FOV):
        from fractions import Fraction

        diagonalView = math.radians(FOV)  # needs to be implemented in client

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

        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.connect('tcp://localhost:%s' % str(port))
        filter_contours = self.vision_handler.Filter_Contours(center_x=centerX, center_y=centerY)
        x = 1
        counter = 0
        start_time = time.time()
        fps = 0
        while True:
            obj = socket.recv_json()
            image = socket.recv_pyobj()
            curr_pipeline = obj["pipeline"]
            if curr_pipeline['orientation'] == "Inverted":
                M = cv2.getRotationMatrix2D((width / 2, height / 2), 180, 1)
                image = cv2.warpAffine(image, M, (width, height))
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
            socket.send_pyobj(res)
            socket.send_json(dict(
                pitch=pitch,
                yaw=yaw,
                valid=valid,
                raw_point=center,
                fps=fps
            ))
            counter += 1
            if (time.time() - start_time) > x:
                fps = (counter / (time.time() - start_time))
                counter = 0
                start_time = time.time()



