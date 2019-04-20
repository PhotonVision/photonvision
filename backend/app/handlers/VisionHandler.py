import cscore
from networktables import NetworkTables
import cv2
import numpy
from cscore import CameraServer
from app.classes.SettingsManager import SettingsManager


class VisionHandler:
    def __init__(self):
        self.kernel = numpy.ones((5, 5), numpy.uint8)

    def _hsv_threshold(self, hue: list, saturation: list, value: list, img: numpy.ndarray, is_erode: bool, is_dilate: bool):
        # img = cv2.medianBlur(img, 1)
        # not sure if we need noise reduction now with erode it hurts the precision if val is to high

        img = cv2.erode(img, kernel=self.kernel, iterations=is_erode)
        img = cv2.dilate(img, kernel=self.kernel, iterations=is_dilate)
        out = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        return cv2.inRange(out, (hue[0], saturation[0], value[0]), (hue[1], saturation[1], value[1]))

    def find_contours(self, binary_img: numpy.ndarray):
        _, contours, _ = cv2.findContours(binary_img, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        return contours

    def filter_contours(self, input_contours, camera_area, min_area, max_area, min_ratio, max_ratio, min_extent, max_extent):
        output = []
        rectangle = []

        for contour in input_contours:

            rect = cv2.minAreaRect(contour)
            # center_point = rect[0]
            contour_area = cv2.contourArea(contour)
            rect_area = rect[1][0] * rect[1][1]

            try:
                extent = float(contour_area) / rect_area
                ratio = float(rect[1][0]) / rect[1][1]
                area = rect_area / camera_area
            except:
                continue

            if area < min_area or area > max_area:
                continue
            if ratio < min_ratio or ratio > max_ratio:
                continue
            if extent < min_extent or extent > max_extent:
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
        NetworkTables.startClientTeam(team=SettingsManager.general_settings.get("team_number", 1577))
        NetworkTables.initialize()

    def camera_process(self, camera, stream):
        def driver_mode_listener(key, value, is_new):
            raise NotImplementedError()

        def pipeline_listener(key, value, is_new):
            raise NotImplementedError()

        image = numpy.zeros(shape=(0, 0, 3), dtype=numpy.uint8)
        table = NetworkTables.getTable("/Chameleon-Vision/" + camera.getInfo().name)
        NetworkTables.addEntryListener("Driver_Mode", driver_mode_listener)
        NetworkTables.addEntryListener("Pipeline", pipeline_listener)

        cv_sink = CameraServer.getInstance().getVideo(camera=camera)

        while True:
            _, image = cv_sink.grabFrame(image)
            hsv_image = self._hsv_threshold()
            filtered_contours = None

            if table.getBoolean("Driver_Mode", False):
                contours = self.find_contours(hsv_image)
                filtered_contours = self.filter_contours(contours)

            image = self.draw_image(input_image=hsv_image, is_binary=False, rectangles=filtered_contours)
            stream.putFrame(image)


