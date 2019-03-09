import tornado.web
import tornado.websocket
import os

from .handlers.MainHandler import MainHandler
from .handlers.SocketHandler import ChameleonWebSocket
from tornado.options import define

define("port", default=8888, help="run on the given port", type=int)


class ChameleonApplication(tornado.web.Application):
    def __init__(self):
        handlers = [(r"/", MainHandler),
                    (r"/websocket", ChameleonWebSocket),
                    (r"/css/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "../../Site/css")}),
                    (r"/js/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "../../Site/js")}),
                    (r"/templates/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "../../Site/templates")}),
                    (r"/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "../../Site")}),
                    (r"/assets/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "../../Site/assets")})]

        settings = dict(
            template_path=os.path.join(os.path.dirname(__file__), "../../Site")
        )

        super(ChameleonApplication, self).__init__(handlers, **settings)
