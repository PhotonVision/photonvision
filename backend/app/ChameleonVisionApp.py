import tornado.web
import tornado.websocket
import os
from .handlers.MainHandler import MainHandler
from .handlers.SocketHandler import ChameleonWebSocket
from tornado.options import define

define("port", default=8888, help="run on the given port", type=int)


class ChameleonApplication(tornado.web.Application):
    def __init__(self):
        handlers = [
            (r"/", MainHandler),
            (r"/websocket", ChameleonWebSocket),
            (r"/(.*)", tornado.web.StaticFileHandler,
             {"path": r"{0}".format(os.path.join(os.path.dirname(__file__), "site"))}),
        ]

        settings = dict({
            "template_path": os.path.join(os.path.dirname(__file__), "site"),
            "static_path": os.path.join(os.path.dirname(__file__), "site"),
            "debug": True
            }
        )

        super(ChameleonApplication, self).__init__(handlers, **settings)
