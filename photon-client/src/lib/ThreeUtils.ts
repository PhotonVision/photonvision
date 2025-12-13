import type { JsonMatOfDouble, Resolution } from "@/types/SettingTypes";
const { PerspectiveCamera } = await import("three");

/**
 * Convert a camera intrinsics matrix and image resolution to a Three.js PerspectiveCamera. This assumes no skew and square pixels (same focal length in x and y), which is a sane assumption for most FRC cameras
 *
 * @param resolution video mode width/height
 * @param intrinsicsCore camera intrinsics from the backend, row-major
 * @returns a Three.js PerspectiveCamera matching the provided intrinsics
 */
export const createPerspectiveCamera = (
  resolution: Resolution,
  intrinsicsCore: JsonMatOfDouble,
  frustumMax: number = 1
) => {
  const imageWidth = resolution.width;
  const imageHeight = resolution.height;
  const focalLengthY = intrinsicsCore.data[4];
  const fovY = 2 * Math.atan(imageHeight / (2 * focalLengthY)) * (180 / Math.PI);
  const aspect = imageWidth / imageHeight;

  return new PerspectiveCamera(fovY, aspect, 0.1, frustumMax);
};
