from .ChameleonPipeLine import ChameleonPipeline


class ChameleonCamera:

    def __init__(self, dic):
        self.pipelines = {}

        for pipeline_id in dic["pipelines"]:
            self.pipelines[pipeline_id] = ChameleonPipeline(dic[pipeline_id])

    def change_value(self, pipline_id, key, value):
        self.pipelines[pipline_id][key] = value