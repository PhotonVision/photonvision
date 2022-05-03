<template>
  <div>
    <v-row>
      <v-col
          align="center"
          cols="12"
      >
        <span class="white--text">Target Location</span>
        <canvas
            id="canvasId"
            class="mt-2"
            width="800"
            height="800"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>

import {
  ArrowHelper,
  BoxGeometry,
  Mesh,
  MeshNormalMaterial,
  PerspectiveCamera, Quaternion,
  Scene,
  TrackballControls,
  Vector3,
  WebGLRenderer
} from "three-full";

export default {
  name: "MiniMap",
  props: {
    // eslint-disable-next-line vue/require-default-prop
    targets: Array,
    // eslint-disable-next-line vue/require-default-prop
    horizontalFOV: Number
  },
  data() {
    return {
      scene: undefined,
      cubes: [],

    }
  },
  watch: {
    targets: {
      deep: true,
      handler() {
        this.drawTargets();
      }
    },
  },
  methods: {
    drawTargets() {
      this.scene.remove(...this.cubes)
      this.cubes = []

      console.log(this.targets)

      for (const target of this.targets) {
        const geometry = new BoxGeometry(0.5, 0.5, 0.5 / 5);
        const material = new MeshNormalMaterial();
        const cube = new Mesh(geometry, material);
        cube.position.set(target.pose.x, target.pose.y, target.pose.z)
        cube.rotation.setFromQuaternion(new Quaternion(
            target.pose.qx,
            target.pose.qy,
            target.pose.qz,
            target.pose.qw,
        ))
        this.cubes.push(cube)
      }
      this.scene.add(...this.cubes);
    }
  },
  mounted() {
    const scene = new Scene();
    this.scene = scene
    const camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);

    const canvas = document.getElementById("canvasId"); // getting the canvas element
    const renderer = new WebGLRenderer({"canvas": canvas});
    // document.body.appendChild(renderer.domElement);
    // canvas.appendChild(renderer.dome)

    renderer.setSize(800, 800);


    scene.add(new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0),
        2, // length
        0xff0000,
        0.5,
        0.5,
    ))
    scene.add(new ArrowHelper(new Vector3(0, 1, 0).normalize(), new Vector3(0, 0, 0),
        2, // length
        0x00ff00,
        0.5,
        0.5,
    ))
    scene.add(new ArrowHelper(new Vector3(0, 0, 1).normalize(), new Vector3(0, 0, 0),
        2, // length
        0x0000ff,
        0.5,
        0.5,
    ))

    var controls = new TrackballControls(
        camera,
        renderer.domElement
    );
    controls.rotateSpeed = 1.0;
    controls.zoomSpeed = 1.2;
    controls.panSpeed = 0.8;
    controls.noZoom = false;
    controls.noPan = false;
    controls.staticMoving = true;
    controls.dynamicDampingFactor = 0.3;
    controls.keys = [65, 83, 68];


    camera.position.z = 5;
    controls.update();

    function animate() {
      requestAnimationFrame(animate);

      controls.update();
      renderer.render(scene, camera);
    }

    this.drawTargets()

    animate();
  }
}
</script>

<style scoped>
</style>
