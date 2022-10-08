<template>
  <div
    id="MapContainer"
    style="flex-grow:1"
  >
    <v-row>
      <v-col
        align="center"
        cols="12"
      >
        <span class="white--text">Target Location</span>
      </v-col>
    </v-row>
    <v-row>
      <v-col
        align="center"
        cols="12"
        align-self="stretch"
      >
        <canvas
          id="canvasId"
          style="width:100%;height:100%"
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
  PerspectiveCamera,
  Quaternion,
  Scene,
  TrackballControls,
  Vector3,
    Color,
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

  mounted() {
    const scene = new Scene();
    this.scene = scene;
    const camera = new PerspectiveCamera(75, 800 / 800, 0.1, 1000);
    this.camera = camera;

    const canvas = document.getElementById("canvasId"); // getting the canvas element
    this.canvas = canvas;
    const renderer = new WebGLRenderer({"canvas": canvas});
    this.renderer = renderer;
    scene.background = new Color(0xa9a9a9)


    this.onWindowResize();
    window.addEventListener( 'resize', this.onWindowResize, false );

    scene.add(new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0),
        1, // length
        0xff0000,
        0.5,
        0.5,
    ))
    scene.add(new ArrowHelper(new Vector3(0, 1, 0).normalize(), new Vector3(0, 0, 0),
        1, // length
        0x00ff00,
        0.5,
        0.5,
    ))
    scene.add(new ArrowHelper(new Vector3(0, 0, 1).normalize(), new Vector3(0, 0, 0),
        1, // length
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


    camera.position.set(-0.1,0,0);
    camera.rotation.set(-90, 0, 90);
    camera.up.set(0,0,1);
    controls.update();

    function animate() {
      requestAnimationFrame(animate);

      controls.update();
      renderer.render(scene, camera);
    }

    this.drawTargets()

    animate();
  },
  methods: {
    drawTargets() {
      this.scene.remove(...this.cubes)
      this.cubes = []

      for (const target of this.targets) {
        const geometry = new BoxGeometry(0.2, 0.2, 0.3 / 5);
        const material = new MeshNormalMaterial();
        let quat = (new Quaternion(
            target.pose.qx,
            target.pose.qy,
            target.pose.qz,
            target.pose.qw,
        ))
        const cube = new Mesh(geometry, material);
        cube.position.set(target.pose.x, target.pose.y, target.pose.z)
        cube.rotation.setFromQuaternion(quat);
        this.cubes.push(cube)

        let arrow = (new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0),
            1, // length
            0xff0000,
            0.1,
            0.1,
        ));
        arrow.rotation.setFromQuaternion(quat)
        arrow.rotateZ(-Math.PI / 2)
        arrow.position.set(target.pose.x, target.pose.y, target.pose.z)
        this.cubes.push(arrow);

        arrow = (new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0),
            1, // length
            0x00ff00,
            0.1,
            0.1,
        ));
        arrow.rotation.setFromQuaternion(quat)
        // arrow.rotateX(Math.PI / 2)
        arrow.position.set(target.pose.x, target.pose.y, target.pose.z)
        this.cubes.push(arrow);
        arrow = (new ArrowHelper(new Vector3(1, 0, 0).normalize(), new Vector3(0, 0, 0),
            1, // length
            0x0000ff,
            0.1,
            0.1,
        ));
        arrow.setRotationFromQuaternion(quat)
        arrow.rotateX(Math.PI / 2)
        arrow.position.set(target.pose.x, target.pose.y, target.pose.z)
        this.cubes.push(arrow);
      }
      if(this.cubes.length > 0)
        this.scene.add(...this.cubes);
    },
    onWindowResize() {
      var container = document.getElementById("MapContainer")
      if(container){
        this.canvas.width = container.clientWidth * 0.95;
        this.canvas.height = container.clientWidth * 0.85;
        this.camera.aspect = this.canvas.width / this.canvas.height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize( this.canvas.width, this.canvas.height );
      }
    },
  }


}
</script>

<style scoped>
</style>
