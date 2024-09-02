<template>
  <div class="not-found-container">
    <div>
      <v-card-title class="text--white pb-9" style="font-size: 42px">
        The page you are looking for isn't here
      </v-card-title>
      <v-card-subtitle class="text--white" style="font-size: 18px">
        Please use the sidebar to find what you are looking for
      </v-card-subtitle>
    </div>
    <img alt="missing-page" class="mt-16" src="@/assets/images/notfound.webp">
  </div>
</template>

<style>
.not-found-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>
