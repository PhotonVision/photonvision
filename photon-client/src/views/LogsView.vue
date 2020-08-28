<template>
  <div>
    <v-col>
      <v-card
        dark
        class="pl-6 pb-6 pr-6 pt-6"
        style="background-color: #006492;"
      >
        <v-card-title>
          Current Log
        </v-card-title>
        <v-row cols="12">
          <v-col cols="4">
            <v-btn
              color="secondary"
              @click="download('photonlog.log', logString)"
            >
              <v-icon left>
                mdi-download
              </v-icon>
              Download Log
            </v-btn>
          </v-col>

          <v-col
            cols="12"
            sm="6"
            class="py-2"
          >
            <p>Filter logs</p>

            <v-btn-toggle
              v-model="logLevel"
              group
              dark
              multiple
              class="fill"
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
          </v-col>
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
    data() {
        return {
            selectedLevel: [0, 1],
            possibleLevelArray: ['ERROR', 'WARN', 'INFO', 'DEBUG']
        }
    },
    computed: {
        logString() {
            const logArray = this.$store.state.logString.split('\n');
            const regexs = this.selectedLevel.map(level => `\\[[0-9 \\-:]*\\] \\[[a-zA-Z \\- 0-9]*\\] \\[${this.possibleLevelArray[level]}\\]`)
            const out = []
            logArray.forEach(s => {
                for(let patternIdx in regexs) {
                    if (s.match(regexs[patternIdx])) {
                        out.push(s);
                        return;
                    }
                }
            })

            return out.join('\n')
        },
        logLevel: {
            get() {
                return this.selectedLevel
            },
            set(value) {
                this.selectedLevel = value;
                console.log(value)
            }
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

.v-btn-toggle.fill {
    width: 100%;
    height: 100%;
}

.v-btn-toggle.fill > .v-btn {
    width: 20%;
    height: 100%;
}

</style>
