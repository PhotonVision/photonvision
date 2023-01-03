<template>
  <div>
    <v-row class="pa-4">
      <table class="infoTable">
        <tr>
          <th class="infoElem">
            Version
          </th>
          <th class="infoElem">
            Hardware Model
          </th>
          <th class="infoElem">
            Platform
          </th>
          <th class="infoElem">
            GPU Acceleration
          </th>
        </tr>
        <tr>
          <td class="infoElem">
            {{ version.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ hwModel.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ platform.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ gpuAccel.replace(" ", "") }}
          </td>
        </tr>
      </table>

      <table class="infoTable">
        <tr>
          <th class="infoElem">
            CPU Usage
          </th>
          <th class="infoElem">
            CPU Temp
          </th>
          <th class="infoElem">
            CPU Memory Usage
          </th>
          <th class="infoElem">
            GPU Memory Usage
          </th>
          <th class="infoElem">
            Disk Usage
          </th>
          <th class="infoElem">
            <v-tooltip top>
              <template v-slot:activator="{ on, attrs }">
                <span
                  v-bind="attrs"
                  v-on="on"
                >
                ⓘ CPU Throttling
                </span>
              </template>
              <span>
                    Current or Previous Reason for the cpu being held back from maximum performance.
              </span>
            </v-tooltip>
          </th>
          <th class="infoElem">
            CPU Uptime
          </th>
        </tr>
        <tr v-if="metrics.cpuUtil !== 'N/A'">
          <td class="infoElem">
            {{ metrics.cpuUtil }}%
          </td>
          <td class="infoElem">
            {{ parseInt(metrics.cpuTemp) }}&deg;&nbsp;C
          </td>
          <td class="infoElem">
            {{ metrics.ramUtil }}MB of {{ metrics.cpuMem }}MB
          </td>
          <td class="infoElem">
            {{ metrics.gpuMemUtil }}MB of {{ metrics.gpuMem }}MB
          </td>
          <td class="infoElem">
            {{ metrics.diskUtilPct }}
          </td>
          <td class="infoElem">
            {{ metrics.cpuThr }}
          </td>
          <td class="infoElem">
            {{ metrics.cpuUptime }}
          </td>
        </tr>
        <tr v-if="metrics.cpuUtil === 'N/A'">
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
        </tr>
      </table>
    </v-row>

    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
      timeout="-1"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>

    <!-- Special hidden upload input that gets 'clicked' when the user imports settings -->
    <input
      ref="importSettings"
      type="file"
      accept=".zip, .json"
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

    <!-- Special hidden new jar upload input that gets 'clicked' when the user posts a new .jar -->
    <input
      ref="offlineUpdate"
      type="file"
      accept=".jar"
      style="display: none;"
      @change="doOfflineUpdate"
    >
  </div>
</template>

<script>
export default {
    name: 'Stats',
    data() {
        return {
            snack: false,
            uploadPercentage: 0.0,
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
          return  `${this.settings.gpuAcceleration ? "Enabled" : "Unsupported"} ${this.settings.gpuAcceleration ? "(" + this.settings.gpuAcceleration + ")" : ""}`
        },
        metrics() {
          // console.log(this.$store.state.metrics);
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
                    text: "Settings imported successfully! PhotonVision will restart in the background...",
                };
                this.snack = true;
            }).catch(err => {
                if (err.response) {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading settings file! Could not process provided file.",
                  };
                } else if (err.request) {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading settings file! No respond to upload attempt.",
                  };
                } else {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading settings file!",
                  };
                }
                this.snack = true;
            });
        },
        doOfflineUpdate(event) {
          this.snackbar = {
                color: "secondary",
                text: "New Software Upload in Process..."
            };
            this.snack = true;

            let formData = new FormData();
            formData.append("jarData", event.target.files[0]);
            this.axios.post("http://" + this.$address + "/api/settings/offlineUpdate", formData,
                {headers: {"Content-Type": "multipart/form-data"},
                 onUploadProgress: function( progressEvent ) {
                    this.uploadPercentage = parseInt( Math.round( ( progressEvent.loaded / progressEvent.total ) * 100 ) );
                    if(this.uploadPercentage < 99.5){
                      this.snackbar.text = "New Software Upload in Process, " + this.uploadPercentage + "% complete";
                    } else {
                      this.snackbar.text = "Installing uploaded software...";
                    }

                 }.bind(this)
                }).then(() => {
                this.snackbar = {
                    color: "success",
                    text: "New .jar copied successfully! PhotonVision will restart in the background...",
                };
                this.snack = true;
            }).catch(err => {
                if (err.response) {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading new .jar file! Could not process provided file.",
                  };
                } else if (err.request) {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading new .jar file! No respond to upload attempt.",
                  };
                } else {
                  this.snackbar = {
                      color: "error",
                      text: "Error while uploading new .jar file!",
                  };
                }
                this.snack = true;
            });
        },
        showLogs(event) {
          event;
          this.$store.state.logsOverlay = true;
        }
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
  display: block;
  overflow-x: auto;
}

.infoElem {
  padding-right: 15px;
  padding-bottom: 1px;
  padding-top: 1px;
  padding-left: 10px;
  border-right: 1px solid;
}

</style>
