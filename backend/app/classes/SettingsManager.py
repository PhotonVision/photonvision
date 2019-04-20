import os
import jsonpass
from .Singleton import Singleton
from handlers.CamerasHandler import CamerasHandler
from .Exceptions import PipelineAlreadyExistsException


class SettingsManager(metaclass=Singleton):
    cams = {}
    general_settings = {}

    default_pipeline = {
        "exposure": 50,
        "brightness": 50,
        "hue": [0, 100],
        "saturation": [0, 100],
        "value": [0, 100]
    }
    default_general_settings = {
        "team_number": 1577,
        "curr_camera": "",
        "curr_pipeline": ""
    }

    def __init__(self):
        self.settings_path = os.path.join(os.getcwd(), "settings")
        self.cams_path = os.path.join(self.settings_path, "cams")
        self._init_general_settings()
        self._init_cameras()

        if self.general_settings["curr_camera"] not in self.cams and len(list(self.cams.keys())) > 0:
            self.general_settings["curr_camera"] = list(self.cams.keys())[0]

    def _init_general_settings(self):
        try:
            with open(os.path.join(self.settings_path, 'settings.json')) as setting_file:
                self.general_settings = json.load(setting_file)
        except FileNotFoundError:
            self.general_settings = self.default_general_settings.copy()

    def _init_cameras(self):
        cameras = CamerasHandler.get_cameras_info()

        for cam in cameras:
            if os.path.exists(os.path.join(self.cams_path, cam.name)):
                self.cams[cam.name] = {}
                self.cams[cam.name]["pipelines"] = {}

                for file in os.listdir(os.path.join(self.cams_path, cam.name)):
                    with open(os.path.join(self.cams_path, cam.name, file)) as pipeline:
                        self.cams[cam.name]["pipelines"][os.path.splitext(file)[0]] = json.load(pipeline)

                if len(self.cams[cam.name]["pipelines"]) == 0:
                    self.create_new_pipeline(cam_name=cam.name)
            else:
                self.create_new_cam(cam.name)

            self.cams[cam.name]["path"] = cam.otherPaths[0] if len(cam.otherPaths) == 1 else cam.otherPaths[1]

    # Access methods

    def get_curr_pipeline(self):
        return self.cams[self.general_settings["curr_camera"]]["pipelines"][self.general_settings["curr_pipeline"]]

    def get_curr_cam(self):
        return self.cams[self.general_settings["curr_camera"]]

    def set_curr_camera(self, cam_name):
        if cam_name in self.cams:
            self.general_settings["curr_camera"] = cam_name

    def set_curr_pipeline(self, pipe_name):
        if pipe_name in self.get_curr_cam()["pipelines"]:
            self.general_settings["curr_pipeline"] = pipe_name

    def change_pipeline_values(self, dic, cam_name=None, pipe_name=None):

        if not cam_name:
            cam_name = self.general_settings["curr_camera"]

        if not pipe_name:
            pipe_name = self.general_settings["curr_pipeline"]

        for key in dic:
            if self.default_pipeline[key]:
                self.cams[cam_name]["pipelines"][pipe_name][key] = dic[key]

    def change_general_settings_values(self, dic):
        for key in dic:
            if self.default_general_settings[key]:
                self.general_settings[key] = dic[key]

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
        self.create_new_pipeline(cam_name=cam_name)

    # Savers

    def save_settings(self):
        self._save_cameras()
        self._save_general_settings()

    def _save_cameras(self):
        for cam in self.cams:
            for pipeline in self.cams[cam]:
                path = os.path.join(self.cams_path, cam)

                if not os.path.exists(path):
                    os.makedirs(path)

                with open(os.path.join(path, pipeline + '.json'), 'w+') as pipeline_file:
                    json.dump(self.cams[cam][pipeline], pipeline_file)

    def _save_general_settings(self):
        with open(os.path.join(self.settings_path, 'settings.json'), 'w+') as setting_file:
            json.dump(self.general_settings, setting_file)
