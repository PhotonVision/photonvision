<script setup lang="ts">
import type { TagTrackedTarget } from "@/types/PhotonTrackingTypes";
import { onBeforeUnmount, onMounted, watchEffect } from "vue";
import {
  ArrowHelper,
  BoxGeometry,
  Color,
  ConeGeometry,
  Mesh,
  MeshNormalMaterial,
  type Object3D,
  PerspectiveCamera,
  Quaternion,
  Scene,
  Vector3,
  WebGLRenderer
} from "three";
import { TrackballControls } from "three/addons/controls/TrackballControls.js";

const props = defineProps<{
  targets: TagTrackedTarget[];
}>();

let scene: Scene | undefined;
let camera: PerspectiveCamera | undefined;
let renderer: WebGLRenderer | undefined;
let controls: TrackballControls | undefined;

let previousTargets: Object3D[] = [];
const drawTargets = (targets: TagTrackedTarget[]) => {
  // Check here, since if we check in watchEffect this never gets called
  if (scene === undefined || camera === undefined || renderer === undefined || controls === undefined) {
    return;
  }

  scene.remove(...previousTargets);
  previousTargets = [];

  targets.forEach((target) => {
    if (!target.bestTransform) return;

    const geometry = new BoxGeometry(0.3 / 5, 0.2, 0.2);
    const material = new MeshNormalMaterial();

    const quatRaw = target.bestTransform.rotation.quaternion;
    const translation = target.bestTransform.translation;

    const quaternion = new Quaternion(quatRaw.X, quatRaw.Y, quatRaw.Z, quatRaw.W);

    const cube = new Mesh(geometry, material);
    cube.position.set(translation.x, translation.y, translation.z);
    cube.rotation.setFromQuaternion(quaternion);
    previousTargets.push(cube);

    let arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0xff0000, 0.1, 0.1);
    arrow.rotation.setFromQuaternion(quaternion);
    arrow.rotateZ(-Math.PI / 2);
    arrow.position.set(translation.x, translation.y, translation.z);
    previousTargets.push(arrow);

    arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0x00ff00, 0.1, 0.1);
    arrow.rotation.setFromQuaternion(quaternion);
    arrow.position.set(translation.x, translation.y, translation.z);
    previousTargets.push(arrow);

    arrow = new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0), 1, 0x0000ff, 0.1, 0.1);
    arrow.setRotationFromQuaternion(quaternion);
    arrow.rotateX(Math.PI / 2);
    arrow.position.set(translation.x, translation.y, translation.z);
    previousTargets.push(arrow);
  });

  if (previousTargets.length > 0) {
    scene.add(...previousTargets);
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

  scene.background = new Color(0xa9a9a9);

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
    if (scene === undefined || camera === undefined || renderer === undefined || controls === undefined) {
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
  <div id="container" class="w-100">
    <v-row>
      <v-col class="d-flex justify-center align-self-center">
        <canvas id="view" />
      </v-col>
    </v-row>
    <v-row class="mb-6">
      <v-col class="d-flex justify-center">
        <v-btn color="accent" text="First Person" @click="resetCamFirstPerson" />
      </v-col>
      <v-col class="d-flex justify-center">
        <v-btn color="accent" text="Third Person" @click="resetCamThirdPerson" />
      </v-col>
    </v-row>
  </div>
</template>
