<script setup lang="ts">
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
// @ts-expect-error Intellisense says these conflict with the dynamic imports below
import type { Mesh, Object3D, PerspectiveCamera, Scene, WebGLRenderer } from "three";
// @ts-expect-error Intellisense says these conflict with the dynamic imports below
import type { TrackballControls } from "three/examples/jsm/controls/TrackballControls";
import { onBeforeUnmount, onMounted, watchEffect } from "vue";
const {
  ArrowHelper,
  BoxGeometry,
  CameraHelper,
  Color,
  ConeGeometry,
  Group,
  Mesh,
  MeshNormalMaterial,
  PerspectiveCamera,
  Quaternion,
  Vector3,
  Scene,
  WebGLRenderer
} = await import("three");
const { TrackballControls } = await import("three/examples/jsm/controls/TrackballControls");

import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { createPerspectiveCamera } from "@/lib/ThreeUtils";
import { useTheme } from "vuetify";

const theme = useTheme();

const calibrationCoeffs = useCameraSettingsStore().getCalibrationCoeffs(
  useCameraSettingsStore().currentCameraSettings.validVideoFormats[
    useCameraSettingsStore().currentPipelineSettings.cameraVideoModeIndex
  ].resolution
);

const props = defineProps<{
  targets: PhotonTarget[];
}>();

let scene: Scene | undefined;
let camera: PerspectiveCamera | undefined;
let renderer: WebGLRenderer | undefined;
let controls: TrackballControls | undefined;

let previousTargets: Object3D[] = [];
const drawTargets = (targets: PhotonTarget[]) => {
  // Check here, since if we check in watchEffect this never gets called
  if (!scene || !camera || !renderer || !controls) {
    return;
  }

  if (theme.global.name.value === "LightTheme") scene.background = new Color(0xa9a9a9);
  else scene.background = new Color(0x000000);

  scene.remove(...previousTargets);
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

  if (calibrationCoeffs) {
    // And show camera frustum
    const calibCamera = createPerspectiveCamera(calibrationCoeffs.resolution, calibrationCoeffs.cameraIntrinsics, 10);
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
const onWindowResize = () => {
  const container = document.getElementById("container");
  const canvas = document.getElementById("view");

  if (!container || !canvas || !camera || !renderer) {
    return;
  }

  canvas.style.width = container.clientWidth * 0.75 + "px";
  canvas.style.height = container.clientWidth * 0.35 + "px";
  camera.aspect = canvas.clientWidth / canvas.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(canvas.clientWidth, canvas.clientHeight);
};
const resetCamFirstPerson = () => {
  if (!scene || !camera || !controls) {
    return;
  }

  controls.reset();
  camera.position.set(0.2, 0, 0);
  camera.up.set(0, 0, 1);
  controls.target.set(4.0, 0.0, 0.0);
  controls.update();
  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};
const resetCamThirdPerson = () => {
  if (!scene || !camera || !controls) {
    return;
  }

  controls.reset();
  camera.position.set(-1.39, -1.09, 1.17);
  camera.up.set(0, 0, 1);
  controls.target.set(4.0, 0.0, 0.0);
  controls.update();
  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};

onMounted(async () => {
  scene = new Scene();
  camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);

  const canvas = document.getElementById("view");
  if (!canvas) return;
  renderer = new WebGLRenderer({ canvas: canvas });

  if (theme.global.name.value === "LightTheme") scene.background = new Color(0xa9a9a9);
  else scene.background = new Color(0x000000);

  onWindowResize();
  window.addEventListener("resize", onWindowResize);

  const referenceFrameCues: Object3D[] = [];
  referenceFrameCues.push(
    new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0xff0000, 0.1, 0.1)
  );
  referenceFrameCues.push(
    new ArrowHelper(new Vector3(0, 1, 0).normalize(), new Vector3(0, 0, 0), 1, 0x00ff00, 0.1, 0.1)
  );
  referenceFrameCues.push(
    new ArrowHelper(new Vector3(0, 0, 1).normalize(), new Vector3(0, 0, 0), 1, 0x0000ff, 0.1, 0.1)
  );

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

  controls = new TrackballControls(camera, renderer.domElement);
  controls.rotateSpeed = 1.0;
  controls.zoomSpeed = 1.2;
  controls.panSpeed = 0.8;
  controls.noZoom = false;
  controls.noPan = false;
  controls.staticMoving = true;
  controls.dynamicDampingFactor = 0.3;

  scene.add(...referenceFrameCues);
  resetCamThirdPerson();

  controls.update();

  const animate = () => {
    if (!scene || !camera || !renderer || !controls) {
      return;
    }

    requestAnimationFrame(animate);

    controls.update();
    renderer.render(scene, camera);
  };

  drawTargets(props.targets);
  animate();
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", onWindowResize);
});
watchEffect(() => {
  drawTargets(props.targets);
});
</script>

<template>
  <div id="container" style="width: 100%">
    <div class="d-flex flex-wrap pt-0 pb-2">
      <v-col cols="12" md="6" class="pl-0">
        <v-card-title class="pa-0"> Target Visualization </v-card-title>
      </v-col>
      <v-col cols="6" md="3" class="d-flex align-center pt-0 pt-md-3 pl-6 pl-md-3">
        <v-btn
          style="width: 100%"
          color="buttonActive"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="resetCamFirstPerson"
        >
          First Person
        </v-btn>
      </v-col>
      <v-col cols="6" md="3" class="d-flex align-center pt-0 pt-md-3 pr-0">
        <v-btn
          style="width: 100%"
          color="buttonActive"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="resetCamThirdPerson"
        >
          Third Person
        </v-btn>
      </v-col>
    </div>
    <canvas id="view" class="w-100" />
  </div>
</template>
