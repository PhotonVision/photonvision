<template>
    <div>
        <CVselect name="SortMode" v-model="value.sortMode" :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']" @input="handleInput('sortMode',value.sortMode)"></CVselect>
        <span>Calibrate:</span><v-divider dark color="white"></v-divider>
        <v-row align="center" justify="start">
            <v-col style="padding-right:0px" :cols="3"> 
                <v-btn small color="#4baf62" @click="takePointA">Take Point A</v-btn>
            </v-col>
            <v-col style="margin-left:0px" :cols="3">
                <v-btn small color="#4baf62" @click="takePointB">Take Point B</v-btn>
            </v-col>
            <v-col>
                <v-btn small @click="clearSlope" color="yellow darken-3">Clear All Points</v-btn>
            </v-col>
            
        </v-row>
        <v-snackbar :timeout="3000" v-model="snackbar" top color="error">
            <span style="color:#000">Points are too close</span>
            <v-btn color="black" text @click="snackbar = false">Close</v-btn>
        </v-snackbar>
    </div>
</template>

<script>
import CVselect from '../../components/cv-select'
    export default {
        name: 'Output',
        props:['value'],
        components:{
            CVselect
        },
        methods:{
            takePointA(){
                this.pointA = this.rawPoint;
                this.calcSlope();
            },
            takePointB(){
                this.pointB = this.rawPoint;
                this.calcSlope();
            },
            calcSlope(){
                if(this.pointA !== undefined && this.pointB !== undefined){
                    let m = (this.pointB[1] - this.pointA[1]) / (this.pointB[0] - this.pointA[0]);
                    let b = this.pointA[1] - (m * this.pointA[0]);
                    if(isNaN(m) === false && isNaN(b) === false){
                        this.sendSlope(m,b,true);
                    } else {
                         this.snackbar = true;
                    }
                    this.pointA = undefined;
                    this.pointB = undefined;
                }
            },
            sendSlope(m,b,valid){
                this.handleInput('m',m);
                this.handleInput('b',b);
                this.handleInput('isCalibrated',valid);
            },
            clearSlope(){
                this.sendSlope(1,0,false);
                this.pointA = undefined;
                this.pointB = undefined;
            }
        },

        data() {
            return {
                snackbar: false,
                pointA: undefined,
                pointB: undefined 
            }
        },
        computed:{
            rawPoint:{
                get(){
                    return this.$store.state.point.rawPoint;
                }
            }
        }
    }
</script>

<style scoped>
</style>