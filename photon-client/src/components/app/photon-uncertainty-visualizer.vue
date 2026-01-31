<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, watchEffect, type Ref } from "vue";
const {
  AmbientLight,
  AxesHelper,
  BoxGeometry,
  Color,
  ConeGeometry,
  Mesh,
  MeshNormalMaterial,
  PerspectiveCamera,
  Scene,
  WebGLRenderer
} = await import("three");
const { TrackballControls } = await import("three/examples/jsm/controls/TrackballControls");
import type { CvPoint3 } from "@/types/SettingTypes";
import axios from "axios";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useTheme } from "vuetify";

const theme = useTheme();

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
  title: string;
}>();

let scene: Scene | undefined;
let camera: PerspectiveCamera | undefined;
let renderer: WebGLRenderer | undefined;
let controls: TrackballControls | undefined;

const uncertaintyData: Ref<CvPoint3[] | null> = ref(null);
const isLoading: Ref<boolean> = ref(true);
const error: Ref<string | null> = ref(null);

let baseAspect: number | undefined;
const drawUncertainty = (data: CvPoint3[]) => {
  // TODO
  console.log('hailo')
  console.log(data)
}

const fetchUncertaintyData = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    const response = await axios.get("/settings/camera/getUncertainty", {
      params: {
        cameraUniqueName: props.cameraUniqueName,
        width: props.resolution.width,
        height: props.resolution.height
      }
    });
    uncertaintyData.value = response.data;
  } catch (err) {
    console.error("Failed to fetch uncertainty data:", err);
    error.value = "Failed to load uncertainty data";
  } finally {
    isLoading.value = false;
  }
};

const onWindowResize = () => {
  const container = document.getElementById("container");
  const canvas = document.getElementById("view");

  if (!container || !canvas || !camera || !renderer) {
    return;
  }

  // Compute a concrete width from the container and derive height from a
  // stable base aspect ratio (calculated on mount) to avoid feedback loops
  // where updating canvas size changes container size while resizing
  const width = Math.max(1, Math.floor(container.clientWidth));
  let height: number;
  if (baseAspect && baseAspect > 0) {
    height = Math.max(1, Math.floor(width / baseAspect));
  } else {
    height = Math.max(1, Math.floor(container.clientHeight));
  }

  // Use updateStyle=false so Three.js does not write to canvas style,
  // which can affect layout and re-trigger resize events
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
};

const resetCamFirstPerson = () => {
  if (!scene || !camera || !controls) {
    return;
  }

  controls.reset();
  camera.position.set(0, 0, 0.05);
  camera.up.set(0, -1, 0);
  controls.target.set(0.0, 0.0, 1.0);
  controls.update();
  // if (previousTargets.length > 0) {
  //   scene.add(...previousTargets);
  // }
};

const resetCamThirdPerson = () => {
  if (!scene || !camera || !controls) {
    return;
  }

  controls.reset();
  camera.position.set(-0.3, -0.2, -0.3);
  camera.up.set(0, -1, 0);
  controls.target.set(0.0, 0.0, 0.4);
  controls.update();
  // if (previousTargets.length > 0) {
  //   scene.add(...previousTargets);
  // }
};

let animationFrameId: number | null = null;

onMounted(async () => {
  // Grab data first off
  fetchUncertaintyData();

  scene = new Scene();
  camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);

  const canvas = document.getElementById("view");
  if (!canvas) return;
  renderer = new WebGLRenderer({ canvas: canvas });

  // Add lights
  const ambientLight = new AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  if (theme.global.name.value === "LightTheme") scene.background = new Color(0xa9a9a9);
  else scene.background = new Color(0x000000);

  // Initialize a stable aspect ratio so subsequent resize events derive
  // height from width, avoiding layout feedback during continuous resizing
  try {
    const initWidth = Math.max(1, Math.floor(document.getElementById("container")?.clientWidth || 1));
    const initHeight = Math.max(1, Math.floor(document.getElementById("container")?.clientHeight || 1));
    baseAspect = initWidth / Math.max(1, initHeight);
  } catch {
    baseAspect = undefined;
  }

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
    if (!scene || !camera || !renderer || !controls) {
      return;
    }

    animationFrameId = requestAnimationFrame(animate);

    controls.update();
    renderer.render(scene, camera);
  };

  animate();
});

const cleanup = () => {
  window.removeEventListener("resize", onWindowResize);

  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
  }

  if (controls) {
    controls.dispose();
  }

  if (renderer) {
    renderer.dispose();
    renderer.forceContextLoss();
  }

  if (scene) {
    scene.traverse((object) => {
      if (object instanceof Mesh) {
        object.geometry?.dispose();
        if (object.material) {
          if (Array.isArray(object.material)) {
            object.material.forEach((material) => material.dispose());
          } else {
            object.material.dispose();
          }
        }
      }
    });
  }

  scene = undefined;
  camera = undefined;
  renderer = undefined;
  controls = undefined;
  // previousTargets = [];
};

onBeforeUnmount(cleanup);

// If hot-reloading, cleanup on hot reload
if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    cleanup();
  });
}

watchEffect(() => {
  drawUncertainty(uncertaintyData.value);
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
    fetchUncertaintyData();
  }
);
</script>

<template>
  <div style="width: 100%; height: 100%" class="d-flex flex-column">
    <div class="d-flex flex-wrap pt-0 pb-2">
      <v-col cols="12" md="6" class="pl-0">
        <v-card-title class="pa-0">
          {{ props.title }}
        </v-card-title>
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
    <div id="container" style="flex: 1 1 auto">
      <canvas id="view" class="w-100 h-100" />
    </div>
  </div>
</template>
