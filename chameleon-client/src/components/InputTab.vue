<template>
    <div id="InputTab">
        <Slider v-for="slider in sliders" v-bind:key="slider.id" v-model="slider.value"></Slider>
    </div>
</template>
    
<script> 
    export default {
        name: 'InputTab',
        data () {
            return {
                 sliders: [
                    { id: 'lightning', value: 25 },
                    { id: 'saturation', value: 30 }
                ]
            }
        }
    }
</script>

<style scoped>
 
</style>
