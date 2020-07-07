<template>
  <div>
    <v-row
      align="center"
      justify="start"
      dense
    >
      <v-col :cols="6">
        <CVswitch
          v-model="value.is3D"
          :disabled="allow3D"
          name="Enable 3D"
          @input="handleData('is3D')"
          @rollback="e=> rollback('is3D',e)"
        />
      </v-col>
      <v-col>
        <input
          ref="file"
          type="file"
          style="display: none"
          accept=".csv"
          @change="readFile"
        >
        <v-btn
          small
          @click="$refs.file.click()"
        >
          <v-icon>mdi-upload</v-icon>
          upload model
        </v-btn>
      </v-col>
    </v-row>
    <CVslider
      v-model="value.accuracy"
      name="Contour simplification"
      :min="0"
      :max="100"
      @input="handleData('accuracy')"
      @rollback="e=> rollback('accuracy',e)"
    />
    <v-row>
      <v-col>
        <mini-map
          class="miniMapClass"
          :targets="targets"
          :horizontal-f-o-v="horizontalFOV"
        />
      </v-col>
      <v-col>
        <v-select
          v-model="selectedModel"
          :items="FRCtargets"
          item-text="name"
          item-value="data"
          dark
          color="#ffd843"
          item-color="green"
        />
        <v-btn
          v-if="selectedModel !== null"
          small
          @click="uploadPremade"
        >
          Upload Premade
        </v-btn>
      </v-col>
    </v-row>
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
    import CVswitch from '../../components/common/cv-switch';
    import CVslider from '../../components/common/cv-slider'
    import FRCtargetsConfig from '../../assets/FRCtargets'

    export default {
        name: "SolvePNP",
        components: {
            CVswitch,
            CVslider,
            miniMap
        },
      // eslint-disable-next-line vue/require-prop-types
        props: ['value'],
        data() {
            return {
                is3D: false,
                selectedModel: null,
                FRCtargets: null,
                snackbar: {
                    color: "success",
                    text: ""
                },
                snack: false
            }
        },
        computed: {
            targets: {
                get() {
                    return 330; // TODO fix
                }
            },
            horizontalFOV: {
                get() {
                    let index = this.$store.state.cameraSettings.resolution;
                    let FOV = this.$store.state.cameraSettings.fov;
                    let resolution = this.$store.getters.videoFormatList[index];
                    let diagonalView = FOV * (Math.PI / 180);
                    let diagonalAspect = Math.hypot(resolution.width, resolution.height);
                    return Math.atan(Math.tan(diagonalView / 2) * (resolution.width / diagonalAspect)) * 2 * (180 / Math.PI)
                }
            },
            allow3D: {
                get() {
                    let index = this.$store.state.cameraSettings.resolution;
                    let currentRes = this.$store.getters.videoFormatList[index];
                    for (let res of this.$store.state.cameraSettings.calibrated) {
                        if (currentRes.width === res.width && currentRes.height === res.height) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        },
        mounted() {
            let tmp = [];
            for (let t in FRCtargetsConfig) {
                if (FRCtargetsConfig.hasOwnProperty(t)) {
                    tmp.push({name: t, data: FRCtargetsConfig[t]})
                }
            }
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
            onParse(result) {
                if (result.data.length > 0) {


                    let data = [];
                    for (let item of result.data) {
                        let tmp = [];
                        tmp.push(Number(item[0]));
                        tmp.push(Number(item[1]));
                        if (isNaN(tmp[0]) || isNaN(tmp[1])) {
                            this.snackbar = {
                                color: "error",
                                text: "Error: cvs did parse correctly"
                            };
                            this.snack = true;
                            return;
                        }
                        data.push(tmp);
                    }
                    this.uploadModel(data);
                } else {
                    this.snackbar = {
                        color: "error",
                        text: "Error: cvs did not contain any data"
                    };
                    this.snack = true;
                }
            },
            uploadPremade() {
                this.uploadModel(this.selectedModel);
            },
            uploadModel(model) {
                this.axios.post("http://" + this.$address + "/api/vision/pnpModel", model).then(() => {
                    this.snackbar = {
                        color: "success",
                        text: "File uploaded successfully"
                    };
                    this.snack = true;
                }).catch(() => {
                    this.snackbar = {
                        color: "error",
                        text: "An error occurred"
                    };
                    this.snack = true;
                })
            }
        }
    }
</script>

<style scoped>
    .miniMapClass {
        width: 50% !important;
        height: 50% !important;
    }
</style>