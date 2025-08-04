<script setup lang="ts">
const devMode = process.env.NODE_ENV === "development";
</script>
<template>
  <div style="overflow: hidden; height: 100vh; width: 100%">
    <div v-if="devMode" style="width: 60%; height: 100%; margin: auto">
      <v-card
        dark
        flat
        class="pl-6 pr-6 d-flex flex-column justify-center align-center"
        style="background-color: transparent; height: 100%"
      >
        <v-card-text class="d-flex flex-column" style="flex: 0">
          <v-icon size="64" color="primary">mdi-web-off</v-icon>
        </v-card-text>
        <v-card-text style="width: 100%; flex-grow: 0; text-align: center">
          PhotonClient is in development mode so the documentation page will not load. Please recompile in production
          mode with the documentation copied over after a full build.
        </v-card-text>
      </v-card>
    </div>
    <div v-else style="width: 100%; height: 100%">
      <!--suppress HtmlUnknownTarget -->
      <iframe src="docs/index.html" style="overflow: hidden; height: 100%; width: 100%; border: 0" />
    </div>
  </div>
</template>
