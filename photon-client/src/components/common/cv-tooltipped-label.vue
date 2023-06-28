<template>
  <div>
    <v-tooltip
      :disabled="tooltip === undefined"
      right
      open-delay="300"
    >
      <template v-slot:activator="{ on, attrs }">
        <span
          style="cursor: text !important;"
          class="white--text"
          v-bind="attrs"
          v-on="on"
        >{{ label }}</span>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<script lang="ts">
    export default {
        name: "TooltippedLabel",
        props: {
          label: {type: String, required: false},
          tooltip: {type: String, required: false}
        }
    };
</script>
