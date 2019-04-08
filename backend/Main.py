import tornado.ioloop

from app.ChameleonVisionApp import ChameleonApplication
from app.classes.Settings import SettingsManager
from tornado.options import options

if __name__ == "__main__":
    mng = SettingsManager.get_instance()
    tornado.options.parse_command_line()
    app = ChameleonApplication()
    print(f"Serving on port {options.port}")
    app.listen(options.port)
    tornado.ioloop.IOLoop.current().start()
