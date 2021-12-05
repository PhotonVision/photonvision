<template>
  <v-row style="overflow-y: scroll; max-height: 400px;" class="ml-6 mr-6">
    <v-col
        v-for="img in snapshots"
        :key="img"
        cols="3"
    >
      <v-img
          :src="'http://localhost:5800/api/getSnapshot?path=' + img"
          @mouseover="registerHover(img)"
          @mouseleave="registerHover(null)"
          :alt="'Snapshot ' + img">
        <v-overlay
            absolute
            :opacity="0.2"
            :value="img === hovered">
          <v-btn small color="secondary" @click="sendImage(img)">
            <v-icon small>mdi-check</v-icon>
          </v-btn>
          <v-btn small color="red" @click="deleteImage(img)">
            <v-icon small>mdi-delete</v-icon>
          </v-btn>
        </v-overlay>
      </v-img>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: "SnapshotReplay",
  data() {
    return {
      shown: false,
      snapshots: [],
      hovered: null,
    }
  },
  created() {
    this.show();
  },
  methods: {
    registerHover(image) {
      this.hovered = image;
    },
    deleteImage(image) {
      console.log(image)
    },
    sendImage(image) {
      console.log(image)
    },
    show() {
      this.shown = true;

      // Make an HTTP request to get the current snapshots
      const camUri = '/api/allSnapshots?cam=' + this.$store.getters.cameraList[this.$store.getters.currentCameraIndex];
      this.axios.get("http://" + this.$address + camUri)
          .then((response) => {
            // Apparently we need this for v-images
            this.snapshots = response.data.map(it => it.replace("\\", "/"))
            console.log(this.snapshots)
          })
          .catch(err => console.log(err));

    }
  }
}
</script>

<style scoped>

</style>