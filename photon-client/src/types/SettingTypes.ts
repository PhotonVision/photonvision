import { type ActivePipelineSettings, DefaultAprilTagPipelineSettings } from "@/types/PipelineTypes";
import type { Pose3d } from "@/types/PhotonTrackingTypes";

export interface GeneralSettings {
  version?: string;
  gpuAcceleration?: string;
  hardwareModel?: string;
  hardwarePlatform?: string;
  mrCalWorking: boolean;
  rknnSupported: boolean;
  availableModels: string[];
}

export interface MetricData {
  cpuTemp?: string;
  cpuUtil?: string;
  cpuMem?: string;
  gpuMem?: string;
  ramUtil?: string;
  gpuMemUtil?: string;
  cpuThr?: string;
  cpuUptime?: string;
  diskUtilPct?: string;
  npuUsage?: string;
}

export enum NetworkConnectionType {
  DHCP = 0,
  Static = 1
}

export interface NetworkInterfaceType {
  connName: string;
  devName: string;
}

export interface NetworkSettings {
  ntServerAddress: string;
  connectionType: NetworkConnectionType;
  staticIp: string;
  hostname: string;
  runNTServer: boolean;
  shouldManage: boolean;
  shouldPublishProto: boolean;
  canManage: boolean;
  networkManagerIface?: string;
  setStaticCommand?: string;
  setDHCPcommand?: string;
  networkInterfaceNames: NetworkInterfaceType[];
  networkingDisabled: boolean;
  matchCamerasOnlyByPath: boolean;
}

export type ConfigurableNetworkSettings = Omit<
  NetworkSettings,
  "canManage" | "networkInterfaceNames" | "networkingDisabled"
>;

export interface LightingSettings {
  supported: boolean;
  brightness: number;
}

export enum LogLevel {
  ERROR = 0,
  WARN = 1,
  INFO = 2,
  DEBUG = 3,
  TRACE = 4
}

export interface LogMessage {
  level: LogLevel;
  message: string;
}

export interface Resolution {
  width: number;
  height: number;
}

export interface VideoFormat {
  resolution: Resolution;
  fps: number;
  pixelFormat: string;
  index?: number;
  diagonalFOV?: number;
  horizontalFOV?: number;
  verticalFOV?: number;
  mean?: number;
}

export enum CvType {
  CV_8U = 0,
  CV_8S = 1,
  CV_16U = 2,
  CV_16S = 3,
  CV_32S = 4,
  CV_32F = 5,
  CV_64F = 6,
  CV_16F = 7
}

export interface JsonMatOfDouble {
  rows: number;
  cols: number;
  type: CvType;
  data: number[];
}

export interface JsonImageMat {
  rows: number;
  cols: number;
  type: CvType;
  data: string; // base64 encoded
}

export interface CvPoint3 {
  x: number;
  y: number;
  z: number;
}
export interface CvPoint {
  x: number;
  y: number;
}

export interface BoardObservation {
  locationInObjectSpace: CvPoint3[];
  locationInImageSpace: CvPoint[];
  reprojectionErrors: CvPoint[];
  optimisedCameraToObject: Pose3d;
  includeObservationInCalibration: boolean;
  snapshotName: string;
  snapshotData: JsonImageMat;
}

export interface CameraCalibrationResult {
  resolution: Resolution;
  cameraIntrinsics: JsonMatOfDouble;
  distCoeffs: JsonMatOfDouble;
  observations: BoardObservation[];
  calobjectWarp?: number[];
  // We might have to omit observations for bandwith, so backend will send us this
  numSnapshots: number;
  meanErrors: number[];
}

export enum ValidQuirks {
  AWBGain = "AWBGain",
  AdjustableFocus = "AdjustableFocus",
  ArduOV9281 = "ArduOV9281",
  ArduOV2311 = "ArduOV2311",
  ArduOV9782 = "ArduOV9782",
  ArduCamCamera = "ArduCamCamera",
  CompletelyBroken = "CompletelyBroken",
  FPSCap100 = "FPSCap100",
  Gain = "Gain",
  PiCam = "PiCam",
  StickyFPS = "StickyFPS"
}

export interface QuirkyCamera {
  baseName: string;
  usbVid: number;
  usbPid: number;
  displayName: string;
  quirks: Record<ValidQuirks, boolean>;
}

export interface CameraSettings {
  nickname: string;
  uniqueName: string;

  fov: {
    value: number;
    managedByVendor: boolean;
  };
  stream: {
    inputPort: number;
    outputPort: number;
  };

  validVideoFormats: VideoFormat[];
  completeCalibrations: CameraCalibrationResult[];

  lastPipelineIndex?: number;
  currentPipelineIndex: number;
  pipelineNicknames: string[];
  pipelineSettings: ActivePipelineSettings;

  cameraQuirks: QuirkyCamera;
  isCSICamera: boolean;
}

export interface CameraSettingsChangeRequest {
  fov: number;
  quirksToChange: Record<ValidQuirks, boolean>;
}

export const PlaceholderCameraSettings: CameraSettings = {
  nickname: "Placeholder Camera",
  uniqueName: "Placeholder Name",
  fov: {
    value: 70,
    managedByVendor: false
  },
  stream: {
    inputPort: 0,
    outputPort: 0
  },
  validVideoFormats: [
    {
      resolution: { width: 1920, height: 1080 },
      fps: 60,
      pixelFormat: "RGB"
    },
    {
      resolution: { width: 1280, height: 720 },
      fps: 60,
      pixelFormat: "RGB"
    },
    {
      resolution: { width: 640, height: 480 },
      fps: 30,
      pixelFormat: "RGB"
    }
  ],
  completeCalibrations: [
    {
      resolution: { width: 1920, height: 1080 },
      cameraIntrinsics: {
        rows: 1,
        cols: 1,
        type: 1,
        data: [1, 2, 3, 4, 5, 6, 7, 8, 9]
      },
      distCoeffs: {
        rows: 1,
        cols: 1,
        type: 1,
        data: [10, 11, 12, 13]
      },
      observations: [
        {
          locationInImageSpace: [
            { x: 100, y: 100 },
            { x: 210, y: 100 },
            { x: 320, y: 101 }
          ],
          locationInObjectSpace: [{ x: 0, y: 0, z: 0 }],
          optimisedCameraToObject: {
            translation: { x: 1, y: 2, z: 3 },
            rotation: { quaternion: { W: 1, X: 0, Y: 0, Z: 0 } }
          },
          reprojectionErrors: [
            { x: 1, y: 1 },
            { x: 2, y: 1 },
            { x: 3, y: 1 }
          ],
          includeObservationInCalibration: false,
          snapshotName: "img0.png",
          snapshotData: { rows: 480, cols: 640, type: CvType.CV_8U, data: "" }
        }
      ],
      numSnapshots: 1,
      meanErrors: [123.45]
    }
  ],
  pipelineNicknames: ["Placeholder Pipeline"],
  lastPipelineIndex: 0,
  currentPipelineIndex: 0,
  pipelineSettings: DefaultAprilTagPipelineSettings,
  cameraQuirks: {
    displayName: "Blank 1",
    baseName: "Blank 2",
    usbVid: -1,
    usbPid: -1,
    quirks: {
      AWBGain: false,
      AdjustableFocus: false,
      ArduOV9281: false,
      ArduOV2311: false,
      ArduOV9782: false,
      ArduCamCamera: false,
      CompletelyBroken: false,
      FPSCap100: false,
      Gain: false,
      PiCam: false,
      StickyFPS: false
    }
  },
  isCSICamera: false
};

export enum CalibrationBoardTypes {
  Chessboard = 0,
  Charuco = 1
}

export enum CalibrationTagFamilies {
  Dict_4X4_1000 = 0,
  Dict_5X5_1000 = 1,
  Dict_6X6_1000 = 2,
  Dict_7X7_1000 = 3
}

export enum RobotOffsetType {
  Clear = 0,
  Single = 1,
  DualFirst = 2,
  DualSecond = 3
}
