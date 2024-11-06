import ntcore as nt
from ..generated.PhotonPipelineResultSerde import PhotonPipelineResultSerde
from wpimath.geometry import Transform3d

PhotonPipelineResult_TYPE_STRING = (
    "photonstruct:PhotonPipelineResult:" + PhotonPipelineResultSerde.MESSAGE_FORMAT
)


class NTTopicSet:

    def __init__(self) -> None:
        self.subTable = nt.NetworkTable()

    def updateEntries(self) -> None:
        options = nt.PubSubOptions()
        options.periodic = 0.01
        options.sendAll = True
        self.rawBytesEntry = self.subTable.getRawTopic("rawBytes").publish(
            PhotonPipelineResult_TYPE_STRING, options
        )
        self.pipelineIndexPublisher = self.subTable.getIntegerTopic(
            "pipelineIndexState"
        ).publish()
        self.pipelineIndexRequestSub = self.subTable.getIntegerTopic(
            "pipelineIndexRequest"
        ).subscribe(0)

        self.driverModePublisher = self.subTable.getBooleanTopic("driverMode").publish()
        self.driverModeSubscriber = self.subTable.getBooleanTopic(
            "driverModeRequest"
        ).subscribe(False)

        self.driverModeSubscriber.getTopic().publish().setDefault(False)

        self.latencyMillisEntry = self.subTable.getDoubleTopic(
            "latencyMillis"
        ).publish()
        self.hasTargetEntry = self.subTable.getBooleanTopic("hasTargets").publish()

        self.targetPitchEntry = self.subTable.getDoubleTopic("targetPitch").publish()
        self.targetAreaEntry = self.subTable.getDoubleTopic("targetArea").publish()
        self.targetYawEntry = self.subTable.getDoubleTopic("targetYaw").publish()
        self.targetPoseEntry = self.subTable.getStructTopic(
            "targetPose", Transform3d
        ).publish()
        self.targetSkewEntry = self.subTable.getDoubleTopic("targetSkew").publish()

        self.bestTargetPosX = self.subTable.getDoubleTopic("targetPixelsX").publish()
        self.bestTargetPosY = self.subTable.getDoubleTopic("targetPixelsY").publish()

        self.heartbeatTopic = self.subTable.getIntegerTopic("heartbeat")
        self.heartbeatPublisher = self.heartbeatTopic.publish()

        self.cameraIntrinsicsPublisher = self.subTable.getDoubleArrayTopic(
            "cameraIntrinsics"
        ).publish()
        self.cameraDistortionPublisher = self.subTable.getDoubleArrayTopic(
            "cameraDistortion"
        ).publish()
