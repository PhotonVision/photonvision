<template>
  <img
    id="CameraStream"
    crossOrigin="anonymous"
    :style="styleObject"
    :src="src"
    alt=""
    @error="showError()"
    @click="e => $emit('click', e)"
  >
</template>

<script>
    export default {
        name: "CvImage",
        // eslint-disable-next-line vue/require-prop-types
        props: ['address', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightXl', 'colorPicking'],
        data: () => {
            return {
              addressData: undefined,
            }
        },
        computed: {
            styleObject: {
                get() {
                    let ret = {
                      "object-fit": "contain",
                      "object-position": "50% 50%",
                      "max-height": this.maxHeight,
                      width: `${this.scale}%`,
                      height: `${this.scale}%`,
                      cursor: (this.colorPicking ? `url(${require("../../assets/eyedropper.svg")}),` : "") + "default",
                    };
                    console.log(ret);

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
                return this.addressData || this.address;
              },
              set(value) {
                this.addressData = value;
              }
            }
        },
        methods: {
          showError() {
            this.addressData = require("../../assets/noStream.jpg")
          }
        }
    }
</script>