<template>
  <div>
    <v-row
      align="start"
      class="pb-4"
      style="height: 300px;"
    >
      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <v-simple-table
        fixed-header
        height="100%"
        dense
        dark
      >
        <template v-slot:default>
          <thead style="font-size: 1.25rem;">
            <tr>
              <th class="text-center">
                Target
              </th>
              <template v-if="!$store.getters.currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  Pitch
                </th>
                <th class="text-center">
                  Yaw
                </th>
                <th class="text-center">
                  Skew
                </th>
              </template>
              <th class="text-center">
                Area
              </th>
              <template v-if="$store.getters.currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  X
                </th>
                <th class="text-center">
                  Y
                </th>
                <th class="text-center">
                  Angle
                </th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(value, index) in $store.getters.currentPipelineResults.targets"
              :key="index"
            >
              <td>{{ index }}</td>
              <template v-if="!$store.getters.currentPipelineSettings.solvePNPEnabled">
                <td>{{ parseFloat(value.pitch).toFixed(2) }}</td>
                <td>{{ parseFloat(value.yaw).toFixed(2) }}</td>
                <td>{{ parseFloat(value.skew).toFixed(2) }}</td>
              </template>
              <td>{{ parseFloat(value.area).toFixed(2) }}</td>
              <template v-if="$store.getters.currentPipelineSettings.solvePNPEnabled">
                <!-- TODO: Make sure that units are correct -->
                <td>{{ parseFloat(value.pose.x).toFixed(2) }}&nbsp;m</td>
                <td>{{ parseFloat(value.pose.y).toFixed(2) }}&nbsp;m</td>
                <td>{{ parseFloat(value.pose.rot).toFixed(2) }}&deg;</td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: "TargetsTab",
    }
</script>

<style scoped>
    .v-data-table {
        text-align: center;
        background-color: transparent !important;
        width: 100%;
        height: 100%;
        overflow-y: auto;
    }

    .v-data-table th {
        background-color: #006492 !important;
    }

    .v-data-table th,td {
        font-size: 1rem !important;
    }

    .v-data-table td {
      font-family: monospace !important;
    }
</style>