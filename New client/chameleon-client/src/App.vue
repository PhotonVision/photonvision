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
    this.$options.sockets.onmessage = (data) =>{
      try{
		let info = new Uint8Array(data.data.substring(1, data.data.length-1).split(","));//Converts incoming data to data that msgpack can decode
        let message = this.$msgPack().decode(info);
        for(let prop in message){
          if(message.hasOwnProperty(prop)){
            console.log(message);
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