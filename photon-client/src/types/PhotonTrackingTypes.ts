export interface Transform3d {
  x: number;
  y: number;
  z: number;
  qw: number;
  qx: number;
  qy: number;
  qz: number;
  angle_z: number;
}

export interface AprilTagFieldLayout {
  field: {
    length: number;
    width: number;
  };
  tags: {
    ID: number;
    pose: {
      translation: {
        x: number;
        y: number;
        z: number;
      };
      rotation: {
        quaternion: {
          X: number;
          Y: number;
          Z: number;
          W: number;
        };
      };
    };
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
  // undefined if 3d isn't enabled
  pose?: Transform3d;
}

export interface MultitagResult {
  bestTransform: Transform3d;
  bestReprojectionError: number;
  fiducialIDsUsed: number[];
}

export interface PipelineResult {
  fps: number;
  latency: number;
  targets: PhotonTarget[];
  // undefined if multitag failed or non-tag pipeline
  multitagResult?: MultitagResult;
}
