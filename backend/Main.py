import tornado.ioloop
import multiprocessing

from app.ChameleonVisionApp import ChameleonApplication
from app.classes.SettingsManager import SettingsManager
from tornado.options import options
from app.handlers.VisionHandler import VisionHandler

def run_server():
    tornado.options.parse_command_line()
    app = ChameleonApplication()
    print(f"Serving on port {options.port}")
    app.listen(options.port)
    tornado.ioloop.IOLoop.current().start()

if __name__ == "__main__":
    SettingsManager()
    
    procs = VisionHandler().run()
    # proc = multiprocessing.Process(target=run_server)
    # procs.append(proc)
    # proc.start()


    for i in procs:
        i.start()
    # SettingsManager().save_settings()



#TODO: create process for each camera
# create proccess loop and camera publisher
# bridge network tables for each camera