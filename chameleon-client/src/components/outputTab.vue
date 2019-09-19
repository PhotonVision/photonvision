<template>
    <div id="OutputTab">
        <chselect class="spacing" title="Sort Mode" Xkey="sort_mode" 
        :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Closest']"></chselect>
        <Row type="flex" justify="start" align="middle" class="spacing" :gutter="10">
            <col>
                <Button type="primary" size="small" v-on:click="takePointA">Take Point A</Button>
            </col>
            <col style="margin-left:10px">
                <Button type="primary" size="small" v-on:click="takePointB">Take Point B</Button>
            </col>
        </Row>
        <Row type="flex" align="middle" class="spacing" :gutter="10">
            <col>
                <Button type="warning" size="small" v-on:click="clearPoints">Clear All Points</Button>
            </col>
        </Row>
     </div>
</template>

<script>
import chslider from './ch-slider.vue'
import chselect from './ch-select.vue'
import chrange from './ch-range.vue'

    export default {
        name: 'OutputTab',
        components:{
            chslider,
            chselect,
            chrange
        },
        methods:{
            takePointA:function(){
                this.pointA = this.raw_point;
                this.calcSlope();
            },
            takePointB:function(){
                this.pointB = this.raw_point;
                this.calcSlope();
            },
            calcSlope:function(){
                if(this.pointA !== undefined && this.pointB !== undefined){
                    let m = (this.pointB[1] - this.pointA[1]) / (this.pointB[0] - this.pointA[0]);
                    let b = this.pointA[1] - (m * this.pointA[0]);
                    if(isNaN(m) === false && isNaN(b) === false){
                        this.sendSlope(m,b,true);
                    } else{
                        this.$Message.error("Point A and B are to close apart");
                    }
                this.pointA = undefined;
                this.pointB = undefined;
                }
            },
            clearPoints:function(){
                this.sendSlope(1,0,false);
                this.pointA = undefined;
                this.pointB = undefined;
            },
            sendSlope(m,b,valid){
                this.$socket.sendObj({'M':m});
                this.$socket.sendObj({'B':b});
                this.$socket.sendObj({'is_calibrated':valid});
            }
        },
        computed: {
            raw_point:{
                get:function(){
                    return this.$store.state.raw_point;
                }
            }
        },
        data() {
            return {
                pointA:undefined,
                pointB:undefined
            }
        }
    }
</script>

<style scoped>

.spacing{
    margin-top: 20px;
}

</style>