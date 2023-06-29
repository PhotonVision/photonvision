export interface Pipeline {
    cameraExposure: number,
    cameraBrightness: number,
    cameraAutoExposure: boolean,
    cameraRedGain: number,
    cameraBlueGain: number,
    inputImageRotationMode: number,
    cameraVideoModeIndex: number,
    streamingFrameDivisor: number,

    nickname: string
}

export type CalibrationPipeline = Pipeline

export type DriverModePipeline = Pipeline

export interface ReflectivePipeline extends Pipeline {
    hsvHue: [number, number],
    hsvSaturation: [number, number],
    hsvValue: [number, number],
    hueInverted: boolean,
    contourArea: [number, number],
    contourRatio: [number, number],
    contourFullness: [number, number],
    contourSpecklePercentage: number,
    contourFilterRangeX: number
    contourFilterRangeY: number,
    contourGroupingMode: number,
    contourIntersection: number,
    contourSortMode: number,
    inputShouldShow: boolean,
    outputShouldShow: boolean,
    outputShouldDraw: boolean,
    outputShowMultipleTargets: boolean,
    offsetRobotOffsetMode: number
    solvePNPEnabled: boolean,
    targetRegion: number,
    contourTargetOrientation: number,
    cornerDetectionAccuracyPercentage: number
}

export type ColoredShapePipeline = Pipeline

export interface AprilTagPipeline extends Pipeline {
    tagFamily: number,
    decimate: number,
    blur: number,
    threads: number,
    debug: boolean,
    refineEdges: boolean,
    numIterations: number,
    decisionMargin: number,
    hammingDist: number,
}