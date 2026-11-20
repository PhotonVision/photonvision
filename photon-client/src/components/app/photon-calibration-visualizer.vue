<script setup lang="ts">
import { ref, watch, watchEffect, type Ref } from "vue";
const {
  AxesHelper,
  BoxGeometry,
  CameraHelper,
  ConeGeometry,
  Group,
  Mesh,
  MeshNormalMaterial,
  MeshPhongMaterial,
  SphereGeometry
} = await import("three");
import type { Group as GroupType, Object3D } from "three";
import type { BoardObservation, CameraCalibrationResult } from "@/types/SettingTypes";
import axios from "axios";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import {
  createPerspectiveCamera,
  disposeObject,
  type ThreeVisualizerContext,
  type VisualizerCameraPose
} from "@/lib/ThreeUtils";
import PhotonThreeVisualizer from "@/components/app/photon-three-visualizer.vue";

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
  title: string;
}>();

let threeContext: ThreeVisualizerContext | undefined;

const firstPersonPose: VisualizerCameraPose = {
  position: [0, 0, 0.05],
  up: [0, -1, 0],
  target: [0, 0, 1]
};
const thirdPersonPose: VisualizerCameraPose = {
  position: [-0.3, -0.2, -0.3],
  up: [0, -1, 0],
  target: [0, 0, 0.4]
};

const createChessboard = (obs: BoardObservation, cal: CameraCalibrationResult): GroupType => {
  const group = new Group();

  if (obs.locationInImageSpace.length === 0) return group;

  // Add corner spheres
  obs.locationInObjectSpace.forEach((corner, idx) => {
    if (corner.x < 0 || corner.y < 0) return;

    const isOutlier = !obs.cornersUsed[idx];

    const color = isOutlier ? 0xff3333 : 0x33ff33;

    const sphereGeom = new SphereGeometry(cal.calobjectSpacing / 8, 8, 8);
    const sphereMat = new MeshPhongMaterial({
      color: color,
      opacity: 1,
      transparent: !isOutlier
    });
    const sphere = new Mesh(sphereGeom, sphereMat);
    sphere.position.set(corner.x, corner.y, corner.z);
    group.add(sphere);
  });

  return group;
};

let previousTargets: Object3D[] = [];
const drawCalibration = async (cal: CameraCalibrationResult | null) => {
  // Check here, since if we check in watchEffect this never gets called
  if (!cal || !threeContext) {
    return;
  }
  const { scene } = threeContext;

  scene.remove(...previousTargets);
  // Redrawing recreates every geometry and material -- dispose of the old
  // ones so repeated redraws don't leak GPU resources
  previousTargets.forEach(disposeObject);
  previousTargets = [];

  // Draw all chessboards with transparency
  cal.observations.forEach((obs) => {
    const pose = obs.optimisedCameraToObject;

    // Create chessboard
    const board = createChessboard(obs, cal);
    board.userData.isCalibrationObject = true;

    // Apply transform from camera to chessboard
    const pos = pose.translation;
    board.position.set(pos.x, pos.y, pos.z);

    if (pose.rotation.quaternion) {
      const q = pose.rotation.quaternion;
      board.quaternion.set(q.X, q.Y, q.Z, q.W);
    }

    previousTargets.push(board);
  });

  // And show camera frustum
  const calibCamera = await createPerspectiveCamera(props.resolution, cal.cameraIntrinsics);
  const helper = new CameraHelper(calibCamera);

  // Flip to +Z forward
  const helperGroup = new Group();
  helperGroup.add(helper);
  helperGroup.rotateY(Math.PI);

  previousTargets.push(helperGroup);

  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};

const calibrationData: Ref<CameraCalibrationResult | null> = ref(null);

const fetchCalibrationData = async () => {
  try {
    const response = await axios.get("settings/camera/getCalibration", {
      params: {
        cameraUniqueName: props.cameraUniqueName,
        width: props.resolution.width,
        height: props.resolution.height
      }
    });
    calibrationData.value = response.data;
  } catch (err) {
    console.error("Failed to fetch calibration data:", err);
  }
};

const onSceneReady = (context: ThreeVisualizerContext) => {
  threeContext = context;

  const referenceFrameCues: Object3D[] = [];

  // Draw the reference frame
  referenceFrameCues.push(new AxesHelper(0.3));

  // Draw the Camera Body
  const camSize = 0.04;
  const camBodyGeometry = new BoxGeometry(camSize, camSize, camSize);
  const camLensGeometry = new ConeGeometry(camSize * 0.4, camSize * 0.8, 30);
  const camMaterial = new MeshNormalMaterial();
  const camBody = new Mesh(camBodyGeometry, camMaterial);
  const camLens = new Mesh(camLensGeometry, camMaterial);
  camBody.position.set(0, 0, 0);
  camLens.rotateX(-Math.PI / 2);
  camLens.position.set(0, 0, camSize * 0.8);
  referenceFrameCues.push(camBody);
  referenceFrameCues.push(camLens);

  context.scene.add(...referenceFrameCues);

  // The fetch may have resolved before the scene was ready
  void drawCalibration(calibrationData.value);
};

watchEffect(() => {
  void drawCalibration(calibrationData.value);
});

watch(
  () => [
    props.cameraUniqueName,
    props.resolution.width,
    props.resolution.height,
    useCameraSettingsStore().getCalibrationCoeffs(props.resolution)
  ],
  async () => {
    console.log("Camera or resolution changed, refetching calibration");
    await fetchCalibrationData();
  },
  { immediate: true }
);
</script>

<template>
  <PhotonThreeVisualizer
    :title="props.title"
    :first-person-pose="firstPersonPose"
    :third-person-pose="thirdPersonPose"
    @scene-ready="onSceneReady"
  />
</template>
