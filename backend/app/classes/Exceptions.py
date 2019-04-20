
class PipelineAlreadyExistsException(Excetion):

    def __init__(self, pipe_name):
        super(f"Pipeline {pipe_name} already exists")
