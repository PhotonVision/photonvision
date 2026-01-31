<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, watchEffect, type Ref } from "vue";
const {
  AmbientLight,
  AxesHelper,
  BoxGeometry,
  BufferAttribute,
  BufferGeometry,
  Color,
  ConeGeometry,
  Mesh,
  MeshNormalMaterial,
  MeshPhongMaterial,
  Object3D,
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
const drawUncertainty = (data: CvPoint3[] | null) => {
  if (!scene || !data || data.length === 0) return;

  // Remove any existing uncertainty mesh
  const existingMesh = scene.getObjectByName("uncertaintyMesh");
  if (existingMesh) {
    scene.remove(existingMesh);
    if (existingMesh instanceof Mesh) {
      existingMesh.geometry?.dispose();
      if (existingMesh.material) {
        if (Array.isArray(existingMesh.material)) {
          existingMesh.material.forEach((m) => m.dispose());
        } else {
          existingMesh.material.dispose();
        }
      }
    }
  }

  // Create a grid from the data points
  // Group points by x coordinate to determine grid structure
  const pointsByX = new Map<number, CvPoint3[]>();
  data.forEach((point) => {
    if (!pointsByX.has(point.x)) {
      pointsByX.set(point.x, []);
    }
    pointsByX.get(point.x)!.push(point);
  });

  const xValues = Array.from(pointsByX.keys()).sort((a, b) => a - b);
  const yValues = Array.from(new Set(data.map((p) => p.y))).sort((a, b) => a - b);

  // Normalize coordinates to [-0.5, 0.5] range for visualization
  const xMin = Math.min(...xValues);
  const xMax = Math.max(...xValues);
  const yMin = Math.min(...yValues);
  const yMax = Math.max(...yValues);
  const zMin = Math.min(...data.map((p) => p.z));
  const zMax = Math.max(...data.map((p) => p.z));

  const xRange = xMax - xMin || 1;
  const yRange = yMax - yMin || 1;
  const zRange = zMax - zMin || 1;

  // Create a 3D surface geometry
  const geometry = new BufferGeometry();
  const vertices: number[] = [];
  const indices: number[] = [];

  // Create a map for quick point lookup
  const pointMap = new Map<string, CvPoint3>();
  data.forEach((point) => {
    pointMap.set(`${point.x},${point.y}`, point);
  });

  // Build vertices
  xValues.forEach((x) => {
    yValues.forEach((y) => {
      const point = pointMap.get(`${x},${y}`);
      const normX = (x - xMin) / xRange - 0.5;
      const normY = (y - yMin) / yRange - 0.5;
      const normZ = (point ? (point.z - zMin) / zRange : 0) * 0.3; // Scale Z for visibility

      vertices.push(normX, normY, normZ);
    });
  });

  // Build indices to create triangles
  const xCount = xValues.length;
  const yCount = yValues.length;

  for (let xi = 0; xi < xCount - 1; xi++) {
    for (let yi = 0; yi < yCount - 1; yi++) {
      const a = xi * yCount + yi;
      const b = xi * yCount + (yi + 1);
      const c = (xi + 1) * yCount + yi;
      const d = (xi + 1) * yCount + (yi + 1);

      // Two triangles per grid quad
      indices.push(a, c, b);
      indices.push(b, c, d);
    }
  }

  geometry.setAttribute("position", new BufferAttribute(new Float32Array(vertices), 3));
  geometry.setIndex(new BufferAttribute(new Uint32Array(indices), 1));
  geometry.computeVertexNormals();

  // Create vertex colors based on Z values
  const colors: number[] = [];
  data.forEach((point) => {
    const normalizedZ = (point.z - zMin) / zRange;
    // Color gradient: blue (low) -> cyan -> green -> yellow -> red (high)
    let r = 0, g = 0, b = 0;
    
    if (normalizedZ < 0.25) {
      // Blue to Cyan
      const t = normalizedZ / 0.25;
      r = 0;
      g = t;
      b = 1;
    } else if (normalizedZ < 0.5) {
      // Cyan to Green
      const t = (normalizedZ - 0.25) / 0.25;
      r = 0;
      g = 1;
      b = 1 - t;
    } else if (normalizedZ < 0.75) {
      // Green to Yellow
      const t = (normalizedZ - 0.5) / 0.25;
      r = t;
      g = 1;
      b = 0;
    } else {
      // Yellow to Red
      const t = (normalizedZ - 0.75) / 0.25;
      r = 1;
      g = 1 - t;
      b = 0;
    }
    
    colors.push(r, g, b);
  });

  geometry.setAttribute("color", new BufferAttribute(new Float32Array(colors), 3));

  // Create material with vertex colors
  const material = new MeshPhongMaterial({
    vertexColors: true,
    shininess: 200,
    wireframe: false
  });

  const mesh = new Mesh(geometry, material);
  mesh.name = "uncertaintyMesh";
  scene.add(mesh);
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

  controls = new TrackballControls(camera, renderer.domElement);
  controls.rotateSpeed = 1.0;
  controls.zoomSpeed = 1.2;
  controls.panSpeed = 0.8;
  controls.noZoom = false;
  controls.noPan = false;
  controls.staticMoving = true;
  controls.dynamicDampingFactor = 0.3;

  // Set camera to a position where the entire surface is visible
  camera.position.set(0.5, 0.5, 0.6);
  camera.up.set(0, -1, 0);
  controls.target.set(0.0, 0.0, 0.15);

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
    </div>
    <div id="container" style="flex: 1 1 auto">
      <canvas id="view" class="w-100 h-100" />
    </div>
  </div>
</template>
