
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure"></chslider>
        <chslider class="spacing" title="Brightness"></chslider>
        <chselect class="spacing" title="Orientation" placeholdert=""  :list="['Normal','Inverted']" ></chselect>
        <chselect class="spacing" title="Resolution" placeholdert="" :list="['idk']"></chselect>
        </div>

</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'InputTab',
        data () {
            return {
                 sliders: [
                    { id: 'lightning', value: 25 },
                ]
            }
        },
        components: {
            chslider,
            chselect
        }
    }
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}

</style>
