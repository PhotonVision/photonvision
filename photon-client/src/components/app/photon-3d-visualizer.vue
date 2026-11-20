<script setup lang="ts">
import { computed, watchEffect } from "vue";
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import type { Object3D } from "three";
const { ArrowHelper, BoxGeometry, CameraHelper, ConeGeometry, Group, Mesh, MeshNormalMaterial, Quaternion, Vector3 } =
  await import("three");

import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import {
  createPerspectiveCamera,
  disposeObject,
  type ThreeVisualizerContext,
  type VisualizerCameraPose
} from "@/lib/ThreeUtils";
import PhotonThreeVisualizer from "@/components/app/photon-three-visualizer.vue";

const currentVideoFormat = computed(() => useCameraSettingsStore().currentVideoFormat);
const calibrationCoeffs = computed(() => {
  const format = currentVideoFormat.value;
  return format ? useCameraSettingsStore().getCalibrationCoeffs(format.resolution) : undefined;
});

const props = defineProps<{
  targets: PhotonTarget[];
}>();

let threeContext: ThreeVisualizerContext | undefined;

const firstPersonPose: VisualizerCameraPose = {
  position: [0.2, 0, 0],
  up: [0, 0, 1],
  target: [4, 0, 0]
};
const thirdPersonPose: VisualizerCameraPose = {
  position: [-1.39, -1.09, 1.17],
  up: [0, 0, 1],
  target: [4, 0, 0]
};

let previousTargets: Object3D[] = [];
const drawTargets = async (targets: PhotonTarget[]) => {
  // Check here, since if we check in watchEffect this never gets called
  if (!threeContext) {
    return;
  }
  const { scene } = threeContext;

  scene.remove(...previousTargets);
  // Redrawing recreates every geometry and material -- dispose of the old
  // ones so repeated redraws don't leak GPU resources
  previousTargets.forEach(disposeObject);
  previousTargets = [];

  targets.forEach((target) => {
    if (!target.pose) return;

    const geometry = new BoxGeometry(0.3 / 5, 0.2, 0.2);
    const material = new MeshNormalMaterial();

    const quaternion = new Quaternion(target.pose.qx, target.pose.qy, target.pose.qz, target.pose.qw);

    const cube = new Mesh(geometry, material);
    cube.position.set(target.pose.x, target.pose.y, target.pose.z);
    cube.rotation.setFromQuaternion(quaternion);
    previousTargets.push(cube);

    let arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0xff0000, 0.1, 0.1);
    arrow.rotation.setFromQuaternion(quaternion);
    arrow.rotateZ(-Math.PI / 2);
    arrow.position.set(target.pose.x, target.pose.y, target.pose.z);
    previousTargets.push(arrow);

    arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0x00ff00, 0.1, 0.1);
    arrow.rotation.setFromQuaternion(quaternion);
    arrow.position.set(target.pose.x, target.pose.y, target.pose.z);
    previousTargets.push(arrow);

    arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0x0000ff, 0.1, 0.1);
    arrow.setRotationFromQuaternion(quaternion);
    arrow.rotateX(Math.PI / 2);
    arrow.position.set(target.pose.x, target.pose.y, target.pose.z);
    previousTargets.push(arrow);
  });

  if (calibrationCoeffs.value) {
    // And show camera frustum
    const calibCamera = await createPerspectiveCamera(
      calibrationCoeffs.value.resolution,
      calibrationCoeffs.value.cameraIntrinsics,
      10
    );
    const helper = new CameraHelper(calibCamera);
    const helperGroup = new Group();
    helperGroup.add(helper);
    // Flip to +Z forward
    helperGroup.rotateX(-Math.PI / 2.0);
    helperGroup.rotateY(-Math.PI / 2.0);
    previousTargets.push(helperGroup);
  }

  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};

const onSceneReady = (context: ThreeVisualizerContext) => {
  threeContext = context;

  const referenceFrameCues: Object3D[] = [
    new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0xff0000, 0.1, 0.1),
    new ArrowHelper(new Vector3(0, 1, 0).normalize(), new Vector3(0, 0, 0), 1, 0x00ff00, 0.1, 0.1),
    new ArrowHelper(new Vector3(0, 0, 1).normalize(), new Vector3(0, 0, 0), 1, 0x0000ff, 0.1, 0.1)
  ];

  // Draw the Camera Body
  const camSize = 0.2;
  const camBodyGeometry = new BoxGeometry(camSize, camSize, camSize);
  const camLensGeometry = new ConeGeometry(camSize * 0.4, camSize * 0.8, 30);
  const camMaterial = new MeshNormalMaterial();
  const camBody = new Mesh(camBodyGeometry, camMaterial);
  const camLens = new Mesh(camLensGeometry, camMaterial);
  camBody.position.set(0, 0, 0);
  camLens.rotateZ(Math.PI / 2);
  camLens.position.set(camSize * 0.8, 0, 0);
  referenceFrameCues.push(camBody);
  referenceFrameCues.push(camLens);

  context.scene.add(...referenceFrameCues);

  // Targets may already have arrived before the scene was ready
  void drawTargets(props.targets);
};

watchEffect(() => {
  void drawTargets(props.targets);
});
</script>

<template>
  <PhotonThreeVisualizer
    :first-person-pose="firstPersonPose"
    :third-person-pose="thirdPersonPose"
    :default-aspect="1.8"
    @scene-ready="onSceneReady"
  />
</template>
