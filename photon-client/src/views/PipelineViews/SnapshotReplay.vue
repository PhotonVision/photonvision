<template>
  <v-dialog
    v-model="shown"
  >
    <v-card max-height="60%">
      <v-card-title>Replay Snapshots</v-card-title>
      <v-row>
        <CvImage
          :address="$store.getters.streamAddress[0]"
          :disconnected="!$store.state.backendConnected"
        />
        <div
          v-for="img in snapshots"
          :key="img"
        >
          <img
            :src="'http://localhost:5800/api/getSnapshot?path=' + img"
            alt=""
          >
        </div>
      </v-row>
    </v-card>
  </v-dialog>
</template>

<script>
import CvImage from "@/components/common/cv-image";
export default {
    name: "SnapshotReplay",
    components: {CvImage},
    data() {
        return {
            shown: false,
            snapshots: []
        }
    },
    created() {
        this.show();
    },
    methods: {
        show() {
            this.shown = true;

            // Make an HTTP request to get the current snapshots
            this.axios.get("http://" + this.$address + '/api/allSnapshots?cam=HP_Wide_Vision_HD_Camera')
                .then((response) => {
                    this.snapshots = response.data
                })
                .catch(err => console.log(err));

        }
    }
}
</script>

<style scoped>

</style>