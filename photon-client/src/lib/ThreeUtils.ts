import type { JsonMatOfDouble, Resolution } from "@/types/SettingTypes";
import type { Mesh, Object3D, PerspectiveCamera, Scene, WebGLRenderer } from "three";
import type { TrackballControls } from "three/examples/jsm/controls/TrackballControls.js";
const three = import("three");

/**
 * Convert a camera intrinsics matrix and image resolution to a Three.js PerspectiveCamera. This assumes no skew and square pixels (same focal length in x and y), which is a sane assumption for most FRC cameras
 *
 * @param resolution video mode width/height
 * @param intrinsicsCore camera intrinsics from the backend, row-major
 * @returns a Three.js PerspectiveCamera matching the provided intrinsics
 */
export const createPerspectiveCamera = async (
  resolution: Resolution,
  intrinsicsCore: JsonMatOfDouble,
  frustumMax: number = 1
) => {
  const { PerspectiveCamera } = await three;
  const imageWidth = resolution.width;
  const imageHeight = resolution.height;
  const focalLengthY = intrinsicsCore.data[4];
  const fovY = 2 * Math.atan(imageHeight / (2 * focalLengthY)) * (180 / Math.PI);
  const aspect = imageWidth / imageHeight;

  return new PerspectiveCamera(fovY, aspect, 0.1, frustumMax);
};

/**
 * A camera position/up/target triplet used to reset the visualizer camera
 */
export interface VisualizerCameraPose {
  position: [number, number, number];
  up: [number, number, number];
  target: [number, number, number];
}

/**
 * The three.js objects handed to consumers of the shared three.js visualizer
 * once the scene has been mounted
 */
export interface ThreeVisualizerContext {
  scene: Scene;
  camera: PerspectiveCamera;
  renderer: WebGLRenderer;
  controls: TrackballControls;
}

/**
 * Dispose of all geometries and materials belonging to an object and its
 * descendants. Removing an object from a scene does not free its GPU
 * resources -- geometries and materials must be disposed explicitly.
 *
 * @param root object (sub)tree to dispose
 */
export const disposeObject = (root: Object3D): void => {
  root.traverse((object) => {
    const maybeMesh = object as Partial<Mesh>;
    maybeMesh.geometry?.dispose();

    const material = maybeMesh.material;
    if (Array.isArray(material)) {
      material.forEach((entry) => entry.dispose());
    } else {
      material?.dispose();
    }
  });
};
