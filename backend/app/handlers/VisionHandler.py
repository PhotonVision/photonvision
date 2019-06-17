from networktables import NetworkTables
import networktables
import cv2
import numpy
from cscore import CameraServer
from app.classes.SettingsManager import SettingsManager
from ..classes.Singleton import Singleton
import time
from multiprocessing import Process
import threading
import zmq


class VisionHandler(metaclass=Singleton):
    def __init__(self):
        self.kernel = numpy.ones((5, 5), numpy.uint8)

    def _hsv_threshold(self, hue: list, saturation: list, value: list, img: numpy.ndarray, is_erode: bool,
                       is_dilate: bool):
        # img = cv2.medianBlur(img, 1)
        # not sure if we need noise reduction now with erode it hurts the precision if val is to high

        img = cv2.erode(img, kernel=self.kernel, iterations=is_erode)
        img = cv2.dilate(img, kernel=self.kernel, iterations=is_dilate)
        out = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        return cv2.inRange(out, (hue[0], saturation[0], value[0]), (hue[1], saturation[1], value[1]))

    def find_contours(self, binary_img: numpy.ndarray):
        _, contours, _ = cv2.findContours(binary_img, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        return contours

    def filter_contours(self, input_contours, camera_area, area, ratio, extent):
        output = []
        rectangle = []

        for contour in input_contours:

            rect = cv2.minAreaRect(contour)
            # center_point = rect[0]
            contour_area = cv2.contourArea(contour)
            rect_area = rect[1][0] * rect[1][1]

            try:
                extent_percent = float(contour_area) / rect_area
                ratio_percent = float(rect[1][0]) / rect[1][1]
                area_percent = rect_area / camera_area
            except:
                continue

            if area_percent < area[0] or area_percent > area[1]:
                continue
            if ratio_percent < ratio[0] or ratio_percent > ratio[1]:
                continue
            if extent_percent < extent[0] or extent_percent > extent[1]:
                continue

            output.append(contour)
            rectangle.append(rect)

        return [output, rectangle]

    def draw_image(self, input_image: numpy.ndarray, is_binary: bool, rectangles):
        if is_binary:
            input_image = cv2.cvtColor(input_image, cv2.COLOR_GRAY2RGB)
        for rectangle in rectangles[1]:
            box = cv2.boxPoints(rectangle)
            box = numpy.int0(box)
            cv2.drawContours(input_image, [box], 0, (0, 0, 255), 2)
            center_point = (int(rectangle[0][0]), int(rectangle[0][1]))
            cv2.circle(input_image, center_point, 0, (0, 255, 0), thickness=3, lineType=8, shift=0)
        return input_image

    def run(self):
        # NetworkTables.startClientTeam(team=SettingsManager.general_settings.get("team_number", 1577))
        NetworkTables.initialize("localhost")
        # NetworkTables.initialize()
        cs = CameraServer.getInstance()
        port = 5550

        for cam_name in SettingsManager().usb_cameras:
            threading.Thread(target=self.thread_proc, args=(cs, cam_name, str(port))).start()
            port += 1

    def thread_proc(self, cs, cam_name, port="5557"):
        cv_sink = cs.getVideo(camera=SettingsManager.usb_cameras[cam_name])

        width = SettingsManager().cams[cam_name]["video_mode"]["width"]
        height = SettingsManager().cams[cam_name]["video_mode"]["height"]

        image = numpy.zeros(shape=(width, height, 3), dtype=numpy.uint8)

        cv_publish = cs.putVideo(name=cam_name, width=width, height=height)

        context = zmq.Context()
        socket = context.socket(zmq.REQ)
        socket.bind("tcp://*:%s" % port)
        p = Process(target=self.camera_process, args=(cam_name, port))
        p.start()
        pipeline = SettingsManager().cams[cam_name]["pipelines"]["pipeline0"]
        while True:
            # start = time.time()
            if(pipeline != SettingsManager().cams[cam_name]["pipelines"]["pipeline0"]):

                pipeline = SettingsManager().cams[cam_name]["pipelines"]["pipeline0"]

            _, image = cv_sink.grabFrame(image)
            socket.send_pyobj({'image': image,
                               'pipeline': pipeline})
            # end = time.time()
            image = socket.recv_pyobj()
            cv_publish.putFrame(image)

            # print(cam_name + "  " + str(1 / (end - start)))

    def camera_process(self, cam_name, port):



        # def change_camera_values():
        #     camera.setBrightness(0)
        #     camera.setExposureManual(0)
        #
        # def pipeline_listener(table, key, value, is_new):
        #     if (is_new):
        #         curr_pipline = SettingsManager.cams[cam_name]["pipelines"][value]
        #         change_camera_values()
        #
        # def mode_listener(table, key, value, is_new):
        #     pass
        #
        # table = NetworkTables.getTable("/Chameleon-Vision/" + camera.getInfo().name)
        #
        # table.addEntryListenerEx(pipeline_listener, key="Pipeline",
        #                          flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        # table.addEntryListenerEx(mode_listener, key="Driver_Mode",
        #                          flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        # change_camera_values()

        width = SettingsManager().cams[cam_name]["video_mode"]["width"]
        height = SettingsManager().cams[cam_name]["video_mode"]["height"]
        cam_area = width * height

        context = zmq.Context()
        socket = context.socket(zmq.REP)
        socket.connect("tcp://localhost:%s" % port)

        while True:
            obj = socket.recv_pyobj()
            image = obj['image']
            curr_pipeline = obj["pipeline"]
            hsv_image = self._hsv_threshold(curr_pipeline["hue"],
                                            curr_pipeline["saturation"], curr_pipeline["value"],
                                            image, curr_pipeline["erode"], curr_pipeline["dilate"])
            # if table.getBoolean("Driver_Mode", False):
            contours = self.find_contours(hsv_image)
            filtered_contours = self.filter_contours(contours, cam_area, curr_pipeline["area"], curr_pipeline["ratio"],
                                                     curr_pipeline["extent"])
            res = self.draw_image(input_image=image, is_binary=False, rectangles=filtered_contours)
            socket.send_pyobj(res)
