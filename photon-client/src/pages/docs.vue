<script setup lang="ts">
const devMode = process.env.NODE_ENV === "development";
</script>

<template>
  <div style="overflow: hidden; height: 100%; width: 100%">
    <div v-if="devMode" style="width: 100%; height: 100%; padding: 16px">
      <span style="color: white; font-weight: bold">
        PhotonClient is in development mode so the local documentation page will not load. Please recompile in production mode
        with the documentation copied over after a full build. Currently showing the documentation published at <a href="https://docs.photonvision.org/">https://docs.photonvision.org/</a>.
      </span>
      <iframe src="https://docs.photonvision.org/" style="overflow: hidden; height: 100%; width: 100%; border: 0" class="pt-4 pb-8" />
    </div>
    <div v-else style="width: 100%; height: 100%">
      <!--suppress HtmlUnknownTarget -->
      <iframe src="docs/index.html" style="overflow: hidden; height: 100%; width: 100%; border: 0" />
    </div>
  </div>
</template>
