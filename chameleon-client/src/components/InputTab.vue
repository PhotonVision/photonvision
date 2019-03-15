
<template>
    <div id="InputTab">
        <chslider title="exposure"></chslider>
        <chslider title="fuck off"></chslider>
        <chselect title="tet" placeholdert="fuck of this took time"  :list="[1,2,23]" ></chselect>
        </div>

</template>
    
<script> 
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
    export default {
        name: 'InputTab',
        data () {
            return {
                 sliders: [
                    { id: 'lightning', value: 25 },
                ]
            }
        },
        components: {
            chslider,
            chselect
        }
    }
</script>

<style scoped>
</style>
