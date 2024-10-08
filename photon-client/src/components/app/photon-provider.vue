<script setup lang="ts">
import { useClientStore } from "@/stores/ClientStore";
import { computed, inject, ref } from "vue";
import { useServerStore } from "@/stores/ServerStore";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import { DropdownSelectItem } from "@/types/Components";
import { ValidQuirks } from "@/types/SettingTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const exportLogFile = ref();
const backendHost = inject<string>("backendHost");

const currentCamHasQuirkMismatch = computed<boolean>(() => {
  const currentCameraQuirks = serverStore.currentCameraSettings?.cameraQuirks.quirks;
  return currentCameraQuirks !== undefined && currentCameraQuirks.ArduCamCamera && (!currentCameraQuirks.ArduOV2311 || !currentCameraQuirks.ArduOV9281 || !currentCameraQuirks.ArduOV9782);
});

const arducamModel = ref<ValidQuirks.ArduOV2311 | ValidQuirks.ArduOV9281 | ValidQuirks.ArduOV9782>();
const arducamModels = computed<DropdownSelectItem<ValidQuirks.ArduOV2311 | ValidQuirks.ArduOV9281 | ValidQuirks.ArduOV9782>[]>(() => [
  {
    name: ValidQuirks.ArduOV2311,
    value: ValidQuirks.ArduOV2311
  },
  {
    name: ValidQuirks.ArduOV9281,
    value: ValidQuirks.ArduOV9281
  },
  {
    name: ValidQuirks.ArduOV9782,
    value: ValidQuirks.ArduOV9782
  }
]);
</script>

<template>
  <div>
<!--    <v-dialog max-width="600px" :model-value="!clientStore.backendConnected" persistent>-->
<!--      <v-card>-->
<!--        <v-row>-->
<!--          <v-col>-->
<!--            <v-card-title class="pt-4">Awaiting Backend Connection</v-card-title>-->
<!--          </v-col>-->
<!--          <v-col class="d-flex justify-center align-center" cols="2">-->
<!--            <v-progress-circular color="accent" indeterminate />-->
<!--          </v-col>-->
<!--        </v-row>-->
<!--        <v-card-text>-->
<!--          PhotonClient is still trying to establish a connection to the backend process.-->
<!--        </v-card-text>-->
<!--        <v-card-actions>-->
<!--          <v-btn-->
<!--            class="mb-2 mr-2"-->
<!--            color="secondary"-->
<!--            prepend-icon="mdi-download"-->
<!--            style="margin-left: auto; max-width: 500px"-->
<!--            text="Attempt to Download Current Log"-->
<!--            variant="flat"-->
<!--            @click="exportLogFile.click()"-->
<!--          />-->
<!--          <a-->
<!--            ref="exportLogFile"-->
<!--            class="d-none"-->
<!--            download="photonvision-journalctl.txt"-->
<!--            :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"-->
<!--            target="_blank"-->
<!--          />-->
<!--        </v-card-actions>-->
<!--      </v-card>-->
<!--    </v-dialog>-->
    <v-dialog v-model="currentCamHasQuirkMismatch" max-width="600px" persistent>
      <v-card-title class="pt-4">Camera Quirk Mismatch for {{ serverStore.currentCameraName }}</v-card-title>
      <v-card-subtitle>User set quirks aren't properly defined for this camera. You must define these quirks for PhotonVision to properly function.</v-card-subtitle>
      <pv-dropdown
        v-model="arducamModel"
        class="pl-4 pr-4"
        :items="arducamModels"
        label="ArduCam Model"
      />
      <v-card-actions>
        <v-btn
          color="secondary"
          :disabled="!arducamModel"
          prepend-icon="mdi-download"
          style="margin-left: auto; max-width: 500px"
          text="Download Current Log"
          variant="flat"
          @click="serverStore.setArduCamModel(arducamModel as ValidQuirks. ArduOV9281 | ValidQuirks. ArduOV2311 | ValidQuirks. ArduOV9782, true)"
        />
      </v-card-actions>
    </v-dialog>
    <slot />
  </div>
</template>
