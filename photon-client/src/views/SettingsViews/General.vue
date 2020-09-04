<template>
  <div>
    <span>Version: {{ settings.version }}</span>
    &mdash;
    <span>Hardware model: {{ settings.hardwareModel }}</span>
    &mdash;
    <span>Platform: {{ settings.hardwarePlatform }}</span>
    &mdash;
    <span>GPU Acceleration: {{ settings.gpuAcceleration ? "Enabled" : "Unsupported" }}{{ settings.gpuAcceleration ? " (" + settings.gpuAcceleration + " mode)" : "" }}</span>
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
          </v-icon> Export Settings
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
          </v-icon> Import Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="3"
      >
        <v-btn
          color="red"
          @click="axios.post('http://' + this.$address + '/api/restartProgram')"
        >
          <v-icon left>
            mdi-restart
          </v-icon> Restart Photon
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="3"
      >
        <v-btn
          color="red"
          @click="axios.post('http://' + this.$address + '/api/restartDevice')"
        >
          <v-icon left>
            mdi-restart
          </v-icon> Restart Device
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
        }
    },
    methods: {
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