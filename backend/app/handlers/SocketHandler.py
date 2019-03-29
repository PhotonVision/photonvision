import tornado.websocket
import json
from app.classes.ChameleonCamera import ChameleonCamera


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    def check_origin(self, origin):
        return True

    def open(self):

        # TODO: read setting from default file store them
        setting = json.load("file.json")
        self.cam_name = setting["curr_cam"]
        self.pipe_line = setting["curr_pipeline"]

        # TODO: get current camera pipeline file
        curr_setting = json.load(self.cam_name + "/" + self.pipe_line)

        #  TODO: wrtie current pipeline setting to client
        self.write_message(curr_setting)

        print("WebSocket opened")

    def on_message(self, message):
        cams = []

        dic = {
                'cam1': {
                    'pipelines': {
                        "1": {"exposure": 45, "brightness": 123123},
                        "2": {"exposure": 12, "brightness": 123},
                        "3": {"exposure": 23, "brightness": 12}
                    }
                },
                'cam2': {
                    'pipelines': {
                        "1": {"exposure": 15, "brightness": 1233},
                        "2": {"exposure": 68, "brightness": 453}
                    }
                }
            }

        for cam in dic:
            cams.append(ChameleonCamera(dic[cam]))

        print(message)
        self.write_message(message)

    def on_close(self):
        print("WebSocket closed")
