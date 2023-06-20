<template>
  <div>
    <v-row>
      <v-col
        cols="12"
        lg="4"
        md="6"
      >
        <v-btn
          color="red"
          @click="restartProgram()"
        >
          <v-icon left>
            mdi-restart
          </v-icon>
          Restart PhotonVision
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="4"
        md="6"
      >
        <v-btn
          color="red"
          @click="restartDevice()"
        >
          <v-icon left>
            mdi-restart-alert
          </v-icon>
          Restart Device
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="4"
      >
        <v-btn
          color="secondary"
          @click="$refs.offlineUpdate.click()"
        >
          <v-icon left>
            mdi-upload
          </v-icon>
          Offline Update
        </v-btn>
      </v-col>
    </v-row>
    <v-divider style="margin: 12px 0;" />
    <v-row>
      <v-col
        cols="12"
        sm="6"
      >
        <v-btn
          color="secondary"
          @click="() => showImportDialog = true"
        >
          <v-icon left>
            mdi-import
          </v-icon>
          Import Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        sm="6"
      >
        <v-btn
          color="secondary"
          @click="$refs.exportSettings.click()"
        >
          <v-icon left>
            mdi-export
          </v-icon>
          Export Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        sm="6"
      >
        <v-btn
          color="secondary"
          @click="$refs.exportLogFile.click()"
        >
          <v-icon left>
            mdi-download
          </v-icon>
          Download Current Log

          <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
          <a
            ref="exportLogFile"
            style="color: black; text-decoration: none; display: none"
            :href="'http://' + this.$address + '/api/utils/logs/photonvision-journalctl.txt'"
            download="photonvision-journalctl.txt"
          />
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        sm="6"
      >
        <v-btn
          color="secondary"
          @click="showLogs()"
        >
          <v-icon left>
            mdi-eye
          </v-icon>
          Show log viewer
        </v-btn>
      </v-col>
    </v-row>
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
      :timeout="snackbarTimeout"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>
    <v-dialog
      v-model="showImportDialog"
      width="600"
      @input="() => {
        importType = undefined;
        importFile = null;
      }"
    >
      <v-card
        color="primary"
        dark
      >
        <v-card-title>Import Settings</v-card-title>
        <v-card-text>
          Upload and apply previously saved or exported PhotonVision settings to this device
          <v-row
            class="mt-6 ml-8"
          >
            <CVselect
              v-model="importType"
              name="Type"
              tooltip="Select the type of settings file you are trying to upload"
              :list="['All Settings', 'Hardware Config', 'Hardware Settings', 'Network Config']"
              :select-cols="10"
            />
          </v-row>
          <v-row
            class="mt-6 ml-8 mr-8"
          >
            <v-file-input
              :disabled="importType === undefined"
              :error-messages="importType === undefined ? 'Settings type not selected' : ''"
              :accept="importType === 0 ? '.zip' : '.json'"
              @change="(file) => importFile = file"
            />
          </v-row>
          <v-row
            class="mt-12 ml-8 mr-8 mb-1"
            style="display: flex; align-items: center; justify-content: center"
            align="center"
          >
            <v-btn
              color="secondary"
              :disabled="importFile === null"
              @click="uploadSettings"
            >
              <v-icon left>
                mdi-import
              </v-icon>
              Import Settings
            </v-btn>
          </v-row>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Special hidden link that gets 'clicked' when the user exports settings -->
    <a
      ref="exportSettings"
      style="color: black; text-decoration: none; display: none"
      :href="`http://${this.$address}/api/settings/photonvision_config.zip`"
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
import CVselect from "../../components/common/cv-select";

export default {
  // eslint-disable-next-line
  name: "DeviceControl",
  components: {
    CVselect
  },
  data() {
    return {
      snack: false,
      snackbarTimeout: 2000,
      uploadPercentage: 0.0,
      showImportDialog: false,
      importType: undefined,
      importFile: null,
      snackbar: {
        color: "success",
        text: "",
      }
    };
  },
  computed: {
    settings() {
      return this.$store.state.settings.general;
    },
    version() {
      return `${this.settings.version}`;
    },
    hwModel() {
      if (this.settings.hardwareModel !== "") {
        return `${this.settings.hardwareModel}`;
      } else {
        return `Unknown`;
      }
    },
    platform() {
      return `${this.settings.hardwarePlatform}`;
    },
    gpuAccel() {
      return `${this.settings.gpuAcceleration ? "Enabled" : "Unsupported"} ${
        this.settings.gpuAcceleration
          ? "(" + this.settings.gpuAcceleration + ")"
          : ""
      }`;
    },
    metrics() {
      // console.log(this.$store.state.metrics);
      return this.$store.state.metrics;
    }
  },
  methods: {
    restartProgram() {
      this.axios.post("http://" + this.$address + "/api/utils/restartProgram")
          .then(() => {
            this.snackbar = {
              color: "success",
              text: "Successfully sent program restart request"
            }
            this.snack = true;
          })
          .catch(error => {
            // This endpoint always return 204 regardless of outcome
            if(error.request) {
              this.snackbar = {
                color: "error",
                text: "Error while trying to process the request! The backend didn't respond.",
              };
            } else {
              this.snackbar = {
                color: "error",
                text: "An error occurred while trying to process the request.",
              };
            }
            this.snack = true;
          })
    },
    restartDevice() {
      this.axios.post("http://" + this.$address + "/api/utils/restartDevice")
          .then(() => {
            this.snackbar = {
              color: "success",
              text: "Successfully dispatched the restart command. It isn't confirmed if a device restart will occur."
            }
            this.snack = true;
          })
          .catch(error => {
            if(error.response) {
              this.snackbar = {
                color: "error",
                text: "The backend is unable to fulfil the request to restart the device."
              }
            } else if(error.request) {
              this.snackbar = {
                color: "error",
                text: "Error while trying to process the request! The backend didn't respond.",
              };
            } else {
              this.snackbar = {
                color: "error",
                text: "An error occurred while trying to process the request.",
              };
            }
            this.snack = true;
          })
    },
    uploadSettings() {
      let formData = new FormData();
      formData.append("data", this.importFile);

      let settingsType
      switch (this.importType) {
        case 0:
          settingsType = ""
          break;
        case 1:
          settingsType = "/hardwareConfig"
          break;
        case 2:
          settingsType = "/hardwareSettings"
          break;
        case 3:
          settingsType = "/networkConfig"
          break;
      }

      const requestUrl = `http://${this.$address}/api/settings${settingsType}`;
      this.axios.post(requestUrl, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      })
          .then(response => {
            this.snackbar = {
              color: "success",
              text: response.data.text
            }
            this.snack = true;
          })
          .catch(error => {
            if(error.response) {
              this.snackbar = {
                color: "error",
                text: error.response.data.text
              }
            } else if(error.request) {
              this.snackbar = {
                color: "error",
                text: "Error while trying to process the request! The backend didn't respond.",
              };
            } else {
              this.snackbar = {
                color: "error",
                text: "An error occurred while trying to process the request.",
              };
            }
            this.snack = true;
          })

      this.showImportDialog = false
      this.importType = undefined;
      this.importFile = null;
    },
    doOfflineUpdate(event) {
      this.snackbar = {
        color: "secondary",
        text: "New Software Upload in Process...",
      };
      this.snack = true;
      this.snackbarTimeout = -1

      let formData = new FormData();
      formData.append("jarData", event.target.files[0]);
      this.axios
        .post(
          "http://" + this.$address + "/api/utils/offlineUpdate",
          formData,
          {
            headers: { "Content-Type": "multipart/form-data" },
            onUploadProgress: function(progressEvent) {
              this.uploadPercentage = parseInt(
                Math.round((progressEvent.loaded / progressEvent.total) * 100)
              );
              if (this.uploadPercentage < 99.5) {
                this.snackbar.text =
                  "New Software Upload in Process, " +
                  this.uploadPercentage +
                  "% complete";
              } else {
                this.snackbar.text = "Installing uploaded software...";
              }
            }.bind(this),
          }
        )
          .then(response => {
            this.snackbar = {
              color: "success",
              text: response.data.text
            }
            this.snack = true;
          })
          .catch(error => {
            if(error.response) {
              this.snackbar = {
                color: "error",
                text: error.response.data.text
              };
            } else if(error.request) {
              this.snackbar = {
                color: "error",
                text: "Error while trying to process the request! The backend didn't respond.",
              };
            } else {
              this.snackbar = {
                color: "error",
                text: "An error occurred while trying to process the request.",
              };
            }
            this.snack = true;
          })

      // Reset the timeout after the loading bar
      this.snackbarTimeout = 2000
    },
    showLogs() {
      this.$store.state.logsOverlay = true;
    },
  },
};
</script>

<style lang="css" scoped>
.v-btn {
  width: 100%;
}

.infoTable {
  border: 1px solid;
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 5px;
  text-align: left;
  margin-bottom: 10px;
  width: 100%;
  display: block;
  overflow-x: auto;
}
w
.infoElem {
    padding: 1px 15px 1px 10px;
    border-right: 1px solid;
}
</style>
