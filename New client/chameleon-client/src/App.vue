<template>
 <v-app>
    <v-app-bar app clipped-left dark>
      <img class="imgClass" src="./assets/logo.png">
      <v-toolbar-title id="title">Chameleon Vision</v-toolbar-title>
      <div class="flex-grow-1"></div>
      <v-toolbar-items>
      <v-tabs dark height="64" slider-color="#4baf62">
        <v-tab to="Vision">Vision</v-tab>
        <v-tab to="Settings">Settings</v-tab>
    </v-tabs>
      </v-toolbar-items>
    </v-app-bar>
    <v-content>
      <v-container fluid fill-height>
        <v-layout>
          <v-flex>
            <router-view></router-view>
          </v-flex>
        </v-layout>
      </v-container>
    </v-content>
  </v-app>
</template>

<script>
export default {
  name: 'App',
  components: {
  },
  data: () => ({
  }),
};
</script>

<style>
  html{
    overflow-y: hidden !important;
  }
  .imgClass{
    width: auto;
    height: 58px;
    vertical-align: middle;
    padding-right: 5px;
  }
  .tabClass{
    color: #4baf62;
  }
  .container{
    background-color: #212121;
    padding: 0!important;
  }
  #title{
    color:#4baf62;
  }
  span{
    color: white;
  }
</style>

