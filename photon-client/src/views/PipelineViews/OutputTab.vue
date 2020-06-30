<template>
  <div>
    <CVselect
      v-model="value.sortMode"
      name="Sort Mode"
      :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']"
      @input="handleData('sortMode')"
      @rollback="rollback('sortMode',e)"
    />

    <CVselect
      v-model="value.targetRegion"
      name="Target Region"
      :list="['Center','Top','Bottom','Left','Right']"
      @input="handleData('targetRegion')"
      @rollback="e=> rollback('targetRegion',e)"
    />

    <CVselect
      v-model="value.targetOrientation"
      name="Target Orientation"
      :list="['Portrait', 'Landscape']"
      @input="handleData('targetOrientation')"
      @rollback="e=> rollback('targetOrientation',e)"
    />

    <CVswitch
      v-model="value.multiple"
      name="Output multiple"
      @input="handleData('multiple')"
      @rollback="e=> rollback('multiple',e)"
    />
    <span>Calibrate:</span>
    <v-divider
      dark
      color="white"
    />
    <CVselect
      v-model="value.calibrationMode"
      name="Calibration Mode"
      :list="['None','Single point','Dual point']"
      @input="handleData('calibrationMode')"
      @rollback="e=> rollback('calibrationMode',e)"
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
        props: ['value'],

        data() {
            return {
                snackbar: false,
                snackbarText: ""
            }
        },
        computed: {
            selectedComponent: {
                get() {
                    switch (this.value.calibrationMode) {
                        case 0:
                            return "";
                        case 1:
                            return "SingleCalibration";
                        case 2:
                            return "DualCalibration"
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