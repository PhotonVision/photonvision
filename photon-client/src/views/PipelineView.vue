<template>
  <div>
    <v-container
      class="pa-3"
      fluid
    >
      <v-row
        no-gutters
        align="center"
        justify="center"
      >
        <v-col
          cols="12"
          :class="['pb-3 ', $store.getters.isDriverMode ? '' : 'pr-lg-3']"
          :lg="$store.getters.isDriverMode ? 12 : 8"
          align-self="stretch"
        >
          <v-card
            color="primary"
            height="100%"
            dark
          >
            <v-card-title
              class="pb-0 mb-0 pl-4 pt-1"
              style="height: 10%;"
            >
              Cameras
            </v-card-title>
            <v-row
              align="center"
              style="height: 90%;"
            >
              <v-col
                v-for="idx in (selectedOutputs instanceof Array ? selectedOutputs : [selectedOutputs])"
                :key="idx"
                cols="12"
                :md="selectedOutputs.length === 1 ? 12 : Math.floor(12 / selectedOutputs.length)"
              >
                <div style="position: relative; width: 100%; height: 100%;">
                  <cvImage
                    :address="$store.getters.streamAddress[idx]"
                    scale="100"
                    max-height="300px"
                    max-height-md="320px"
                    max-height-xl="450px"
                    :alt="'Stream' + idx"
                    @click="onImageClick"
                  />
                  <span style="position: absolute; top: 2%; left: 2%; font-size: 28px; -webkit-text-stroke: 1px black;">{{ parseFloat(fps).toFixed(2) }}</span>
                </div>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
        <v-col
          cols="12"
          class="pb-3"
          :lg="$store.getters.isDriverMode ? 12 : 4"
          align-self="stretch"
        >
          <v-card
            color="primary"
          >
            <camera-and-pipeline-select />
          </v-card>
          <v-card
            v-if="!$store.getters.isDriverMode"
            class="mt-3"
            color="primary"
          >
            <v-row
              align="center"
              class="pl-3 pr-3"
            >
              <v-col lg="12">
                <p style="color: white;">
                  Processing mode:
                </p>
                <v-btn-toggle
                  v-model="processingMode"
                  mandatory
                  dark
                  class="fill"
                >
                  <v-btn color="secondary">
                    <v-icon>mdi-cube-outline</v-icon>
                    <span>3D</span>
                  </v-btn>
                  <v-btn color="secondary">
                    <v-icon>mdi-crop-square</v-icon>
                    <span>2D</span>
                  </v-btn>
                </v-btn-toggle>
              </v-col>
              <v-col lg="12">
                <p style="color: white;">
                  Stream display:
                </p>
                <v-btn-toggle
                  v-model="selectedOutputs"
                  :multiple="$vuetify.breakpoint.mdAndUp"
                  mandatory
                  dark
                  class="fill"
                >
                  <v-btn
                    color="secondary"
                    class="fill"
                  >
                    <v-icon>mdi-palette</v-icon>
                    <span>Normal</span>
                  </v-btn>
                  <v-btn
                    color="secondary"
                    class="fill"
                  >
                    <v-icon>mdi-compare</v-icon>
                    <span>Threshold</span>
                  </v-btn>
                </v-btn-toggle>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
      </v-row>
      <v-row no-gutters>
        <v-col
          v-for="(tabs, idx) in tabGroups"
          :key="idx"
          :cols="Math.floor(12 / tabGroups.length)"
          :class="idx != tabGroups.length - 1 ? 'pr-3' : ''"
          align-self="stretch"
        >
          <v-card
            color="primary"
            height="100%"
            class="pr-4 pl-4"
          >
            <v-tabs
              v-if="!$store.getters.isDriverMode"
              v-model="selectedTabs[idx]"
              grow
              background-color="primary"
              dark
              height="48"
              slider-color="accent"
            >
              <v-tab
                v-for="(tab, i) in tabs.filter(it => it.name !== '3D' || is3D)"
                :key="i"
              >
                {{ tab.name }}
              </v-tab>
            </v-tabs>
            <div class="pl-4 pr-4 pt-2">
              <keep-alive>
                <!-- vision component -->
                <component
                  :is="(tabs[selectedTabs[idx]] || tabs[0]).component"
                  ref="component"
                  v-model="$store.getters.pipeline"
                  :is3d="is3D"
                  @update="$emit('save')"
                />
              </keep-alive>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
    <!-- snack bar -->
    <v-snackbar
      v-model="snackbar"
      :timeout="3000"
      top
      color="error"
    >
      <span style="color:#000">Can not remove the only pipeline!</span>
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
    import CameraAndPipelineSelect from "../components/pipeline/CameraAndPipelineSelect";
    import cvImage from '../components/common/cv-image';
    import InputTab from './PipelineViews/InputTab';
    import ThresholdTab from './PipelineViews/ThresholdTab';
    import ContoursTab from './PipelineViews/ContoursTab';
    import OutputTab from './PipelineViews/OutputTab';
    import TargetsTab from "./PipelineViews/TargetsTab";
    import PnPTab from './PipelineViews/PnPTab';

    export default {
        name: 'CameraTab',
        components: {
            CameraAndPipelineSelect,
            cvImage,
            InputTab,
            ThresholdTab,
            ContoursTab,
            OutputTab,
            TargetsTab,
            PnPTab,
        },
        data() {
            return {
                selectedTabs: [0, 0, 0, 0],
                snackbar: false,
                is3D: false,
            }
        },
        computed: {
            tabGroups: {
                get() {
                    let tabs = {
                        input: {
                            name: "Input",
                            component: "InputTab",
                        },
                        threshold: {
                            name: "Threshold",
                            component: "ThresholdTab",
                        },
                        contours: {
                            name: "Contours",
                            component: "ContoursTab",
                        },
                        output: {
                            name: "Output",
                            component: "OutputTab",
                        },
                        targets: {
                            name: "Target Info",
                            component: "TargetsTab",
                        },
                        pnp: {
                            name: "3D",
                            component: "PnPTab",
                        }
                    };

                    // 2D array of tab names and component names; each sub-array is a separate tab group
                    let ret = [];
                    if (this.$vuetify.breakpoint.smAndDown || !this.$store.state.compactMode || this.$store.getters.isDriverMode) {
                        // One big tab group with all the tabs
                        ret[0] = Object.values(tabs);
                    } else if (this.$vuetify.breakpoint.mdAndDown) {
                        // Two tab groups, one with "input, threshold, contours, output" and the other with "target info, 3D"
                        ret[0] = [tabs.input, tabs.threshold, tabs.contours, tabs.output];
                        ret[1] = [tabs.targets, tabs.pnp];
                    } else if (this.$vuetify.breakpoint.lgAndDown) {
                        // Three tab groups, one with "input", one with "threshold, contours, output", and the other with "target info, 3D"
                        ret[0] = [tabs.input];
                        ret[1] = [tabs.threshold, tabs.contours, tabs.output];
                        ret[2] = [tabs.targets, tabs.pnp];
                    } else if (this.$vuetify.breakpoint.xl) {
                        // Three tab groups, one with "input", one with "threshold, contours", and the other with "output, target info, 3D"
                        ret[0] = [tabs.input];
                        ret[1] = [tabs.threshold];
                        ret[2] = [tabs.contours, tabs.output]
                        ret[3] = [tabs.targets, tabs.pnp];
                    }

                    return ret;
                }
            },
            processingMode: {
                get() {
                    return this.is3D ? 0 : 1;
                },
                set(value) {
                    this.is3D = value === 0;
                }
            },
            selectedOutputs: {
                // All this logic exists to deal with the reality that the output select buttons sometimes need an array and sometimes need a number (depending on whether or not they're exclusive)
                get() {
                    // We switch the selector to single-select only on sm-and-down size devices, so we have to return a Number instead of an Array in that state
                    let ret = 0;
                    if (!this.$store.getters.isDriverMode) {
                        ret = this.$store.state.selectedOutputs || [0];
                    }

                    if (this.$vuetify.breakpoint.mdAndUp) {
                        return ret;
                    } else {
                        return ret[0] || 0;
                    }
                },
                set(value) {
                    let valToCommit = [0];
                    if (value instanceof Array) {
                        // Value is already an array, we don't need to do anything
                        value.sort(); // Sort for visual consistency
                        valToCommit = value;
                    } else if (value) {
                        // Value is assumed to be a number, so we wrap it into an array
                        valToCommit = [value];
                    }
                    this.$store.commit("selectedOutputs", valToCommit);
                    // TODO: Currently the backend just sends both streams regardless of the selected outputs value, so we don't need to send anything
                    // this.handlePipelineUpdate('selectedOutputs', valToCommit);
                }
            },
            fps: {
                get() {
                    return this.$store.getters.currentCameraFPS;
                }
            },
            latency: {
                get() {
                    return this.$store.getters.currentPipelineResults.latency;
                }
            }
        },
        methods: {
            onImageClick(event) {
                if (this.selectedTab === 1) {
                    this.$refs.component.onClick(event);
                }
            },
        }
    }
</script>

<style scoped>
    .v-btn-toggle.fill {
        width: 100%;
        height: 100%;
    }

    .v-btn-toggle.fill > .v-btn {
        width: 50%;
        height: 100%;
    }

    .colsClass {
        padding: 0 !important;
    }

    .videoClass {
        text-align: center;
    }

    th {
        width: 80px;
        text-align: center;
    }
</style>