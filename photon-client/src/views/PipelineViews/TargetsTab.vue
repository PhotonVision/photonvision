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
              <th
                  v-if="$store.getters.pipelineType === 4 || (($store.getters.pipelineType - 2) === 3)"
                class="text-center"
              >
                Fiducial ID
              </th>
              <template v-if="!$store.getters.currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  Pitch,&nbsp;&deg;
                </th>
                <th class="text-center">
                  Yaw,&nbsp;&deg;
                </th>
                <th class="text-center">
                  Skew,&nbsp;&deg;
                </th>
                <th class="text-center">
                  Area, %
                </th>
              </template>
              <template v-else>
                <th class="text-center">
                  X,&nbsp;m
                </th>
                <th class="text-center">
                  Y,&nbsp;m
                </th>
                <th class="text-center">
                  Z Angle,&nbsp;&deg;
                </th>
              </template>
              <template v-if="$store.getters.pipelineType === 4 && $store.getters.currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  Ambiguity
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
              <td v-if="$store.getters.pipelineType === 4 || (($store.getters.pipelineType - 2) === 3)">
                {{ parseInt(value.fiducialId) }}
              </td>
              <template v-if="!$store.getters.currentPipelineSettings.solvePNPEnabled">
                <td>{{ parseFloat(value.pitch).toFixed(2) }}</td>
                <td>{{ parseFloat(value.yaw).toFixed(2) }}</td>
                <td>{{ parseFloat(value.skew).toFixed(2) }}</td>
                <td>{{ parseFloat(value.area).toFixed(2) }}</td>
              </template>
              <template v-else-if="$store.getters.currentPipelineSettings.solvePNPEnabled && $store.getters.pipelineType === 4">
                <td>{{ parseFloat(value.pose.x).toFixed(2) }}&nbsp;m</td>
                <td>{{ parseFloat(value.pose.y).toFixed(2) }}&nbsp;m</td>
                <td>{{ (parseFloat(value.pose.angle_z) * 180 / Math.PI).toFixed(2) }}&deg;</td>
              </template>
              <template v-else-if="$store.getters.currentPipelineSettings.solvePNPEnabled">
                <td>{{ parseFloat(value.pose.x).toFixed(2) }}&nbsp;m</td>
                <td>{{ parseFloat(value.pose.y).toFixed(2) }}&nbsp;m</td>
                <td>{{ (parseFloat(value.pose.angle_z) * 180 / Math.PI).toFixed(2) }}&deg;</td>
              </template>
              <template v-if="$store.getters.pipelineType === 4 && $store.getters.currentPipelineSettings.solvePNPEnabled">
                <td>
                  {{ parseFloat(value.ambiguity).toFixed(2) }}
                </td>
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
