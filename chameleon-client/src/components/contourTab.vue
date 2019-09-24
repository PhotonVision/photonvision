<template>
    <div id="ContourTab">
     <chrange class="spacing" title="Area" Xkey="area"></chrange>
     <chrange class="spacing" title="Ratio (W/H)" Xkey="ratio" :steps="0.1"></chrange>
     <chrange class="spacing" title="Extent" Xkey="extent"></chrange>
     <chselect class="spacing" title="Target Group" Xkey="target_group" 
        :list="['Single','Dual','Triple','Quadruple','Quintuple']"></chselect>
    <chselect class="spacing" title="Target Intersection" Xkey="target_intersection" 
        :list="['Up','Down','Left','Right','Parallel']" :isDisabled="isSingle"></chselect>
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
        },
        computed: {
            isSingle:function(){
                if (this.$store.state.target_group == 'Single'){
                    return true;
                } else {
                    return false;
                }
            }
        },
        watch:{
        }
    }
</script>

<style scoped>

.spacing{
    margin-top: 20px;
}
</style>