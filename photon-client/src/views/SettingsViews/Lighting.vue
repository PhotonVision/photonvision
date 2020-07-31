<template>
  <div>
    <CVslider
      v-model="settings.brightness"
      class="pt-2"
      slider-cols="12"
      name="Brightness"
      min="0"
      max="100"
      @input="handleData('accuracy')"
      @rollback="e => rollback('accuracy', e)"
    />
  </div>
</template>

<script>
    import CVslider from "../../components/common/cv-slider";

    export default {
        name: 'LEDs',
        components: {
            CVslider,
        },
        computed: {
            isDHCP() {
                return this.settings.connectionType === 0;
            },
            settings() {
                return this.$store.state.settings.lighting;
            }
        },
    }
</script>

<style lang="" scoped>

</style>