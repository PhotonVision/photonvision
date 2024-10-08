import { type WebsocketNumberPair } from "@/types/WebsocketTypes";
import {
  CalibrationBoardTypes,
  CalibrationTagFamilies,
  CvPoint,
  Resolution
} from "@/types/SettingTypes";

export enum PipelineType {
  // eslint-disable-next-line no-unused-vars
  Calib3d = -2,
  // eslint-disable-next-line no-unused-vars
  DriverMode = -1,
  // eslint-disable-next-line no-unused-vars
  Reflective = 0,
  // eslint-disable-next-line no-unused-vars
  ColoredShape = 1,
  // eslint-disable-next-line no-unused-vars
  AprilTag = 2,
  // eslint-disable-next-line no-unused-vars
  Aruco = 3,
  // eslint-disable-next-line no-unused-vars
  ObjectDetection = 4
}
export enum ImageRotationMode {
  // eslint-disable-next-line no-unused-vars
  Deg0 = -1,
  // eslint-disable-next-line no-unused-vars
  Deg90 = 0,
  // eslint-disable-next-line no-unused-vars
  Deg180 = 1,
  // eslint-disable-next-line no-unused-vars
  Deg270 = 2
}
export enum FrameDivisor {
  // eslint-disable-next-line no-unused-vars
  NONE = 1,
  // eslint-disable-next-line no-unused-vars
  HALF = 2,
  // eslint-disable-next-line no-unused-vars
  QUARTER = 4,
  // eslint-disable-next-line no-unused-vars
  SIXTH = 6
}
interface PipelineSettings {
  pipelineIndex: number;
  pipelineType: PipelineType;
  inputImageRotationMode: ImageRotationMode;
  pipelineNickname: string;
  cameraAutoExposure: boolean;
  cameraExposure: number;
  cameraBrightness: number;
  cameraGain: number;
  cameraRedGain: number;
  cameraBlueGain: number;
  cameraVideoModeIndex: number;
  streamingFrameDivisor: FrameDivisor;
  ledMode: boolean;
  inputShouldShow: boolean;
  outputShouldShow: boolean;
}
export interface DriverModePipelineSettings extends PipelineSettings {
  pipelineIndex: -1;
  pipelineNickname: "Driver Mode";
  pipelineType: PipelineType.DriverMode;
  inputShouldShow: true;
}
export enum TargetModel {
  // eslint-disable-next-line no-unused-vars
  StrongholdHighGoal = 0,
  // eslint-disable-next-line no-unused-vars
  DeepSpaceDualTarget = 1,
  // eslint-disable-next-line no-unused-vars
  InfiniteRechargeHighGoalOuter = 2,
  // eslint-disable-next-line no-unused-vars
  CircularPowerCell7in = 3,
  // eslint-disable-next-line no-unused-vars
  RapidReactCircularCargoBall = 4,
  // eslint-disable-next-line no-unused-vars
  AprilTag6in_16h5 = 5,
  // eslint-disable-next-line no-unused-vars
  AprilTag6p5in_36h11 = 6
}
export enum ContourSortMode {
  // eslint-disable-next-line no-unused-vars
  Largest,
  // eslint-disable-next-line no-unused-vars
  Smallest,
  // eslint-disable-next-line no-unused-vars
  Highest,
  // eslint-disable-next-line no-unused-vars
  Lowest,
  // eslint-disable-next-line no-unused-vars
  Leftmost,
  // eslint-disable-next-line no-unused-vars
  Rightmost,
  // eslint-disable-next-line no-unused-vars
  Centermost
}
export enum TargetOffsetPointEdge {
  // eslint-disable-next-line no-unused-vars
  Center,
  // eslint-disable-next-line no-unused-vars
  Top,
  // eslint-disable-next-line no-unused-vars
  Bottom,
  // eslint-disable-next-line no-unused-vars
  Left,
  // eslint-disable-next-line no-unused-vars
  Right
}
export enum TargetOrientation {
  // eslint-disable-next-line no-unused-vars
  Portrait,
  // eslint-disable-next-line no-unused-vars
  Landscape
}
export enum ContourIntersectionDirection {
  // eslint-disable-next-line no-unused-vars
  None,
  // eslint-disable-next-line no-unused-vars
  Up,
  // eslint-disable-next-line no-unused-vars
  Down,
  // eslint-disable-next-line no-unused-vars
  Left,
  // eslint-disable-next-line no-unused-vars
  Right
}
export enum RobotOffsetPointMode {
  // eslint-disable-next-line no-unused-vars
  None = 0,
  // eslint-disable-next-line no-unused-vars
  Single = 1,
  // eslint-disable-next-line no-unused-vars
  Dual = 2
}
export interface AdvancedPipelineSettings extends PipelineSettings {
  hsvHue: WebsocketNumberPair | [number, number];
  hsvSaturation: WebsocketNumberPair | [number, number];
  hsvValue: WebsocketNumberPair | [number, number];
  hueInverted: boolean;

  outputShouldDraw: boolean;
  outputShowMultipleTargets: boolean;

  contourArea: WebsocketNumberPair | [number, number];
  contourRatio: WebsocketNumberPair | [number, number];
  contourFullness: WebsocketNumberPair | [number, number];
  contourSpecklePercentage: number;

  contourSortMode: ContourSortMode;

  contourTargetOffsetPointEdge: TargetOffsetPointEdge;

  contourTargetOrientation: TargetOrientation;

  offsetRobotOffsetMode: RobotOffsetPointMode;

  offsetSinglePoint: CvPoint;

  offsetDualPointA: CvPoint;
  offsetDualPointAArea: number;
  offsetDualPointB: CvPoint;
  offsetDualPointBArea: number;

  // See ContourGroupingMode
  contourGroupingMode: number;

  contourIntersection: ContourIntersectionDirection;

  solvePNPEnabled: boolean;
  targetModel: TargetModel;

  cornerDetectionStrategy: number;
  cornerDetectionUseConvexHulls: boolean;
  cornerDetectionExactSideCount: boolean;
  cornerDetectionSideCount: number;
  cornerDetectionAccuracyPercentage: number;
}
export interface Calibration3dPipelineSettings extends AdvancedPipelineSettings {
  pipelineIndex: -2;
  pipelineType: PipelineType.Calib3d;
  // inches
  boardHeight: number;
  // inches
  boardWidth: number;
  boardType: CalibrationBoardTypes;
  tagFamily: CalibrationTagFamilies;
  // meters
  gridSize: number;
  // meters
  markerSize: number;
  resolution: Resolution;
  useMrCal: boolean;
  useOldPattern: boolean;
}
export interface ReflectivePipelineSettings extends AdvancedPipelineSettings {
  pipelineType: PipelineType.Reflective;
  contourFilterRangeY: number;
  contourFilterRangeX: number;
}
export enum ContourShape {
  // eslint-disable-next-line no-unused-vars
  Circle = 0,
  // eslint-disable-next-line no-unused-vars
  Custom = -1,
  // eslint-disable-next-line no-unused-vars
  Triangle = 3,
  // eslint-disable-next-line no-unused-vars
  Quadrilateral = 4
}
export interface ColoredShapePipelineSettings extends AdvancedPipelineSettings {
  pipelineType: PipelineType.ColoredShape;
  contourShape: ContourShape;
  contourPerimeter: WebsocketNumberPair | [number, number];
  accuracyPercentage: number;
  circleDetectThreshold: number;
  contourRadius: WebsocketNumberPair | [number, number];
  minDist: number;
  maxCannyThresh: number;
  circleAccuracy: number;
  // erode: boolean;
  // dilate: boolean;
}
export enum AprilTagFamily {
  // eslint-disable-next-line no-unused-vars
  kTag36h11,
  // eslint-disable-next-line no-unused-vars
  kTag25h9,
  // eslint-disable-next-line no-unused-vars
  kTag16h5,
  // eslint-disable-next-line no-unused-vars
  kTagCircle21h7,
  // eslint-disable-next-line no-unused-vars
  kTagCircle49h12,
  // eslint-disable-next-line no-unused-vars
  kTagStandard41h12,
  // eslint-disable-next-line no-unused-vars
  kTagStandard52h13,
  // eslint-disable-next-line no-unused-vars
  kTagCustom48h11
}
export interface AprilTagPipelineSettings extends AdvancedPipelineSettings {
  pipelineType: PipelineType.AprilTag;
  decimate: number;
  blur: number;
  threads: number;
  debug: boolean;
  refineEdges: boolean;
  numIterations: number;
  hammingDist: number;
  decisionMargin: number;
  tagFamily: AprilTagFamily;
  doMultiTarget: boolean;
  doSingleTargetAlways: boolean;
}
export interface ArucoPipelineSettings extends AdvancedPipelineSettings {
  pipelineType: PipelineType.Aruco;

  tagFamily: AprilTagFamily;

  threshWinSizes: WebsocketNumberPair | [number, number];
  threshStepSize: number;
  threshConstant: number;
  debugThreshold: boolean;

  useCornerRefinement: boolean;
  refineNumIterations: number;
  refineMinErrorPx: number;

  useAruco3: boolean;
  aruco3MinMarkerSideRatio: number;
  aruco3MinCanonicalImgSide: number;

  doMultiTarget: boolean;
  doSingleTargetAlways: boolean;
}
export interface ObjectDetectionPipelineSettings extends AdvancedPipelineSettings {
  pipelineType: PipelineType.ObjectDetection;
  confidence: number;
  nms: number;
}

type ReadonlyPipelineKeys = "pipelineNickname" | "pipelineIndex" | "pipelineType" | "ledMode";
type ReadonlyAdvancedPipelineKeys =
  | "offsetDualPointAArea"
  | "cornerDetectionSideCount"
  | "cornerDetectionUseConvexHulls"
  | "offsetDualPointA"
  | "offsetDualPointB"
  | "offsetSinglePoint"
  | "offsetDualPointBArea"
  | "cornerDetectionExactSideCount"
  | "cornerDetectionStrategy";

type ConfigurablePipelineSettings<T, K extends keyof any = never> = Omit<T, K | ReadonlyAdvancedPipelineKeys | ReadonlyPipelineKeys>

export type ConfigurableReflectivePipelineSettings = ConfigurablePipelineSettings<ReflectivePipelineSettings>
export type ConfigurableColoredShapePipelineSettings = ConfigurablePipelineSettings<ColoredShapePipelineSettings, "minDist">;
export type ConfigurableAprilTagPipelineSettings = ConfigurablePipelineSettings<AprilTagPipelineSettings, "hammingDist" | "debug">;
export type ConfigurableArucoPipelineSettings = ConfigurablePipelineSettings<ArucoPipelineSettings>
export type ConfigurableObjectDetectionPipelineSettings = ConfigurablePipelineSettings<ObjectDetectionPipelineSettings>

export type UserPipelineSettings =
  | ReflectivePipelineSettings
  | ColoredShapePipelineSettings
  | AprilTagPipelineSettings
  | ArucoPipelineSettings
  | ObjectDetectionPipelineSettings;
export type PossiblePipelineSettings =
  | UserPipelineSettings
  | DriverModePipelineSettings
  | Calibration3dPipelineSettings;
export type ConfigurableUserPipelineSettings =
  | ConfigurableReflectivePipelineSettings
  | ConfigurableColoredShapePipelineSettings
  | ConfigurableAprilTagPipelineSettings
  | ConfigurableArucoPipelineSettings
  | ConfigurableObjectDetectionPipelineSettings;

export type UserPipelineType = Exclude<PipelineType, PipelineType.DriverMode | PipelineType.Calib3d>;

// export const DefaultPipelineSettings: Omit<
//   PipelineSettings,
//   "cameraGain" | "targetModel" | "ledMode" | "outputShowMultipleTargets" | "cameraExposure" | "pipelineType"
// > = {
//   offsetRobotOffsetMode: RobotOffsetPointMode.None,
//   streamingFrameDivisor: 0,
//   offsetDualPointBArea: 0,
//   contourGroupingMode: 0,
//   hsvValue: { first: 50, second: 255 },
//   cameraBlueGain: 20,
//   cameraRedGain: 11,
//   cornerDetectionSideCount: 4,
//   contourRatio: { first: 0, second: 20 },
//   contourTargetOffsetPointEdge: 0,
//   pipelineNickname: "Placeholder Pipeline",
//   inputImageRotationMode: 0,
//   contourArea: { first: 0, second: 100 },
//   solvePNPEnabled: true,
//   contourFullness: { first: 0, second: 100 },
//   pipelineIndex: 0,
//   inputShouldShow: false,
//   cameraAutoExposure: false,
//   contourSpecklePercentage: 5,
//   contourTargetOrientation: 1,
//   cornerDetectionUseConvexHulls: true,
//   outputShouldShow: true,
//   outputShouldDraw: true,
//   offsetDualPointA: { x: 0, y: 0 },
//   offsetDualPointB: { x: 0, y: 0 },
//   hsvHue: { first: 50, second: 180 },
//   hueInverted: false,
//   contourSortMode: 0,
//   offsetSinglePoint: { x: 0, y: 0 },
//   cameraBrightness: 50,
//   offsetDualPointAArea: 0,
//   cornerDetectionExactSideCount: false,
//   cameraVideoModeIndex: 0,
//   cornerDetectionStrategy: 0,
//   cornerDetectionAccuracyPercentage: 10,
//   hsvSaturation: { first: 50, second: 255 },
//   contourIntersection: 1
// };
// export const DefaultReflectivePipelineSettings: ReflectivePipelineSettings = {
//   ...DefaultPipelineSettings,
//   cameraGain: 20,
//   targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
//   ledMode: true,
//   outputShowMultipleTargets: false,
//   cameraExposure: 6,
//   pipelineType: PipelineType.Reflective,
//
//   contourFilterRangeY: 2,
//   contourFilterRangeX: 2
// };
// export const DefaultColoredShapePipelineSettings: ColoredShapePipelineSettings = {
//   ...DefaultPipelineSettings,
//   cameraGain: 75,
//   targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
//   ledMode: true,
//   outputShowMultipleTargets: false,
//   cameraExposure: 20,
//   pipelineType: PipelineType.ColoredShape,
//
//   erode: false,
//   cameraCalibration: null,
//   dilate: false,
//   circleAccuracy: 20,
//   contourRadius: { first: 0, second: 100 },
//   circleDetectThreshold: 5,
//   accuracyPercentage: 10,
//   contourShape: 2,
//   contourPerimeter: { first: 0, second: 1.7976931348623157e308 },
//   minDist: 20,
//   maxCannyThresh: 90
// };
// export const DefaultAprilTagPipelineSettings: AprilTagPipelineSettings = {
//   ...DefaultPipelineSettings,
//   cameraGain: 75,
//   targetModel: TargetModel.AprilTag6p5in_36h11,
//   ledMode: false,
//   outputShowMultipleTargets: true,
//   cameraExposure: 20,
//   pipelineType: PipelineType.AprilTag,
//
//   hammingDist: 0,
//   numIterations: 40,
//   decimate: 1,
//   blur: 0,
//   decisionMargin: 35,
//   refineEdges: true,
//   debug: false,
//   threads: 4,
//   tagFamily: AprilTagFamily.Family36h11,
//   doMultiTarget: false,
//   doSingleTargetAlways: false
// };
// export const DefaultArucoPipelineSettings: ArucoPipelineSettings = {
//   ...DefaultPipelineSettings,
//   cameraGain: 75,
//   outputShowMultipleTargets: true,
//   targetModel: TargetModel.AprilTag6p5in_36h11,
//   cameraExposure: -1,
//   cameraAutoExposure: true,
//   ledMode: false,
//   pipelineType: PipelineType.Aruco,
//
//   tagFamily: AprilTagFamily.Family36h11,
//   threshWinSizes: { first: 11, second: 91 },
//   threshStepSize: 40,
//   threshConstant: 10,
//   debugThreshold: false,
//   useCornerRefinement: true,
//   useAruco3: false,
//   aruco3MinMarkerSideRatio: 0.02,
//   aruco3MinCanonicalImgSide: 32,
//   doMultiTarget: false,
//   doSingleTargetAlways: false
// };
// export const DefaultObjectDetectionPipelineSettings: ObjectDetectionPipelineSettings = {
//   ...DefaultPipelineSettings,
//   pipelineType: PipelineType.ObjectDetection,
//   cameraGain: 20,
//   targetModel: TargetModel.InfiniteRechargeHighGoalOuter,
//   ledMode: true,
//   outputShowMultipleTargets: false,
//   cameraExposure: 6,
//   confidence: 0.9,
//   nms: 0.45,
//   box_thresh: 0.25
// };

// export type ActivePipelineSettings =
//   | ReflectivePipelineSettings
//   | ColoredShapePipelineSettings
//   | AprilTagPipelineSettings
//   | ArucoPipelineSettings
//   | ObjectDetectionPipelineSettings;
//
// export type ActiveConfigurablePipelineSettings =
//   | ConfigurableReflectivePipelineSettings
//   | ConfigurableColoredShapePipelineSettings
//   | ConfigurableAprilTagPipelineSettings
//   | ConfigurableArucoPipelineSettings
//   | ConfigurableObjectDetectionPipelineSettings;
