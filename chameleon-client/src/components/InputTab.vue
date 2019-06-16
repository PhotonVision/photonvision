
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure" Xkey="exposure"></chslider>
        <chslider class="spacing" title="Brightness" Xkey="brightness"></chslider>
        <chselect class="spacing" title="Orientation" Xkey="orientation" :list="['Normal','Inverted']"></chselect>
        <ch-index-select class="spacing" title="Resolution" Xkey="resolution"  :list="resolutionList"></ch-index-select>
        </div>
</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
import chIndexSelect from './ch-IndexSelect.vue'
    export default {
        name: 'InputTab',
        data () {
            return{
            }
        },
        components: {
            chslider,
            chselect,
            chIndexSelect
        },
        methods: {
        },
        computed:{
            resolutionList:{
                get:function(){
                    return this.$store.state.resolutionList;
                }
            }
        }
    }
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>
