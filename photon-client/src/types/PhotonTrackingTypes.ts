export interface Quaternion {
  X: number;
  Y: number;
  Z: number;
  W: number;
}

export interface Translation3d {
  x: number;
  y: number;
  z: number;
}

export interface Rotation3d {
  quaternion: Quaternion;
}

export interface Pose3d {
  translation: Translation3d;
  rotation: Rotation3d;
}

// TODO update backend to serialize this using correct layout
export interface Transform3d {
  x: number;
  y: number;
  z: number;
  qw: number;
  qx: number;
  qy: number;
  qz: number;
  angle_x: number;
  angle_y: number;
  angle_z: number;
}

export interface AprilTagFieldLayout {
  field: {
    length: number;
    width: number;
  };
  tags: {
    ID: number;
    pose: Pose3d;
  }[];
}

export interface PhotonTarget {
  yaw: number;
  pitch: number;
  skew: number;
  area: number;
  // -1 if not set
  ambiguity: number;
  // -1 if not set
  fiducialId: number;
  confidence: number;
  classId: number;
  // undefined if 3d isn't enabled
  pose?: Transform3d;
}

export interface MultitagResult {
  bestTransform: Transform3d;
  bestReprojectionError: number;
  fiducialIDsUsed: number[];
}

export interface PipelineResult {
  sequenceID: number;
  fps: number;
  latency: number;
  targets: PhotonTarget[];
  // undefined if multitag failed or non-tag pipeline
  multitagResult?: MultitagResult;
  // Object detection class names -- empty if not doing object detection
  classNames: string[];
}
