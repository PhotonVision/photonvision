<script setup lang="ts">
import { onBeforeUnmount, onMounted, useTemplateRef, watch } from "vue";
import type {
  Scene as SceneType,
  PerspectiveCamera as PerspectiveCameraType,
  WebGLRenderer as WebGLRendererType
} from "three";
import type { TrackballControls as TrackballControlsType } from "three/examples/jsm/controls/TrackballControls.js";
const { AmbientLight, Color, PerspectiveCamera, Scene, WebGLRenderer } = await import("three");
const { TrackballControls } = await import("three/examples/jsm/controls/TrackballControls.js");
import { useTheme } from "@/composables/useTheme";
import { disposeObject, type ThreeVisualizerContext, type VisualizerCameraPose } from "@/lib/ThreeUtils";

const theme = useTheme();

const props = withDefaults(
  defineProps<{
    title?: string;
    firstPersonPose: VisualizerCameraPose;
    thirdPersonPose: VisualizerCameraPose;
    /**
     * Width-to-height ratio used to size the canvas when the surrounding container has no
     * intrinsic height. When unset, the canvas aspect ratio follows the container's size
     * measured on mount.
     */
    defaultAspect?: number;
  }>(),
  { title: undefined, defaultAspect: undefined }
);

const emit = defineEmits<{
  sceneReady: [context: ThreeVisualizerContext];
}>();

let scene: SceneType | undefined;
let camera: PerspectiveCameraType | undefined;
let renderer: WebGLRendererType | undefined;
let controls: TrackballControlsType | undefined;

const containerRef = useTemplateRef<HTMLDivElement>("containerRef");
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
const FALLBACK_ASPECT = 1.8;

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
  renderer = new WebGLRenderer({ canvas: canvasRef.value, antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);

  // Add lights
  scene.add(new AmbientLight(0xffffff, 0.6));

  if (theme.isDark.value) scene.background = new Color(0x151515);
  else scene.background = new Color(0x232c37);

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

watch(theme.isDark, () => {
  if (!scene) return;
  scene.background = theme.isDark.value ? new Color(0x151515) : new Color(0x232c37);
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
  <div class="flex h-full w-full flex-col">
    <div v-if="props.title" class="flex flex-wrap items-center py-2">
      <div class="flex-1 pl-0 text-base font-semibold">
        {{ props.title }}
      </div>
    </div>
    <div ref="containerRef" class="min-h-0 flex-1">
      <canvas ref="canvasRef" class="h-auto max-w-full rounded-xl border border-white/10" />
    </div>
    <div class="flex w-full gap-2 pt-2">
      <div class="flex w-full items-center">
        <pv-button variant="primary" block @click="resetCamFirstPerson"> First Person </pv-button>
      </div>
      <div class="flex w-full items-center">
        <pv-button variant="primary" block @click="resetCamThirdPerson"> Third Person </pv-button>
      </div>
    </div>
  </div>
</template>
