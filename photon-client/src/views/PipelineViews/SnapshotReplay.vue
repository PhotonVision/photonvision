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
        <template v-if="img === hovered">
          <v-btn style="position: absolute; top: 0; left: 0;"
                 class="ma-2"
                 small
                 color="secondary"
                 @click="sendImage(img)">
            <v-icon small>mdi-check</v-icon>
          </v-btn>
          <v-btn style="position: absolute; top: 0; right: 0;"
                 class="ma-2"
                 small
                 color="red"
                 @click="deleteImage(img)">
            <v-icon small>mdi-delete</v-icon>
          </v-btn>
        </template>
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
      this.axios.get("http://" + this.$address + "/api/deleteSnapshot?path=" + image)
          .then(() => {
            // Re-get the snapshot list
            this.show()
          })
          .catch(err => console.log(err));
    },
    sendImage(image) {
      console.log(image)
      this.axios.post("http://" + this.$address + "/api/selectSnapshot?camIdx=" + this.$store.getters.currentCameraIndex + "&imgName=" + image)
          .then(() => {
          })
          .catch(err => console.log(err));
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