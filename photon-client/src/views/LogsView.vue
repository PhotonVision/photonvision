<template>
  <div>
    <v-col
      cols="12"
    >
      <v-card
        dark
        class="pa-6"
        style="background-color: #006492;"
      >
        <v-row>
          <v-card-title>
            Current Log
          </v-card-title>
          <v-btn
            color="secondary"
            @click="download('photonlog.log', logString)"
          >
            <v-icon left>
              mdi-download
            </v-icon>
            Download Log
          </v-btn>
        </v-row>

        <log-view
          class="loggerClass"
          :log="logString"
        />
      </v-card>
    </v-col>
  </div>
</template>

<script>
import logView from '@femessage/log-viewer';

export default {
    name: "Logs",
    components: {
        logView
    },
    computed: {
        logString() {
            return this.$store.state.logString;
        }
    },
    methods: {
        download(filename, text) {
            const element = document.createElement('a');
            element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
            element.setAttribute('download', filename);

            element.style.display = 'none';
            document.body.appendChild(element);

            element.click();

            document.body.removeChild(element);
        }
    }
}

</script>

<style scoped>
.loggerClass {
    /*    position: absolute;*/
    /*    bottom: 0;*/
    /*    height: 25% !important;*/
    /*    left: 0;*/
    /*    right: 0;*/
    /*    box-shadow: #282828 0 0 5px 1px;*/
    /*    background-color: #2b2b2b;*/
    background-color: #232C37 !important;
}
</style>
