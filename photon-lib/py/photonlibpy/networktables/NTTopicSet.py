import ntcore as nt
from wpimath.geometry import Transform3d

from ..generated.PhotonPipelineResultSerde import PhotonPipelineResultSerde

PhotonPipelineResult_TYPE_STRING = (
    "photonstruct:PhotonPipelineResult:" + PhotonPipelineResultSerde.MESSAGE_VERSION
)


class NTTopicSet:
    """This class is a wrapper around all per-pipeline NT topics that PhotonVision should be publishing
    It's split here so the sim and real-camera implementations can share a common implementation of
    the naming and registration of the NT content.

    However, we do expect that the actual logic which fills out values in the entries will be
    different for sim vs. real camera
    """

    def __init__(self, tableName: str, cameraName: str) -> None:
        instance = nt.NetworkTableInstance.getDefault()
        photonvision_root_table = instance.getTable(tableName)
        self.subTable = photonvision_root_table.getSubTable(cameraName)

    def updateEntries(self) -> None:
        options = nt.PubSubOptions()
        options.periodic = 0.01
        options.sendAll = True
        self.rawBytesEntry = self.subTable.getRawTopic("rawBytes").publish(
            PhotonPipelineResult_TYPE_STRING, options
        )
        self.rawBytesEntry.getTopic().setProperty(
            "message_uuid", PhotonPipelineResultSerde.MESSAGE_VERSION
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
