<template>
  <div>
    <v-row
      class="pa-3"
      no-gutters
    >
      <v-col
        cols="12"
        style="max-width: 1400px"
      >
        <v-card
          v-for="item in tabList"
          :key="item.name"
          dark
          class="mb-3 pr-6 pb-3"
          style="background-color: #006492;"
        >
          <v-card-title>{{ item.name }}</v-card-title>
          <component
            :is="item"
            class="ml-5"
          />
        </v-card>
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
    import Networking from './SettingsViews/Networking'
    import Lighting from "./SettingsViews/Lighting";
    import cvImage from '../components/common/cv-image'
    import Stats from "./SettingsViews/Stats";
    import DeviceControl from "./SettingsViews/DeviceControl";

    export default {
        name: 'SettingsTab',
        components: {
            cvImage,
            // General,
        },
        data() {
            return {
                selectedTab: 0,
                snack: false,
                calibrationInProgress: false,
                snackbar: {
                  color: "accent",
                  text: ""
                },
            }
        },
        computed: {
            selectedComponent: {
                get() {
                    return this.tabList[this.selectedTab];
                }
            },
            settings: {
                get() {
                    return this.$store.state.settings;
                }
            },
            tabList: {
                get() {
                    return [Stats, DeviceControl, Networking].concat(this.$store.state.settings.lighting.supported ? Lighting : []);
                }
            }
        },
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
</style>
