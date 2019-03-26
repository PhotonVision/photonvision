
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure" :parentData="$store.getters.exposure" @input="onChange('exposure',$event)"></chslider>
        <chslider class="spacing" title="Brightness" :parentData="$store.getters.brightness" @input="onChange('brightness',$event)"></chslider>
        <chselect class="spacing" title="Orientation" :list="['Normal','Inverted']" :parentData="$store.getters.orientation" @input="onChange('orientation',$event)"></chselect>
        <chselect class="spacing" title="Resolution" :list="['idk']" :parentData="$store.getters.resolution" @input="onChange('resolution',$event)"></chselect>
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
            onChange: function(key,event) {
                this.$store.commit(key, event);
                console.log(this.$store.getters);
            }
        }
    }
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>
