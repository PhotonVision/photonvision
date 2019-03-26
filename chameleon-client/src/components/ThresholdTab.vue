<template>
    <div id="Threshold">
        <chrange class="spacing" title="Hue" :parentData="$store.getters.hue" @input="onChange('hue',$event)"></chrange>
        <chrange class="spacing" title="Saturation" :parentData="$store.getters.saturation" @input="onChange('saturation',$event)"></chrange>
        <chrange class="spacing" title="Value" :parentData="$store.getters.value" @input="onChange('value',$event)"></chrange>
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
            onChange: function(key,event) {
                this.$store.commit(key, event);
            }
        }
    }

</script>

<style scoped>
.spacing{
    margin-top: 20px;
}
</style>