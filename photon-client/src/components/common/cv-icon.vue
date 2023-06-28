<template>
  <div>
    <v-tooltip
      :right="right"
      :bottom="!right"
      nudge-right="10"
    >
      <template v-slot:activator="{ on, attrs }">
        <v-icon
          :class="hoverClass"
          :color="color"
          v-on="on"
          v-bind="attrs"
          @click="$emit('click')"
        >
          {{ iconName }}
        </v-icon>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<script lang="ts">
    import {computed} from "vue";

    export default {
        emits: ["click"],
        props: {
            iconName: {type: String, required: true},
            color: {type: String, required: false, default: undefined},
            tooltip: {type: String, required: true},
            right: {type: Boolean, required: false, default: false},
            hover: {type: Boolean, required: false, default: false}
        },
        setup(props: {color?: string, tooltip?: string, iconName: string, right: boolean, hover: boolean}) {
          const hoverClass = computed<string>(() => props.hover || false ? "hover" : "");

          return {
            hoverClass
          };
        }
    };
</script>

<style scoped>
    .hover:hover {
        color: white !important;
    }
</style>
