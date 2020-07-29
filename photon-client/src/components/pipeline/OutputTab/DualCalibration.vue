<template>
  <div>
    <v-row
      align="center"
      justify="start"
    >
      <v-col cols="4">
        <v-btn
          small
          color="accent"
          style="width: 100%;"
          class="black--text"
          @click="takePointA"
        >
          Take Point A
        </v-btn>
      </v-col>
      <v-col cols="4">
        <v-btn
          small
          color="accent"
          style="width: 100%;"
          class="black--text"
          @click="takePointB"
        >
          Take Point B
        </v-btn>
      </v-col>
      <v-col cols="4">
        <v-btn
          small
          color="yellow darken-3"
          style="width: 100%;"
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
      // eslint-disable-next-line vue/require-prop-types
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