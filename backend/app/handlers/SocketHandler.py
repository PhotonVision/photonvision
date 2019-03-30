import tornado.websocket
import json
import os
from app.classes.ChameleonCamera import ChameleonCamera


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    actions = {}
    settings = {}
    cams = {}

    def __init__(self, application, request, **kwargs):
        super().__init__(application, request, **kwargs)
        self.settings_path = os.path.join(os.getcwd(), "settings")
        self.cams_path = os.path.join(self.settings_path, "cams")
        self.init_settings()
        self.init_actions()

    def init_settings(self):
        with open(os.path.join(self.settings_path, 'settings.json')) as setting_file:
            self.settings = json.load(setting_file)

        self.init_cameras_settings()

    def init_cameras_settings(self):

        for curr_dir in os.listdir(self.cams_path):
            pipelines = {}
            for file in os.listdir(os.path.join(self.cams_path, curr_dir)):
                with open(os.path.join(self.cams_path, curr_dir, file)) as pipeline:
                    pipelines[os.path.splitext(file)[0]] = json.load(pipeline)

            self.cams[curr_dir] = pipelines

    def init_actions(self):
        self.actions["change_pipeline_value"] = self.change_pipeline_value

    def check_origin(self, origin):
        return True

    def open(self):
        self.write_message(self.cams[self.settings["curr_camera"]][self.settings["curr_pipeline"]])

        print("WebSocket opened")

    def on_message(self, message):

        for key in message:
            if key in self.actions:
                self.actions[key](message[key])

        print(message)
        self.write_message(message)

    def on_close(self):
        print("WebSocket closed")

    def change_pipeline_value(self, dic):
        for key in dic:
            self.cams[self.settings["curr_camera"]][self.settings["curr_pipeline"]][key] = dic[key]
