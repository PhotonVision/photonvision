<template>
    <div>
        <CVselect name="SortMode" v-model="value.SortMode" :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Closest']"></CVselect>
        <span>Calibrate:</span><v-divider dark color="white"></v-divider>
        <v-row align="center" justify="start">
            <v-col style="padding-right:0px" :cols="3"> 
                <v-btn small color="#4baf62">Take Point A</v-btn>
            </v-col>
            <v-col style="margin-left:0px" :cols="3">
                <v-btn small color="#4baf62">Take Point B</v-btn>
            </v-col>
            <v-col>
                <v-btn small color="yellow darken-3">Clear All Points</v-btn>
            </v-col>
            
        </v-row>
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
        data() {
            return {
                
            }
        }
    }
</script>

<style scoped>
</style>