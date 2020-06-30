<template>
  <div>
    <camera-and-pipeline-select />
    <v-row no-gutters>
      <!-- vision tabs -->
      <v-col
        cols="12"
        sm="12"
        md="7"
        xl="6"
        class="colsClass pr-8"
      >
        <v-tabs
          v-if="($store.getters.currentPipelineIndex + 1) !== 0"
          v-model="selectedTab"
          fixed-tabs
          background-color="#232c37"
          dark
          height="48"
          slider-color="#ffd843"
        >
          <v-tab>Input</v-tab>
          <v-tab>Threshold</v-tab>
          <v-tab>Contours</v-tab>
          <v-tab>Output</v-tab>
          <v-tab>3D</v-tab>
        </v-tabs>
        <div
          v-else
          style="height: 48px"
        />
        <div style="padding-left:30px">
          <keep-alive>
            <!-- vision component -->
            <component
              :is="selectedComponent"
              ref="component"
              v-model="$store.getters.pipeline"
              @update="$emit('save')"
            />
          </keep-alive>
        </div>
      </v-col>
      <v-col
        cols="12"
        sm="12"
        md="5"
        xl="6"
        class="colsClass"
      >
        <div>
          <!-- camera image tabs -->
          <v-tabs
            v-if="($store.getters.currentPipelineIndex + 1) !== 0"
            v-model="isBinaryNumber"
            background-color="#232c37"
            dark
            height="48"
            slider-color="#ffd843"
            centered
            style="padding-bottom:10px"
            @change="handleInput('isBinary',$store.getters.pipeline.isBinary)"
          >
            <v-tab>Normal</v-tab>
            <v-tab>Threshold</v-tab>
          </v-tabs>
          <div
            v-else
            style="height: 58px"
          />
          <!-- camera image stream -->
          <div class="videoClass">
            <v-row align="center">
              <div style="position: relative; width: 100%; height: 100%;">
                <cvImage
                  :address="$store.getters.streamAddress"
                  :scale="75"
                  @click="onImageClick"
                />
                <span style=" position: absolute; top: 0.2%; left: 13%;">FPS:{{ parseFloat(fps).toFixed(2) }}</span>
              </div>
            </v-row>

            <v-row align="center">
              <v-simple-table
                style="text-align: center;background-color: transparent; display: block;margin: auto"
                dense
                dark
              >
                <template v-slot:default>
                  <thead>
                    <tr>
                      <th class="text-center">
                        Target
                      </th>
                      <th class="text-center">
                        Pitch
                      </th>
                      <th class="text-center">
                        Yaw
                      </th>
                      <th class="text-center">
                        Area
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(value, index) in $store.getters.currentPipelineResults.targets"
                      :key="index"
                    >
                      <td>{{ index }}</td>
                      <td>{{ parseFloat(value['pitch']).toFixed(2) }}</td>
                      <td>{{ parseFloat(value['yaw']).toFixed(2) }}</td>
                      <td>{{ parseFloat(value['area']).toFixed(2) }}</td>
                    </tr>
                  </tbody>
                </template>
              </v-simple-table>
            </v-row>
          </div>
        </div>
      </v-col>
    </v-row>
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
    import cvImage from '../components/common/cv-image'
    import InputTab from './PipelineViews/InputTab'
    import ThresholdTab from './PipelineViews/ThresholdTab'
    import ContoursTab from './PipelineViews/ContoursTab'
    import OutputTab from './PipelineViews/OutputTab'
    import pnpTab from './PipelineViews/3D'

    export default {
        name: 'CameraTab',
        components: {
            CameraAndPipelineSelect,
            cvImage,
            InputTab,
            ThresholdTab,
            ContoursTab,
            OutputTab,
            pnpTab,
        },
        data() {
            return {
                selectedTab: 0,
                snackbar: false,
            }
        },
        computed: {
            isBinaryNumber: {
                get() {
                    return this.$store.getters.pipeline.isBinary ? 1 : 0;
                },
                set(value) {
                    this.$store.commit('isBinary', !!value);
                }
            },
            selectedComponent: {
                get() {
                    return (this.$store.getters.currentPipelineIndex + 1) === 0 ? "InputTab" : ["InputTab", "ThresholdTab", "ContoursTab", "OutputTab", "pnpTab"][this.selectedTab];
                }
            },
            fps: {
                get() {
                  return this.$store.getters.currentCameraFPS;
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