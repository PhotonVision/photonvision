<script setup lang="ts">
import { onBeforeUnmount, onMounted, useTemplateRef } from "vue";
import type {
  Scene as SceneType,
  PerspectiveCamera as PerspectiveCameraType,
  WebGLRenderer as WebGLRendererType
} from "three";
import type { TrackballControls as TrackballControlsType } from "three/examples/jsm/controls/TrackballControls.js";
const { AmbientLight, Color, PerspectiveCamera, Scene, WebGLRenderer } = await import("three");
const { TrackballControls } = await import("three/examples/jsm/controls/TrackballControls.js");
import { useTheme } from "vuetify";
import { disposeObject, type ThreeVisualizerContext, type VisualizerCameraPose } from "@/lib/ThreeUtils";

const theme = useTheme();

const props = withDefaults(
  defineProps<{
    title: string;
    firstPersonPose: VisualizerCameraPose;
    thirdPersonPose: VisualizerCameraPose;
    /**
     * Width-to-height ratio used to size the canvas when the surrounding container has no
     * intrinsic height. When unset, the canvas aspect ratio follows the container's size
     * measured on mount.
     */
    defaultAspect?: number;
  }>(),
  { defaultAspect: undefined }
);

const emit = defineEmits<{
  sceneReady: [context: ThreeVisualizerContext];
}>();

let scene: SceneType | undefined;
let camera: PerspectiveCameraType | undefined;
let renderer: WebGLRendererType | undefined;
let controls: TrackballControlsType | undefined;

const containerRef = useTemplateRef<HTMLElement>("containerRef");
const canvasRef = useTemplateRef<HTMLCanvasElement>("canvasRef");

const applyCameraPose = (pose: VisualizerCameraPose) => {
  if (!camera || !controls) {
    return;
  }

  controls.reset();
  camera.position.set(...pose.position);
  camera.up.set(...pose.up);
  controls.target.set(...pose.target);
  controls.update();
};

const resetCamFirstPerson = () => applyCameraPose(props.firstPersonPose);
const resetCamThirdPerson = () => applyCameraPose(props.thirdPersonPose);

// Aspect ratio measured on mount so subsequent resize events derive height
// from width, avoiding feedback loops where updating canvas size changes
// container size while resizing
let baseAspect: number | undefined;
const FALLBACK_ASPECT = 2;

const onWindowResize = () => {
  if (!containerRef.value || !camera || !renderer) {
    return;
  }

  const width = Math.max(1, Math.floor(containerRef.value.clientWidth));
  const aspect = props.defaultAspect ?? (baseAspect && baseAspect > 0 ? baseAspect : FALLBACK_ASPECT);
  const height = Math.max(1, Math.floor(width / aspect));

  if (props.defaultAspect) {
    // The container has no intrinsic height -- give it one so the canvas doesn't collapse
    containerRef.value.style.height = `${height}px`;
  }

  // Use updateStyle=false so Three.js does not write to canvas style,
  // which can affect layout and re-trigger resize events
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
};

let animationFrameId: number | null = null;

onMounted(() => {
  if (!canvasRef.value) {
    return;
  }

  scene = new Scene();
  camera = new PerspectiveCamera(75, 1, 0.1, 1000);
  renderer = new WebGLRenderer({ canvas: canvasRef.value });

  // Add lights
  scene.add(new AmbientLight(0xffffff, 0.6));

  if (theme.global.current.value.dark) scene.background = new Color(0x000000);
  else scene.background = new Color(0xa9a9a9);

  const initWidth = Math.max(1, Math.floor(containerRef.value?.clientWidth || 1));
  const initHeight = Math.floor(containerRef.value?.clientHeight || 0);
  baseAspect = initHeight > 0 ? initWidth / initHeight : undefined;

  onWindowResize();
  window.addEventListener("resize", onWindowResize);

  controls = new TrackballControls(camera, renderer.domElement);
  controls.rotateSpeed = 1.0;
  controls.zoomSpeed = 1.2;
  controls.panSpeed = 0.8;
  controls.noZoom = false;
  controls.noPan = false;
  controls.staticMoving = true;
  controls.dynamicDampingFactor = 0.3;

  emit("sceneReady", { scene, camera, renderer, controls });

  resetCamThirdPerson();

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

  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }

  if (controls) {
    controls.dispose();
  }
  controls = undefined;

  if (scene) {
    disposeObject(scene);
  }
  scene = undefined;
  camera = undefined;

  if (renderer) {
    renderer.dispose();
    renderer.forceContextLoss();
  }
  renderer = undefined;
};

onBeforeUnmount(cleanup);

// If hot-reloading, cleanup on hot reload
if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    cleanup();
  });
}
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
          :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
          @click="resetCamFirstPerson"
        >
          First Person
        </v-btn>
      </v-col>
      <v-col cols="6" md="3" class="d-flex align-center pt-0 pt-md-3 pr-0">
        <v-btn
          style="width: 100%"
          color="buttonActive"
          :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
          @click="resetCamThirdPerson"
        >
          Third Person
        </v-btn>
      </v-col>
    </div>
    <div ref="containerRef" style="flex: 1 1 auto">
      <canvas ref="canvasRef" class="w-100 h-100" />
    </div>
  </div>
</template>
