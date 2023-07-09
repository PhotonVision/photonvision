export enum PipelineType {
    Reflective=2,
    Colored=3,
    AprilTag=4
}

enum AprilTagFamily {
    Family36h11=0,
    Family25h9=1,
    Family16h5=2
}

// driver mode index -1
// calibration mode index -2

// TODO, determine Readonly values

export interface PipelineSettings {
    offsetRobotOffsetMode: number
    streamingFrameDivisor: number
    offsetDualPointBArea: number
    contourGroupingMode: number
    hsvValue: {first: number, second: number} | [number, number]
    cameraGain: number
    cameraBlueGain: number
    cameraRedGain: number
    cornerDetectionSideCount: number
    contourRatio: {first: number, second: number} | [number, number]
    contourTargetOffsetPointEdge: number
    pipelineNickname: string
    inputImageRotationMode: number
    contourArea: {first: number, second: number} | [number, number]
    solvePNPEnabled: boolean
    contourFullness: {first: number, second: number} | [number, number]
    pipelineIndex: number
    inputShouldShow: boolean
    cameraAutoExposure: boolean
    contourSpecklePercentage: number
    contourTargetOrientation: number
    targetModel: number
    cornerDetectionUseConvexHulls: boolean
    outputShouldShow: boolean
    outputShouldDraw: boolean
    offsetDualPointA: {x: number, y: number} | [number, number]
    offsetDualPointB: {x: number, y: number} | [number, number]
    hsvHue: {first: number, second: number}
    ledMode: boolean
    hueInverted: boolean
    outputShowMultipleTargets: boolean
    contourSortMode: number
    cameraExposure: number
    offsetSinglePoint: {x: number, y: number}
    cameraBrightness: number
    offsetDualPointAArea: number
    cornerDetectionExactSideCount: boolean
    cameraVideoModeIndex: number
    cornerDetectionStrategy: number
    cornerDetectionAccuracyPercentage: number
    hsvSaturation: {first: number, second: number} | [number, number]
    pipelineType: PipelineType
    contourIntersection: number
}
// Omitted things are based on previous usage and I have no clue whether they should or shouldn't be
export type ConfigurablePipelineSettings = Partial<Omit<PipelineSettings, "offsetDualPointAArea" | "cornerDetectionSideCount" | "pipelineNickname" | "pipelineIndex" | "cornerDetectionUseConvexHulls" | "offsetDualPointA" | "offsetDualPointB" | "ledMode" | "offsetSinglePoint" | "offsetDualPointBArea" | "cornerDetectionExactSideCount" | "cornerDetectionStrategy">>
export const DefaultPipelineSettings: PipelineSettings = {
    offsetRobotOffsetMode: 0,
    streamingFrameDivisor: 0,
    offsetDualPointBArea: 0,
    contourGroupingMode: 0,
    hsvValue: {"first": 50, "second": 255},
    cameraBlueGain: 20,
    cameraRedGain: 11,
    cornerDetectionSideCount: 4,
    contourRatio: {"first": 0, "second": 20},
    contourTargetOffsetPointEdge: 0,
    pipelineNickname: "Placeholder Pipeline",
    inputImageRotationMode: 0,
    contourArea: {"first": 0, "second": 100},
    solvePNPEnabled: false,
    contourFullness: {"first": 0, "second": 100},
    pipelineIndex: 0,
    inputShouldShow: false,
    cameraAutoExposure: false,
    contourSpecklePercentage: 5,
    contourTargetOrientation: 1,
    cornerDetectionUseConvexHulls: true,
    outputShouldShow: true,
    outputShouldDraw: true,
    offsetDualPointA: {"x": 0, "y": 0},
    offsetDualPointB: {"x": 0, "y": 0},
    hsvHue: {"first": 50, "second": 180},
    hueInverted: false,
    contourSortMode: 0,
    offsetSinglePoint: {"x": 0, "y": 0},
    cameraBrightness: 50,
    offsetDualPointAArea: 0,
    cornerDetectionExactSideCount: false,
    cameraVideoModeIndex: 0,
    cornerDetectionStrategy: 0,
    cornerDetectionAccuracyPercentage: 10,
    hsvSaturation: {"first": 50, "second": 255},
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
    contourFilterRangeY: number
    contourFilterRangeX: number
}
export type ConfigurableReflectivePipelineSettings = Partial<ReflectivePipelineSettings> & ConfigurablePipelineSettings
export const DefaultReflectivePipelineSettings: ReflectivePipelineSettings = {
    ...DefaultPipelineSettings,
    cameraGain: 20,
    targetModel: 0,
    ledMode: true,
    outputShowMultipleTargets: false,
    cameraExposure: 6,
    pipelineType: PipelineType.Reflective,

    contourFilterRangeY: 2,
    contourFilterRangeX: 2
};

export interface ColoredShapePipelineSettings extends PipelineSettings {
    erode: boolean
    cameraCalibration: null
    dilate: boolean
    circleAccuracy: number
    contourRadius: {first: number, second: number} | [number, number]
    circleDetectThreshold: number
    accuracyPercentage: number
    contourShape: number
    contourPerimeter: {first: number, second: number} | [number, number]
    minDist: number
    maxCannyThresh: number
}
// Omitted things are based on previous usage and I have no clue whether they should or shouldn't be
export type ConfigurableColoredShapePipelineSettings = Partial<Omit<ColoredShapePipelineSettings, "erode" | "cameraCalibration" | "dilate" | "circleAccuracy" | "minDist" >> & ConfigurablePipelineSettings
export const DefaultColoredShapePipelineSettings: ColoredShapePipelineSettings = {
    ...DefaultPipelineSettings,
    cameraGain: 75,
    targetModel: 0,
    ledMode: true,
    outputShowMultipleTargets: false,
    cameraExposure: 20,
    pipelineType: PipelineType.Colored,

    erode: false,
    cameraCalibration: null,
    dilate: false,
    circleAccuracy: 20,
    contourRadius: {"first": 0, "second": 100},
    circleDetectThreshold: 5,
    accuracyPercentage: 10,
    contourShape: 2,
    contourPerimeter: {"first": 0, "second": 1.7976931348623157e+308},
    minDist: 20,
    maxCannyThresh: 90
};

export interface AprilTagPipelineSettings extends PipelineSettings {
    hammingDist: number
    numIterations: number
    decimate: number
    blur: number
    decisionMargin: number
    refineEdges: boolean
    debug: boolean
    threads: number
    tagFamily: AprilTagFamily
}
// Omitted things are based on previous usage and I have no clue whether they should or shouldn't be
export type ConfigurableAprilTagPipelineSettings = Partial<Omit<AprilTagPipelineSettings, "hammingDist" | "debug">> & ConfigurablePipelineSettings
export const DefaultAprilTagPipelineSettings: AprilTagPipelineSettings = {
    ...DefaultPipelineSettings,
    cameraGain: 75,
    targetModel: 8,
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
