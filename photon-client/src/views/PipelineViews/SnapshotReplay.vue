<template>
  <v-dialog
    v-model="shown"
  >
    <v-card max-height="60%">
      <v-card-title>Replay Snapshots</v-card-title>
      <v-row>
        <div
          v-for="img in snapshots"
          :key="img"
        >
          <img
            :src="'http://localhost:5800/api/getSnapshot?path=' + img"
            :alt="img"
            @click="click"
          >
        </div>
      </v-row>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
    name: "SnapshotReplay",
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
        click(e) {
            console.log(e.target.alt)
        },
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