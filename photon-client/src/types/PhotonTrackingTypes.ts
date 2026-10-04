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

export interface FieldDimensions {
  length: number;
  width: number;
}

export interface FieldTag {
  ID: number;
  pose: Pose3d;
}

export interface Field {
  "field-dimensions": FieldDimensions;
  "field-tags": FieldTag[] | null;
}
