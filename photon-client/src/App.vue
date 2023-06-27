<script setup lang="ts">
import HelloWorld from "./components/HelloWorld.vue";
</script>

<template>
  <v-app>
    <photon-sidebar/>
    <v-main>
      <v-container
          fluid
          fill-height
      >
        <v-layout>
          <v-flex>
            <router-view @switch-to-cameras="() => {}" />
          </v-flex>
        </v-layout>
      </v-container>
    </v-main>

    <photon-log-view/>
  </v-app>
</template>

<style lang="scss">
  ::-webkit-scrollbar {
    width: 0.5em;
    border-radius: 5px;
  }

  ::-webkit-scrollbar-track {
    -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
    border-radius: 10px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #ffd843;
    border-radius: 10px;
  }

  .container {
    background-color: #232c37;
    padding: 0 !important;
  }

  #title {
    color: #ffd843;
  }
</style>

<style lang="scss">
@import 'vuetify/src/styles/settings/_variables';

@media #{map-get($display-breakpoints, 'md-and-down')} {
  html {
    font-size: 14px !important;
  }
}
</style>
