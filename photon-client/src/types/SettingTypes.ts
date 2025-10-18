import { type ActivePipelineSettings, DefaultAprilTagPipelineSettings } from "@/types/PipelineTypes";
import type { Pose3d } from "@/types/PhotonTrackingTypes";
import type { WebsocketCameraSettingsUpdate } from "./WebsocketDataTypes";

export interface GeneralSettings {
  version?: string;
  gpuAcceleration?: string;
  hardwareModel?: string;
  hardwarePlatform?: string;
  mrCalWorking: boolean;
  availableModels: Record<string, string[]>;
  supportedBackends: string[];
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
  ipAddress?: string;
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
            },
            {
              x: 0.07620000094175339,
              y: 0,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0,
              z: 0
            },
            {
              x: 0,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.02539999969303608,
              z: 0
            },
            {
              x: 0,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.05079999938607216,
              z: 0
            },
            {
              x: 0,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.07620000094175339,
              z: 0
            },
            {
              x: 0,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.10159999877214432,
              z: 0
            },
            {
              x: 0,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.12700000405311584,
              z: 0
            },
            {
              x: 0,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.15240000188350677,
              z: 0
            },
            {
              x: 0,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.1777999997138977,
              z: 0
            },
            {
              x: 0,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.20319999754428864,
              z: 0
            },
            {
              x: 0,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.02539999969303608,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.05079999938607216,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.07620000094175339,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.10159999877214432,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.12700000405311584,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.15240000188350677,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.1777999997138977,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.20319999754428864,
              y: 0.22859999537467957,
              z: 0
            },
            {
              x: 0.22859999537467957,
              y: 0.22859999537467957,
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
            },
            {
              x: 207.7600555419922,
              y: 107.31533813476562
            },
            {
              x: 255.73614501953125,
              y: 106.99311828613281
            },
            {
              x: 303.44744873046875,
              y: 106.82320404052734
            },
            {
              x: 350.2528381347656,
              y: 106.60562133789062
            },
            {
              x: 396.2431945800781,
              y: 106.8026351928711
            },
            {
              x: 441.4327392578125,
              y: 106.79154968261719
            },
            {
              x: 485.5487060546875,
              y: 107.00993347167969
            },
            {
              x: 58.63193893432617,
              y: 154.77313232421875
            },
            {
              x: 110.17645263671875,
              y: 154.2306671142578
            },
            {
              x: 160.28346252441406,
              y: 153.31809997558594
            },
            {
              x: 209.3062744140625,
              y: 152.52520751953125
            },
            {
              x: 257.7140808105469,
              y: 151.71612548828125
            },
            {
              x: 305.75341796875,
              y: 151.1773223876953
            },
            {
              x: 353.0191650390625,
              y: 150.71673583984375
            },
            {
              x: 399.1933288574219,
              y: 150.30233764648438
            },
            {
              x: 444.4662780761719,
              y: 150.1077880859375
            },
            {
              x: 488.73272705078125,
              y: 149.67855834960938
            },
            {
              x: 60.28325271606445,
              y: 201.95692443847656
            },
            {
              x: 111.83313751220703,
              y: 200.7225799560547
            },
            {
              x: 161.75807189941406,
              y: 199.46006774902344
            },
            {
              x: 211.08206176757812,
              y: 198.1295166015625
            },
            {
              x: 260.0283203125,
              y: 196.93878173828125
            },
            {
              x: 308.3556823730469,
              y: 196.0634307861328
            },
            {
              x: 355.7143859863281,
              y: 195.0876007080078
            },
            {
              x: 402.1219482421875,
              y: 194.3774871826172
            },
            {
              x: 447.5528564453125,
              y: 193.53919982910156
            },
            {
              x: 491.9447937011719,
              y: 192.82876586914062
            },
            {
              x: 61.84204864501953,
              y: 249.58982849121094
            },
            {
              x: 113.34342193603516,
              y: 247.8232879638672
            },
            {
              x: 163.6376953125,
              y: 246.02194213867188
            },
            {
              x: 213.07028198242188,
              y: 244.3157196044922
            },
            {
              x: 262.3406066894531,
              y: 242.71572875976562
            },
            {
              x: 310.8130187988281,
              y: 241.25222778320312
            },
            {
              x: 358.5671081542969,
              y: 239.88723754882812
            },
            {
              x: 405.214599609375,
              y: 238.5273895263672
            },
            {
              x: 450.8108215332031,
              y: 237.31747436523438
            },
            {
              x: 495.4122009277344,
              y: 236.06512451171875
            },
            {
              x: 63.49900436401367,
              y: 297.6044006347656
            },
            {
              x: 115.1602554321289,
              y: 295.3501892089844
            },
            {
              x: 165.52125549316406,
              y: 293.1644287109375
            },
            {
              x: 215.2859344482422,
              y: 291.04681396484375
            },
            {
              x: 264.7484436035156,
              y: 288.8113708496094
            },
            {
              x: 313.50079345703125,
              y: 286.91162109375
            },
            {
              x: 361.5191345214844,
              y: 285.0365295410156
            },
            {
              x: 408.25750732421875,
              y: 283.1592712402344
            },
            {
              x: 454.1041259765625,
              y: 281.4944152832031
            },
            {
              x: 498.7144470214844,
              y: 279.7467346191406
            },
            {
              x: 65.38162231445312,
              y: 345.9874267578125
            },
            {
              x: 117.00627899169922,
              y: 343.3543395996094
            },
            {
              x: 167.5782012939453,
              y: 340.7157287597656
            },
            {
              x: 217.63848876953125,
              y: 338.1772766113281
            },
            {
              x: 267.2597351074219,
              y: 335.5164794921875
            },
            {
              x: 316.23870849609375,
              y: 333.0758972167969
            },
            {
              x: 364.3795471191406,
              y: 330.5984191894531
            },
            {
              x: 411.4840087890625,
              y: 328.2593078613281
            },
            {
              x: 457.2756042480469,
              y: 325.91705322265625
            },
            {
              x: 502.18939208984375,
              y: 323.7250671386719
            },
            {
              x: 67.15499114990234,
              y: 394.97161865234375
            },
            {
              x: 119.00973510742188,
              y: 391.77410888671875
            },
            {
              x: 169.8325958251953,
              y: 388.85931396484375
            },
            {
              x: 220.0489501953125,
              y: 385.68231201171875
            },
            {
              x: 269.952392578125,
              y: 382.6337585449219
            },
            {
              x: 318.9443359375,
              y: 379.56072998046875
            },
            {
              x: 367.27301025390625,
              y: 376.6636962890625
            },
            {
              x: 414.4603576660156,
              y: 373.6673889160156
            },
            {
              x: 460.4902648925781,
              y: 370.91802978515625
            },
            {
              x: 505.392578125,
              y: 368.00909423828125
            },
            {
              x: 68.96627807617188,
              y: 444.2562255859375
            },
            {
              x: 121.18102264404297,
              y: 440.65863037109375
            },
            {
              x: 172.115966796875,
              y: 437.2845764160156
            },
            {
              x: 222.2353057861328,
              y: 433.7698059082031
            },
            {
              x: 272.23626708984375,
              y: 430.14031982421875
            },
            {
              x: 321.5489196777344,
              y: 426.63018798828125
            },
            {
              x: 368.70965576171875,
              y: 422.3192443847656
            },
            {
              x: 417.4640808105469,
              y: 419.6300354003906
            },
            {
              x: 463.5595397949219,
              y: 416.259521484375
            },
            {
              x: 508.8578186035156,
              y: 412.8741149902344
            },
            {
              x: 70.67405700683594,
              y: 494.0494079589844
            },
            {
              x: 123.33006286621094,
              y: 489.9080505371094
            },
            {
              x: 174.38775634765625,
              y: 486.1134948730469
            },
            {
              x: 224.7969207763672,
              y: 482.2079772949219
            },
            {
              x: 274.7309875488281,
              y: 478.33941650390625
            },
            {
              x: 324.0686950683594,
              y: 474.1112976074219
            },
            {
              x: 372.7146301269531,
              y: 470.130126953125
            },
            {
              x: 420.24676513671875,
              y: 465.9164123535156
            },
            {
              x: 466.73193359375,
              y: 461.965576171875
            },
            {
              x: 512.0394897460938,
              y: 458.3018798828125
            },
            {
              x: 72.61973571777344,
              y: 544.1264038085938
            },
            {
              x: 125.5038070678711,
              y: 539.5327758789062
            },
            {
              x: 176.95079040527344,
              y: 535.1825561523438
            },
            {
              x: 227.44509887695312,
              y: 531.0090942382812
            },
            {
              x: 277.3024597167969,
              y: 526.53955078125
            },
            {
              x: 326.773681640625,
              y: 522.027099609375
            },
            {
              x: 375.393798828125,
              y: 517.5185546875
            },
            {
              x: 423.1520080566406,
              y: 512.8489990234375
            },
            {
              x: 469.6885681152344,
              y: 508.4466247558594
            },
            {
              x: 515.3706665039062,
              y: 504.0155029296875
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
          cornersUsed: [true, true, true, false, false, true],
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
  hasConnected: true
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
