<template>
  <div>
    <v-select
      v-model="selectedFamily"
      dark
      color="accent"
      item-color="secondary"
      label="Select target family"
      :items="familyList"
      @input="handlePipelineUpdate('tagFamily', targetList.indexOf(selectedModel))"
    />
    <CVslider
      v-model="decimate"
      class="pt-2"
      slider-cols="8"
      name="Decimate"
      min="0"
      max="3"
      step=".5"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      @input="handlePipelineData('decimate')"
    />
    <CVslider
      v-model="blur"
      class="pt-2"
      slider-cols="8"
      name="Blur"
      min="0"
      max="5"
      step=".01"
      tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
      @input="handlePipelineData('blur')"
    />
    <CVslider
      v-model="threads"
      class="pt-2"
      slider-cols="8"
      name="Threads"
      min="1"
      max="8"
      step="1"
      tooltip="Number of threads spawned by the AprilTag detector"
      @input="handlePipelineData('threads')"
    />
    <CVswitch
      v-model="refineEdges"
      class="pt-2"
      slider-cols="8"
      name="Refine Edges"
      tooltip="Further refines the apriltag corner position initial estimate, suggested left on"
      @input="handlePipelineData('refineEdges')"
    />
    <CVslider
      v-model="numIterations"
      class="pt-2 pb-4"
      slider-cols="8"
      name="Pose Estimation Iterations"
      min="0"
      max="500"
      step="1"
      tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
      @input="handlePipelineData('numIterations')"
    />
  </div>
</template>

<script>
    import CVslider from '../../components/common/cv-slider'
    import CVswitch from '../../components/common/cv-switch'

    export default {
        name: "AprilTag",
        components: {
          CVslider,
          CVswitch,
        },
        data() {
            return {
              familyList: ["tag36h11"],
            }
        },
        computed: {
          selectedFamily: {
            get() {
                let ret = this.$store.getters.currentPipelineSettings.tagFamily
                return this.familyList[ret];
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"tagFamily": this.familyList.indexOf(val)})
            }
          },
          decimate: {
            get() {
                return this.$store.getters.currentPipelineSettings.decimate
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"decimate": val});
            }
          },
          numIterations: {
            get() {
                return this.$store.getters.currentPipelineSettings.numIterations
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"numIterations": val});
            }
          },
          blur: {
            get() {
                return this.$store.getters.currentPipelineSettings.blur
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"blur": val});
            }
          },
          threads: {
            get() {
                return this.$store.getters.currentPipelineSettings.threads
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"threads": val});
            }
          },
          refineEdges: {
            get() {
                return this.$store.getters.currentPipelineSettings.refineEdges
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"refineEdges": val});
            }
          },
        },
        methods: {
        }
    }
</script>
