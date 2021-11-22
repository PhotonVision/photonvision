<template>
  <div>
    <v-row
      align="center"
      justify="start"
    >
      <v-col cols="6">
        <v-btn
          small
          color="accent"
          class="black--text"
          style="width: 100%;"
          @click="takePoint"
        >
          Take Point
        </v-btn>
      </v-col>
      <v-col cols="6">
        <v-btn
          small
          color="yellow darken-3"
          style="width: 100%;"
          @click="clearPoint"
        >
          Clear Point
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: "SingleCalibration",
        methods: {
            clearPoint() {
              this.handleInputWithIndex("robotOffsetPoint", 0, this.$store.state.currentCameraIndex)
            },
            takePoint() {
              this.handleInputWithIndex("robotOffsetPoint", 1, this.$store.state.currentCameraIndex)
            }
        }
    }
</script>

<style scoped>

</style>
