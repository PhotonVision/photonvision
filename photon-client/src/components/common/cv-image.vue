<template>
  <img
    :id="id"
    crossOrigin="anonymous"
    :style="styleObject"
    :src="src"
    alt=""
    @click="e => {this.openThinclientStream(e)}"
  >
</template>

<script>
    export default {
        name: "CvImage",
        // eslint-disable-next-line vue/require-prop-types
        props: ['idx', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightLg', 'maxHeightXl', 'colorPicking', 'id', 'disconnected'],
        data() {
            return {
                seed: 1.0,
                src: ""
            }
        },
        computed: {
            styleObject: {
                get() {
                    let ret = {
                      "border-radius": "3px",
                      "display": "block",
                      "object-fit": "contain",
                      "object-position": "50% 50%",
                      "max-width": "100%",
                      "margin-left": "auto",
                      "margin-right": "auto",
                      "max-height": this.maxHeight,
                      height: `${this.scale}%`,
                      cursor: (this.colorPicking ? `url(${require("../../assets/eyedropper.svg")}),` : "") + "default",
                    };

                    if (this.$vuetify.breakpoint.xl) {
                      ret["max-height"] = this.maxHeightXl;
                    } else if (this.$vuetify.breakpoint.lg) {
                      ret["max-height"] = this.maxHeightLg;
                    } else if (this.$vuetify.breakpoint.md) {
                      ret["max-height"] = this.maxHeightMd;
                    }

                    return ret;
                }
            },
            port: {
              get() {
                if(this.idx == 0){
                  return this.$store.state.cameraSettings[this.$store.state.currentCameraIndex].inputStreamPort;
                } else {
                  return this.$store.state.cameraSettings[this.$store.state.currentCameraIndex].outputStreamPort;
                }
              }
            }
        },
        watch : {
          port(newPort, oldPort){
            newPort;
            oldPort;
            this.reload();
          },
          disconnected(newVal, oldVal){
            oldVal;
            if(newVal){
              this.wsStream.setPort(0);
            } else {
              this.wsStream.setPort(this.port);
            }
          }
        },
        mounted() {
          var wsvs = require('../../plugins/WebsocketVideoStream');
          this.wsStream = new wsvs.WebsocketVideoStream(this.id, this.port, this.$address);
        },
        unmounted() {
          this.wsStream.setPort(0);
        },
        methods: {
            reload() {
              console.log("Reloading " + this.id + " with port " + String(this.port));
              this.wsStream.setPort(this.port);
            },
            openThinclientStream(e){
              e;
              var URL = "/thinclient.html?port=" + String(this.port) + "&host=" + window.location.hostname;
              window.open(URL, '_blank');
            }
        },
    }
</script>
