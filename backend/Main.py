import tornado.ioloop
import logging
from app.ChameleonVisionApp import ChameleonApplication
from app.classes.SettingsManager import SettingsManager
from tornado.options import options
from app.handlers.VisionHandler import VisionHandler
import threading
import asyncio


def run_server():
    asyncio.set_event_loop(asyncio.new_event_loop())
    tornado.options.parse_command_line()
    app = ChameleonApplication()
    print(f"Serving on port {options.port}")
    app.listen(options.port)
    tornado.ioloop.IOLoop.current().start()


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    SettingsManager()

    VisionHandler().run()
    server_thread = threading.Thread(target=run_server)
    server_thread.start()

    while True:
        pass
