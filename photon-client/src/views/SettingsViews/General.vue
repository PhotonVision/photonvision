<template>
  <div>
    <v-row class="pa-4">
      <table class="infoTable">
        <tr>
          <th class="infoElem"> Version </th>
          <th class="infoElem"> Hardware Model </th>
          <th class="infoElem"> Platform </th>
          <th class="infoElem"> GPU Acceleration </th>
        </tr>
        <tr>
          <td class="infoElem">{{ version.replace(" ", "") }}</td>
          <td class="infoElem">{{ hwModel.replace(" ", "") }}</td>
          <td class="infoElem">{{ platform.replace(" ", "") }}</td>
          <td class="infoElem">{{ gpuAccel.replace(" ", "") }}</td>
        </tr>
      </table>

      <table class="infoTable">
        <tr>
          <th class="infoElem"> CPU Usage </th>
          <th class="infoElem"> CPU Temp </th>
          <th class="infoElem"> CPU Memory Usage </th>
          <th class="infoElem"> GPU Memory Usage </th>
          <th class="infoElem"> Disk Usage </th>
        </tr>
        <tr v-if="metrics.cpuUtil !== 'N/A'">
          <td class="infoElem">{{ metrics.cpuUtil.replace(" ", "") }}%</td>
          <td class="infoElem">{{ parseInt(metrics.cpuTemp) }}&deg;&nbsp;C</td>
          <td class="infoElem">{{ metrics.ramUtil.replace(" ", "") }}MB of {{ metrics.cpuMem }}MB</td>
          <td class="infoElem">{{ metrics.gpuMemUtil.replace(" ", "") }}MB of {{ metrics.gpuMem }}MB</td>
          <td class="infoElem">{{ metrics.diskUtilPct.replace(" ", "") }}</td>
        </tr>
        <tr v-if="metrics.cpuUtil === 'N/A'">
          <td class="infoElem">---</td>
          <td class="infoElem">---</td>
          <td class="infoElem">---</td>
          <td class="infoElem">---</td>
          <td class="infoElem">---</td>
        </tr>
      </table>

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
        version() {
          return `${this.settings.version}`;
        },
        hwModel() {
            if (this.settings.hardwareModel !== '') {
                return `${this.settings.hardwareModel}`;
            } else {
              return `Unknown`;
            }
        },
        platform() {
          return `${this.settings.hardwarePlatform}`;
        },
        gpuAccel() {
          return  `${this.settings.gpuAcceleration ? "Enabled" : "Unsupported"}${this.settings.gpuAcceleration ? " (" + this.settings.gpuAcceleration + " mode)" : ""}`
        },
        metrics() {
          console.log(this.$store.state.metrics);
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

.infoTable{
  border: 1px solid;
  border-collapse: separate;
  border-spacing: 0px;
  border-radius: 5px;
  text-align: left;
  margin-bottom: 10px;
  width: 100%;
}

.infoElem {
  padding-right: 15px;
  padding-bottom: 1px;
  padding-top: 1px;
  padding-left: 10px;
  border-right: 1px solid;
}

</style>