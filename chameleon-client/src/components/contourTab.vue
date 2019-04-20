<template>
    <div id="ContourTab">
     <chrange class="spacing" title="Area" Xkey="area"></chrange>
     <chrange class="spacing" title="Ratio (W/H)" Xkey="ratio"></chrange>
     <chrange class="spacing" title="Extent" Xkey="extent"></chrange>
     </div>
</template>

<script>
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
import chrange from './ch-range.vue'

    export default {
        name: 'ContourTab',
        components:{
            chslider,
            chselect,
            chrange
        },
        methods:{

        },
        data() {
            return {
                
            }
        }
    }
</script>

<style scoped>

.spacing{
    margin-top: 20px;
}
</style>