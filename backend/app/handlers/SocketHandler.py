import asyncio

import tornado.websocket
import json
from ..classes.Exceptions import NoCameraConnectedException
from ..classes.SettingsManager import SettingsManager


web_socket_clients = []


def send_all_async(message):
    for ws in web_socket_clients:
            try:
                ws.write_message(json.dumps(message))
            except AssertionError as a:
                pass


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    actions = {}

    set_this_camera_settings = ["exposure", "brightness"]

    def __init__(self, application, request, **kwargs):
        super().__init__(application, request, **kwargs)
        self.settings_manager = SettingsManager()
        self.init_actions()

    def init_actions(self):
        self.actions["change_pipeline_values"] = self.change_pipeline_values
        self.actions["change_general_settings_values"] = self.settings_manager.change_general_settings_values
        self.actions["curr_camera"] = self.change_curr_camera
        self.actions["curr_pipeline"] = self.change_curr_pipeline
        self.actions['resolution'] = self.set_resolution
        self.actions['FOV'] = self.set_fov

    def open(self):
        self.send_full_settings()
        if self not in web_socket_clients:
            web_socket_clients.append(self)

        print("WebSocket opened")

    def on_message(self, message):
        try:
            message_dic = json.loads(message)

            for key in message_dic:
                self.actions.get(key, self.actions["change_pipeline_values"])(message_dic)
            print(message)
        except:
            print("crash " + message)

    def on_close(self):
        self.settings_manager.save_settings()
        if self in web_socket_clients:
            web_socket_clients.remove(self)
        print("WebSocket closed")

    def check_origin(self, origin):
        return True

    def set_resolution(self, message):
        self.settings_manager.get_curr_cam()['resolution'] = message['resolution']
        SettingsManager().set_camera_settings(camera_name=SettingsManager().general_settings['curr_camera'],
                                              dic=message)
        self.settings_manager.save_settings()

    def set_fov(self, message):
        self.settings_manager.get_curr_cam()['FOV'] = message['FOV']
        self.settings_manager.save_settings()

    def send_curr_pipeline(self):
        try:
            self.write_message(self.settings_manager.get_curr_pipeline())
        except NoCameraConnectedException:
            # TODO: return something if no camera connected
            self.write_message("No camera connected")

    def send_curr_cam(self):
        try:
            self.write_message(self.settings_manager.get_curr_cam())
        except NoCameraConnectedException:
            # TODO: return something if no camera connected
            self.write_message("No camera connected")

    def send_curr_port(self):
        self.write_message({
            'port': self.settings_manager.cams_port[self.settings_manager.general_settings["curr_camera"]]
        })

    def send_full_settings(self):
        full_settings = self.settings_manager.general_settings.copy()
        full_settings["cameraList"] = list(self.settings_manager.cams.copy().keys())
        try:
            full_settings.update(self.settings_manager.get_curr_pipeline())
            full_settings["pipelineList"] = list(self.settings_manager.cams[self.settings_manager.general_settings["curr_camera"]]["pipelines"].keys())
            full_settings["resolutionList"] = self.settings_manager.get_resolution_list()
            full_settings['resolution'] = self.settings_manager.get_curr_cam()['resolution']
            full_settings['FOV'] = self.settings_manager.get_curr_cam()['FOV']
            full_settings['port'] = self.settings_manager.cams_port[self.settings_manager.general_settings["curr_camera"]]
        except NoCameraConnectedException:
            # TODO: return something if no camera connected
            full_settings["data"] = None

        self.write_message(full_settings)

    def change_curr_camera(self, dic):
        self.settings_manager.set_curr_camera(cam_name=dic["curr_camera"])
        self.send_curr_port()
        self.send_curr_cam()

    def change_curr_pipeline(self, dic):
        self.settings_manager.set_curr_pipeline(pipe_name=dic["curr_pipeline"])
        self.settings_manager.cams_curr_pipeline[self.settings_manager.general_settings['curr_camera']] = dic["curr_pipeline"]
        self.send_curr_pipeline()

    def change_pipeline_values(self, dic):
        self.settings_manager.change_pipeline_values(dic)
        for key in self.set_this_camera_settings:
            if key in dic:
                self.settings_manager.set_camera_settings(self.settings_manager.general_settings["curr_camera"],
                                                          dic)
