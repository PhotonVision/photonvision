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
        >{{ text }}</span>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<script>
    export default {
        name: 'TooltippedLabel',
        // eslint-disable-next-line vue/require-prop-types
        props: ['text', 'tooltip'],
    }
</script>
