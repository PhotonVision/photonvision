<template>
  <div>
    <CVslider
      v-model="enabledLEDPercentage"
      class="pt-2"
      slider-cols="12"
      name="Brightness"
      min="0"
      max="100"
      @input="handleData('enabledLEDPercentage')"
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
            enabledLEDPercentage: {
                get() {
                    return this.settings.brightness
                },
                set(value) {
                    this.$store.commit("mutateEnabledLEDPercentage", value)
                }
            },
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
