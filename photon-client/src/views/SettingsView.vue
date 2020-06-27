<template>
  <div>
    <v-row>
      <v-col
        class="colsClass"
        cols="6"
      >
        <v-tabs
          v-model="selectedTab"
          background-color="#212121"
          dark
          fixed-tabs
          height="50"
          slider-color="#4baf62"
        >
          <v-tab to="">
            General
          </v-tab>
          <v-tab to="">
            Cameras
          </v-tab>
        </v-tabs>
        <div style="padding-left:30px">
          <component
            :is="selectedComponent"
            @update="$emit('save')"
          />
        </div>
      </v-col>
      <v-col
        v-show="selectedTab === 1"
        class="colsClass"
      >
        <div class="videoClass">
          <cvImage
            :address="$store.getters.streamAddress"
            :scale="75"
          />
        </div>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    import General from './SettingsViewes/General'
    import Cameras from './SettingsViewes/Cameras'
    import cvImage from '../components/common/cv-image'


    export default {
        name: 'SettingsTab',
        components: {
            cvImage,
            General,
            Cameras,
        },
        data() {
            return {
                selectedTab: 0,
                tabList: [General, Cameras]
            }
        },
        computed: {
            selectedComponent: {
                get() {
                    return this.tabList[this.selectedTab];
                }
            },
        }
    }
</script>

<style scoped>
    .videoClass {
        text-align: center;
    }

    .videoClass img {
        padding-top: 10px;
        height: auto !important;
        vertical-align: middle;
    }

    .colsClass {
        padding: 0 !important;
    }
</style>