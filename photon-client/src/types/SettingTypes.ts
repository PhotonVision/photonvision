import type {AprilTagPipeline, ColoredShapePipeline, ReflectivePipeline} from "@/types/PipelineTypes";

export type PhotonVersion = string

export interface GeneralSettings {
    version?: PhotonVersion
    gpuAcceleration?: string
    hardwareModel?: string
    hardwarePlatform?: string
}

export interface MetricData {
    cpuTemp?: string,
    cpuUtil?: string,
    cpuMem?: string,
    gpuMem?: string,
    ramUtil?: string,
    gpuMemUtil?: string,
}

export enum NetworkConnectionType {
    DHCP = 0,
    Static = 1
}

export interface NetworkSettings {
    ntServerAddress?: string
    supported: boolean,

    connectionType: NetworkConnectionType,
    staticIp?: string,
    hostname: string,
    runNTServer: boolean
}

export interface LightingSettings {
    supported: boolean,
    brightness: number
}


export enum LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
}

export interface LogMessage {
    level: LogLevel,
    message: string
}

interface Resolution {
    width: number,
    height: number
}

interface VideoFormat {
    resolution: Resolution
    fps: number,
    pixelFormat: "BGR" | "RGB" | "MJPEG" | "YUYV"
}

interface CameraCalibrationResult {
    resolution: Resolution
    standardDeviation: number,
    perViewErrors: number[],
    intrinsics: number[]
}

export interface CameraSettings {
    nickname: string,
    fov: {
        value: number,
        locked: boolean
    }
    stream: {
        inputPort: number,
        outputPort: number
    }

    validVideoFormats: VideoFormat[]
    completeCalibration: CameraCalibrationResult[]

    // Calibration data from the backend
    calibrationData: {
        imageCount: number,
        videoFormatIndex: number,
        minimumImageCount: number,
        hasEnoughImages: boolean
    }

    currentPipelineIndex: number
    pipelines: (ReflectivePipeline | ColoredShapePipeline | AprilTagPipeline)[]
}