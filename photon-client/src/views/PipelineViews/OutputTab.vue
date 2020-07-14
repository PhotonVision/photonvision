<template>
  <div>
    <span>Contour Sorting</span>
    <v-divider class="mt-2" />
    <CVselect
      v-model="contourSortMode"
      name="Sort Mode"
      :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']"
      @input="handlePipelineData('contourSortMode')"
      @rollback="e => rollback('contourSortMode', e)"
    />

    <CVselect
      v-model="contourTargetOffsetPointEdge"
      name="Target Offset Point"
      :list="['Center','Top','Bottom','Left','Right']"
      @input="handlePipelineData('contourTargetOffsetPointEdge')"
      @rollback="e=> rollback('contourTargetOffsetPointEdge', e)"
    />

    <CVselect
      v-model="contourTargetOrientation"
      name="Target Orientation"
      :list="['Portrait', 'Landscape']"
      @input="handlePipelineData('contourTargetOrientation')"
      @rollback="e=> rollback('contourTargetOrientation', e)"
    />

    <CVswitch
      v-model="outputShowMultipleTargets"
      name="Show Multiple Targets"
      class="mb-4"
      @input="handlePipelineData('outputShowMultipleTargets')"

      @rollback="e=> rollback('outputShowMultipleTargets', e)"
    />
    <span>Robot Offset</span>
    <v-divider class="mt-2" />
    <CVselect
      v-model="offsetRobotOffsetMode"
      name="Robot Offset Mode"
      :list="['None','Single Point','Dual Point']"
      @input="handlePipelineData('offsetRobotOffsetMode')"
      @rollback="e=> rollback('offsetRobotOffsetMode',e)"
    />
    <component
      :is="selectedComponent"
      :raw-point="rawPoint"
      @update="doUpdate"
      @snackbar="showSnackbar"
    />
    <v-snackbar
      v-model="snackbar"
      :timeout="3000"
      top
      color="error"
    >
      <span style="color:#000">{{ snackbarText }}</span>
      <v-btn
        color="black"
        text
        @click="snackbar = false"
      >
        Close
      </v-btn>
    </v-snackbar>
  </div>
</template>

<script>
    import CVselect from '../../components/common/cv-select'
    import CVswitch from '../../components/common/cv-switch'
    import DualCalibration from "../../components/pipeline/OutputTab/DualCalibration";
    import SingleCalibration from "../../components/pipeline/OutputTab/SingleCalibration";


    export default {
        name: 'Output',
        components: {
            CVselect,
            CVswitch,
            SingleCalibration,
            DualCalibration,

        },
        // eslint-disable-next-line vue/require-prop-types
        props: ['value'],

        data() {
            return {
                snackbar: false,
                snackbarText: ""
            }
        },
        computed: {

            contourSortMode: {

                get() {
                    return this.$store.getters.currentPipelineSettings.contourSortMode
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"contourSortMode": val});
                }
            },
            contourTargetOffsetPointEdge: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourTargetOffsetPointEdge
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"contourTargetOffsetPointEdge": val});
                }
            },
            contourTargetOrientation: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourTargetOrientation
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"contourTargetOrientation": val});
                }
            },
            outputShowMultipleTargets: {
                get() {
                    return this.$store.getters.currentPipelineSettings.outputShowMultipleTargets
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"outputShowMultipleTargets": val});
                }
            },
            offsetRobotOffsetMode: {
                get() {
                    return this.$store.getters.currentPipelineSettings.offsetRobotOffsetMode
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"offsetRobotOffsetMode": val});
                }
            },

            selectedComponent: {
                get() {
                    switch (this.value.calibrationMode) {
                        case 0:
                            return "";
                        case 1:
                            return "Single Point";
                        case 2:
                            return "Dual Point"
                    }
                    return ""
                }
            },
            rawPoint: {
                get() {
                    return undefined; // TODO fix
                }
            }
        },
        methods: {
            doUpdate() {
                this.$emit('update')
            },
            showSnackbar(message) {
                this.snackbarText = message;
                this.snackbar = true;
            },
        }
    }
</script>

<style scoped>
</style>