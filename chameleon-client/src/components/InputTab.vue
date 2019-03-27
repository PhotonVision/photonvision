
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure" Xkey="exposure"></chslider>
        <chslider class="spacing" title="Brightness" Xkey="brightness"></chslider>
        <chselect class="spacing" title="Orientation" Xkey="orientation" :list="['Normal','Inverted']"></chselect>
        <chselect class="spacing" title="Resolution" Xkey="resolution"  :list="['Normal','Inverted']"></chselect>
        <button v-on:click="test">test</button>
        </div>
</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'InputTab',
        data () {
            return{
            }
        },
        components: {
            chslider,
            chselect
        },
        methods: {
            test(){
                console.log(this.$store.state)
            }
        }
    }
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>
