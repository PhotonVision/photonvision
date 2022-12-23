<template>
  <img
    :id="id"
    crossOrigin="anonymous"
    :style="styleObject"
    :src="src"
    alt=""
    @click="clickHandler"
    @error="loadErrHandler"
  />
</template>

<script>
    export default {
        name: "CvImage",
        // eslint-disable-next-line vue/require-prop-types
        props: ['address', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightLg', 'maxHeightXl', 'colorPicking', 'id', 'disconnected'],
        data() {
            return {
                seed: 1.0,
            }
        },
        computed: {
            styleObject: {
                get() {
                    let ret = {
                      "border-radius": "3px",
                      "display": "block",
                      "object-fit": "contain",
                      "background-size:": "contain",
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
            src: {
              get() {
                if(this.disconnected){
                  //Disconnected, do spinny gif
                  return require("../../assets/loading.gif");
                } else {
                  //Connected - get the port
                  var port = -1;
                  if(this.id == 'raw-stream'){
                    port = this.$store.state.cameraSettings[this.$store.state.currentCameraIndex].inputStreamPort
                  } else {
                    port = this.$store.state.cameraSettings[this.$store.state.currentCameraIndex].outputStreamPort
                  }

                  if(port <= 0){
                    //Invalid port, keep it spinny
                    return require("../../assets/loading.gif");
                  } else {
                    //Valid port, connect
                    
                    return "http://" + location.hostname + ":" + port + "/stream.mjpg" + "?" + this.seed;
                  }
                }
              },
            },
        },
        mounted() {
            this.reload(); // Force reload image on creation
        },
        methods: {
            loadErrHandler() {
                console.log("Error loading image, attempting to do it again...");
                this.reload();
            },
            clickHandler() {
                console.log("TODO: open new tab with image contents");
            },
            reload() {
                this.seed = new Date().getTime();
            }
        },
    }
</script>
