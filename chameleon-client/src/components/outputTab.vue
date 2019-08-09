<template>
    <div id="OutputTab">
        <chselect class="spacing" title="Sort Mode" Xkey="sort_mode" 
        :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Closest']"></chselect>
        <h6>calibrate crosshair</h6>
     </div>
</template>

<script>
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
import chrange from './ch-range.vue'

    export default {
        name: 'OutputTab',
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