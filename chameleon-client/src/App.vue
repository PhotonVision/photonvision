<template>
 <v-app>
    <v-app-bar app dense clipped-left dark>
      <img class="imgClass" src="./assets/logo.png">
      <v-toolbar-title id="title">Chameleon Vision</v-toolbar-title>
      <div class="flex-grow-1"></div>
      <v-toolbar-items>
      <v-tabs dark height="48" slider-color="#4baf62">
        <v-tab to="vision">Vision</v-tab>
        <v-tab to="settings">Settings</v-tab>
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
  methods:{
    handleMessage(key,value){
      if(this.$store.state.hasOwnProperty(key)){
        this.$store.commit(key,value);
      } else if(this.$store.state.pipeline.hasOwnProperty(key)){
        this.$store.commit('setPipeValues',{[key]:value});
      }
      else{
        switch(key){
          
          default:{
            console.log(key + " : " + value);
          }
        }
      }
    }
  },
  data: () => ({

  }),
  created(){
    this.$options.sockets.onmessage = async (data) =>{
      try{
        var buffer = await data.data.arrayBuffer();
        let message = this.$msgPack.decode(buffer);
        for(let prop in message){
          if(message.hasOwnProperty(prop)){
            this.handleMessage(prop, message[prop]);
          }
        }
      }
      catch(error){
        console.error('error: ' + data.data+ " , "+ error);
      }      
    }
  }
};
</script>

<style>
  html{
    overflow-y: hidden !important;
  }
  .imgClass{
    width: auto;
    height: 45px;
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