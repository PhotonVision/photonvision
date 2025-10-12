<script setup lang="ts">
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import { onBeforeUnmount, onMounted, watchEffect } from "vue";
import {
  AmbientLight,
  ArrowHelper,
  AxesHelper,
  BoxGeometry,
  BufferGeometry,
  Color,
  ConeGeometry,
  DoubleSide,
  GridHelper,
  Group,
  Line,
  LineBasicMaterial,
  Mesh,
  MeshNormalMaterial,
  MeshPhongMaterial,
  type Object3D,
  PerspectiveCamera,
  PlaneGeometry,
  Quaternion,
  Scene,
  SphereGeometry,
  Vector3,
  WebGLRenderer
} from "three";
import { TrackballControls } from "three/examples/jsm/controls/TrackballControls";
import type { CameraCalibrationResult, CvPoint3 } from "@/types/SettingTypes";

const props = defineProps<{
  calibration: CameraCalibrationResult;
}>();

let scene: Scene | undefined;
let camera: PerspectiveCamera | undefined;
let renderer: WebGLRenderer | undefined;
let controls: TrackballControls | undefined;

const createChessboard = (corners: CvPoint3[], isActive: boolean, cal: CameraCalibrationResult): Group => {
  const group = new Group();

  if (corners.length === 0) return group;

  console.log("Creating chessboard with size:", cal.calobjectSize, "and spacing:", cal.calobjectSpacing);

  for (let i = 0; i < cal.calobjectSize.width; i++) {
    for (let j = 0; j < cal.calobjectSize.height; j++) {
      const isBlack = (i + j) % 2 === 0;
      const color = isBlack ? 0x333333 : 0xeeeeee;

      const squareGeom = new PlaneGeometry(0.03, 0.03);
      const squareMat = new MeshPhongMaterial({
        color,
        opacity: isActive ? 1.0 : 0.3,
        transparent: !isActive,
        side: DoubleSide
      });
      const square = new Mesh(squareGeom, squareMat);

      square.position.x = i * cal.calobjectSpacing;
      square.position.y = j * cal.calobjectSpacing;
      square.position.z = 0;

      group.add(square);
    }
  }

  // Add corner spheres
  corners.forEach((corner, idx) => {
    const sphereGeom = new SphereGeometry(0.003, 8, 8);
    const sphereMat = new MeshPhongMaterial({
      color: 0xff5722,
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
const drawCalibration = (cal: CameraCalibrationResult, snapshotIndex: number = 1) => {
  // Check here, since if we check in watchEffect this never gets called
  if (scene === undefined || camera === undefined || renderer === undefined || controls === undefined) {
    return;
  }

  scene.remove(...previousTargets);
  previousTargets = [];

  // Draw all chessboards with transparency
  cal.observations.forEach((obs, idx) => {
    const isActive = idx === snapshotIndex || true;
    const pose = obs.optimisedCameraToObject;

    // Create chessboard
    const board = createChessboard(obs.locationInObjectSpace, isActive, cal);
    board.userData.isCalibrationObject = true;

    // Apply transform from camera to chessboard
    const pos = pose.translation;
    console.log("Placing board at:", pos.x, pos.y, pos.z);
    board.position.set(pos.x, pos.y, pos.z);

    if (pose.rotation.quaternion) {
      const q = pose.rotation.quaternion;
      board.quaternion.set(q.X, q.Y, q.Z, q.W);
    }

    previousTargets.push(board);

    // Add coordinate frame for active board
    if (isActive) {
      const frameAxes = new AxesHelper(0.15);
      frameAxes.position.copy(board.position);
      frameAxes.quaternion.copy(board.quaternion);
      frameAxes.userData.isCalibrationObject = true;
      previousTargets.push(frameAxes);
    }
  });

  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
}

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
  camera.position.set(0.2, 0, 0);
  camera.up.set(0, 0, 1);
  controls.target.set(4.0, 0.0, 0.0);
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
  camera.position.set(-1.39, -1.09, 1.17);
  camera.up.set(0, 0, 1);
  controls.target.set(4.0, 0.0, 0.0);
  controls.update();
  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
  }
};

onMounted(() => {
  scene = new Scene();
  camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);

  const canvas = document.getElementById("view");
  if (canvas === null) return;
  renderer = new WebGLRenderer({ canvas: canvas });

  // Add lights
  const ambientLight = new AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  scene.background = new Color(0xa9a9a9);

  // Add grid
  const gridHelper = new GridHelper(2, 10, 0x444444, 0x222222);
  gridHelper.rotateOnWorldAxis(new Vector3(1, 0, 0), Math.PI / 2);
  gridHelper.position.z = -0.05;
  scene.add(gridHelper);

  onWindowResize();
  window.addEventListener("resize", onWindowResize);

  const referenceFrameCues: Object3D[] = [];

  // Draw the reference frame
  referenceFrameCues.push(new AxesHelper(0.3));

  // Draw the Camera Body
  const camSize = 0.1;
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

  drawCalibration(props.calibration);
  animate();
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", onWindowResize);
});
watchEffect(() => {
  drawCalibration(props.calibration);
});
</script>

<template>
  <div id="container" style="width: 100%">
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
