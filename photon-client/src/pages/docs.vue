<script setup lang="ts">
const devMode = process.env.NODE_ENV === "development";
</script>

<template>
  <div class="w-100 fill-height" style="overflow: hidden">
    <div v-if="devMode" class="w-100 fill-height pa-4">
      <span style="color: white; font-weight: bold">
        PhotonClient is in development mode so the local documentation page will not load. Please recompile in
        production mode with the documentation copied over after a full build. Currently showing the documentation
        published at <a href="https://docs.photonvision.org/">https://docs.photonvision.org/</a>.
      </span>
      <iframe
        class="fill-height w-100 pt-4 pb-8"
        src="https://docs.photonvision.org/"
        style="overflow: hidden; border: 0"
      />
    </div>
    <div v-else class="fill-height w-100">
      <!--suppress HtmlUnknownTarget -->
      <iframe class="fill-height w-100" src="docs/index.html" style="overflow: hidden; border: 0" />
    </div>
  </div>
</template>
