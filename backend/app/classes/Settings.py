import os
import json
from Singleton import Singleton


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
        if os.path.exists(self.cams_path):
            for curr_dir in os.listdir(self.cams_path):
                pipelines = {}
                for file in os.listdir(os.path.join(self.cams_path, curr_dir)):
                    with open(os.path.join(self.cams_path, curr_dir, file)) as pipeline:
                        pipelines[os.path.splitext(file)[0]] = json.load(pipeline)

                self.cams[curr_dir] = pipelines
        else:
            self.create_new_cam("cam1")
            self.settings["curr_camera"] = "cam1"
            self.settings["curr_pipeline"] = "pipeline1"

    # Access methods

    def get_curr_pipeline(self):
        return self.cams[self.settings["curr_camera"]][self.settings["curr_pipeline"]]

    def get_curr_cam(self):
        return self.cams[self.settings["curr_camera"]]

    def set_curr_camera(self, cam_name):
        if cam_name in self.cams:
            self.settings["curr_camera"] = cam_name

    def set_curr_pipeline(self, pipe_name):
        if pipe_name in self.cams:
            self.settings["curr_pipeline"] = pipe_name

    # Creators

    def create_new_pipeline(self, pipe_name, cam=None):
        if not cam:
            cam = self.settings["curr_camera"]

        with open(os.path.join(self.settings_path, 'default_pipeline.json')) as default_pipeline:
            self.cams[cam][pipe_name] = json.load(default_pipeline)

    def create_new_cam(self, cam):
        self.cams[cam] = {}
        self.create_new_pipeline('pipeline1', cam)

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