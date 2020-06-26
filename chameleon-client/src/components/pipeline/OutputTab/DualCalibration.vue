<template>
  <div>
    <v-row
      align="center"
      justify="start"
    >
      <v-col
        style="padding-right:0"
        :cols="3"
      >
        <v-btn
          small
          color="#4baf62"
          @click="takePointA"
        >
          Take Point A
        </v-btn>
      </v-col>
      <v-col
        style="margin-left:0"
        :cols="3"
      >
        <v-btn
          small
          color="#4baf62"
          @click="takePointB"
        >
          Take Point B
        </v-btn>
      </v-col>
      <v-col>
        <v-btn
          small
          color="yellow darken-3"
          @click="clearSlope"
        >
          Clear All Points
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: "DualCalibration",
        props: ['rawPoint'],
        data() {
            return {
                pointA: undefined,
                pointB: undefined
            }
        },
        methods: {
            takePointA() {
                this.pointA = this.rawPoint;
                this.calcSlope();
            },
            takePointB() {
                this.pointB = this.rawPoint;
                this.calcSlope();
            },
            calcSlope() {
                if (this.pointA !== undefined && this.pointB !== undefined) {
                    let m = (this.pointB[1] - this.pointA[1]) / (this.pointB[0] - this.pointA[0]);
                    let b = this.pointA[1] - (m * this.pointA[0]);
                    if (isNaN(m) === false && isNaN(b) === false) {
                        this.sendSlope(m, b, true);
                    } else {
                        this.$emit('snackbar', "Points are too close");
                    }
                    this.pointA = undefined;
                    this.pointB = undefined;
                }
            },
            sendSlope(m, b) {
                this.handleInput('dualTargetCalibrationM', m);
                this.handleInput('dualTargetCalibrationB', b);
                this.$emit('update');
            },
            clearSlope() {
                this.sendSlope(1, 0, false);
                this.pointA = undefined;
                this.pointB = undefined;
            }
        }
    }
</script>

<style scoped>

</style>