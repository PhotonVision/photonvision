<template>
  <div>
    <CVselect
      v-model="contourTargetOffsetPointEdge"
      name="Target Offset Point"
      tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
      :list="['Center','Top','Bottom','Left','Right']"
      @input="handlePipelineData('contourTargetOffsetPointEdge')"
      @rollback="e=> rollback('contourTargetOffsetPointEdge', e)"
    />

    <CVselect
      v-if="!isTagPipeline"
      v-model="contourTargetOrientation"
      name="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
      :list="['Portrait', 'Landscape']"
      @input="handlePipelineData('contourTargetOrientation')"
      @rollback="e=> rollback('contourTargetOrientation', e)"
    />

    <CVswitch
      v-model="outputShowMultipleTargets"
      name="Show Multiple Targets"
      tooltip="If enabled, up to five targets will be displayed and sent to user code, instead of just one"
      :disabled="isTagPipeline"
      class="mb-4"
      text-cols="3"
      @input="handlePipelineData('outputShowMultipleTargets')"

      @rollback="e=> rollback('outputShowMultipleTargets', e)"
    />
    <v-divider />
    <CVselect
      v-model="offsetRobotOffsetMode"
      name="Robot Offset Mode"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
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
                    switch (this.offsetRobotOffsetMode) {
                        case 0:
                            return null;
                        case 1:
                            return SingleCalibration;
                        case 2:
                            return DualCalibration;
                    }
                    return ""
                }
            },
            rawPoint: {
                get() {
                    return undefined; // TODO fix
                }
            },
            isTagPipeline: {
                get() {
                     return this.$store.getters.currentPipelineSettings.pipelineType > 3;
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
