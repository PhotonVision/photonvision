<script setup lang="ts">
import IconWebOff from "~icons/mdi/web-off";

const devMode = process.env.NODE_ENV === "development";
const docsSrc = import.meta.env.MODE === "demo" ? "https://docs.photonvision.org" : "docs/index.html";
</script>
<template>
  <div style="overflow: hidden; height: 100vh; width: 100%">
    <div v-if="devMode" style="width: 60%; height: 100%; margin: auto">
      <pv-card
        class="flex h-full flex-col items-center justify-center gap-4 text-center bg-transparent border-0"
      >
        <div class="flex flex-col">
          <pv-icon size="64" color="primary" :icon="IconWebOff" />
        </div>
        <div class="text-base">
          PhotonClient is in development mode so the documentation page will not load. Please recompile in production
          mode with the documentation copied over after a full build.
        </div>
      </pv-card>
    </div>
    <div v-else style="width: 100%; height: 100%">
      <!--suppress HtmlUnknownTarget -->
      <iframe :src="docsSrc" style="overflow: hidden; height: 100%; width: 100%; border: 0" />
    </div>
  </div>
</template>
