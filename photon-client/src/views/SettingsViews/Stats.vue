<template>
  <div>
    <v-row class="pa-4">
      <table class="infoTable">
        <tr>
          <th class="infoElem infoElemTitle">
            Version
          </th>
          <th class="infoElem infoElemTitle">
            Hardware Model
          </th>
          <th class="infoElem infoElemTitle">
            Platform
          </th>
          <th class="infoElem infoElemTitle">
            GPU Acceleration
          </th>
        </tr>
        <tr>
          <td class="infoElem">
            {{ version.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ hwModel.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ platform.replace(" ", "") }}
          </td>
          <td class="infoElem">
            {{ gpuAccel.replace(" ", "") }}
          </td>
        </tr>
      </table>

      <table class="infoTable">
        <tr>
          <th class="infoElem infoElemTitle">
            CPU Usage
          </th>
          <th class="infoElem infoElemTitle">
            CPU Temp
          </th>
          <th class="infoElem infoElemTitle">
            CPU Memory Usage
          </th>
          <th class="infoElem infoElemTitle">
            GPU Memory Usage
          </th>
          <th class="infoElem infoElemTitle">
            Disk Usage
          </th>
          <th class="infoElem">
            <v-tooltip top>
              <template v-slot:activator="{ on, attrs }">
                <span
                  v-bind="attrs"
                  class="infoElemTitle"
                  v-on="on"
                >
                  CPU Throttling
                </span>
              </template>
              <span>
                Current or Previous Reason for the cpu being held back from maximum performance.
              </span>
            </v-tooltip>
          </th>
          <th class="infoElem infoElemTitle">
            CPU Uptime
          </th>
        </tr>
        <tr v-if="metrics.cpuUtil !== 'N/A'">
          <td class="infoElem">
            {{ metrics.cpuUtil }}%
          </td>
          <td class="infoElem">
            {{ parseInt(metrics.cpuTemp) }}&deg;&nbsp;C
          </td>
          <td class="infoElem">
            {{ metrics.ramUtil }}MB of {{ metrics.cpuMem }}MB
          </td>
          <td class="infoElem">
            {{ metrics.gpuMemUtil }}MB of {{ metrics.gpuMem }}MB
          </td>
          <td class="infoElem">
            {{ metrics.diskUtilPct }}
          </td>
          <td class="infoElem">
            {{ metrics.cpuThr }}
          </td>
          <td class="infoElem">
            {{ metrics.cpuUptime }}
          </td>
        </tr>
        <tr v-if="metrics.cpuUtil === 'N/A'">
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
          <td class="infoElem">
            ---
          </td>
        </tr>
      </table>
    </v-row>
  </div>
</template>

<script>
export default {
    name: 'Stats',
    computed: {
        settings() {
            return this.$store.state.settings.general;
        },
        version() {
          return `${this.settings.version}`;
        },
        hwModel() {
            if (this.settings.hardwareModel !== '') {
                return `${this.settings.hardwareModel}`;
            } else {
              return `Unknown`;
            }
        },
        platform() {
          return `${this.settings.hardwarePlatform}`;
        },
        gpuAccel() {
          return  `${this.settings.gpuAcceleration ? "Enabled" : "Unsupported"} ${this.settings.gpuAcceleration ? "(" + this.settings.gpuAcceleration + ")" : ""}`
        },
        metrics() {
          // console.log(this.$store.state.metrics);
          return this.$store.state.metrics;
        }
    },
}
</script>

<style lang="css" scoped>
.v-btn {
    width: 100%;
}

.infoTable{
  border: 1px solid;
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 5px;
  text-align: left;
  margin-bottom: 10px;
  width: 100%;
  display: block;
  overflow-x: auto;
}

.infoElem {
  padding: 1px 15px 1px 10px;
  border-right: 1px solid;
  font-weight: normal;
}

.infoElemTitle {
  font-size: 18px;
  text-decoration: underline;
  text-decoration-color: #ffd843;
}

</style>
