import { type ActivePipelineSettings, DefaultAprilTagPipelineSettings } from "@/types/PipelineTypes";
import type { Pose3d } from "@/types/PhotonTrackingTypes";
import type { WebsocketCameraSettingsUpdate } from "./WebsocketDataTypes";

export interface GeneralSettings {
  version?: string;
  gpuAcceleration?: string;
  hardwareModel?: string;
  hardwarePlatform?: string;
  mrCalWorking: boolean;
  availableModels: ObjectDetectionModelProperties[];
  supportedBackends: string[];
  conflictingHostname: boolean;
  conflictingCameras: string;
}

export interface ObjectDetectionModelProperties {
  modelPath: string;
  nickname: string;
  labels: string[];
  resolutionWidth: number;
  resolutionHeight: number;
  family: "RKNN" | "RUBIK";
  version: "YOLOV5" | "YOLOV8" | "YOLOV11";
}

export interface MetricData {
  cpuTemp?: number;
  cpuUtil?: number;
  cpuThr?: string;
  ramMem?: number;
  ramUtil?: number;
  gpuMem?: number;
  gpuMemUtil?: number;
  diskUtilPct?: number;
  npuUsage?: number[];
  ipAddress?: string;
  uptime?: number;
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
}

export type ConfigurableNetworkSettings = Omit<
  NetworkSettings,
  "canManage" | "networkInterfaceNames" | "networkingDisabled"
>;

export interface PVCameraInfoBase {
  /*
  Huge hack. In Jackson, this is set based on the underlying type -- this
  then maps to one of the 3 subclasses here below. Not sure how to best deal with this.
  */
  cameraTypename: "PVUsbCameraInfo" | "PVCSICameraInfo" | "PVFileCameraInfo";
}

export interface PVUsbCameraInfo {
  dev: number;
  name: string;
  otherPaths: string[];
  path: string;
  vendorId: number;
  productId: number;

  // In Java, PVCameraInfo provides a uniquePath property so we can have one Source of Truth here
  uniquePath: string;
}
export interface PVCSICameraInfo {
  baseName: string;
  path: string;

  // In Java, PVCameraInfo provides a uniquePath property so we can have one Source of Truth here
  uniquePath: string;
}
export interface PVFileCameraInfo {
  path: string;
  name: string;

  // In Java, PVCameraInfo provides a uniquePath property so we can have one Source of Truth here
  uniquePath: string;
}

// This camera info will only ever hold one of its members - the others should be undefined.
export class PVCameraInfo {
  PVUsbCameraInfo: PVUsbCameraInfo | undefined;
  PVCSICameraInfo: PVCSICameraInfo | undefined;
  PVFileCameraInfo: PVFileCameraInfo | undefined;
}

export interface VsmState {
  disabledConfigs: WebsocketCameraSettingsUpdate[];
  allConnectedCameras: PVCameraInfo[];
}

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
  timestamp: Date;
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
  cornersUsed: boolean[];
  snapshotName: string;
  snapshotData: JsonImageMat;
}

export interface CameraCalibrationResult {
  resolution: Resolution;
  cameraIntrinsics: JsonMatOfDouble;
  distCoeffs: JsonMatOfDouble;
  observations: BoardObservation[];
  calobjectWarp?: number[];
  calobjectSize: { width: number; height: number };
  calobjectSpacing: number;
  lensModel: string;

  // We have to omit observations for bandwidth, so backend will send us this from UICameraCalibrationCoefficients
  numSnapshots: number;
  meanErrors: number[];
  numOutliers: number[];
  numMissing: number[];
}

export enum ValidQuirks {
  AWBGain = "AWBGain",
  AdjustableFocus = "AdjustableFocus",
  InnoOV9281Controls = "InnoOV9281Controls",
  ArduOV9281Controls = "ArduOV9281Controls",
  ArduOV2311Controls = "ArduOV2311Controls",
  ArduOV9782Controls = "ArduOV9782Controls",
  ArduCamCamera = "ArduCamCamera",
  CompletelyBroken = "CompletelyBroken",
  FPSCap100 = "FPSCap100",
  Gain = "Gain",
  PiCam = "PiCam",
  StickyFPS = "StickyFPS",
  LifeCamControls = "LifeCamControls",
  PsEyeControls = "PsEyeControls"
}

export interface QuirkyCamera {
  baseName: string;
  usbVid: number;
  usbPid: number;
  displayName: string;
  quirks: Record<ValidQuirks, boolean>;
}

export interface UiCameraConfiguration {
  cameraPath: string;

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

  minExposureRaw: number;
  maxExposureRaw: number;

  minWhiteBalanceTemp: number;
  maxWhiteBalanceTemp: number;

  matchedCameraInfo: PVCameraInfo;
  isConnected: boolean;
  hasConnected: boolean;
  mismatch: boolean;
}

export interface CameraSettingsChangeRequest {
  fov: number;
  quirksToChange: Record<ValidQuirks, boolean>;
}

export const PlaceholderCameraSettings: UiCameraConfiguration = {
  cameraPath: "/dev/null",

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
      pixelFormat: "RGB",
      index: 0
    },
    {
      resolution: { width: 1280, height: 720 },
      fps: 60,
      pixelFormat: "RGB",
      index: 1
    },
    {
      resolution: { width: 640, height: 480 },
      fps: 30,
      pixelFormat: "RGB",
      index: 2
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
          locationInObjectSpace: [
            {
              x: 0,
              y: 0,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0,
              z: 0
            }
          ],
          locationInImageSpace: [
            {
              x: 57.062007904052734,
              y: 108.12601470947266
            },
            {
              x: 108.72974395751953,
              y: 108.0336685180664
            },
            {
              x: 158.78118896484375,
              y: 107.8104019165039
            }
          ],
          optimisedCameraToObject: {
            translation: {
              x: -0.28942385915178886,
              y: -0.12895727420625547,
              z: 0.5933086404370699
            },
            rotation: {
              quaternion: {
                W: 0.9890028788589879,
                X: -0.0507354429843431,
                Y: -0.13458187019694584,
                Z: -0.034452004994036174
              }
            }
          },
          reprojectionErrors: [
            { x: 1, y: 1 },
            { x: 2, y: 1 },
            { x: 3, y: 1 }
          ],
          cornersUsed: [true, true, false],
          snapshotName: "img0.png",
          snapshotData: { rows: 480, cols: 640, type: CvType.CV_8U, data: "" }
        }
      ],
      calobjectSize: {
        width: 10,
        height: 10
      },
      calobjectSpacing: 0.0254,
      lensModel: "opencv8",
      numSnapshots: 1,
      meanErrors: [123.45],
      numMissing: [0],
      numOutliers: [1],
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
      ArduOV9281Controls: false,
      ArduOV2311Controls: false,
      ArduOV9782Controls: false,
      ArduCamCamera: false,
      CompletelyBroken: false,
      FPSCap100: false,
      Gain: false,
      PiCam: false,
      StickyFPS: false,
      InnoOV9281Controls: false,
      LifeCamControls: false,
      PsEyeControls: false
    }
  },
  isCSICamera: false,
  minExposureRaw: 1,
  maxExposureRaw: 100,
  minWhiteBalanceTemp: 2000,
  maxWhiteBalanceTemp: 10000,
  matchedCameraInfo: {
    PVFileCameraInfo: {
      name: "Foobar",
      path: "/dev/foobar",
      uniquePath: "/dev/foobar2"
    },
    PVCSICameraInfo: undefined,
    PVUsbCameraInfo: undefined
  },
  isConnected: true,
  hasConnected: true,
  mismatch: false
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
