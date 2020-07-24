<template>
  <img
    :id="id"
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
        props: ['address', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightXl', 'colorPicking', 'id'],
        data: () => {
            return {
              addressData: undefined,
            }
        },
        computed: {
            styleObject: {
                get() {
                    let ret = {
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