import math

import cscore as cs
import cv2 as cv
import numpy as np
from wpimath import Translation3d, units

from ..estimation.open_cv_help import OpenCVHelp
from ..estimation.rot_trl_transform3d import RotTrlTransform3d
from .sim_camera_properties import SimCameraProperties


class VideoSimUtil:
    TAG_DICT = cv.aruco.getPredefinedDictionary(cv.aruco.DICT_APRILTAG_36h11)
    # Tag IDs start at 0, this should be set to 1 greater than the maximum tag ID
    # required
    NUM_TAGS_36h11: int = 40

    FIELD_LENGTH: units.meters = 16.54175
    FIELD_WIDTH: units.meters = 8.0137

    TAG_36h11_IMAGES: dict[int, cv.typing.MatLike]
    TAG_36h11_MARK_PTS: np.ndarray

    @staticmethod
    def get36h11TagImage(id: int) -> cv.typing.MatLike:
        # 8x8 = 6x6 data + 1-bit black border. OpenCV's 36h11 codes are stored rotated 180°
        # compared to the AprilTag reference images and WPILib's generator, so rotate back.
        marker = cv.aruco.generateImageMarker(
            VideoSimUtil.TAG_DICT, id, 8, borderBits=1
        )
        marker = cv.rotate(marker, cv.ROTATE_180)
        # Add the 1px white quiet zone -> 10x10, same as Generate36h11AprilTagImage
        return cv.copyMakeBorder(marker, 1, 1, 1, 1, cv.BORDER_CONSTANT, value=255)

    @staticmethod
    def loadAprilTagImages() -> dict[int, cv.typing.MatLike]:
        return {
            i: VideoSimUtil.get36h11TagImage(i)
            for i in range(VideoSimUtil.NUM_TAGS_36h11)
        }

    @staticmethod
    def getImageCorners(width: int, height: int) -> np.ndarray:
        # /**
        # * Gets the points representing the corners of this image. Because image pixels
        # * are accessed through a cv::Mat, the point (0,0) actually represents the
        # * center of the top-left pixel and not the actual top-left corner.
        # *
        # * <p>Order of corners returned is: [BL, BR, TR, TL]
        # *
        # * @param size Size of image
        # * @return The corners
        # */
        return np.array(
            [
                [[-0.5, height - 0.5]],
                [[width - 0.5, height - 0.5]],
                [[width - 0.5, -0.5]],
                [[-0.5, -0.5]],
            ],
            dtype=np.float32,
        )

    @staticmethod
    def get36h11MarkerPts(scale: int = 1) -> np.ndarray:
        # /**
        # * Gets the points representing the marker(black square) corners.
        # *
        # * @param scale The scale of the tag image (10*scale x 10*scale image)
        # * @return The points
        # */
        x = y = 1 * scale
        width = height = 8 * scale

        return VideoSimUtil.getImageCorners(width, height) + np.array(
            [x, y], dtype=np.float32
        )

    @staticmethod
    def updateVideoProp(video: cs.CvSource, prop: SimCameraProperties) -> None:
        # /** Updates the properties of this cs::CvSource video stream with the given
        # * camera properties. */
        video.set_resolution(prop.getResWidth(), prop.getResHeight())
        video.set_fps(prop.getFPS())

    @staticmethod
    def warp165h5TagImage(
        tag_id: int, dst_points: np.ndarray, antialiasing: bool, destination: np.ndarray
    ) -> None:
        # /**
        # * Warps the image of a specific 36h11 AprilTag onto the destination image at
        # * the given points.
        # *
        # * @param tagId The id of the specific tag to warp onto the destination image
        # * @param dstPoints Points(4) in destination image where the tag marker(black
        # * square) corners should be warped onto.
        # * @param antialiasing If antialiasing should be performed by automatically
        # *     supersampling/interpolating the warped image. This should be used if
        # * better stream quality is desired or target detection is being done on the
        # * stream, but can hurt performance.
        # * @param destination The destination image to place the warped tag image onto.
        # */

        if not tag_id in VideoSimUtil.TAG_36h11_IMAGES:
            return

        tag_image = VideoSimUtil.TAG_36h11_IMAGES[tag_id]
        tag_points = VideoSimUtil.TAG_36h11_MARK_PTS
        tag_image_corners = VideoSimUtil.getImageCorners(
            tag_image.shape[1], tag_image.shape[0]
        )
        dst_point_mat = dst_points.copy()
        perspec_trf = cv.getPerspectiveTransform(tag_points, dst_point_mat)
        extreme_corners = cv.perspectiveTransform(tag_image_corners, perspec_trf)
        bounding_rect = cv.boundingRect(extreme_corners)

        warped_contour_area = cv.contourArea(extreme_corners)
        warped_tag_upscale = math.sqrt(warped_contour_area) / math.sqrt(
            tag_image.shape[1] * tag_image.shape[0]
        )
        warp_strat = cv.INTER_NEAREST

        super_sampling = 6
        if warped_tag_upscale > 0:
            super_sampling = math.ceil(super_sampling / warped_tag_upscale)
        else:
            super_sampling = 10  # zero-area tag: the limit of 6/upscale, clamped to the max
        super_sampling = max(min(super_sampling, 10), 1)

        if warped_tag_upscale > 2.0:
            warp_strat = cv.INTER_LINEAR
            scale_factor = int(warped_tag_upscale / 3.0 + 2)
            scale_factor = max(min(scale_factor, 40), 1)
            scale_factor = scale_factor * super_sampling
            scaled_tag_image = cv.resize(
                tag_image,
                None,
                fx=scale_factor,
                fy=scale_factor,
                interpolation=cv.INTER_NEAREST,
            )
            tag_points = VideoSimUtil.get36h11MarkerPts(scale_factor)
        else:
            scaled_tag_image = tag_image

        x, y, w, h = bounding_rect

        x -= 1
        y -= 1
        w += 2
        h += 2

        if x < 0:
            w += x
            x = 0
        if y < 0:
            h += y
            y = 0
        dest_h, dest_w = destination.shape[:2]
        w = min(dest_w - x, w)
        h = min(dest_h - y, h)

        if w <= 0 or h <= 0:
            return

        scaled_dst_pts = dst_point_mat
        if super_sampling > 1:
            scaled_dst_pts = dst_point_mat * super_sampling
            x *= super_sampling
            y *= super_sampling
            w *= super_sampling
            h *= super_sampling

        scaled_dst_pts = scaled_dst_pts - np.array([x, y], dtype=np.float32)
        perspec_trf = cv.getPerspectiveTransform(tag_points, scaled_dst_pts)

        temp_roi = cv.warpPerspective(
            scaled_tag_image, perspec_trf, (w, h), flags=warp_strat
        )
        if super_sampling > 1:
            x //= super_sampling
            y //= super_sampling
            w //= super_sampling
            h //= super_sampling
            temp_roi = cv.resize(temp_roi, (w, h), interpolation=cv.INTER_AREA)

        temp_mask = np.zeros(temp_roi.shape[:2], dtype=np.uint8)

        extreme_corners = extreme_corners - np.array([x, y], dtype=np.float32)

        temp_center = extreme_corners.mean(axis=0)

        diff = extreme_corners - temp_center
        diff += np.sign(diff)
        extreme_corners = temp_center + diff

        extreme_corner_int = np.rint(extreme_corners).astype(np.int32)
        cv.fillConvexPoly(temp_mask, extreme_corner_int, 255)

        cv.copyTo(temp_roi, temp_mask, destination[y : y + h, x : x + w])

    @staticmethod
    def getScaledThickness(
        thickness_480p: float, destination: cv.typing.MatLike
    ) -> float:
        # /**
        # * Given a line thickness in a 640x480 image, try to scale to the given
        # * destination image resolution.
        # *
        # * @param thickness480p A hypothetical line thickness in a 640x480 image
        # * @param destination The destination image to scale to
        # * @return Scaled thickness which cannot be less than 1
        # */
        scale_x = destination.shape[1] / 640.0
        scale_y = destination.shape[0] / 480.0
        min_scale = min(scale_x, scale_y)
        return max(thickness_480p * min_scale, 1.0)

    @staticmethod
    def drawInscribedEllipse(
        dst_points: np.ndarray,
        color: cv.typing.Scalar,
        destination: cv.typing.MatLike,
    ) -> None:
        # /**
        # * Draw a filled ellipse in the destination image.
        # *
        # * @param dstPoints The points in the destination image representing the
        # * rectangle in which the ellipse is inscribed.
        # * @param color The color of the ellipse. This is a scalar with BGR values
        # * (0-255)
        # * @param destination The destination image to draw onto. The image should be in
        # * the BGR color space.
        # */

        rect = OpenCVHelp.getMinAreaRect(dst_points)
        # OpenCV stubs type `box` as a cv.typing.RotatedRect, not cv.RotatedRect; both work at runtime
        cv.ellipse(destination, rect, color, thickness=-1, lineType=cv.LINE_AA)  # type: ignore[call-overload]

    @staticmethod
    def drawPoly(
        dst_points: np.ndarray,
        thickness: int,
        color: cv.typing.Scalar,
        is_closed: bool,
        destination: cv.typing.MatLike,
    ) -> None:
        list_of_list_of_points = [np.rint(dst_points).astype(np.int32)]
        if thickness > 0:
            cv.polylines(
                destination,
                list_of_list_of_points,
                is_closed,
                color,
                thickness=thickness,
                lineType=cv.LINE_AA,
            )
        else:
            cv.fillPoly(destination, list_of_list_of_points, color, lineType=cv.LINE_AA)

    @staticmethod
    def drawTagDetection(
        id: int, dst_points: np.ndarray, destination: cv.typing.MatLike
    ) -> None:

        thickness = VideoSimUtil.getScaledThickness(1, destination)
        VideoSimUtil.drawPoly(
            dst_points, int(thickness), [0, 0, 255], True, destination
        )
        x, y, w, _ = cv.boundingRect(dst_points)

        text_pt = (round(x + w + thickness + 0.5), round(y + thickness))
        cv.putText(
            destination,
            str(id),
            text_pt,
            cv.FONT_HERSHEY_PLAIN,
            1.5 * thickness,
            [0, 200, 0],
            thickness=int(thickness),
            lineType=cv.LINE_AA,
        )

    @staticmethod
    def getFieldWallLines() -> list[list[Translation3d]]:
        # /**
        # * The translations used to draw the field side walls and driver station walls.
        # * It is a vector of vectors because the translations are not all connected.
        # */

        SIDE_HT = units.inches_to_meters(19.5)
        DRIVE_HT = units.inches_to_meters(35.0)
        TOP_HT = units.inches_to_meters(78.0)

        translations: list[list[Translation3d]] = []

        # field floor
        translations.append(
            [
                Translation3d(0.0, 0.0, 0.0),
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, 0.0),
                Translation3d(VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, 0.0),
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, 0.0),
                Translation3d(0.0, 0.0, 0.0),
            ]
        )

        # right side wall
        translations.append(
            [
                Translation3d(0.0, 0.0, 0.0),
                Translation3d(0.0, 0.0, SIDE_HT),
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, SIDE_HT),
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, 0.0),
            ]
        )

        # red driverstation
        translations.append(
            [
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, SIDE_HT),
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, TOP_HT),
                Translation3d(
                    VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, TOP_HT
                ),
                Translation3d(
                    VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, SIDE_HT
                ),
            ]
        )
        translations.append(
            [
                Translation3d(VideoSimUtil.FIELD_LENGTH, 0.0, DRIVE_HT),
                Translation3d(
                    VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, DRIVE_HT
                ),
            ]
        )

        # left side wall
        translations.append(
            [
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, 0.0),
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, SIDE_HT),
                Translation3d(
                    VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, SIDE_HT
                ),
                Translation3d(VideoSimUtil.FIELD_LENGTH, VideoSimUtil.FIELD_WIDTH, 0.0),
            ]
        )

        # blue driverstation
        translations.append(
            [
                Translation3d(0.0, 0.0, SIDE_HT),
                Translation3d(0.0, 0.0, TOP_HT),
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, TOP_HT),
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, SIDE_HT),
            ]
        )
        translations.append(
            [
                Translation3d(0.0, 0.0, DRIVE_HT),
                Translation3d(0.0, VideoSimUtil.FIELD_WIDTH, DRIVE_HT),
            ]
        )

        return translations

    @staticmethod
    def getFieldFloorLines(sub_divisions: int) -> list[list[Translation3d]]:
        # /**
        # * The translations used to draw the field floor subdivisions (not the floor
        # * outline). It is a vector of vectors because the translations are not all
        # * connected.
        # *
        # * @param subdivisions How many "subdivisions" along the width/length of the
        # * floor. E.g. 3 subdivisions would mean 2 lines along the length and 2 lines
        # * along the width creating a 3x3 "grid".
        # */
        translations: list[list[Translation3d]] = []
        SUB_LENGTH = VideoSimUtil.FIELD_LENGTH / sub_divisions
        SUB_WIDTH = VideoSimUtil.FIELD_WIDTH / sub_divisions
        for i in range(sub_divisions):
            translations.append(
                [
                    Translation3d(0.0, SUB_WIDTH * (i + 1), 0.0),
                    Translation3d(VideoSimUtil.FIELD_LENGTH, SUB_WIDTH * (i + 1), 0.0),
                ]
            )
            translations.append(
                [
                    Translation3d(SUB_LENGTH * (i + 1), 0.0, 0.0),
                    Translation3d(SUB_LENGTH * (i + 1), VideoSimUtil.FIELD_WIDTH, 0.0),
                ]
            )

        return translations

    @staticmethod
    def polyFrom3dLines(
        cam_rt: RotTrlTransform3d,
        prop: SimCameraProperties,
        trls: list[Translation3d],
        resolution: float,
        is_closed: bool,
        destination: cv.typing.MatLike,
    ) -> list[np.ndarray]:
        # /**
        # * Convert 3D lines represented by the given series of translations into a
        # * polygon(s) in the camera's image.
        # *
        # * @param camRt The change in basis from world coordinates to camera
        # * coordinates. See RotTrlTransform3d#makeRelativeTo(Pose3d).
        # * @param prop The simulated camera's properties.
        # * @param trls A sequential series of translations defining the polygon to be
        # * drawn.
        # * @param resolution Resolution as a fraction(0 - 1) of the video frame's
        # * diagonal length in pixels. Line segments will be subdivided if they exceed
        # * this resolution.
        # * @param isClosed If the final translation should also draw a line to the first
        # * translation.
        # * @param destination The destination image that is being drawn to.
        # * @return A list of polygons(which are an array of points)
        # */
        resolution = math.hypot(destination.shape[0], destination.shape[1]) * resolution
        pts = list(trls)
        if is_closed:
            pts.append(pts[0])

        poly_pts_list: list[np.ndarray] = []
        for i in range(len(pts) - 1):
            pta = pts[i]
            ptb = pts[i + 1]

            inter1, inter2 = prop.getVisibleLine(cam_rt, pta, ptb)
            if inter1 is None or inter2 is None:
                continue

            base_delta = ptb - pta
            old_pta = pta
            if inter1 > 0:
                pta = old_pta + base_delta * inter1
            if inter2 < 1:
                ptb = old_pta + base_delta * inter2
            base_delta = ptb - pta

            poly = OpenCVHelp.projectPoints(
                prop.getIntrinsics(), prop.getDistCoeffs(), cam_rt, [pta, ptb]
            )
            pxa = poly[0, 0]
            pxb = poly[1, 0]

            px_dist = math.hypot(pxb[0] - pxa[0], pxb[1] - pxa[1])
            subdivisions = int(px_dist / resolution)
            sub_delta = base_delta / (subdivisions + 1)
            sub_pts = [pta + sub_delta * (j + 1) for j in range(subdivisions)]

            if len(sub_pts) > 0:
                to_add = OpenCVHelp.projectPoints(
                    prop.getIntrinsics(), prop.getDistCoeffs(), cam_rt, sub_pts
                )
                poly = np.concatenate([poly[:1], to_add, poly[1:]])

            poly_pts_list.append(poly)

        return poly_pts_list

    @staticmethod
    def drawFieldWireFrame(
        cam_rt: RotTrlTransform3d,
        prop: SimCameraProperties,
        resolution: float,
        wall_thickness: float,
        wall_color: cv.typing.Scalar,
        floor_subdivisions: int,
        floor_thickness: float,
        floor_color: cv.typing.Scalar,
        destination: cv.typing.MatLike,
    ) -> None:
        # /**
        # * Draw a wireframe of the field to the given image.
        # *
        # * @param camRt The change in basis from world coordinates to camera
        # * coordinates. See RotTrlTransform3d#makeRelativeTo(wpi::math::Pose3d).
        # * @param prop The simulated camera's properties.
        # * @param resolution Resolution as a fraction(0 - 1) of the video frame's
        # * diagonal length in pixels. Line segments will be subdivided if they exceed
        # * this resolution.
        # * @param wallThickness Thickness of the lines used for drawing the field walls
        # * in pixels. This is scaled by #getScaledThickness(double, cv::Mat).
        # * @param wallColor Color of the lines used for drawing the field walls.
        # * @param floorSubdivisions A NxN "grid" is created from the floor where this
        # * parameter is N, which defines the floor lines.
        # * @param floorThickness Thickness of the lines used for drawing the field floor
        # * grid in pixels. This is scaled by #getScaledThickness(double, cv::Mat).
        # * @param floorColor Color of the lines used for drawing the field floor grid.
        # * @param destination The destination image to draw to.
        # */

        for trls in VideoSimUtil.getFieldFloorLines(floor_subdivisions):
            polys = VideoSimUtil.polyFrom3dLines(
                cam_rt, prop, trls, resolution, False, destination
            )
            for poly in polys:
                VideoSimUtil.drawPoly(
                    poly,
                    round(
                        VideoSimUtil.getScaledThickness(floor_thickness, destination)
                    ),
                    floor_color,
                    False,
                    destination,
                )

        for trls in VideoSimUtil.getFieldWallLines():
            polys = VideoSimUtil.polyFrom3dLines(
                cam_rt, prop, trls, resolution, False, destination
            )
            for poly in polys:
                VideoSimUtil.drawPoly(
                    poly,
                    round(VideoSimUtil.getScaledThickness(wall_thickness, destination)),
                    wall_color,
                    False,
                    destination,
                )


VideoSimUtil.TAG_36h11_IMAGES = VideoSimUtil.loadAprilTagImages()
VideoSimUtil.TAG_36h11_MARK_PTS = VideoSimUtil.get36h11MarkerPts()
