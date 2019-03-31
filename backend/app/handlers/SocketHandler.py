import tornado.websocket
import json
import os


class ChameleonWebSocket(tornado.websocket.WebSocketHandler):

    actions = {}
    settings = {}
    cams = {}

    def __init__(self, application, request, **kwargs):
        super().__init__(application, request, **kwargs)
        self.settings_path = os.path.join(os.getcwd(), "settings")
        self.cams_path = os.path.join(self.settings_path, "cams")
        self.init_settings()
        self.init_actions()

    # Init methods

    def init_settings(self):
        with open(os.path.join(self.settings_path, 'settings.json')) as setting_file:
            self.settings = json.load(setting_file)

        self.init_cameras_settings()

    def init_cameras_settings(self):
        if os.path.exists(self.cams_path):
            for curr_dir in os.listdir(self.cams_path):
                pipelines = {}
                for file in os.listdir(os.path.join(self.cams_path, curr_dir)):
                    with open(os.path.join(self.cams_path, curr_dir, file)) as pipeline:
                        pipelines[os.path.splitext(file)[0]] = json.load(pipeline)

                self.cams[curr_dir] = pipelines
        else:
            self.create_new_cam("cam1")
            self.settings["curr_camera"] = "cam1"
            self.settings["curr_pipeline"] = "pipeline1"

    def init_actions(self):
        self.actions["camera"] = self.change_curr_camera
        self.actions["pipeline"] = self.change_curr_pipeline
        self.actions["change_pipeline_values"] = self.change_pipeline_values

    def check_origin(self, origin):
        return True

    # Socket methods

    def open(self):

        # TODO: send full settings(team number....)
        self.send_curr_cam()

        print("WebSocket opened")

    def on_message(self, message):

        message_dic = json.loads(message)

        for key in message_dic:
            self.actions.get(key, self.actions["change_pipeline_values"])(message_dic)
        print(message)

    def on_close(self):
        self.save_settings()
        self.save_cameras()
        print("WebSocket closed")

    def send_curr_cam(self):
        self.write_message(self.get_curr_pipeline())

    # Access methods

    def get_curr_pipeline(self):
        return self.cams[self.settings["curr_camera"]][self.settings["curr_pipeline"]]

    def get_curr_cam(self):
        return self.cams[self.settings["curr_camera"]]

    def set_curr_camera(self, cam_name):
        if cam_name in self.cams:
            self.settings["curr_camera"] = cam_name

    def set_curr_pipeline(self, pipe_name):
        if pipe_name in self.cams:
            self.settings["curr_pipeline"] = pipe_name

    # Actions

    def change_pipeline_values(self, dic):
        for key in dic:
            self.get_curr_pipeline()[key] = dic[key]

    def change_curr_camera(self, dic):
        cam_name = 'cam' + dic["camera"]

        if cam_name not in self.cams:
            self.create_new_cam(cam_name)

        self.set_curr_camera(cam_name)
        self.send_curr_cam()

    def change_curr_pipeline(self, dic):
        pipe_name = 'pipeline' + dic["pipeline"]

        if pipe_name not in self.get_curr_cam():
            self.create_new_pipeline(pipe_name)

        self.set_curr_pipeline(pipe_name)

    # Creators

    def create_new_pipeline(self, pipe_name, cam=None):
        if not cam:
            cam = self.settings["curr_camera"]

        with open(os.path.join(self.settings_path, 'default_pipeline.json')) as default_pipeline:
            self.cams[cam][pipe_name] = json.load(default_pipeline)

    def create_new_cam(self, cam):
        self.cams[cam] = {}
        self.create_new_pipeline('pipeline1', cam)

    # Save data functions

    def save_cameras(self):
        for cam in self.cams:
            for pipeline in self.cams[cam]:
                path = os.path.join(self.cams_path, cam)

                if not os.path.exists(path):
                    os.makedirs(path)

                with open(os.path.join(path, pipeline + '.json'), 'w+') as pipeline_file:
                    json.dump(self.cams[cam][pipeline], pipeline_file)

    def save_settings(self):
        with open(os.path.join(self.settings_path, 'settings.json'), 'w') as setting_file:
            json.dump(self.settings, setting_file)
