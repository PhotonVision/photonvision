export interface Pose {
    x: number,
    y: number,
    z: number,
    angle_z: number,
    qw: number,
    qx: number,
    qy: number,
    qz: number
}

export interface PhotonTarget {
    yaw: number,
    pitch: number,
    skew: number,
    area: number,
    // -1 if not set
    ambiguity: number,
    // -1 if not set
    fiducialId: number,
    // undefined if 3d isn't enabled
    pose?: Pose
}

export interface PipelineResult {
    fps: number,
    latency: number,
    targets: PhotonTarget[]
}
