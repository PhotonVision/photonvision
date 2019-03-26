
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure" :parentData="values.exposure" @input="values.exposure = $event"></chslider>
        <chslider class="spacing" title="Brightness" :parentData="values.brightness" @input="values.brightness = $event"></chslider>
        <chselect class="spacing" title="Orientation" :list="['Normal','Inverted']" :parentData="values.orientation" @input="values.orientation = $event" ></chselect>
        <chselect class="spacing" title="Resolution" :list="['idk']" :parentData="values.resolution" @input="values.resolution = $event" ></chselect>
        </div>

</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'InputTab',
        data () {
            return{
                values:{
                    exposure:0,
                    brightness:0,
                    orientation:0,
                    resolution:0
                }
            }
        },
        components: {
            chslider,
            chselect
        },
        methods: {
            onChange(i) {
                console.log(i);
            }
        }
    }
    
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}

</style>
