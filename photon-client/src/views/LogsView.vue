<template>
  <v-card
    dark
    class="pt-3"
    color="primary"
    flat
  >
    <v-card-title>
      View Program Logs

      <v-btn
        color="secondary"
        style="margin-left: auto;"
        depressed
        @click="$refs.exportLogFile.click()"
      >
        <v-icon left>
          mdi-download
        </v-icon>
        Download Log

        <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
        <a
          ref="exportLogFile"
          style="color: black; text-decoration: none; display: none"
          :href="'http://' + this.$address + '/api/settings/photonvision-journalctl.txt'"
          download="photonvision-journalctl.txt"
        />

      </v-btn>
    </v-card-title>
    <div class="pr-6 pl-6">
      <v-btn-toggle
        v-model="logLevel"
        dark
        multiple
        class="fill mb-4"
      >
        <v-btn
          v-for="(level) in possibleLevelArray"
          :key="level"
          color="secondary"
          class="fill"
        >
          {{ level }}
        </v-btn>
      </v-btn-toggle>
      <!-- Logs -->

      <v-virtual-scroll
        :items="logMessageArray"
        item-height="50"
        height="600"
      >
        <template v-slot="{ item }">
          <div :class="[getColor(item) + '--text', 'log-item']">
            {{ item.message }}
          </div>
        </template>
      </v-virtual-scroll>
    </div>

    <v-divider />

    <v-card-actions>
      <v-spacer />
      <v-btn
        color="white"
        text
        @click="$store.state.logsOverlay = false"
      >
        Close
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>

export default {
    name: "Logs",
    components: {
    },
    data() {
        return {
            selectedLevel: [0, 1, 2],
            possibleLevelArray: ['ERROR', 'WARN', 'INFO', 'DEBUG'],
            colorArray: ['red', 'yellow','green', 'white'],
        }
    },
    computed: {
        rawLogs() {
            return this.$store.state.logMessages;
        },
        logMessageArray() {
            const logArray = this.$store.state.logMessages;
            return logArray.filter(it => this.selectedLevel.includes(it.level));
        },
        logLevel: {
            get() {
                return this.selectedLevel
            },
            set(value) {
                this.selectedLevel = value;
            }
        }
    },
    methods: {
        getColor(message) {
            return this.colorArray[message.level];
        },

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
.v-btn-toggle.fill {
    width: 100%;
    height: 100%;
}

.v-btn-toggle.fill > .v-btn {
    width: 25%;
    height: 100%;
}

</style>
