
class PipelineAlreadyExistsException(Exception):

    def __init__(self, pipe_name):
        super(f"Pipeline {pipe_name} already exists")
