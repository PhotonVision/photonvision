import type { GeneralSettings, LightingSettings, LogLevel, MetricData, NetworkSettings } from "@/types/SettingTypes";
import type { ActivePipelineSettings } from "@/types/PipelineTypes";
import type { AprilTagFieldLayout, PipelineResult } from "@/types/PhotonTrackingTypes";

export interface WebsocketLogMessage {
  logMessage: {
    logLevel: LogLevel;
    logMessage: string;
  };
}
export interface WebsocketSettingsUpdate {
  general: Required<GeneralSettings>;
  lighting: Required<LightingSettings>;
  networkSettings: NetworkSettings;
  atfl: AprilTagFieldLayout;
}

export interface WebsocketNumberPair {
  first: number;
  second: number;
}

export interface WebsocketCompleteCalib {
  distCoeffs: number[];
  height: number;
  width: number;
  standardDeviation: number;
  perViewErrors: number[];
  intrinsics: number[];
}

export type WebsocketVideoFormat = Record<
  number,
  {
    fps: number;
    height: number;
    width: number;
    pixelFormat: string;
    index?: number;
    diagonalFOV?: number;
    horizontalFOV?: number;
    verticalFOV?: number;
    standardDeviation?: number;
    mean?: number;
  }
>;

export interface WebsocketCameraSettingsUpdate {
  calibrations: WebsocketCompleteCalib[];
  currentPipelineIndex: number;
  currentPipelineSettings: ActivePipelineSettings;
  fov: number;
  inputStreamPort: number;
  isFovConfigurable: boolean;
  nickname: string;
  outputStreamPort: number;
  pipelineNicknames: string[];
  videoFormatList: WebsocketVideoFormat;
}
export interface WebsocketNTUpdate {
  connected: boolean;
  address?: string;
  clients?: number;
}

// key is the index of the camera, value is that camera's result
export type WebsocketPipelineResultUpdate = Record<string, PipelineResult>;

export interface WebsocketCalibrationData {
  patternWidth: number;
  boardType: number;
  hasEnough: boolean;
  count: number;
  minCount: number;
  videoModeIndex: number;
  patternHeight: number;
  squareSizeIn: number;
}

export interface IncomingWebsocketData {
  log?: WebsocketLogMessage;
  settings?: WebsocketSettingsUpdate;
  cameraSettings?: WebsocketCameraSettingsUpdate[];
  ntConnectionInfo?: WebsocketNTUpdate;
  metrics?: Required<MetricData>;
  updatePipelineResult?: WebsocketPipelineResultUpdate;
  networkInfo?: {
    possibleRios: string[];
    deviceIps: string[];
  };
  mutatePipelineSettings?: Partial<ActivePipelineSettings>;
  cameraIndex?: number; // Sent when mutating pipeline settings to check against currently active
  calibrationData?: WebsocketCalibrationData;
}

export enum WebsocketPipelineType {
  Calib3d = -2,
  DriverMode = -1,
  Reflective = 0,
  ColoredShape = 1,
  AprilTag = 2,
  Aruco = 3
}
