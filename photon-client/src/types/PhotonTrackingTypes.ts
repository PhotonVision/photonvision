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
  angle_x?: number;
  angle_y?: number;
  angle_z?: number;
}

export interface Pose3d {
  translation: Translation3d;
  rotation: Rotation3d;
}

// TODO update backend to serialize this using correct layout
export interface Transform3d {
  translation: Translation3d;
  rotation: Rotation3d;
}

export interface Apriltag {
  ID: number;
  pose: Pose3d;
}

export interface AprilTagFieldLayout {
  field: {
    length: number;
    width: number;
  };
  tags: Apriltag[];
}

export interface TrackedTarget {
  yaw: number;
  pitch: number;
  skew: number;
  area: number;
}

export interface TagTrackedTarget extends TrackedTarget {
  fiducialId: number;
  ambiguity: number;
  // undefined if 3d isn't enabled
  bestTransform?: Transform3d;
}

export interface ObjectDetectionTrackedTarget extends TrackedTarget {
  confidence: number;
  classId: number;
}

export type PhotonTarget = TrackedTarget | TagTrackedTarget | ObjectDetectionTrackedTarget;

export interface MultitagResult {
  bestTransform: Transform3d;
  bestReprojectionError: number;
  fiducialIDsUsed: number[];
}

export interface PipelineResult {
  fps: number;
  latency: number;
  targets: TrackedTarget[] | TagTrackedTarget[] | ObjectDetectionTrackedTarget[];
  // undefined if multitag failed or non-tag pipeline
  multitagResult?: MultitagResult;
  // Object detection class names -- empty if not doing object detection
  classNames: string[];
}
