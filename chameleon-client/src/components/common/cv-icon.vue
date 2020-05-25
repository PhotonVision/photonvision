<template>
    <div>
        <v-tooltip :right="right" :bottom="!right" nudge-right="10">
            <template v-slot:activator="{ on }">
                <v-icon :class="hoverClass" @click="handleClick" v-on="on" :color="color">{{text}}</v-icon>
            </template>
            <span>{{tooltip}}</span>
        </v-tooltip>
    </div>
</template>

<script>
    export default {
        name: 'Icon',
        props: ['color', 'tooltip', 'text', 'right', 'hover'],
        data() {
            return {}
        },
        methods: {
            handleClick() {
                this.$emit('click');
            }
        },
        computed: {
            hoverClass: {
                get() {
                    if (this.hover !== undefined) {
                        return "hover";
                    }
                    return "";
                }
            }
        },

    }
</script>

<style scoped>
    .hover:hover {
        color: white !important;
    }
</style>