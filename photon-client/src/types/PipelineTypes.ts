import type { WebsocketNumberPair } from "@/types/WebsocketDataTypes";

export enum PipelineType {
  DriverMode = 1,
  Reflective = 2,
  ColoredShape = 3,
  AprilTag = 4,
  Aruco = 5
}

export enum AprilTagFamily {
  Family36h11 = 0,
  Family25h9 = 1,
  Family16h5 = 2
}

export enum RobotOffsetPointMode {
  None = 0,
  Single = 1,
  Dual = 2
}

export enum TargetModel {
  InfiniteRechargeHighGoalOuter = 0,
  InfiniteRechargeHighGoalInner = 1,
  DeepSpaceDualTarget = 2,
  CircularPowerCell7in = 3,
  RapidReactCircularCargoBall = 4,
  StrongholdHighGoal = 5,
  Apriltag_200mm = 6,
  Aruco6in_16h5 = 7,
  Apriltag6in_16h5 = 8
}

export interface PipelineSettings {
  offsetRobotOffsetMode: RobotOffsetPointMode;
  streamingFrameDivisor: number;
  offsetDualPointBArea: number;
  contourGroupingMode: number;
  hsvValue: WebsocketNumberPair | [number, number];
  cameraGain: number;
  cameraBlueGain: number;
  cameraRedGain: number;
  cornerDetectionSideCount: number;
  contourRatio: WebsocketNumberPair | [number, number];
  contourTargetOffsetPointEdge: number;
  pipelineNickname: string;
  inputImageRotationMode: number;
  contourArea: WebsocketNumberPair | [number, number];
  solvePNPEnabled: boolean;
  contourFullness: WebsocketNumberPair | [number, number];
  pipelineIndex: number;
  inputShouldShow: boolean;
  cameraAutoExposure: boolean;
  contourSpecklePercentage: number;
  contourTargetOrientation: number;
  targetModel: TargetModel;
  cornerDetectionUseConvexHulls: boolean;
  outputShouldShow: boolean;
  outputShouldDraw: boolean;
  offsetDualPointA: { x: number; y: number };
  offsetDualPointB: { x: number; y: number };
  hsvHue: WebsocketNumberPair | [number, number];
  ledMode: boolean;
  hueInverted: boolean;
  outputShowMultipleTargets: boolean;
  contourSortMode: number;
  cameraExposure: number;
  offsetSinglePoint: { x: number; y: number };
  cameraBrightness: number;
  offsetDualPointAArea: number;
  cornerDetectionExactSideCount: boolean;
  cameraVideoModeIndex: number;
  cornerDetectionStrategy: number;
  cornerDetectionAccuracyPercentage: number;
  hsvSaturation: WebsocketNumberPair | [number, number];
  pipelineType: PipelineType;
  contourIntersection: number;
}
export type ConfigurablePipelineSettings = Partial<
  Omit<
    PipelineSettings,
    | "offsetDualPointAArea"
    | "cornerDetectionSideCount"
    | "pipelineNickname"
    | "pipelineIndex"
    | "pipelineType"
    | "cornerDetectionUseConvexHulls"
    | "offsetDualPointA"
    | "offsetDualPointB"
    | "ledMode"
    | "offsetSinglePoint"
    | "offsetDualPointBArea"
    | "cornerDetectionExactSideCount"
    | "cornerDetectionStrategy"
  >
>;
export const DefaultPipelineSettings: PipelineSettings = {
  offsetRobotOffsetMode: RobotOffsetPointMode.None,
  streamingFrameDivisor: 0,
  offsetDualPointBArea: 0,
  contourGroupingMode: 0,
  hsvValue: { first: 50, second: 255 },
  cameraBlueGain: 20,
  cameraRedGain: 11,
  cornerDetectionSideCount: 4,
  contourRatio: { first: 0, second: 20 },
  contourTargetOffsetPointEdge: 0,
  pipelineNickname: "Placeholder Pipeline",
  inputImageRotationMode: 0,
  contourArea: { first: 0, second: 100 },
  solvePNPEnabled: false,
  contourFullness: { first: 0, second: 100 },
  pipelineIndex: 0,
  inputShouldShow: false,
  cameraAutoExposure: false,
  contourSpecklePercentage: 5,
  contourTargetOrientation: 1,
  cornerDetectionUseConvexHulls: true,
  outputShouldShow: true,
  outputShouldDraw: true,
  offsetDualPointA: { x: 0, y: 0 },
  offsetDualPointB: { x: 0, y: 0 },
  hsvHue: { first: 50, second: 180 },
  hueInverted: false,
  contourSortMode: 0,
  offsetSinglePoint: { x: 0, y: 0 },
  cameraBrightness: 50,
  offsetDualPointAArea: 0,
  cornerDetectionExactSideCount: false,
  cameraVideoModeIndex: 0,
  cornerDetectionStrategy: 0,
  cornerDetectionAccuracyPercentage: 10,
  hsvSaturation: { first: 50, second: 255 },
  contourIntersection: 1,

  // These settings will be overridden by different pipeline types
  cameraGain: -1,
  targetModel: -1,
  ledMode: false,
  outputShowMultipleTargets: false,
  cameraExposure: -1,
  pipelineType: -1
};

export interface ReflectivePipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.Reflective;
  contourFilterRangeY: number;
  contourFilterRangeX: number;
}
export type ConfigurableReflectivePipelineSettings = Partial<Omit<ReflectivePipelineSettings, "pipelineType">> &
  ConfigurablePipelineSettings;
export const DefaultReflectivePipelineSettings: ReflectivePipelineSettings = {
  ...DefaultPipelineSettings,
  cameraGain: 20,
  targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
  ledMode: true,
  outputShowMultipleTargets: false,
  cameraExposure: 6,
  pipelineType: PipelineType.Reflective,

  contourFilterRangeY: 2,
  contourFilterRangeX: 2
};

export interface ColoredShapePipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.ColoredShape;
  erode: boolean;
  cameraCalibration: null;
  dilate: boolean;
  circleAccuracy: number;
  contourRadius: WebsocketNumberPair | [number, number];
  circleDetectThreshold: number;
  accuracyPercentage: number;
  contourShape: number;
  contourPerimeter: WebsocketNumberPair | [number, number];
  minDist: number;
  maxCannyThresh: number;
}
export type ConfigurableColoredShapePipelineSettings = Partial<
  Omit<ColoredShapePipelineSettings, "pipelineType" | "erode" | "cameraCalibration" | "dilate" | "minDist">
> &
  ConfigurablePipelineSettings;
export const DefaultColoredShapePipelineSettings: ColoredShapePipelineSettings = {
  ...DefaultPipelineSettings,
  cameraGain: 75,
  targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
  ledMode: true,
  outputShowMultipleTargets: false,
  cameraExposure: 20,
  pipelineType: PipelineType.ColoredShape,

  erode: false,
  cameraCalibration: null,
  dilate: false,
  circleAccuracy: 20,
  contourRadius: { first: 0, second: 100 },
  circleDetectThreshold: 5,
  accuracyPercentage: 10,
  contourShape: 2,
  contourPerimeter: { first: 0, second: 1.7976931348623157e308 },
  minDist: 20,
  maxCannyThresh: 90
};

export interface AprilTagPipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.AprilTag;
  hammingDist: number;
  numIterations: number;
  decimate: number;
  blur: number;
  decisionMargin: number;
  refineEdges: boolean;
  debug: boolean;
  threads: number;
  tagFamily: AprilTagFamily;
}
export type ConfigurableAprilTagPipelineSettings = Partial<
  Omit<AprilTagPipelineSettings, "pipelineType" | "hammingDist" | "debug">
> &
  ConfigurablePipelineSettings;
export const DefaultAprilTagPipelineSettings: AprilTagPipelineSettings = {
  ...DefaultPipelineSettings,
  cameraGain: 75,
  targetModel: TargetModel.Apriltag6in_16h5,
  ledMode: false,
  outputShowMultipleTargets: true,
  cameraExposure: 20,
  pipelineType: PipelineType.AprilTag,

  hammingDist: 0,
  numIterations: 40,
  decimate: 1,
  blur: 0,
  decisionMargin: 35,
  refineEdges: true,
  debug: false,
  threads: 4,
  tagFamily: AprilTagFamily.Family16h5
};

export interface ArucoPipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.Aruco;
  threshMinSize: number;
  threshStepSize: number;
  threshMaxSize: number;
  threshConstant: number;
  debugThreshold: boolean;

  useCornerRefinement: boolean,

  useAruco3: boolean;
  aruco3MinMarkerSideRatio: number;
  aruco3MinCanonicalImgSide: number;
}
export type ConfigurableArucoPipelineSettings = Partial<Omit<ArucoPipelineSettings, "pipelineType">> &
  ConfigurablePipelineSettings;
export const DefaultArucoPipelineSettings: ArucoPipelineSettings = {
  ...DefaultPipelineSettings,
  outputShowMultipleTargets: true,
  targetModel: TargetModel.Aruco6in_16h5,
  cameraExposure: -1,
  cameraAutoExposure: true,
  ledMode: false,
  pipelineType: PipelineType.Aruco,

  threshMinSize: 11,
  threshStepSize: 40,
  threshMaxSize: 91,
  threshConstant: 10,
  debugThreshold: false,
  useCornerRefinement: true,
  useAruco3: false,
  aruco3MinMarkerSideRatio: 0.02,
  aruco3MinCanonicalImgSide: 32
};

export type ActivePipelineSettings =
  | ReflectivePipelineSettings
  | ColoredShapePipelineSettings
  | AprilTagPipelineSettings
  | ArucoPipelineSettings;
export type ActiveConfigurablePipelineSettings =
  | ConfigurableReflectivePipelineSettings
  | ConfigurableColoredShapePipelineSettings
  | ConfigurableAprilTagPipelineSettings
  | ConfigurableArucoPipelineSettings;
