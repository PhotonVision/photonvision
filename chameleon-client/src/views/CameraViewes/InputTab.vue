<template>
    <div>
        <CVslider name="Exposure" v-model="value.exposure" :min="0" :max="100" @input="handleData('exposure')"/>
        <CVslider name="Brightness" v-model="value.brightness" :min="0" :max="100" @input="handleData('brightness')"/>
        <CVselect name="Orientation" v-model="value.flipMode" :list="['Normal','Inverted']"
                  @input="handleData('orientation')"/>
    </div>
</template>

<script>
    import CVslider from '../../components/cv-slider'
    import CVselect from '../../components/cv-select'

    export default {
        name: 'Input',
        props: ['value'],
        components: {
            CVslider,
            CVselect,
        },
        methods: {
            handleData(val) {
                this.handleInput(val, this.value[val]);
                this.$emit('update')
            }
        },
        data() {
            return {
                t: 0,
                a: 1
            }
        },
        computed: {}
    }
</script>

<style scoped>

</style>