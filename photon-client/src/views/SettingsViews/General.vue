<template>
  <div>
    <v-row class="pa-4">
      <span>{{ infoTabs.join('  —  ') }}</span>
    </v-row>

    <v-row class="pa-4">
      <span>CPU Usage: {{ metrics.cpuUtil.replace(" ", "") }}%</span>
      &nbsp;&ndash;&nbsp;
      <span>CPU Temp: {{ parseInt(metrics.cpuTemp) }}&deg;&nbsp;C</span>
      &nbsp;&ndash;&nbsp;
      <span>CPU Memory Usage: {{ metrics.ramUtil.replace(" ", "") }}MB of {{ metrics.cpuMem }}MB</span>
      &ndash;
      <span>GPU Temp: {{ parseInt(metrics.gpuTemp) }}&deg;&nbsp;C</span>
      &ndash;
      <span>GPU Memory Usage: {{ metrics.gpuMem }}MB of {{ metrics.gpuMemUtil }}MB</span>
    </v-row>

    <v-row>
      <v-col
        cols="12"
        sm="6"
        lg="3"
      >
        <v-btn
          color="secondary"
          @click="$refs.exportSettings.click()"
        >
          <v-icon left>
            mdi-download
          </v-icon>
          Export Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        sm="6"
        lg="3"
      >
        <v-btn
          color="secondary"
          @click="$refs.importSettings.click()"
        >
          <v-icon left>
            mdi-upload
          </v-icon>
          Import Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="3"
      >
        <v-btn
          color="red"
          @click="restartProgram()"
        >
          <v-icon left>
            mdi-restart
          </v-icon>
          Restart Photon
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="3"
      >
        <v-btn
          color="red"
          @click="restartDevice()"
        >
          <v-icon left>
            mdi-restart
          </v-icon>
          Restart Device
        </v-btn>
      </v-col>
    </v-row>
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
      timeout="0"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>

    <!-- Special hidden upload input that gets 'clicked' when the user imports settings -->
    <input
      ref="importSettings"
      type="file"
      accept=".zip"
      style="display: none;"

      @change="readImportedSettings"
    >
    <!-- Special hidden link that gets 'clicked' when the user exports settings -->
    <a
      ref="exportSettings"
      style="color: black; text-decoration: none; display: none"
      :href="'http://' + this.$address + '/api/settings/photonvision_config.zip'"
      download="photonvision-settings.zip"
    />
  </div>
</template>

<script>
export default {
    name: 'General',
    data() {
        return {
            snack: false,
            snackbar: {
                color: "success",
                text: ""
            },
        }
    },
    computed: {
        settings() {
            return this.$store.state.settings.general;
        },
        infoTabs() {
            let ret = [];
            let idx = 0;
            ret[idx++] = `Version: ${this.settings.version}`;
            if (this.settings.hardwareModel !== '') {
                ret[idx++] = `Hardware model: ${this.settings.hardwareModel}`;
            }
            ret[idx++] = `Platform: ${this.settings.hardwarePlatform}`;
            ret[idx++] = `GPU Acceleration: ${this.settings.gpuAcceleration ? "Enabled" : "Unsupported"}${this.settings.gpuAcceleration ? " (" + this.settings.gpuAcceleration + " mode)" : ""}`

            return ret;
        },
        metrics() {
            return this.$store.state.metrics;
        }
    },
    methods: {
        restartProgram() {
            this.axios.post('http://' + this.$address + '/api/restartProgram', {});
        },
        restartDevice() {
            this.axios.post('http://' + this.$address + '/api/restartDevice', {});
        },
        readImportedSettings(event) {
            let formData = new FormData();
            formData.append("zipData", event.target.files[0]);
            this.axios.post("http://" + this.$address + "/api/settings/import", formData,
                {headers: {"Content-Type": "multipart/form-data"}}).then(() => {
                this.snackbar = {
                    color: "success",
                    text: "Settings imported successfully! Program will now exit...",
                };
                this.snack = true;
            }).catch(() => {
                this.snackbar = {
                    color: "success",
                    text: "Settings imported successfully! Program will now exit...",
                };
                this.snack = true;
            });
        },
    }
}
</script>

<style lang="css" scoped>
.v-btn {
    width: 100%;
}
</style>