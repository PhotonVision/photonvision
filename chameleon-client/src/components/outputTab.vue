<template>
    <div id="OutputTab">
     <chselect class="spacing" title="Target region" Xkey="target_region" 
        :list="['Upmost','Rightmost','Downmost','Leftmost','Centermost']"></chselect>
        
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