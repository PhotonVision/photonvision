<template>
  <img
    id="CameraStream"
    :style="styleObject"
    :src="address"
    alt=""
    @click="e => $emit('click', e)"
  >
</template>

<script>
    export default {
        name: "CvImage",
      // eslint-disable-next-line vue/require-prop-types
        props: ['address', 'scale', 'maxHeight', 'maxHeightMd', 'maxHeightXl'],
        data: () => {
            return {}
        },
        computed: {
            styleObject: {
                get() {
                    let ret = {
                      "object-fit": "contain",
                      "max-height": this.maxHeight,
                      width: `${this.scale}%`,
                      height: `${this.scale}%`,
                    };

                    if (this.$vuetify.breakpoint.xl) {
                      ret["max-height"] = this.maxHeightXl;
                    } else if (this.$vuetify.breakpoint.mdAndUp) {
                      ret["max-height"] = this.maxHeightMd;
                    }

                    return ret;
                }

            }
        }
    }
</script>