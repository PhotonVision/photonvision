<template>
  <div>
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
      v-model="cornerIterations"
      class="pt-2"
      slider-cols="8"
      name="cornerIterations"
      min="50"
      value = "60"
      max="500"
      step="1"
      tooltip="Corner iterations lmao"
      @input="handlePipelineData('cornerIterations')"
    />
    <CVswitch
      v-model="useAruco3"
      class="pt-2"
      slider-cols="8"
      name="Use Aruco3 Detection"
      tooltip="make that stuff fast asf but also a little sus"
      @input="handlePipelineData('useAruco3')"
  />
    <CVslider
        v-model="threads"
        class="pt-2"
        slider-cols="8"
        name="Threads"
        min="1"
        max="8"
        step="1"
        tooltip="Number of threads spawned by the Aruco detector"
        @input="handlePipelineData('threads')"
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
          CVswitch,
        },
        computed: {
          decimate: {
            get() {
                return this.$store.getters.currentPipelineSettings.decimate
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"decimate": val});
            }
          },
          cornerIterations: {
            get() {
                return this.$store.getters.currentPipelineSettings.cornerIterations
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"cornerIterations": val});
            }
          },
          useAruco3: {
            get() {
                return this.$store.getters.currentPipelineSettings.useAruco3
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"useAruco3": val});
            }
          },
        },
        methods: {
        }
    }
</script>
