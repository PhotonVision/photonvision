<template>
  <div>
    <v-tooltip
      :right="right"
      :bottom="!right"
      nudge-right="10"
    >
      <template v-slot:activator="{ on }">
        <v-icon
          :class="hoverClass"
          :color="color"
          @click="handleClick"
          v-on="on"
        >
          {{ text }}
        </v-icon>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<script>
    export default {
        name: 'Icon',
      // eslint-disable-next-line vue/require-prop-types
        props: ['color', 'tooltip', 'text', 'right', 'hover'],
        data() {
            return {}
        },
        computed: {
            hoverClass: {
                get() {
                    if (this.hover !== undefined) {
                        return "hover";
                    }
                    return "";
                }
            }
        },
        methods: {
            handleClick() {
                this.$emit('click');
            }
        },

    }
</script>

<style scoped>
    .hover:hover {
        color: white !important;
    }
</style>
