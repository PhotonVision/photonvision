<template>
<!--  <v-dialog-->
<!--    v-model="shown"-->
<!--  >-->
<!--    <v-card color="primary" dark height="1000px">-->
<!--      <v-card-title>Replay Snapshots</v-card-title>-->
      <v-row style="overflow-y: scroll; max-height: 300px;" class="ml-6 mr-6">
        <v-col
          v-for="img in snapshots"
          :key="img"
          cols="4"
        >
          <v-btn x-small color="red" @click="deleteImage(img)">
            <v-icon small>mdi-delete</v-icon>
          </v-btn>
            <img
                :src="'http://localhost:5800/api/getSnapshot?path=' + img"
                :alt="img"
                @click="click"
                class="align-center justify-center"
                style="width: 100%"
            >
        </v-col>
      </v-row>
<!--    </v-card>-->
<!--  </v-dialog>-->
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
        deleteImage(image) {
          console.log(image)
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
.box {
  position: relative;
  display: inline-block;
  width: 200px;
  height: 100%;
  background-color: #fff;
  border-radius: 5px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  -webkit-transition: all 0.6s cubic-bezier(0.165, 0.84, 0.44, 1);
  transition: all 0.6s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.box::after {
  content: "";
  border-radius: 5px;
  position: absolute;
  z-index: -1;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  opacity: 0;
  -webkit-transition: all 0.6s cubic-bezier(0.165, 0.84, 0.44, 1);
  transition: all 0.6s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.box:hover {
  -webkit-transform: scale(1.25, 1.25);
  transform: scale(1.25, 1.25);
}

.box:hover::after {
  opacity: 1;
}
</style>