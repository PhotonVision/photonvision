import tornado.websocket


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):
    def open(self):
        print("WebSocket opened")

    def on_message(self, message):
        print(message)
        self.write_message("why the fuck did you send a message")

    def on_close(self):
        print("WebSocket closed")

