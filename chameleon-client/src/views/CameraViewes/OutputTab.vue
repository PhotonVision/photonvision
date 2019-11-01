<template>
    <div>
        <CVselect name="SortMode" v-model="value.sortMode"
                  :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']"
                  @input="handleData('sortMode')"/>
        <span>Calibrate:</span>
        <v-divider dark color="white"/>
        <CVselect name="Calibration Mode" v-model="value.calibrationMode" :list="['None','Single point','Dual point']"
                  @input="handleData('calibrationMode')"/>
        <component :raw-point="rawPoint" :is="selectedComponent" @update="doUpdate"/>
        <v-snackbar :timeout="3000" v-model="snackbar" top color="error">
            <span style="color:#000">Points are too close</span>
            <v-btn color="black" text @click="snackbar = false">Close</v-btn>
        </v-snackbar>
    </div>
</template>

<script>
    import CVselect from '../../components/cv-select'
    import DualCalibration from "../../components/OutputTab/DualCalibration";
    import SingleCalibration from "../../components/OutputTab/SingleCalibration";

    export default {
        name: 'Output',
        props: ['value'],
        components: {
            CVselect,
            SingleCalibration,
            DualCalibration,

        },
        methods: {
            handleData(val) {
                this.handleInput(val, this.value[val]);
                this.$emit('update')
            },
            doUpdate() {
                this.$emit('update')
            }
        },

        data() {
            return {
                snackbar: false,
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
                    return this.$store.state.point.rawPoint;
                }
            }
        }
    }
</script>

<style scoped>
</style>