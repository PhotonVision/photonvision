<template>
 <v-app>
    <!-- <v-navigation-drawer app clipped dark width="200">
      <v-list dense>
        <v-list-item link to="Vision">
          <v-list-item-action>
            <v-icon>mdi-video</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Vision</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link to="Settings">
          <v-list-item-action>
            <v-icon>mdi-settings</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer> -->

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
    <v-footer app dark>
      <span>&copy; 2019 Chameleon Vision Project</span>
    </v-footer>
  </v-app>
</template>

<script>


export default {
  name: 'App',
  components: {
    
  },
  data: () => ({
    drawer:null,
    //
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
</style>

