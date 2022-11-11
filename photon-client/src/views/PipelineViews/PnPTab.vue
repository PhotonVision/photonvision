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
      :items="targetList"
      item-text="name"
      item-value="data"
      @input="handlePipelineUpdate('targetModel', targetList.indexOf(selectedModel))"
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
    import CVslider from '../../components/common/cv-slider'

    export default {
        name: "PnP",
        components: {
            CVslider
        },
        data() {
            return {
                targetList: ['2020 High Goal Outer', '2020 High Goal Inner', '2019 Dual Target', '2020 Power Cell (7in)','2022 Cargo Ball (9.5in)', '2016 High Goal', '6.5in (36h11) AprilTag', '6in (16h5) AprilTag'], //Keep in sync with TargetModel.java
                snackbar: {
                    color: "Success",
                    text: ""
                },
                snack: false,
            }
        },
        computed: {
            selectedModel: {
                get() {
                    let ret = this.$store.getters.currentPipelineSettings.targetModel
                    return this.targetList[ret];
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"targetModel": this.targetList.indexOf(val)})
                }
            },
            cornerDetectionAccuracyPercentage: {
                get() {
                    return this.$store.getters.currentPipelineSettings.cornerDetectionAccuracyPercentage
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cornerDetectionAccuracyPercentage": val});
                }
            },
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
