<template>
  <div class="not-found-container">
    <div>
      <v-card-title style="color: white; padding-bottom: 36px; font-size: 42px">
        The page you are looking for isn't here
      </v-card-title>
      <v-card-subtitle style="color: white; font-size: 18px">
        Please use the sidebar to find what you are looking for
      </v-card-subtitle>
    </div>
    <img src="@/assets/images/notfound.webp" alt="missing-page" style="margin-top: 64px" />
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
