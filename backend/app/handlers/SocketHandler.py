import tornado.websocket
import json
from classes.ChameleonCamera import ChameleonCamera


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    def check_origin(self, origin):
        return True

    def open(self):
        self.write_message(json.dumps(
            {
                'cam1': {
                    'pipelines': [
                        {"id": 1, "Exposure": 45}
                    ]
                }
            }
        ))
        print("WebSocket opened")

    def on_message(self, message):
        # print(json.loads(message))

        cams = []

        dic = {
                'cam1': {
                    'pipelines': [
                        {"id": 1, "exposure": 45, "brightness": 123123},
                        {"id": 2, "exposure": 12, "brightness": 123},
                        {"id": 3, "exposure": 23, "brightness": 12}
                    ]
                },
                'cam2': {
                    'pipelines': [
                        {"id": 1, "exposure": 15, "brightness": 1233},
                        {"id": 2, "exposure": 68, "brightness": 453}
                    ]
                }
            }

        for cam in dic:
            cams.append(ChameleonCamera(dic[cam]))

        print(message)
        self.write_message(message)

    def on_close(self):
        print("WebSocket closed")
