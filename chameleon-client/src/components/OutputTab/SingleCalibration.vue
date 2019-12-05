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
        methods: {
            clearPoint() {
                this.handleInput('point', [0, 0]);
                this.$emit('update');
            },
            takePoint() {
                let hasNaN = false;
                console.log(this.rawPoint);
                for (let i = 0; i < this.rawPoint.length&&!hasNaN; i++) {
                    hasNaN = !(this.rawPoint[i]||this.rawPoint[i]===0);
                }
                if(!hasNaN){//if array doesnt have undefined values
                    console.log("sending points");
                    this.handleInput('point', this.rawPoint);
                    this.$emit('update');
                }
                else
                {
                    console.log("sending error");
                    this.$emit('snackbar');
                }
            }
        }
    }
</script>

<style scoped>

</style>