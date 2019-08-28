import cv2
import numpy
import math
from enum import Enum, unique


class VisionHandler():
    def __init__(self):
        self.kernel = numpy.ones((5, 5), numpy.uint8)

    def _hsv_threshold(self, hue: list, saturation: list, value: list, img: numpy.ndarray, is_erode: bool,
                       is_dilate: bool):

        hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        thresh = cv2.inRange(hsv, (hue[0], saturation[0], value[0]), (hue[1], saturation[1], value[1]))
        erode_img = cv2.erode(thresh, kernel=self.kernel, iterations=is_erode)
        dilate_img = cv2.dilate(erode_img, kernel=self.kernel, iterations=is_dilate)
        return dilate_img

    def find_contours(self, binary_img: numpy.ndarray):

        contours, _ = cv2.findContours(binary_img, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_TC89_L1)
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

    def output_contour(self, sorted_contours):
        if len(sorted_contours) > 0:
            selected_contour = sorted_contours[0]
            rect = cv2.minAreaRect(selected_contour)
        else:
            return []
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
        pitch *= -1
        return pitch

    def calculate_yaw(self, pixel_x, center_x, h_focal_length):
        yaw = math.degrees(math.atan((pixel_x - center_x) / h_focal_length))
        return yaw
