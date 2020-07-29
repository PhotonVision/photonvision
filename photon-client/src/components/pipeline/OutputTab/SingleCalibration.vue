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
      // eslint-disable-next-line vue/require-prop-types
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