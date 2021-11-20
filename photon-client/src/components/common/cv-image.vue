<template>
  <img
    :id="id"
    crossOrigin="anonymous"
    :style="styleObject"
    :src="src"
    alt=""
    @click="e => $emit('click', e)"
  >
</template>

<script>
    export default {
        name: "CvImage",
        // eslint-disable-next-line vue/require-prop-types
        props: ['address', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightXl', 'colorPicking', 'id', 'disconnected'],
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
                    } else if (this.$vuetify.breakpoint.mdAndUp) {
                      ret["max-height"] = this.maxHeightMd;
                    }

                    return ret;
                }
            },
            src: {
              get() {
                return this.disconnected ? require("../../assets/noStream.jpg") : this.address + "?" + this.seed // This prevents caching
              },
            },
        },
        mounted() {
            this.reload(); // Force reload image on creation
        },
        methods: {
            reload() {
                this.seed = new Date().getTime();
            }
        },
    }
</script>
