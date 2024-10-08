import {
  type CalibrationBoardTypes,
  type CalibrationTagFamilies,
  CameraCalibrationCoefficients,
  CameraConfig,
  InstanceConfig,
  LightingSettings,
  LogLevel,
  MiscellaneousSettings,
  NetworkSettings,
  PlatformMetrics,
  RobotOffsetOperationMode,
  ValidQuirks
} from "@/types/SettingTypes";
import { PipelineResult } from "@/types/PhotonTrackingTypes";
import type { AprilTagFieldLayout } from "@/types/PhotonTrackingTypes";
import { ConfigurableUserPipelineSettings, PipelineType } from "@/types/PipelineTypes";

export type StringifiedCameraIndex = string;

export interface WebsocketLogMessage {
  logMessage: {
    logLevel: LogLevel;
    logMessage: string;
  };
}

export interface WebsocketSettingsUpdate {
  lighting: LightingSettings;
  network: NetworkSettings;
  misc: MiscellaneousSettings;
}

export interface WebsocketNTUpdate {
  connected: boolean;
  address?: string;
  clients?: number;
}

export interface WebsocketPipelineResultUpdate extends PipelineResult {
  cameraIndex: number;
}

export interface StartCalibrationPayload {
  cameraIndex: number;
  videoModeIndex: number;
  squareSizeIn: number;
  markerSizeIn: number;
  patternWidth: number;
  patternHeight: number;
  boardType: CalibrationBoardTypes;
  useMrCal: boolean;
  useOldPattern: boolean;
  tagFamily: CalibrationTagFamilies;
}

export interface OutgoingWebsocketMessage {
  changeActivePipeline: {
    cameraIndex: number;
    newActivePipelineIndex: number;
  };
  driverMode: {
    cameraIndex: number;
    driverMode: boolean;
  };
  changeCameraNickname: {
    cameraIndex: number;
    nickname: string;
  };
  changePipelineNickname: {
    cameraIndex: number;
    pipelineIndex: number;
    nickname: string;
  };
  createNewPipeline: {
    cameraIndex: number;
    nickname: string;
    type: PipelineType;
  };
  duplicatePipeline: {
    cameraIndex: number;
    targetIndex: number;
    nickname: string;
    setActive: boolean;
  };
  resetPipeline: {
    cameraIndex: number;
    pipelineIndex: number;
    type?: PipelineType;
  };
  deletePipeline: {
    cameraIndex: number;
    pipelineIndex: number;
  };
  changePipelineSettings: {
    cameraIndex: number;
    pipelineIndex: number;
    configuredSettings: Partial<ConfigurableUserPipelineSettings>;
  };
  startCalib: {
    cameraIndex: number;
    videoModeIndex: number;
    squareSizeIn: number;
    markerSizeIn: number;
    patternWidth: number;
    patternHeight: number;
    boardType: CalibrationBoardTypes;
    useMrCal: boolean;
    useOldPattern: boolean;
    tagFamily: CalibrationTagFamilies;
  };
  takeCalibSnapshot: {
    cameraIndex: number;
  };
  cancelCalib: {
    cameraIndex: number;
  };
  completeCalib: {
    cameraIndex: number;
  };
  importCalibFromData: {
    cameraIndex: number;
    calibration: CameraCalibrationCoefficients;
  };
  importCalibFromCalibDB: {
    cameraIndex: number;
    calibration: string;
  };
  saveInputSnapshot: {
    cameraIndex: number;
  };
  saveOutputSnapshot: {
    cameraIndex: number;
  };
  ledPercentage: number;
  changeCameraFOV: {
    cameraIndex: number;
    fov: number;
  };
  // Only possible for active pipeline
  robotOffsetPoint: {
    cameraIndex: number;
    type: RobotOffsetOperationMode;
  };
  changeCameraQuirks: {
    cameraIndex: number;
    quirks: Record<ValidQuirks, boolean>;
  };
  restartProgram: true;
  restartDevice: true;
  publishMetrics: true;
}

export interface WebsocketConfigurationUpdate {
  instanceConfig: InstanceConfig;
  settings: WebsocketSettingsUpdate;
  activeATFL: AprilTagFieldLayout;
  cameras: CameraConfig[];
}

export interface WebsocketRoboRIOFinderResults {
  possibleRios: string[];
  deviceIps: string[];
}

export interface WebsocketNumberPair {
  first: number;
  second: number;
}

export interface WebsocketCameraSettingsMutation {
  cameraIndex: number;
  nickname?: string,
  fov?: {
    value: number
  },
  stream?: {
    inputPort: number;
    outputPort: number;
  }
  activePipelineIndex?: number,
  mutatePipelineSettings?: {
    pipelineIndex: number;
    mutation: Partial<ConfigurableUserPipelineSettings>;
  }
  cameraQuirks?: Record<ValidQuirks, boolean>;
}

export interface IncomingWebsocketMessage extends Partial<WebsocketConfigurationUpdate> {
  log?: WebsocketLogMessage;
  ntConnectionInfo?: WebsocketNTUpdate;
  metrics?: PlatformMetrics;
  updatePipelineResult?: WebsocketPipelineResultUpdate;
  networkInfo?: WebsocketRoboRIOFinderResults;
  pipelineSettingMutation?: {
    cameraIndex: number;
    pipelineIndex: number;
    mutation: Partial<ConfigurableUserPipelineSettings>;
  };




  mutateCameraSettings?: WebsocketCameraSettingsMutation;

  calibrationStatus?: {
    inProgress: false
  } | {
    inProgress: true,
    videoFormatIndex: number;
    count: number;
    minCount: number;
  }
}
