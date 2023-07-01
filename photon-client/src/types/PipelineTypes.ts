enum PipelineType {
    Reflective=2,
    Colored=3,
    AprilTag=4
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

export interface ReflectivePipelineSettings extends PipelineSettings {
    contourFilterRangeY: number
    contourFilterRangeX: number
}
export type ConfigurableReflectivePipelineSettings = Partial<ReflectivePipelineSettings> & ConfigurablePipelineSettings

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

export interface AprilTagPipelineSettings extends PipelineSettings {
    hammingDist: number
    numIterations: number
    decimate: number
    blur: number
    decisionMargin: number
    refineEdges: boolean
    debug: boolean
    threads: number
    tagFamily: number
}
// Omitted things are based on previous usage and I have no clue whether they should or shouldn't be
export type ConfigurableAprilTagPipelineSettings = Partial<Omit<AprilTagPipelineSettings, "hammingDist" | "debug">> & ConfigurablePipelineSettings
