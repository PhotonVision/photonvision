<template>
  <div>
    <CVslider
      v-model="decimate"
      class="pt-2"
      slider-cols="8"
      name="Decimate"
      min="1"
      max="5"
      step="1.0"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      @input="handlePipelineData('decimate')"
    />
    <CVslider
      v-model="cornerIterations"
      class="pt-2"
      slider-cols="8"
      name="cornerIterations"
      min="1"
      max="100"
      step="1"
      tooltip="Corner iterations lmao"
      @input="handlePipelineData('threads')"
    />
    <CVswitch
      v-model="useAruco3"
      class="pt-2"
      slider-cols="8"
      name="Use Aruco3 Detection"
      tooltip="make that stuff fast asf but also a little sus"
      @input="handlePipelineData('useAruco3')"
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
