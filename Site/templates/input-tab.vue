<template>
    <div id="input-tab">
        <Slider v-for="slider in sliders" v-bind:key="slider.id" v-model="slider.value"></Slider>
    </div>
</template>
    
<script>
    new Vue({
        el: "#input-tab",
        data: {
            sliders: [
                { id: 'lightning', value: 25 },
                { id: 'saturation', value: 30 }
            ]
        }
    })

    export default {

    }
</script>