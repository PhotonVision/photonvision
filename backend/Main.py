import tornado.ioloop

from app.ChameleonVisionApp import ChameleonApplication
from tornado.options import options

if __name__ == "__main__":
    tornado.options.parse_command_line()
    app = ChameleonApplication()
    print(f"Serving on port {options.port}")
    app.listen(options.port)
    tornado.ioloop.IOLoop.current().start()
