import ntcore as nt
from wpimath import Transform3d

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

    def __init__(
        self,
        ntSubTable: nt.NetworkTable,
    ) -> None:
        self.subTable = ntSubTable

    def updateEntries(self) -> None:
        options = nt.PubSubOptions()
        options.periodic = 0.01
        options.send_all = True
        self.rawBytesEntry = self.subTable.get_raw_topic("rawBytes").publish(
            PhotonPipelineResult_TYPE_STRING, options
        )
        self.rawBytesEntry.get_topic().set_property(
            "message_uuid", PhotonPipelineResultSerde.MESSAGE_VERSION
        )
        self.pipelineIndexPublisher = self.subTable.get_integer_topic(
            "pipelineIndexState"
        ).publish()
        self.pipelineIndexRequestSub = self.subTable.get_integer_topic(
            "pipelineIndexRequest"
        ).subscribe(0)

        self.driverModePublisher = self.subTable.get_boolean_topic(
            "driverMode"
        ).publish()
        self.driverModeSubscriber = self.subTable.get_boolean_topic(
            "driverModeRequest"
        ).subscribe(False)

        self.driverModeSubscriber.get_topic().publish().set_default(False)

        self.fpsLimitPublisher = self.subTable.get_integer_topic("fpsLimit").publish()
        self.fpsLimitSubscriber = self.subTable.get_integer_topic(
            "fpsLimitRequest"
        ).subscribe(-1)

        self.fpsLimitSubscriber.get_topic().publish().set_default(-1)

        self.enabledPublisher = self.subTable.get_boolean_topic("enabled").publish()
        self.enabledSubscriber = self.subTable.get_boolean_topic(
            "enabledRequest"
        ).subscribe(True)

        self.enabledSubscriber.get_topic().publish().set_default(True)

        self.latencyMillisEntry = self.subTable.get_double_topic(
            "latencyMillis"
        ).publish()
        self.hasTargetEntry = self.subTable.get_boolean_topic("hasTargets").publish()

        self.targetPitchEntry = self.subTable.get_double_topic("targetPitch").publish()
        self.targetAreaEntry = self.subTable.get_double_topic("targetArea").publish()
        self.targetYawEntry = self.subTable.get_double_topic("targetYaw").publish()
        self.targetPoseEntry = self.subTable.get_struct_topic(
            "targetPose", Transform3d
        ).publish()
        self.targetSkewEntry = self.subTable.get_double_topic("targetSkew").publish()

        self.bestTargetPosX = self.subTable.get_double_topic("targetPixelsX").publish()
        self.bestTargetPosY = self.subTable.get_double_topic("targetPixelsY").publish()

        self.heartbeatTopic = self.subTable.get_integer_topic("heartbeat")
        self.heartbeatPublisher = self.heartbeatTopic.publish()

        self.cameraIntrinsicsPublisher = self.subTable.get_double_array_topic(
            "cameraIntrinsics"
        ).publish()
        self.cameraDistortionPublisher = self.subTable.get_double_array_topic(
            "cameraDistortion"
        ).publish()
