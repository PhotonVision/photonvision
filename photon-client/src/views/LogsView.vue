<template>
  <div>
    <v-col>
      <v-card
        dark
        class="pl-6 pb-6 pr-6 pt-6"
        style="background-color: #006492;"
      >
        <v-card-title>
          View Program Logs
        </v-card-title>
        <v-row cols="12">
          <v-col cols="4">
            <v-btn-toggle
              v-model="logLevel"
              dark
              multiple
              class="fill"
            >
              <v-btn
                v-for="(level) in possibleLevelArray"
                :key="level"
                color="secondary"
                class="fill"
                small
              >
                {{ level }}
              </v-btn>
            </v-btn-toggle>
          </v-col>

          <v-col cols="4">
            <v-btn
              color="secondary"
              @click="download('photonlog.log', rawLogs.map(it => it.message).join('\n'))"
            >
              <v-icon left>
                mdi-download
              </v-icon>
              Download Log
            </v-btn>
          </v-col>
        </v-row>
        <!-- Logs -->

        <v-virtual-scroll
          :items="logMessageArray"
          :item-height="25"
          height="600"
        >
          <template v-slot="{ item }">
            <span :class="getColor(item) + '--text'">{{ item.message }}</span>
          </template>
        </v-virtual-scroll>
      </v-card>
    </v-col>
  </div>
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

.v-btn-toggle.fill {
    width: 100%;
    height: 100%;
}

.v-btn-toggle.fill > .v-btn {
    width: 25%;
    height: 100%;
}

</style>
