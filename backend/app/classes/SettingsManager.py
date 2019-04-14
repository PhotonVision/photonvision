import os
import json
import cscore
import networktables
from .Singleton import Singleton
from .CamerasHandler import CamerasHandler


class SettingsManager(metaclass=Singleton):
    cams = {}

    def __init__(self):
        self.settings_path = os.path.join(os.getcwd(), "settings")
        self.cams_path = os.path.join(self.settings_path, "cams")
        self._init_general_settings()
        self._init_cameras()

    def _init_general_settings(self):
        def init_default_settings():
            self.settings = {
                "team_number": 1577,
                "curr_camera": "cam1",
                "curr_pipeline": "pipeline1"
            }

        try:
            with open(os.path.join(self.settings_path, 'settings.json')) as setting_file:
                self.settings = json.load(setting_file)
        except FileNotFoundError:
            init_default_settings()

    def _init_cameras(self):
        cameras = CamerasHandler.get_cameras()
        
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
        return self.cams[self.settings["curr_camera"]]["pipelines"][self.settings["curr_pipeline"]]

    def get_curr_cam(self):
        return self.cams[self.settings["curr_camera"]]

    def set_curr_camera(self, cam_name):
        if cam_name in self.cams:
            self.settings["curr_camera"] = cam_name

    def set_curr_pipeline(self, pipe_name):
        if pipe_name in self.cams:
            self.settings["curr_pipeline"] = pipe_name

    # Creators

    def create_new_pipeline(self, pipe_name=None, cam_name=None):

        if not cam_name:
            cam_name = self.settings["curr_camera"]

        if not pipe_name:
            pipe_name = "pipeline" + str(len(self.cams[cam_name]))

        self.cams[cam_name]["pipelines"][pipe_name] = {
            "exposure": 50,
            "brightness": 50
        }

    def create_new_cam(self, cam_name):
        self.cams[cam_name] = {}
        self.cams[cam_name]["pipelines"] = {}
        self.create_new_pipeline(cam_name=cam_name)

    # Savers

    def save_cameras(self):
        for cam in self.cams:
            for pipeline in self.cams[cam]:
                path = os.path.join(self.cams_path, cam)

                if not os.path.exists(path):
                    os.makedirs(path)

                with open(os.path.join(path, pipeline + '.json'), 'w+') as pipeline_file:
                    json.dump(self.cams[cam][pipeline], pipeline_file)

    def save_settings(self):
        with open(os.path.join(self.settings_path, 'settings.json'), 'w+') as setting_file:
            json.dump(self.settings, setting_file)
