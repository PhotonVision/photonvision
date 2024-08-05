import type { WebsocketNumberPair } from "@/types/WebsocketDataTypes";

export enum PipelineType {
  DriverMode = 1,
  Reflective = 2,
  ColoredShape = 3,
  AprilTag = 4,
  Aruco = 5,
  ObjectDetection = 6
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
  StrongholdHighGoal = 0,
  DeepSpaceDualTarget = 1,
  InfiniteRechargeHighGoalOuter = 2,
  CircularPowerCell7in = 3,
  RapidReactCircularCargoBall = 4,
  AprilTag6in_16h5 = 5,
  AprilTag6p5in_36h11 = 6
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
// Omitted settings are changed for all pipeline types
export const DefaultPipelineSettings: Omit<
  PipelineSettings,
  "cameraGain" | "targetModel" | "ledMode" | "outputShowMultipleTargets" | "cameraExposure" | "pipelineType"
> = {
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
  contourIntersection: 1
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
  doMultiTarget: boolean;
  doSingleTargetAlways: boolean;
}
export type ConfigurableAprilTagPipelineSettings = Partial<
  Omit<AprilTagPipelineSettings, "pipelineType" | "hammingDist" | "debug">
> &
  ConfigurablePipelineSettings;
export const DefaultAprilTagPipelineSettings: AprilTagPipelineSettings = {
  ...DefaultPipelineSettings,
  cameraGain: 75,
  targetModel: TargetModel.AprilTag6p5in_36h11,
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
  tagFamily: AprilTagFamily.Family36h11,
  doMultiTarget: false,
  doSingleTargetAlways: false
};

export interface ArucoPipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.Aruco;

  tagFamily: AprilTagFamily;

  threshWinSizes: WebsocketNumberPair | [number, number];
  threshStepSize: number;
  threshConstant: number;
  debugThreshold: boolean;

  useCornerRefinement: boolean;

  useAruco3: boolean;
  aruco3MinMarkerSideRatio: number;
  aruco3MinCanonicalImgSide: number;

  doMultiTarget: boolean;
  doSingleTargetAlways: boolean;
}
export type ConfigurableArucoPipelineSettings = Partial<Omit<ArucoPipelineSettings, "pipelineType">> &
  ConfigurablePipelineSettings;
export const DefaultArucoPipelineSettings: ArucoPipelineSettings = {
  ...DefaultPipelineSettings,
  cameraGain: 75,
  outputShowMultipleTargets: true,
  targetModel: TargetModel.AprilTag6p5in_36h11,
  cameraExposure: -1,
  cameraAutoExposure: true,
  ledMode: false,
  pipelineType: PipelineType.Aruco,

  tagFamily: AprilTagFamily.Family36h11,
  threshWinSizes: { first: 11, second: 91 },
  threshStepSize: 40,
  threshConstant: 10,
  debugThreshold: false,
  useCornerRefinement: true,
  useAruco3: false,
  aruco3MinMarkerSideRatio: 0.02,
  aruco3MinCanonicalImgSide: 32,
  doMultiTarget: false,
  doSingleTargetAlways: false
};

export interface ObjectDetectionPipelineSettings extends PipelineSettings {
  pipelineType: PipelineType.ObjectDetection;
  confidence: number;
  nms: number;
  box_thresh: number;
  model: string;
}
export type ConfigurableObjectDetectionPipelineSettings = Partial<
  Omit<ObjectDetectionPipelineSettings, "pipelineType">
> &
  ConfigurablePipelineSettings;
export const DefaultObjectDetectionPipelineSettings: ObjectDetectionPipelineSettings = {
  ...DefaultPipelineSettings,
  pipelineType: PipelineType.ObjectDetection,
  cameraGain: 20,
  targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
  ledMode: true,
  outputShowMultipleTargets: false,
  cameraExposure: 6,
  confidence: 0.9,
  nms: 0.45,
  box_thresh: 0.25,
  model: "",
};

export type ActivePipelineSettings =
  | ReflectivePipelineSettings
  | ColoredShapePipelineSettings
  | AprilTagPipelineSettings
  | ArucoPipelineSettings
  | ObjectDetectionPipelineSettings;

export type ActiveConfigurablePipelineSettings =
  | ConfigurableReflectivePipelineSettings
  | ConfigurableColoredShapePipelineSettings
  | ConfigurableAprilTagPipelineSettings
  | ConfigurableArucoPipelineSettings
  | ConfigurableObjectDetectionPipelineSettings;
