import tornado.websocket
import json

class ChameleonWebSocket(tornado.websocket.WebSocketHandler):
    def check_origin(self, origin):
        return True
    def open(self):
        print("WebSocket opened")

    def on_message(self, message):
        print(json.loads(message))
        # print(message)
        self.write_message(message)

    def on_close(self):
        print("WebSocket closed")
