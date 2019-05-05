import cscore
from networktables import NetworkTables
import networktables
import cv2
import numpy
from cscore import CameraServer
from app.classes.SettingsManager import SettingsManager
import time
import json


class VisionHandler:
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
        camera_server = cscore.CameraServer.getInstance()
        # NetworkTables.startClientTeam(team=SettingsManager.general_settings.get("team_number", 1577))
        NetworkTables.initialize("localhost")
        # NetworkTables.initialize()

        for cam in SettingsManager().usb_cameras:
            self.camera_process(SettingsManager().usb_cameras[cam],cam)

    def camera_process(self, camera,cam_name):

        curr_pipline = list(SettingsManager.cams[cam_name]["pipelines"].values())[0]

        def change_camera_values():
            camera.setBrightness(0)
            camera.setExposureManual(0)

        def pipeline_listener(table, key, value, is_new):
            if(is_new):
                curr_pipline = SettingsManager.cams[cam_name]["pipelines"][value]
                change_camera_values()

        def mode_listener(table, key, value, is_new):
            pass

        image = numpy.zeros(shape=(SettingsManager().cams[cam_name]["video_mode"]["width"], SettingsManager().cams[cam_name]["video_mode"]["height"], 3), dtype=numpy.uint8)
        table = NetworkTables.getTable("/Chameleon-Vision/" + camera.getInfo().name)

        table.addEntryListenerEx(pipeline_listener, key="Pipeline",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        table.addEntryListenerEx(mode_listener, key="Driver_Mode",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        change_camera_values()
        cs = CameraServer.getInstance()
        cv_sink = cs.getVideo(camera=camera)
        cv_publish = cs.putVideo(name=cam_name, width=SettingsManager().cams[cam_name]["video_mode"]["width"],
                                 height=SettingsManager().cams[cam_name]["video_mode"]["height"])
        cam_area = SettingsManager().cams[cam_name]["video_mode"]["width"] * SettingsManager().cams[cam_name]["video_mode"]["height"]
        while True:
            start = time.time()
            _, image = cv_sink.grabFrame(image)
            hsv_image = self._hsv_threshold(curr_pipline["hue"],
                                            curr_pipline["saturation"], curr_pipline["value"],
                                            image, curr_pipline["erode"], curr_pipline["dilate"])


            # if table.getBoolean("Driver_Mode", False):
            contours = self.find_contours(hsv_image)
            filtered_contours = self.filter_contours(contours, cam_area, curr_pipline["area"], curr_pipline["ratio"], curr_pipline["extent"])
            image = self.draw_image(input_image=image, is_binary=False, rectangles=filtered_contours)
            cv_publish.putFrame(image)
            end = time.time()
            print(1/(end-start))

