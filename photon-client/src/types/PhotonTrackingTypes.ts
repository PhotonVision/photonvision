export interface Pose {
    x: number,
    y: number,
    z: number,
    qw: number,
    qx: number,
    qy: number,
    qz: number
}

export interface PhotonTarget {
    pitch: number,
    yaw: number,
    skew: number,
    area: number,
    pose?: Pose
}

export interface PipelineResult {
    fps: number,
    latency: number,
    targets: PhotonTarget[]
}
