<template>
  <div>
    <!-- Special hidden upload input that gets 'clicked' when the user selects the right dropdown item -->
    <input
      ref="file"
      type="file"
      accept=".csv"
      style="display: none;"

      @change="readFile"
    >

    <v-select
      v-model="selectedModel"
      dark
      color="accent"
      item-color="secondary"
      label="Select a target model"
      :items="FRCtargets"
      item-text="name"
      item-value="data"
      @change="onModelSelect"
    />
    <CVslider
      v-model="cornerDetectionAccuracyPercentage"
      class="pt-2"
      slider-cols="12"
      name="Contour simplification amount"
      :disabled="selectedModel === null"
      min="0"
      max="100"
      @input="handlePipelineData('cornerDetectionAccuracyPercentage')"
      @rollback="e => rollback('cornerDetectionAccuracyPercentage', e)"
    />
    <mini-map
      class="miniMapClass"
      :targets="targets"
      :horizontal-f-o-v="horizontalFOV"
    />
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>
  </div>
</template>

<script>
    import Papa from 'papaparse';
    import miniMap from '../../components/pipeline/3D/MiniMap';
    import CVslider from '../../components/common/cv-slider'
    import FRCtargetsConfig from '../../assets/FRCtargets'

    export default {
        name: "SolvePNP",
        components: {
            CVslider,
            miniMap
        },
        data() {
            return {
                FRCtargets: null,
                snackbar: {
                    color: "Success",
                    text: ""
                },
                snack: false,
                selectedModel: {
                    isCustom: false
                }
            }
        },
        computed: {
            cornerDetectionAccuracyPercentage: {
                get() {
                    return this.$store.getters.currentPipelineSettings.cornerDetectionAccuracyPercentage
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cornerDetectionAccuracyPercentage": val});
                }
            },
            targets: {
                get() {
                    return this.$store.getters.currentPipelineResults.targets;
                }
            },
            horizontalFOV: {
                get() {
                    let index = this.$store.getters.currentPipelineSettings.cameraVideoModeIndex;
                    let FOV = this.$store.getters.currentCameraSettings.fov;
                    let resolution = this.$store.getters.videoFormatList[index];
                    let diagonalView = FOV * (Math.PI / 180);
                    let diagonalAspect = Math.hypot(resolution.width, resolution.height);
                    return Math.atan(Math.tan(diagonalView / 2) * (resolution.width / diagonalAspect)) * 2 * (180 / Math.PI)
                }
            },
            allow3D: {
                get() {
                    return this.$store.getters.currentCameraSettings.calibrated;
                }
            }
        },
        mounted() {
            let tmp = [];
            for (let t in FRCtargetsConfig) {
                if (FRCtargetsConfig.hasOwnProperty(t)) {
                    tmp.push({name: t, data: FRCtargetsConfig[t]});
                }
            }

            // Special dropdown item for uploading your own model
            // data is what gets put in selectedMode, so we add a special field
            tmp.push({name: "Custom model", data: {isCustom: true}});

            this.FRCtargets = tmp;
        },
        methods: {
            readFile(event) {
                let file = event.target.files[0];
                Papa.parse(file, {
                    complete: this.onParse,
                    skipEmptyLines: true
                });
            },
            onModelSelect() {
              if (this.selectedModel.isCustom) {
                this.$refs.file.click();
              } else {
                this.uploadPremade();
              }
            },
            onParse(result) {
                if (result.data.length > 0) {
                    let data = [];
                    for (let i = 0; i < result.data.length; i++) {
                        let item = result.data[i];

                        let tmp = [];
                        tmp.push(Number(item[0]));
                        tmp.push(Number(item[1]));
                        if (isNaN(tmp[0]) || isNaN(tmp[1])) {
                            this.snackbar = {
                                color: "error",
                                text: `Error: custom target CSV contained a non-numeric value on line ${i + 1}`
                            };
                            this.snack = true;

                            this.selectedModel = null;
                            return;
                        }
                        data.push(tmp);
                    }
                    this.uploadModel(data);
                } else {
                    this.snackbar = {
                        color: "error",
                        text: "Error: custom target CSV was empty"
                    };
                    this.snack = true;

                    this.selectedModel = null;
                }
            },
            uploadPremade() {
                this.uploadModel(this.selectedModel, true);
            },
            uploadModel(model, premade = false) {
                this.axios.post("http://" + this.$address + "/api/vision/pnpModel", {
                    ['targetModel']: model,
                    ['index']: this.$store.getters.currentCameraIndex
                }).then(() => {
                    this.snackbar = {
                        color: "success",
                        text: premade ? "Target model changed successfully" : "Custom target model uploaded and selected successfully"
                    };
                    this.snack = true;
                }).catch(() => {
                    this.snackbar = {
                        color: "error",
                        text: "An error occurred selecting a target model"
                    };
                    this.snack = true;

                    this.selectedModel = null;
                });
            }
        }
    }
</script>

<style scoped>
    .miniMapClass {
        width: 400px !important;
        height: 100% !important;

        margin-left: auto;
        margin-right: auto;
    }
</style>