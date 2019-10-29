<template>
    <div>
        <v-row align="center" justify="start">
            <v-col style="padding-right:0" :cols="3">
                <v-btn small color="#4baf62" @click="takePoint">Take Point</v-btn>
            </v-col>
            <v-col>
                <v-btn small @click="clearPoint" color="yellow darken-3">Clear Point</v-btn>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: "SingleCalibration",
        props: ['rawPoint'],
        methods:{
            clearPoint(){
                this.handleInput('point',[0,0]);
            },
            takePoint(){
                this.handleInput('point',this.rawPoint);
            }
        }
    }
</script>

<style scoped>

</style>