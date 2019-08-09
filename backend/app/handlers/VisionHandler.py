import asyncio
from networktables import NetworkTables
import networktables
import cv2
import numpy
from cscore import CameraServer
from app.classes.SettingsManager import SettingsManager
from ..classes.Singleton import Singleton
from multiprocessing import Process
import threading
import zmq
import math
from enum import Enum, unique
from ..handlers.SocketHandler import send_all_async


class VisionHandler(metaclass=Singleton):
    def __init__(self):
        self.kernel = numpy.ones((5, 5), numpy.uint8)

    def _hsv_threshold(self, hue: list, saturation: list, value: list, img: numpy.ndarray, is_erode: bool,
                       is_dilate: bool):
        blur = cv2.blur(img, (3, 3))
        hsv = cv2.cvtColor(blur, cv2.COLOR_BGR2HSV)
        lower = numpy.array([hue[0], saturation[0], value[0]])
        upper = numpy.array([hue[1], saturation[1], value[1]])
        thresh = cv2.inRange(hsv, lower, upper)
        erode_img = cv2.erode(thresh, kernel=self.kernel, iterations=is_erode)
        dilate_img = cv2.dilate(erode_img, kernel=self.kernel, iterations=is_dilate)
        return dilate_img

    def find_contours(self, binary_img: numpy.ndarray):
        _, contours, _ = cv2.findContours(binary_img, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        return contours

    class Filter_Contours:
        def __init__(self,center_x, center_y):
            self.sort_mode = self.SortMode(center_x=center_x, center_y=center_y)
            self.center_y = center_y
            self.center_x = center_x

        class SortMode:
            def __init__(self, center_x, center_y):
                self.center_x = center_x
                self.center_y = center_y

            @classmethod
            def moment_x(cls,contour):
                M = cv2.moments(contour)
                try:
                    x = float(M['m10'] / M['m00'])
                except ZeroDivisionError:
                    x = 0
                return x

            @classmethod
            def moment_y(cls, contour):
                M = cv2.moments(contour)
                try:
                    y = float(M['m01'] / M['m00'])
                except ZeroDivisionError:
                    y = 0
                return y

            @classmethod
            def calc_distance(cls,contour, center_x, center_y):
                M = cv2.moments(contour)
                try:
                    x = int(M['m10'] / M['m00'])
                except ZeroDivisionError:
                    x = 0
                try:
                    y = int(M['m01'] / M['m00'])
                except ZeroDivisionError:
                    y = 0
                # this function was suggested by my girlfriend maya jugend that i really love
                return math.sqrt((center_x-x)**2 + (center_y-y)**2)

            def Largest(self, input_contours):
                return sorted(input_contours, key=lambda x: cv2.contourArea(x), reverse=True)

            def Smallest(self, input_contours):
                return sorted(input_contours, key=lambda x: cv2.contourArea(x))

            def Highest(self, input_contours):
                return sorted(input_contours, key=lambda x: self.moment_y(x))

            def Lowest(self, input_contours):
                return sorted(input_contours, key=lambda x: self.moment_y(x),reverse=True)

            def Rightmost(self, input_contours):
                return sorted(input_contours, key=lambda x: self.moment_x(x), reverse=True)

            def Leftmost(self, input_contours):
                return sorted(input_contours, key=lambda x: self.moment_x(x))

            def Closest(self, input_contours):
                return sorted(input_contours, key=lambda x: self.calc_distance(x, center_x=self.center_x,
                                                                               center_y=self.center_y), reverse=True)

        def filter_contours(self, input_contours, cam_area, area, ratio, extent, sort_mode, target_grouping,
                            target_intersection):
            class TargetGroup(Enum):
                Single = 1
                Dual = 2
                Triple = 3
                Quadruple = 4
                Quintuple = 6

            def group_target(i_contours, target_group, intersection_point):

                def is_intersecting(contour_a, contour_b, intersection_direction):

                    [vx_a, vy_a, x0_a, y0_a] = cv2.fitLine(contour_a, cv2.DIST_L2, 0, 0.01, 0.01)
                    [vx_b, vy_b, x0_b, y0_b] = cv2.fitLine(contour_b, cv2.DIST_L2, 0, 0.01, 0.01)
                    # getting line data of both contours
                    m_a = vy_a / vx_a
                    m_b = vy_b / vx_b
                    # calculating slope of both lines
                    try:
                        intersection_x = ((m_a * x0_a) - y0_a - (m_b * x0_b) + y0_b) / (m_a - m_b)
                    except ZeroDivisionError:
                        if intersection_direction == 'Parallel':
                            return True
                        else:
                            return False
                    intersection_y = (m_a * (intersection_x - x0_a)) + y0_a
                    # finding intersection point
                    if intersection_direction == 'Up':
                        if intersection_y < self.center_y:
                            return True
                    elif intersection_direction == 'Down':
                        if intersection_y > self.center_y:
                            return True
                    elif intersection_direction == 'Left':
                        if intersection_x < self.center_x:
                            return True
                    elif intersection_direction == 'Right':
                        if intersection_x > self.center_x:
                            return True
                    else:
                        return False
                if target_group != TargetGroup.Single:
                    f_contour_list = []
                    for index, g_contour in enumerate(i_contours):
                        final_contour = g_contour
                        for c in range(target_group.value - 1):
                            try:
                                first_contour = i_contours[index + c]
                                second_contour = i_contours[index + c + 1]
                            except IndexError:
                                final_contour = []
                                break
                            if is_intersecting(first_contour, second_contour, intersection_point):
                                final_contour = numpy.concatenate((final_contour, second_contour))

                            else:
                                final_contour = []
                                break
                        if final_contour != []:
                            f_contour_list.append(final_contour)

                    return f_contour_list
                else:
                    return i_contours

            '''start of the first filtration of contours'''
            filtered_contours = []
            for contour in input_contours:
                try:
                    contour_area = cv2.contourArea(contour)
                    target_area = float(contour_area / cam_area)*100

                    if target_area >= area[1] or target_area <= area[0]:
                        continue

                    rect = cv2.minAreaRect(contour)
                    bounding_rect_area = rect[1][0] * rect[1][1]
                    try:
                        target_fullness = float(contour_area / bounding_rect_area)*100
                    except ZeroDivisionError:
                        target_fullness = 0

                    if target_fullness <= extent[0] or target_fullness >= extent[1]:
                        continue
                    try:
                        aspect_ratio = float(rect[1][0]/rect[1][1])
                    except ZeroDivisionError:
                        aspect_ratio = 0
                    if aspect_ratio <= ratio[0] or aspect_ratio >= ratio[1]:
                        continue

                    filtered_contours.append(contour)
                except Exception as e:
                    print(e)
                    continue
            #checking for contour grouping before sorting
            grouped_contours = group_target(filtered_contours, TargetGroup[target_grouping], target_intersection)
            try:
                sorted_contours = getattr(self.sort_mode, sort_mode)(grouped_contours)
            except TypeError:
                sorted_contours = []
            return sorted_contours

    @unique
    class Region(Enum):
        UP_MOST = 0
        RIGHT_MOST = 1
        DOWN_MOST = 2
        LEFT_MOST = 3
        CENTER_MOST = 4
        
    def output_contour(self, sorted_contours):
        if len(sorted_contours) > 0:
            selected_contour = sorted_contours[0]
            rect = cv2.minAreaRect(selected_contour)
        else:
            return []

        # crosshair_calibration function to "put" camera in the middle
        return rect
            
    def draw_image(self, input_image, contour):
        if len(input_image.shape)<3:
            input_image = cv2.cvtColor(input_image, cv2.COLOR_GRAY2RGB)
        if contour != []:
            box = cv2.boxPoints(contour)
            box = numpy.int0(box)
            cv2.drawContours(input_image, [box], 0, (0, 0, 255), 3)

        # center_point = (int(rectangle[0][0]), int(rectangle[0][1]))
        # cv2.circle(input_image, center_point, 0, (0, 255, 0), thickness=3, lineType=8, shift=0)
        return input_image

    def calculate_pitch(self, pixel_y, center_y, v_focal_length):
        pitch = math.degrees(math.atan((pixel_y - center_y) / v_focal_length))
        # Just stopped working have to do this:
        pitch *= -1
        return pitch

    def calculate_yaw(self, pixel_x, center_x, h_focal_length):
        yaw = math.degrees(math.atan((pixel_x - center_x) / h_focal_length))
        return yaw

    def run(self):
        # NetworkTables.startClientTeam(team=SettingsManager.general_settings.get("team_number", 1577))
        NetworkTables.initialize("localhost")

        cs = CameraServer.getInstance()
        port = 5550

        for cam_name in SettingsManager().usb_cameras:
            threading.Thread(target=self.thread_proc, args=(cs, cam_name, port)).start()
            port += 1

    def thread_proc(self, cs, cam_name, port=5557):
        asyncio.set_event_loop(asyncio.new_event_loop())
        pipeline_name = 'pipeline0'
        pipeline = SettingsManager().cams[cam_name]["pipelines"][pipeline_name]
        FOV = SettingsManager().cams[cam_name]["FOV"]

        def change_camera_values(pipline):
            SettingsManager.usb_cameras[cam_name].setBrightness(pipeline['brightness'])
            SettingsManager.usb_cameras[cam_name].setExposureManual(pipeline['exposure'])
            SettingsManager.usb_cameras[cam_name].setWhiteBalanceAuto()

        def pipeline_listener(table, key, value, is_new):
            global pipeline
            if is_new:
                pipeline = SettingsManager.cams[cam_name]["pipelines"][value]
                change_camera_values()

        def mode_listener(table, key, value, is_new):
            pass

        table = NetworkTables.getTable("/Chameleon-Vision/" + cam_name)

        table.addEntryListenerEx(pipeline_listener, key="Pipeline",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)
        table.addEntryListenerEx(mode_listener, key="Driver_Mode",
                                 flags=networktables.NetworkTablesInstance.NotifyFlags.UPDATE)

        cv_sink = cs.getVideo(camera=SettingsManager.usb_cameras[cam_name])

        width = SettingsManager().cams[cam_name]["video_mode"]["width"]
        height = SettingsManager().cams[cam_name]["video_mode"]["height"]

        image = numpy.zeros(shape=(width, height, 3), dtype=numpy.uint8)

        cv_publish = cs.putVideo(name=cam_name, width=width, height=height)

        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.bind('tcp://*:%s' % str(port))

        p = Process(target=self.camera_process, args=(cam_name, port, FOV))
        p.start()

        change_camera_values(pipeline)
        while True:
            _, image = cv_sink.grabFrame(image)
            socket.send_json(dict(
                pipeline=pipeline
            ))

            socket.send_pyobj(image)
            p_image = socket.recv_pyobj()
            nt_data = socket.recv_json()
            table.putBoolean('valid', nt_data['valid'])
            # check if point is valid
            if nt_data['valid']:
                #send the point using network tables
                table.putNumber('pitch', nt_data['pitch'])
                table.putNumber('yaw', nt_data['yaw'])
                #if the selected camera in ui is this cam send the point to the ui
                if SettingsManager().general_settings['curr_camera'] is cam_name:
                    try:
                        if nt_data['raw_point'] is not None:
                            send_all_async({
                                'raw_point': nt_data['raw_point'],
                                'point': {
                                    'pitch': nt_data['pitch'],
                                    'yaw': nt_data['yaw']
                                }
                            })
                    except Exception as e:
                        print(e)
            #send the image to the camera server

            cv_publish.putFrame(p_image)

    def camera_process(self, cam_name, port, FOV):
        from fractions import Fraction

        diagonalView = math.radians(FOV) #needs to be implemented in client

        width = SettingsManager().cams[cam_name]["video_mode"]["width"]
        height = SettingsManager().cams[cam_name]["video_mode"]["height"]
        centerX = (width / 2) - .5
        centerY = (height / 2) - .5
        cam_area = width * height
        
        aspect_fraction = Fraction(width,height)
        horizontal_ratio = aspect_fraction.numerator
        vertical_ratio = aspect_fraction.denominator
        
        horizontalView = math.atan(math.tan(diagonalView/2) * (horizontal_ratio / diagonalView)) * 2
        verticalView = math.atan(math.tan(diagonalView/2) * (vertical_ratio / diagonalView)) * 2
        
        H_FOCAL_LENGTH = width / (2*math.tan((horizontalView/2)))
        V_FOCAL_LENGTH = height / (2*math.tan((verticalView/2)))

        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.connect('tcp://localhost:%s' % str(port))
        filter_contours = self.Filter_Contours(center_x=centerX, center_y=centerY)
        while True:
            obj = socket.recv_json()
            image = socket.recv_pyobj()
            curr_pipeline = obj["pipeline"]
            hsv_image = self._hsv_threshold(curr_pipeline["hue"],
                                            curr_pipeline["saturation"], curr_pipeline["value"],
                                            image, curr_pipeline["erode"], curr_pipeline["dilate"])
            # if table.getBoolean("Driver_Mode", False):
            contours = self.find_contours(hsv_image)
            filtered_contours = filter_contours.filter_contours(input_contours=contours, area=curr_pipeline['area'],
                                                                ratio=curr_pipeline['ratio'],
                                                                extent=curr_pipeline['extent'],
                                                                sort_mode=curr_pipeline['sort_mode'], cam_area=cam_area,
                                                                target_grouping=curr_pipeline['target_group'],
                                                                target_intersection=
                                                                curr_pipeline['target_intersection'])
            final_contour = self.output_contour(filtered_contours)
            try:
                center = final_contour[0]
                pitch = self.calculate_pitch(pixel_y=center[1], center_y=centerY, v_focal_length=V_FOCAL_LENGTH)
                yaw = self.calculate_yaw(pixel_x=center[0], center_x=centerX, h_focal_length=H_FOCAL_LENGTH)
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
            res = self.draw_image(input_image=draw_image, contour=final_contour)
            socket.send_pyobj(res)
            socket.send_json(dict(
                pitch=pitch,
                yaw=yaw,
                valid=valid,
                raw_point=center
            ))


