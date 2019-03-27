<template>
    <div id="Threshold">
        <chrange class="spacing" title="Hue" Xkey="hue"></chrange>
        <chrange class="spacing" title="Saturation" Xkey="saturation"></chrange>
        <chrange class="spacing" title="Value" Xkey="value"></chrange>
    </div>
</template>
    
<script> 
import chrange from './ch-range.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'Threshold',
        data () {
            return {
            }
        },
        components:{
            chrange,
            chselect
        },
        methods:{
        }
    }

</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>