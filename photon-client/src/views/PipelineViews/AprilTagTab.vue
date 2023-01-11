<template>
  <div>
    <CVselect
        v-model="selectedFamily"
        name="Target family"
        :list="['AprilTag family 36h11', 'AprilTag family 25h9', 'AprilTag family 16h5']"
        select-cols="8"
        @input="handlePipelineUpdate('tagFamily', selectedFamily)"
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
import CVselect from '../../components/common/cv-select'

export default {
  name: "AprilTag",
  components: {
    CVslider,
    CVswitch,
    CVselect,
  },
  data() {
    return {
      familyList: ["AprilTag family 36h11", "AprilTag family 25h9", "AprilTag family 16h5"],
    }
  },
  computed: {
    selectedFamily: {
      get() {
        return this.$store.getters.currentPipelineSettings.tagFamily
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"tagFamily": val})
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
