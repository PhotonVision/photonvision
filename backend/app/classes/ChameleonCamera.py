from .ChameleonPipeLine import ChameleonPipeline


class ChameleonCamera:

    def __init__(self, dic):
        self.pipelines = {}

        for pipeline_id in dic:
            self.pipelines[pipeline_id] = ChameleonPipeline(dic[pipeline_id])