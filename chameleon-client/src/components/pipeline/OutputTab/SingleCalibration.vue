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
          @click="takePoint"
        >
          Take Point
        </v-btn>
      </v-col>
      <v-col>
        <v-btn
          small
          color="yellow darken-3"
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
        props: ['rawPoint'],
        methods: {
            clearPoint() {
                this.handleInput('point', []);
                this.$emit('update');
            },
            takePoint() {
                if (this.rawPoint[0] && this.rawPoint[1]) {
                    this.handleInput('point', this.rawPoint);
                    this.$emit('update');
                } else {
                    this.$emit('snackbar', "No target found");
                }
            }
        }
    }
</script>

<style scoped>

</style>