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
      slider-cols="12"
      name="Decimate"
      min="0"
      max="3"
      step=".01"
      @input="handlePipelineData('decimate')"
    />
    <CVslider
      v-model="blur"
      class="pt-2"
      slider-cols="12"
      name="Blur"
      min="0"
      max="5"
      step=".01"
      @input="handlePipelineData('blur')"
    />
    <CVslider
      v-model="threads"
      class="pt-2"
      slider-cols="12"
      name="Threads"
      min="1"
      max="8"
      step="1"
      @input="handlePipelineData('threads')"
    />
    <CVswitch
      v-model="refineEdges"
      class="pt-2"
      slider-cols="12"
      name="Refine Edges"
      @input="handlePipelineData('refineEdges')"
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
