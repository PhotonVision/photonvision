import tornado.websocket

class ChameleonWebSocket(tornado.websocket.WebSocketHandler):
    def open(self):
        print("WebSocke opend")

    def on_message(self, message):
        print(message)
        self.write_message("why the fuck did you send a messege")
    def on_close(self):
        print("websocket closed")

