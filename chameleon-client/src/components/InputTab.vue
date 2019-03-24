
<template>
    <div id="InputTab">
        <chslider class="spacing" title="exposure" v-model="expval" v-on:input="onChange(expval)"></chslider>
        <chslider class="spacing" title="Brightness"></chslider>
        <chselect class="spacing" title="Orientation" placeholdert=""  :list="['Normal','Inverted']" v-model="OrientationVal" v-on:input="onChange(OrientationVal)" ></chselect>
        <chselect class="spacing" title="Resolution" :list="['idk']"></chselect>
        </div>

</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'InputTab',
        data () {
            return{
                expval,
                OrientationVal
            }
        },
        components: {
            chslider,
            chselect
        },
        methods: {
            onChange(i) {
                // console.log(i);
                this.$socket.send(JSON.stringify(i));
                
            }
        }
    }
    
</script>

<style scoped>
.spacing{
    margin-top: 20px;
}

</style>
