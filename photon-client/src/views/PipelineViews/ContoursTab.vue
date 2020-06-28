<template>
  <div>
    <CVrangeSlider
      v-model="value.area"
      name="Area"
      :min="0"
      :max="100"
      :step="0.1"
      @input="handleData('area')"
      @rollback="e=> rollback('area',e)"
    />
    <CVrangeSlider
      v-model="value.ratio"
      name="Ratio (W/H)"
      :min="0"
      :max="100"
      :step="0.1"
      @input="handleData('ratio')"
      @rollback="e=> rollback('ratio',e)"
    />
    <CVrangeSlider
      v-model="value.extent"
      name="Extent"
      :min="0"
      :max="100"
      @input="handleData('extent')"
      @rollback="e=> rollback('extent',e)"
    />
    <CVslider
      v-model="value.speckle"
      name="Speckle Rejection"
      :min="0"
      :max="100"
      @input="handleData('speckle')"
      @rollback="e=> rollback('speckle',e)"
    />
    <CVselect
      v-model="value.targetGroup"
      name="Target Group"
      :list="['Single','Dual']"
      @input="handleData('targetGroup')"
      @rollback="e=> rollback('targetGroup',e)"
    />
    <CVselect
      v-model="value.targetIntersection"
      name="Target Intersection"
      :list="['None','Up','Down','Left','Right']"
      :disabled="isDisabled"
      @input="handleData('targetIntersection')"
      @rollback="e=> rollback('targetIntersection',e)"
    />
  </div>
</template>

<script>
    import CVrangeSlider from '../../components/common/cv-range-slider'
    import CVselect from '../../components/common/cv-select'
    import CVslider from '../../components/common/cv-slider'

    export default {
        name: 'Contours',
        components: {
            CVrangeSlider,
            CVselect,
            CVslider
        },
        props: ['value'],

        data() {
            return {}
        },
        computed: {
            isDisabled() {
                return this.value.targetGroup === 0;

            }
        },
        methods: {},
    }
</script>

<style lang="" scoped>

</style>