from .ChameleonPipeLine import ChameleonPipeline


class ChameleonCamera:

    def __init__(self, dic):
        self.pipelines = []

        for pipeline in dic["pipelines"]:
            self.pipelines.append(ChameleonPipeline(pipeline))

