<template>
  <v-app>
    <v-app-bar app dark>
      <img src="./assets/logo.png" class="imgClass">
      <v-toolbar-title class="headline text-uppercase">
        <span>Chameleon Vision</span>
      </v-toolbar-title>
      <v-spacer></v-spacer>
    </v-app-bar>
    <v-content>
      <v-navigation-drawer
      expand-on-hover
      permanent
    >
  

      <v-list
        nav
        dense
      >
      <v-list-group
        value="true"
        prepend-icon="mdi-xbox-controller">
        <template v-slot:activator>
          <v-list-item-title> test</v-list-item-title>
        </template>
        <v-list-item>
             <v-list-item-icon>
              <v-icon>mdi-account</v-icon>
            </v-list-item-icon>
          <v-list-item-title>test sub</v-list-item-title>
        </v-list-item>
      </v-list-group>

        <v-list-item link>
          <v-list-item-icon>
            <v-icon>mdi-account-multiple</v-icon>
          </v-list-item-icon>
          <v-list-item-title>Shared with me</v-list-item-title>
        </v-list-item>
        <v-list-item link>
          <v-list-item-icon>
            <v-icon>mdi-star</v-icon>
          </v-list-item-icon>
          <v-list-item-title>Starred</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>
    </v-content>
  </v-app>
</template>

<script>


export default {
  name: 'App',
  components: {
    
  },
  data: () => ({
    //
  }),
};
</script>

<style>
  html{
    overflow-y: hidden;
  }
  
  .imgClass{
    width: auto;
    height: 58px;
    vertical-align: middle;
    padding-right: 5px;
  }
</style>

