<template>
    <img id="CameraStream" v-bind:style="styleObject" :src="address" @click="e => $emit('click', e)"
         crossorigin="Anonymous" alt=""/>

</template>

<script>
    export default {
        name: "cv-image",
        data: () => {
            return {}
        },
        props: ['address', 'scale'],
        computed: {
            styleObject: {
                get() {
                    return {
                        width: `${this.scale}%`,
                        height: `${this.scale}%`,
                        display: 'block',
                        margin: 'auto'
                    }
                }

            }
        }
    }
</script>