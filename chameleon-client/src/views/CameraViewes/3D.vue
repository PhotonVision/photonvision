<template>
    <div>
        <v-row align="center" justify="start" dense>
            <v-col :cols="6">
                <CVswitch :disabled="allow3D" v-model="value.is3D" name="Enable 3D" @input="handleData('is3D')"/>
            </v-col>
            <v-col>
                <input type="file" ref="file" style="display: none" accept=".csv" @change="readFile">
                <v-btn @click="$refs.file.click()" small>
                    <v-icon>mdi-upload</v-icon>
                    upload model
                </v-btn>
            </v-col>
        </v-row>
        <v-row>
            <v-col>
                <mini-map class="miniMapClass" :targets="targets" :horizontal-f-o-v="horizontalFOV"/>
            </v-col>
            <v-col>
                <v-select v-model="selectedModel" :items="FRCtargets" item-text="name" item-value="data" dark
                          color="#4baf62" item-color="green"/>
                <v-btn small v-if="selectedModel !== null" @click="uploadPremade">Upload Premade</v-btn>
            </v-col>
        </v-row>
        <v-snackbar v-model="snack" top :color="snackbar.color">
            <span>{{snackbar.text}}</span>
        </v-snackbar>
    </div>
</template>

<script>
    import miniMap from '../../components/3D/MiniMap';
    import CVswitch from '../../components/cv-switch';
    import Papa from 'papaparse';
    import FRCtargetsConfig from '../../assets/FRCtargets'

    export default {
        name: "solvePNP",
        props: ['value'],
        components: {
            CVswitch,
            miniMap
        },
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
        methods: {
            handleData(val) {
                this.handleInput(val, this.value[val]);
                this.$emit('update')
            },
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
                this.axios.post("http://" + this.$address + "/api/vision/pnpModel", model).then((response) => {
                    this.snackbar = {
                        color: "success",
                        text: "File uploaded successfully"
                    };
                    this.snack = true;
                }).catch((error) => {
                    this.snackbar = {
                        color: "error",
                        text: "An error occurred"
                    };
                    this.snack = true;
                })
            }
        },
        computed: {
            targets: {
                get() {
                    return this.$store.state.point.targets;
                }
            },
            horizontalFOV: {
                get() {
                    let index = this.$store.state.cameraSettings.resolution;
                    let FOV = this.$store.state.cameraSettings.fov;
                    let resolution = this.$store.state.resolutionList[index];
                    let diagonalView = FOV * (Math.PI / 180);
                    let diagonalAspect = Math.hypot(resolution.width, resolution.height);
                    return Math.atan(Math.tan(diagonalView / 2) * (resolution.width / diagonalAspect)) * 2 * (180 / Math.PI)
                }
            },
            allow3D: {
                get() {
                    let currentRes = this.$store.state.resolutionList[this.$store.state.pipeline.videoModeIndex];
                    for (let res of this.$store.state.cameraSettings.calibration) {
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
                tmp.push({name: t, data: FRCtargetsConfig[t]})
            }
            this.FRCtargets = tmp;
        }
    }
</script>

<style scoped>
    .miniMapClass {
        width: 50% !important;
        height: 50% !important;
    }
</style>