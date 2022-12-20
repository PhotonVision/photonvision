<template>
  <div>
    <CVslider
        v-model="decimate"
        class="pt-2"
        slider-cols="8"
        name="Decimate"
        min="1"
        max="8"
        step=".5"
        tooltip="Increases FPS at the expense of range by reducing image resolution initially"
        @input="handlePipelineData('decimate')"
    />
    <CVslider
        v-model="numIterations"
        class="pt-2"
        slider-cols="8"
        name="Corner Iterations"
        min="30"
        max="500"
        step="5"
        tooltip="How many iterations are going to be used in order to refine corners. Higher values are lead to more accuracy at the cost of performance"
        @input="handlePipelineData('numIterations')"
    />
    <CVslider
        v-model="cornerAccuracy"
        class="pt-2"
        slider-cols="8"
        name="Corner Accuracy"
        min=".5"
        max="100"
        step=".5"
        tooltip="Minimum accuracy for the corners, lower is better but more performance intensive "
        @input="handlePipelineData('cornerAccuracy')"
    />
  </div>
</template>

<script>
import CVslider from '../../components/common/cv-slider'
import CVswitch from '../../components/common/cv-switch'

export default {
  name: "Aruco",
  components: {
    CVslider,
    CVswitch
  },
  computed: {
    decimate: {
      get() {
        return this.$store.getters.currentPipelineSettings.decimate
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"decimate": val});
      },
    },
      numIterations: {
        get() {
          return this.$store.getters.currentPipelineSettings.numIterations
        },
        set(val) {
          this.$store.commit("mutatePipeline", {"numIterations": val});
        },
      },
        cornerAccuracy: {
          get() {
            return this.$store.getters.currentPipelineSettings.cornerAccuracy
          },
          set(val) {
            this.$store.commit("mutatePipeline", {"cornerAccuracy": val});
          },
        },
    },
    methods: {
    }
  }
</script>
