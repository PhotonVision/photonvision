from networktables import NetworkTables
import tornado.ioloop
import logging
from app.ChameleonVisionApp import ChameleonApplication
from app.classes.SettingsManager import SettingsManager
from tornado.options import options
import threading
import asyncio
from app.handlers.CameraHander import CameraHandler


def run_server():
    asyncio.set_event_loop(asyncio.new_event_loop())
    tornado.options.parse_command_line()
    app = ChameleonApplication()
    print(f"Serving on port {options.port}")
    app.listen(options.port)
    tornado.ioloop.IOLoop.current().start()


def run():
    NetworkTables.startClientTeam(team=settings_manager.general_settings.get("team_number", 1577))
    port = 5550
    for cam_name in settings_manager.usb_cameras:
        CameraHandler(cam_name, port).run()
        port += 1



if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    settings_manager = SettingsManager()
    run()
    server_thread = threading.Thread(target=run_server)
    server_thread.start()

