import os
import json
import cv2
import cscore
from cscore._cscore import VideoMode
from .Singleton import Singleton
from .Exceptions import PipelineAlreadyExistsException, NoCameraConnectedException


class SettingsManager(metaclass=Singleton):
    cams = {}
    usb_cameras = {}
    usb_cameras_info = {}
    general_settings = {}
    cams_port = {}
    cams_curr_pipeline = {}

    default_pipeline = {
        "exposure": 50,
        "brightness": 50,
        "orientation": "Normal",
        "hue": [0, 100],
        "saturation": [0, 100],
        "value": [0, 100],
        "erode": False,
        "dilate": False,
        "area": [0, 100],
        "ratio": [0, 20],
        "extent": [0, 100],
        "is_binary": 0,
        "sort_mode": "Largest",
        "target_group": 'Single',
        "target_intersection": 'Up',
        "M": 1,
        "B": 0
    }
    default_general_settings = {
        "team_number": 1577,
        "connection_type": "DHCP",
        "ip": "",
        "gateway": "",
        "hostname": "",
        "curr_camera": "",
        "curr_pipeline": ""
    }

    def __init__(self):
        self.settings_path = os.path.join(os.getcwd(), "settings")
        self.cams_path = os.path.join(self.settings_path, "cams")
        self._init_general_settings()
        self._init_cameras_info()
        self._init_usb_cameras()
        self._init_cameras()
        self._init_usb_cameras_settings()

        if self.general_settings["curr_camera"] not in self.cams:
            if len(self.cams) > 0:
                cam_name = list(self.cams.keys())[0]
                self.general_settings["curr_camera"] = cam_name
                self.general_settings["curr_pipeline"] = list(self.cams[cam_name]["pipelines"].keys())[0]
            else:
                self.general_settings["curr_camera"] = ""
                self.general_settings["curr_pipeline"] = ""

    def _init_general_settings(self):
        try:
            with open(os.path.join(self.settings_path, 'settings.json')) as setting_file:
                self.general_settings = json.load(setting_file)
        except FileNotFoundError:
            self.general_settings = self.default_general_settings.copy()

    # Initiate our camera's settings
    def _init_cameras(self):
        for cam_name in self.usb_cameras_info:
            if os.path.exists(os.path.join(self.cams_path, cam_name + '.json')):
                with open(os.path.join(self.cams_path, cam_name + '.json'), 'r') as camera:
                    self.cams[cam_name] = json.load(camera)
                if len(self.cams[cam_name]["pipelines"]) == 0:
                    self.create_new_pipeline(cam_name=cam_name)
            else:
                self.create_new_cam(cam_name)

    # Initiate true usb cameras(filters microphones and double cameras)
    def _init_cameras_info(self):
        true_cameras = []
        usb_devices = cscore.UsbCamera.enumerateUsbCameras()

        for index, device in enumerate(usb_devices):
            cap = cv2.VideoCapture(device.dev)
            if cap.isOpened():
                true_cameras.append(index)
                cap.release()

        for i in true_cameras:
            device_name = usb_devices[i].name
            suffix = 0

            while device_name in self.usb_cameras_info:
                suffix += 1
                device_name = f"{device.name}({str(suffix)})"

            self.usb_cameras_info[device_name] = usb_devices[i]

    # Initiate cscore usb devices
    def _init_usb_cameras(self):
        for device_name in self.usb_cameras_info:
            device = self.usb_cameras_info[device_name]

            camera = cscore.UsbCamera(name=device_name, dev=device.dev)

            self.usb_cameras[device_name] = camera

    def _init_usb_cameras_settings(self):
        for cam_name in self.usb_cameras:
            self.usb_cameras[cam_name].setPixelFormat(pixelFormat=getattr(VideoMode.PixelFormat, self.cams[cam_name]["video_mode"]["pixel_format"]))
            self.usb_cameras[cam_name].setFPS(self.cams[cam_name]["video_mode"]["fps"])
            self.usb_cameras[cam_name].setResolution(width=self.cams[cam_name]["video_mode"]["width"], height=self.cams[cam_name]["video_mode"]["height"])

    # Change usb camera settings
    def set_camera_settings(self, camera_name, dic):

        if "brightness" in dic:
            self.usb_cameras[camera_name].setBrightness(dic["brightness"])

        if "exposure" in dic:
            self.usb_cameras[camera_name].setExposureManual(dic["exposure"])

        if "resolution" in dic:
            video_mode: VideoMode = self.usb_cameras[camera_name].enumerateVideoModes()[int(dic["resolution"])]
            self.cams[camera_name]["video_mode"] = {
                "fps": video_mode.fps,
                "width": video_mode.width,
                "height": video_mode.height,
                "pixel_format": str(video_mode.pixelFormat).split('.')[1]
            }

            self.usb_cameras[camera_name].setVideoMode(self.usb_cameras[camera_name].enumerateVideoModes()[int(dic["resolution"])])
        if "FOV" in dic:
            self.cams[camera_name]["FOV"] = float(dic["FOV"])
    # Access methods

    def get_curr_pipeline(self):
        if self.general_settings["curr_pipeline"]:
            return self.cams[self.general_settings["curr_camera"]]["pipelines"][self.general_settings["curr_pipeline"]]

        raise NoCameraConnectedException()

    def get_resolution_list(self):
        if self.general_settings["curr_camera"]:
            str_list = []
            for val in self.usb_cameras[self.general_settings["curr_camera"]].enumerateVideoModes():
                str_list.append("{width} X {height} at {fps} fps".format(width=str(val.width),
                                                                         height=str(val.height), fps=str(val.fps)))

            return str_list

        raise NoCameraConnectedException()

    def get_curr_cam(self):
        if self.general_settings["curr_camera"]:
            return self.cams[self.general_settings["curr_camera"]]

        raise NoCameraConnectedException()

    def set_curr_camera(self, cam_name):
        if cam_name in self.cams:
            self.general_settings["curr_camera"] = cam_name
            self.general_settings["curr_pipeline"] = list(self.get_curr_cam()["pipelines"].keys())[0]

    def set_curr_pipeline(self, pipe_name):
        if pipe_name in self.get_curr_cam()["pipelines"]:
            self.general_settings["curr_pipeline"] = pipe_name

    def change_pipeline_values(self, dic, cam_name=None, pipe_name=None):

        if not cam_name:
            cam_name = self.general_settings["curr_camera"]

        if not pipe_name:
            pipe_name = self.general_settings["curr_pipeline"]

        for key in dic:
            if key in self.default_pipeline:
                self.cams[cam_name]["pipelines"][pipe_name][key] = dic[key]

    def change_general_settings_values(self, dic):
        for key in dic['change_general_settings_values']:
            if self.default_general_settings[key]:
                self.general_settings[key] = dic['change_general_settings_values'][key]
        self.settings_manager.save_settings()
        #after all values has been set change settings
        self.change_general_settings()


    # Creators

    def create_new_pipeline(self, pipe_name=None, cam_name=None):

        if not cam_name:
            cam_name = self.general_settings["curr_camera"]

        if not pipe_name:
            suffix = 0
            pipe_name = "pipeline" + str(suffix)

            while pipe_name in self.cams[cam_name]["pipelines"]:
                suffix += 1
                pipe_name = "pipeline" + str(suffix)
        elif self.cams[cam_name]["pipelines"][pipe_name]:
            raise PipelineAlreadyExistsException(pipe_name)

        self.cams[cam_name]["pipelines"][pipe_name] = self.default_pipeline.copy()

    def create_new_cam(self, cam_name):
        self.cams[cam_name] = {}
        self.cams[cam_name]["pipelines"] = {}
        for i in range(10):
            self.create_new_pipeline(cam_name=cam_name)

        self.cams[cam_name]["path"] = self.usb_cameras_info[cam_name].otherPaths[0] if len(
            self.usb_cameras_info[cam_name].otherPaths) == 1 else self.usb_cameras_info[cam_name].otherPaths[1]

        video_mode: VideoMode = self.usb_cameras[cam_name].enumerateVideoModes()[0]
        self.cams[cam_name]["video_mode"] = {
            "fps": video_mode.fps,
            "width": video_mode.width,
            "height": video_mode.height,
            "pixel_format": str(video_mode.pixelFormat).split('.')[1],
        }
        self.cams[cam_name]['resolution'] = 0
        self.cams[cam_name]["FOV"] = 60.8

    # Savers

    def save_settings(self):
        self._save_general_settings()
        self._save_cameras()

    def _save_cameras(self):

        if not os.path.exists(self.cams_path):
            os.mkdir(self.cams_path)

        for cam in self.cams:
            with open(os.path.join(self.cams_path, cam + '.json'), 'w+') as camera:
                json.dump(self.cams[cam], camera)

    def _save_general_settings(self):
        if not os.path.exists(self.settings_path):
            os.mkdir(self.settings_path)

        with open(os.path.join(self.settings_path, 'settings.json'), 'w+') as setting_file:
            json.dump(self.general_settings, setting_file)

    def change_general_settings(self):
        pass


