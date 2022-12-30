<template>
  <div>
    <v-select
      v-model="selectedFamily"
      dark
      color="accent"
      item-color="secondary"
      label="Select target family"
      :items="familyList"
      @input="handlePipelineUpdate('tagFamily', familyList.indexOf(selectedFamily))"
    />
    <v-select
      v-model="selectedModel"
      dark
      color="accent"
      item-color="secondary"
      label="Select a target model"
      :items="targetList"
      item-text="name"
      item-value="data"
      @input="handlePipelineUpdate('targetModel', targetList.indexOf(selectedModel) + 6)"
    />
    <CVslider
      v-model="decimate"
      class="pt-2"
      slider-cols="8"
      name="Decimate"
      min="1"
      max="8"
      step="1.0"
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
      v-model="hammingDist"
      class="pt-2 pb-4"
      slider-cols="8"
      name="Max error bits"
      min="0"
      max="10"
      step="1"
      tooltip="Maximum number of error bits to correct; potential tags with more will be thrown out. For smaller tags (like 16h5), set this as low as possible."
      @input="handlePipelineData('hammingDist')"
    />
    <CVslider
      v-model="decisionMargin"
      class="pt-2 pb-4"
      slider-cols="8"
      name="Decision Margin Cutoff"
      min="0"
      max="250"
      step="1"
      tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
      @input="handlePipelineData('decisionMargin')"
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
              familyList: ["tag36h11", "tag25h9", "tag16h5"],
              // Selected model is offset (ew) by 6 from the photon ordinal, as we only wanna show the 36h11 and 16h5 options
              targetList: ['6.5in (36h11) AprilTag', '6in (16h5) AprilTag'], // Keep in sync with TargetModel.java
            }
        },
        computed: {
          selectedModel: {
              get() {
                  let ret = this.$store.getters.currentPipelineSettings.targetModel - 6
                  return this.targetList[ret];
              },
              set(val) {
                  this.$store.commit("mutatePipeline", {"targetModel": this.targetList.indexOf(val) + 6})
              }
          },
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
          hammingDist: {
            get() {
                return this.$store.getters.currentPipelineSettings.hammingDist
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"hammingDist": val});
            }
          },
          decisionMargin: {
            get() {
                return this.$store.getters.currentPipelineSettings.decisionMargin
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"decisionMargin": val});
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
