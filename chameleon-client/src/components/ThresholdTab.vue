<template>
    <div id="3D">
        <Slider v-for="slider in sliders" v-bind:key="slider.id" v-model="slider.value"></Slider>
    </div>
</template>
    
<script> 
    export default {
        name: '3D',
        data () {
            return {
                 sliders: [
                    { id: 'lightning', value: 25 }
                ]
            }
        }
    }
</script>

<style scoped>
 
</style>