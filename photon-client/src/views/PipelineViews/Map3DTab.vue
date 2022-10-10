<template>
  <div>
    <mini-map
      class="miniMapClass"
      :targets="targets"
      :horizontal-f-o-v="horizontalFOV"
    />
  </div>
</template>

<script>
    import miniMap from '../../components/pipeline/3D/MiniMap';

    export default {
        name: "Map3D",
        components: {
            miniMap
        },
        data() {
            return {
            }
        },
        computed: {
            targets: {
                get() {
                    return this.$store.getters.currentPipelineResults.targets;
                }
            },
            horizontalFOV: {
                get() {
                    let index = this.$store.getters.currentPipelineSettings.cameraVideoModeIndex;
                    let FOV = this.$store.getters.currentCameraSettings.fov;
                    let resolution = this.$store.getters.videoFormatList[index];
                    let diagonalView = FOV * (Math.PI / 180);
                    let diagonalAspect = Math.hypot(resolution.width, resolution.height);
                    return Math.atan(Math.tan(diagonalView / 2) * (resolution.width / diagonalAspect)) * 2 * (180 / Math.PI)
                }
            },
        },
        methods: {
        }
    }
</script>

<style scoped>
    .miniMapClass {
        width: 400px !important;
        height: 100% !important;

        margin-left: auto;
        margin-right: auto;
    }
</style>
