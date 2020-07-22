<template>
  <div>
    <span>Version: {{ version }}</span>
    &mdash;
    <span>Hardware model: {{ hardwareModel }}</span>
    &mdash;
    <span>Platform: {{ hardwarePlatform }}</span>
    &mdash;
    <span>GPU Acceleration: {{ gpuAcceleration ? "Enabled" : "Unsupported" }}{{ gpuAcceleration ? " (" + gpuAccelerationType + " mode)" : "" }}</span>
    <v-row>
      <v-col
        cols="12"
        sm="6"
        lg="4"
      >
        <v-btn color="secondary">
          <v-icon left>
            mdi-download
          </v-icon> Export Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        sm="6"
        lg="4"
      >
        <v-btn color="secondary">
          <v-icon left>
            mdi-upload
          </v-icon> Import Settings
        </v-btn>
      </v-col>
      <v-col
        cols="12"
        lg="4"
      >
        <v-btn color="red">
          <v-icon left>
            mdi-restart
          </v-icon> Restart Device
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: 'General',
        data() {
            return {
                version: "TODO",
                gpuAcceleration: false,
                gpuAccelerationType: "TODO",

                hardwareModel: "TODO",
                hardwarePlatform: "TODO",
            };
        }
    }
</script>

<style lang="css" scoped>
    .v-btn {
        width: 100%;
    }
</style>