import tornado.websocket
import json

from ..classes.Exceptions import NoCameraConnectedException
from .CamerasHandler import CamerasHandler
from ..classes.SettingsManager import SettingsManager


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    actions = {}

    def __init__(self, application, request, **kwargs):
        super().__init__(application, request, **kwargs)
        self.settings_manager = SettingsManager()
        self.init_actions()

    def init_actions(self):
        self.actions["change_pipeline_values"] = self.change_pipeline_values
        self.actions["change_general_settings_values"] = self.settings_manager.change_general_settings_values
        self.actions["change_cam"] = self.change_curr_camera
        self.actions["change_pipeline"] = self.change_curr_pipeline

    def open(self):

        self.send_full_settings()

        print("WebSocket opened")

    def on_message(self, message):

        message_dic = json.loads(message)

        for key in message_dic:
            self.actions.get(key, self.actions["change_pipeline_values"])(message_dic[key])

        print(message)

    def on_close(self):
        self.settings_manager.save_settings()

        print("WebSocket closed")

    def check_origin(self, origin):
        return True

    def send_curr_cam(self):
        try:
            self.write_message(self.settings_manager.get_curr_pipeline())
        except NoCameraConnectedException:
            # TODO: return something if no camera connected
            self.write_message(None)

    def send_full_settings(self):
        full_settings = self.settings_manager.general_settings.copy()

        try:
            full_settings["data"] = self.settings_manager.get_curr_pipeline()
        except NoCameraConnectedException:
            # TODO: return something if no camera connected
            full_settings["data"] = None

        self.write_message(full_settings)

    def change_curr_camera(self, dic):
        self.settings_manager.set_curr_camera(cam_name=dic["cam"])

    def change_curr_pipeline(self, dic):
        self.settings_manager.set_curr_pipeline(pipe_name=dic["pipeline"])

    def change_pipeline_values(self, dic):
        self.settings_manager.change_pipeline_values(dic)

        CamerasHandler.change_camera_values(CamerasHandler.get_usb_camera_by_name(self.settings_manager.general_settings["curr_camera"]), dic)




