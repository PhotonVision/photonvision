import cscore
import cv2
from cscore._cscore import VideoMode

class CamerasHandler:

    @staticmethod
    # def get_cameras_info():
    #
    #     if not getattr(CamerasHandler, "cams_info", False):
    #
    #         arr = []
    #
    #         usb_devices = cscore.UsbCamera.enumerateUsbCameras()
    #
    #         for index in range(len(usb_devices)):
    #             cap = cv2.VideoCapture(index)
    #             if cap.isOpened():
    #                 arr.append(index)
    #             cap.release()
    #             index += 1
    #
    #         setattr(CamerasHandler, "cams_info", [usb_devices[i] for i in arr])
    #
    #     return getattr(CamerasHandler, "cams_info")

    # @staticmethod
    # def get_or_start_cameras(usb_devices):
    #
    #     if not getattr(CamerasHandler, "cams", False):
    #         cameras = {}
    #         for device in usb_devices:
    #             device_name = device.name
    #
    #             if device.name in cameras:
    #                 suffix = 0
    #                 device_name = device.name + str(suffix)
    #
    #                 while device_name in cameras:
    #                     suffix += 1
    #                     device_name = "pipeline" + str(suffix)
    #
    #             camera = cscore.UsbCamera(name=device_name, dev=device.dev)
    #             camera.setPixelFormat(pixelFormat=
    #                                   getattr(VideoMode.PixelFormat, SettingsManager()
    #                                           .get_curr_cam()["video_mode"]["pixel_format"]))
    #             camera.setFPS(SettingsManager().get_curr_cam()["video_mode"]["fps"])
    #             camera.setResolution(width=SettingsManager().get_curr_cam()["video_mode"]["width"],
    #                                  height=SettingsManager().get_curr_cam()["video_mode"]["height"])
    #
    #             cameras[device_name] = camera
    #
    #         setattr(CamerasHandler, "cams", cameras)
    #
    #     return getattr(CamerasHandler, "cams")

    # @staticmethod
    # def init_camera():
    #     return CamerasHandler.get_or_start_cameras(CamerasHandler.get_cameras_info())
    #
    # @staticmethod
    # def get_usb_camera_by_name(cam_name):
    #     return CamerasHandler.get_or_start_cameras(CamerasHandler.get_cameras_info())[cam_name]

    @staticmethod
    def set_camera_settings(usb_camera: cscore.UsbCamera, dic):

        if "brightness" in dic:
            usb_camera.setBrightness(dic["brightness"])

        if "exposure" in dic:
            usb_camera.setExposureManual(dic["exposure"])

        if "video_mode" in dic:
            usb_camera.setVideoMode(dic["video_mode"])
