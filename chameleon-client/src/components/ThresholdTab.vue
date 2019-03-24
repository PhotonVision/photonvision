<template>
    <div id="Threshold">
        <chrange class="spacing" title="Hue" :value="[0,10]"></chrange>
        <chrange class="spacing" title="Saturation" :value="[0,10]"></chrange>
        <chrange class="spacing" title="Value" :value="[0,10]"></chrange>
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
        }

    }

</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>