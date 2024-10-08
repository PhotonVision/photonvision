import type { Pose3d } from "@/types/PhotonTrackingTypes";
import { PossiblePipelineSettings } from "@/types/PipelineTypes";

export interface InstanceConfig {
  version: string;
  gpuAccelerationSupported: boolean;
  mrCalWorking: boolean;
  rknnSupported: boolean;
  hardwareModel: string;
  hardwarePlatform: string;
}

export interface PlatformMetrics {
  cpuTemp: string;
  cpuUtil: string;
  cpuMem: string;
  cpuThr: string;
  cpuUptime: string;
  gpuMem: string;
  ramUtil: string;
  gpuMemUtil: string;
  diskUtilPct: string;
  npuUsage: string;
  lastReceived: Date;
}

export enum NetworkConnectionType {
  // eslint-disable-next-line no-unused-vars
  DHCP = 0,
  // eslint-disable-next-line no-unused-vars
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
  canManage: boolean;
  networkManagerIface: string;
  setStaticCommand: string;
  setDHCPcommand: string;
  networkInterfaceNames: NetworkInterfaceType[];
  networkingDisabled: boolean;
  shouldPublishProto: boolean;
}

export type ConfigurableNetworkSettings = Omit<
  NetworkSettings,
  "canManage" | "networkInterfaceNames" | "networkingDisabled"
>;

export interface LightingSettings {
  supported: boolean;
  brightness: number;
}

export interface MiscellaneousSettings {
  matchCamerasOnlyByPath: boolean;
}

export type ConfigurableMiscellaneousSettings = MiscellaneousSettings;

export enum LogLevel {
  // eslint-disable-next-line no-unused-vars
  ERROR = 0,
  // eslint-disable-next-line no-unused-vars
  WARN = 1,
  // eslint-disable-next-line no-unused-vars
  INFO = 2,
  // eslint-disable-next-line no-unused-vars
  DEBUG = 3,
  // eslint-disable-next-line no-unused-vars
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

export type PixelFormat = "Picam" | "Unknown" | "MJPEG" | "YUYV" | "RGB565" | "BGR" | "Gray" | "Y16" | "UYVY" | "BGRA";

export interface VideoFormat {
  fps: number;
  resolution: Resolution;
  pixelFormat: PixelFormat;
  sourceIndex: number;
}

export enum CvType {
  // eslint-disable-next-line no-unused-vars
  CV_8U = 0,
  // eslint-disable-next-line no-unused-vars
  CV_8S = 1,
  // eslint-disable-next-line no-unused-vars
  CV_16U = 2,
  // eslint-disable-next-line no-unused-vars
  CV_16S = 3,
  // eslint-disable-next-line no-unused-vars
  CV_32S = 4,
  // eslint-disable-next-line no-unused-vars
  CV_32F = 5,
  // eslint-disable-next-line no-unused-vars
  CV_64F = 6,
  // eslint-disable-next-line no-unused-vars
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

export interface CameraCalibrationCoefficients {
  resolution: Resolution;
  cameraIntrinsics: JsonMatOfDouble;
  distCoeffs: JsonMatOfDouble;
  // Empty on UICameraCalibrationCoefficients
  observations: BoardObservation[];
  calobjectWarp: number[];
  calobjectSize: Resolution;
  calobjectSpacing: number;
  // lensmodel: idc
  // Only exists on UICameraCalibrationCoefficients
  numSnapshots: number;
  meanErrors: number[];
}

export enum ValidQuirks {
  // eslint-disable-next-line no-unused-vars
  AWBGain = "AWBGain",
  // eslint-disable-next-line no-unused-vars
  AdjustableFocus = "AdjustableFocus",
  // eslint-disable-next-line no-unused-vars
  ArduOV9281 = "ArduOV9281",
  // eslint-disable-next-line no-unused-vars
  ArduOV2311 = "ArduOV2311",
  // eslint-disable-next-line no-unused-vars
  ArduOV9782 = "ArduOV9782",
  // eslint-disable-next-line no-unused-vars
  ArduCamCamera = "ArduCamCamera",
  // eslint-disable-next-line no-unused-vars
  CompletelyBroken = "CompletelyBroken",
  // eslint-disable-next-line no-unused-vars
  FPSCap100 = "FPSCap100",
  // eslint-disable-next-line no-unused-vars
  Gain = "Gain",
  // eslint-disable-next-line no-unused-vars
  PiCam = "PiCam",
  // eslint-disable-next-line no-unused-vars
  StickyFPS = "StickyFPS"
}

export interface QuirkyCamera {
  baseName: string;
  usbVid: number;
  usbPid: number;
  displayName: string;
  quirks: Record<ValidQuirks, boolean>;
}

export interface CameraConfig {
  cameraIndex: number;
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

  videoFormats: VideoFormat[];
  calibrations: CameraCalibrationCoefficients[];

  activePipelineIndex: number;
  pipelineSettings: PossiblePipelineSettings[];

  cameraQuirks: QuirkyCamera;
  isCSICamera: boolean;
}

// export interface CameraSettingsChangeRequest {
//   fov: number;
//   quirksToChange: Record<ValidQuirks, boolean>;
// }
//
// export const PlaceholderCameraSettings: CameraSettings = {
//   nickname: "Placeholder Camera",
//   uniqueName: "Placeholder Name",
//   fov: {
//     value: 70,
//     managedByVendor: false
//   },
//   stream: {
//     inputPort: 0,
//     outputPort: 0
//   },
//   validVideoFormats: [
//     {
//       resolution: { width: 1920, height: 1080 },
//       fps: 60,
//       pixelFormat: "RGB"
//     },
//     {
//       resolution: { width: 1280, height: 720 },
//       fps: 60,
//       pixelFormat: "RGB"
//     },
//     {
//       resolution: { width: 640, height: 480 },
//       fps: 30,
//       pixelFormat: "RGB"
//     }
//   ],
//   completeCalibrations: [
//     {
//       resolution: { width: 1920, height: 1080 },
//       cameraIntrinsics: {
//         rows: 1,
//         cols: 1,
//         type: 1,
//         data: [1, 2, 3, 4, 5, 6, 7, 8, 9]
//       },
//       distCoeffs: {
//         rows: 1,
//         cols: 1,
//         type: 1,
//         data: [10, 11, 12, 13]
//       },
//       observations: [
//         {
//           locationInImageSpace: [
//             { x: 100, y: 100 },
//             { x: 210, y: 100 },
//             { x: 320, y: 101 }
//           ],
//           locationInObjectSpace: [{ x: 0, y: 0, z: 0 }],
//           optimisedCameraToObject: {
//             translation: { x: 1, y: 2, z: 3 },
//             rotation: { quaternion: { W: 1, X: 0, Y: 0, Z: 0 } }
//           },
//           reprojectionErrors: [
//             { x: 1, y: 1 },
//             { x: 2, y: 1 },
//             { x: 3, y: 1 }
//           ],
//           includeObservationInCalibration: false,
//           snapshotName: "img0.png",
//           snapshotData: { rows: 480, cols: 640, type: CvType.CV_8U, data: "" }
//         }
//       ],
//       numSnapshots: 1,
//       meanErrors: [123.45]
//     }
//   ],
//   pipelineNicknames: ["Placeholder Pipeline"],
//   lastPipelineIndex: 0,
//   currentPipelineIndex: 0,
//   pipelineSettings: DefaultAprilTagPipelineSettings,
//   cameraQuirks: {
//     displayName: "Blank 1",
//     baseName: "Blank 2",
//     usbVid: -1,
//     usbPid: -1,
//     quirks: {
//       AWBGain: false,
//       AdjustableFocus: false,
//       ArduOV9281: false,
//       ArduOV2311: false,
//       ArduOV9782: false,
//       ArduCamCamera: false,
//       CompletelyBroken: false,
//       FPSCap100: false,
//       Gain: false,
//       PiCam: false,
//       StickyFPS: false
//     }
//   },
//   isCSICamera: false
// };

export enum CalibrationBoardTypes {
  // eslint-disable-next-line no-unused-vars
  Chessboard = 0,
  // eslint-disable-next-line no-unused-vars
  Charuco = 1
}
export enum CalibrationTagFamilies {
  // eslint-disable-next-line no-unused-vars
  Dict_4X4_1000 = 0,
  // eslint-disable-next-line no-unused-vars
  Dict_5X5_1000 = 1,
  // eslint-disable-next-line no-unused-vars
  Dict_6X6_1000 = 2,
  // eslint-disable-next-line no-unused-vars
  Dict_7X7_1000 = 3
}

export enum RobotOffsetOperationMode {
  // eslint-disable-next-line no-unused-vars
  Clear = 0,
  // eslint-disable-next-line no-unused-vars
  Single = 1,
  // eslint-disable-next-line no-unused-vars
  DualFirst = 2,
  // eslint-disable-next-line no-unused-vars
  DualSecond = 3
}
