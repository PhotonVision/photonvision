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
          @click="clearPoints"
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

        methods: {
            clearPoints() {
              this.handleInputWithIndex("robotOffsetPoint", 0, this.$store.state.currentCameraIndex)
            },
            takePointA() {
              this.handleInputWithIndex("robotOffsetPoint", 2, this.$store.state.currentCameraIndex)
            },
            takePointB() {
              this.handleInputWithIndex("robotOffsetPoint", 3, this.$store.state.currentCameraIndex)
            }
        }
    }
</script>

<style scoped>

</style>
