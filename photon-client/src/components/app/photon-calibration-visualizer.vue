<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, watchEffect, type Ref } from "vue";
const {
  AmbientLight,
  AxesHelper,
  BoxGeometry,
  CameraHelper,
  Color,
  ConeGeometry,
  Group,
  Mesh,
  MeshNormalMaterial,
  MeshPhongMaterial,
  PerspectiveCamera,
  Scene,
  SphereGeometry,
  WebGLRenderer
} = await import("three");
const { TrackballControls } = await import("three/examples/jsm/controls/TrackballControls");
import type { BoardObservation, CameraCalibrationResult } from "@/types/SettingTypes";
import axios from "axios";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
}>();

let scene: Scene | undefined;
let camera: PerspectiveCamera | undefined;
let renderer: WebGLRenderer | undefined;
let controls: TrackballControls | undefined;

const createChessboard = (obs: BoardObservation, cal: CameraCalibrationResult): Group => {
  const group = new Group();

  if (obs.locationInImageSpace.length === 0) return group;

  // Add corner spheres
  obs.locationInObjectSpace.forEach((corner, idx) => {
    if (corner.x < 0 || corner.y < 0) return;

    const isActive = !obs.cornersUsed[idx];

    const color = obs.cornersUsed[idx] ? 0x00ff00 : 0xff0000;

    const sphereGeom = new SphereGeometry(cal.calobjectSpacing / 8, 8, 8);
    const sphereMat = new MeshPhongMaterial({
      color: color,
      opacity: isActive ? 1.0 : 0.5,
      transparent: !isActive
    });
    const sphere = new Mesh(sphereGeom, sphereMat);
    sphere.position.set(corner.x, corner.y, corner.z);
    group.add(sphere);
  });

  return group;
};

let previousTargets: Object3D[] = [];
const drawCalibration = (cal: CameraCalibrationResult | null) => {
  // Check here, since if we check in watchEffect this never gets called
  if (!cal || scene === undefined || camera === undefined || renderer === undefined || controls === undefined) {
    return;
  }

  scene.remove(...previousTargets);
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

  // And show camera fov
  const imageWidth = props.resolution.width;
  const imageHeight = props.resolution.height;
  const focalLengthY = cal.cameraIntrinsics.data[4];
  const fovY = 2 * Math.atan(imageHeight / (2 * focalLengthY)) * (180 / Math.PI);
  const aspect = imageWidth / imageHeight;

  const calibCamera = new PerspectiveCamera(fovY, aspect, 0.1, 1.0);
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
const isLoading: Ref<boolean> = ref(true);
const error: Ref<string | null> = ref(null);

const fetchCalibrationData = async () => {
  console.log("Fetching calibration data for camera:", props.cameraUniqueName, "at resolution:", props.resolution);

  isLoading.value = true;
  error.value = null;

  try {
    const response = await axios.get("/settings/camera/getCalibration", {
      params: {
        cameraUniqueName: props.cameraUniqueName,
        width: props.resolution.width,
        height: props.resolution.height
      }
    });
    calibrationData.value = response.data;
    console.log("Received calibration data:", response);
  } catch (err) {
    console.error("Failed to fetch calibration data:", err);
    error.value = "Failed to load calibration data";
  } finally {
    isLoading.value = false;
  }
};

const onWindowResize = () => {
  const container = document.getElementById("container");
  const canvas = document.getElementById("view");

  if (container === null || canvas === null || camera === undefined || renderer === undefined) {
    return;
  }

  canvas.style.width = container.clientWidth * 0.75 + "px";
  canvas.style.height = container.clientWidth * 0.35 + "px";
  camera.aspect = canvas.clientWidth / canvas.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(canvas.clientWidth, canvas.clientHeight);
};
const resetCamFirstPerson = () => {
  if (scene === undefined || camera === undefined || controls === undefined) {
    return;
  }

  controls.reset();
  camera.position.set(0, 0, 0.2);
  camera.up.set(0, -1, 0);
  controls.target.set(0.0, 0.0, 1.0);
  controls.update();
  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};
const resetCamThirdPerson = () => {
  if (scene === undefined || camera === undefined || controls === undefined) {
    return;
  }

  controls.reset();
  camera.position.set(-0.3, -0.2, -0.3);
  camera.up.set(0, -1, 0);
  controls.target.set(0.0, 0.0, 0.4);
  controls.update();
  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};

onMounted(async () => {
  // Grab data first off
  fetchCalibrationData();

  scene = new Scene();
  camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);

  const canvas = document.getElementById("view");
  if (canvas === null) return;
  renderer = new WebGLRenderer({ canvas: canvas });

  // Add lights
  const ambientLight = new AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  scene.background = new Color(0xa9a9a9);

  // // Add grid
  // const gridHelper = new GridHelper(2, 10, 0x444444, 0x222222);
  // gridHelper.rotateOnWorldAxis(new Vector3(1, 0, 0), Math.PI / 2);
  // gridHelper.position.z = -0.05;
  // scene.add(gridHelper);

  onWindowResize();
  window.addEventListener("resize", onWindowResize);

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
    if (scene === undefined || camera === undefined || renderer === undefined || controls === undefined) {
      return;
    }

    requestAnimationFrame(animate);

    controls.update();
    renderer.render(scene, camera);
  };

  animate();
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", onWindowResize);
});
watchEffect(() => {
  console.log("Watch triggered, refetching calibration");
  drawCalibration(calibrationData.value);
});

watch(
  () => [
    props.cameraUniqueName,
    props.resolution.width,
    props.resolution.height,
    useCameraSettingsStore().getCalibrationCoeffs(props.resolution)
  ],
  () => {
    console.log("Camera or resolution changed, refetching calibration");
    fetchCalibrationData();
  }
);
</script>

<template>
  <div id="container" style="width: 100%">
    <!-- <template v-if="calibrationData"> -->
    <v-row>
      <v-col align-self="stretch" style="display: flex; justify-content: center">
        <canvas id="view" />
      </v-col>
    </v-row>
    <v-row style="margin-bottom: 24px">
      <v-col style="display: flex; justify-content: center">
        <v-btn color="secondary" @click="resetCamFirstPerson"> First Person </v-btn>
      </v-col>
      <v-col style="display: flex; justify-content: center">
        <v-btn color="secondary" @click="resetCamThirdPerson"> Third Person </v-btn>
      </v-col>
    </v-row>
  </div>
</template>
