<script setup lang="ts">
import IconPaletteOutline from "~icons/mdi/palette-outline";
import IconAlertCircleOutline from "~icons/mdi/alert-circle-outline";
import IconInformationOutline from "~icons/mdi/information-outline";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, reactive, ref, watchEffect } from "vue";
import { type ConfigurableNetworkSettings, NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import { getThemeColor, setThemeColor, resetTheme } from "@/lib/ThemeManager";
import { statusCheck } from "@/lib/PhotonUtils";
import type { Color } from "reka-ui";
import {
  ColorAreaArea,
  ColorAreaRoot,
  ColorAreaThumb,
  ColorFieldInput,
  ColorFieldRoot,
  ColorSliderRoot,
  ColorSliderThumb,
  ColorSliderTrack,
  ColorSwatch,
  colorToString,
  normalizeColor
} from "reka-ui";

// Copy object to remove reference to store
const tempSettingsStruct = ref<ConfigurableNetworkSettings>(Object.assign({}, useSettingsStore().network));
const resetTempSettingsStruct = () => {
  tempSettingsStruct.value = Object.assign({}, useSettingsStore().network);
};

const settingsValid = computed(() => {
  const network = useSettingsStore().network;

  const ntServerValid = network.runNTServer || isValidNetworkTablesIP(network.ntServerAddress);
  const staticIpValid =
    network.networkingDisabled ||
    network.connectionType !== NetworkConnectionType.Static ||
    !network.shouldManage ||
    !network.canManage ||
    isValidIPv4(network.staticIp);
  const hostnameValid =
    network.networkingDisabled || !network.shouldManage || !network.canManage || isValidHostname(network.hostname);

  return ntServerValid && staticIpValid && hostnameValid;
});

const showThemeConfig = ref(false);

const themeColors = [
  { label: "Background", key: "background" },
  { label: "Surface", key: "surface" },
  { label: "Primary", key: "primary" },
  { label: "Secondary", key: "secondary" }
] as const;

const colorObjects = reactive<Record<string, Color>>({});

const loadCurrentColors = () => {
  for (const { key } of themeColors) {
    colorObjects[key] = normalizeColor(getThemeColor(key));
  }
};

function handleColorUpdate(key: string, newColor: Color) {
  colorObjects[key] = newColor;
  setThemeColor(key as "background" | "surface" | "primary" | "secondary", colorToString(newColor, "hex"));
}

function handleHexUpdate(key: string, hex: string) {
  colorObjects[key] = normalizeColor(hex);
  setThemeColor(key as "background" | "surface" | "primary" | "secondary", hex);
}

function getHexColor(key: string): string {
  return colorObjects[key] ? colorToString(colorObjects[key], "hex") : "#000000";
}

const isValidNetworkTablesIP = (v: string | undefined): boolean => {
  // Check if it is a valid team number between 1-99999 (5 digits)
  const teamNumberRegex = /^[1-9][0-9]{0,4}$/;
  // Check if it is a team number longer than 5 digits
  const badTeamNumberRegex = /^[0-9]{6,}$/;

  if (v === undefined) return false;
  if (teamNumberRegex.test(v)) return true;
  if (isValidIPv4(v)) return true;
  // need to check these before the hostname. "0" and "99999" are valid hostnames, but we don't want to allow then
  if (v === "0") return false;
  if (badTeamNumberRegex.test(v)) return false;
  return isValidHostname(v);
};
const isValidIPv4 = (v: string | undefined) => {
  // https://stackoverflow.com/a/17871737
  const ipv4Regex = /^((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$/;

  if (v === undefined) return false;
  return ipv4Regex.test(v);
};
const isValidHostname = (v: string | undefined) => {
  // https://stackoverflow.com/a/18494710
  const hostnameRegex = /^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)+(\.([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*))*$/;

  if (v === undefined) return false;
  return hostnameRegex.test(v);
};

const settingsHaveChanged = (): boolean => {
  const a = useSettingsStore().network;
  const b = tempSettingsStruct.value;

  return (
    a.ntServerAddress !== b.ntServerAddress ||
    a.connectionType !== b.connectionType ||
    a.staticIp !== b.staticIp ||
    a.hostname !== b.hostname ||
    a.runNTServer !== b.runNTServer ||
    a.shouldManage !== b.shouldManage ||
    a.shouldPublishProto !== b.shouldPublishProto ||
    a.networkManagerIface !== b.networkManagerIface ||
    a.setStaticCommand !== b.setStaticCommand ||
    a.setDHCPcommand !== b.setDHCPcommand
  );
};

const saveGeneralSettings = async () => {
  // replace undefined members with empty strings for backend
  const payload = {
    connectionType: tempSettingsStruct.value.connectionType,
    hostname: tempSettingsStruct.value.hostname,
    networkManagerIface: tempSettingsStruct.value.networkManagerIface || "",
    ntServerAddress: tempSettingsStruct.value.ntServerAddress,
    runNTServer: tempSettingsStruct.value.runNTServer,
    setDHCPcommand: tempSettingsStruct.value.setDHCPcommand || "",
    setStaticCommand: tempSettingsStruct.value.setStaticCommand || "",
    shouldManage: tempSettingsStruct.value.shouldManage,
    shouldPublishProto: tempSettingsStruct.value.shouldPublishProto,
    staticIp: tempSettingsStruct.value.staticIp
  };

  const changingStaticIP =
    useSettingsStore().network.connectionType === NetworkConnectionType.Static &&
    tempSettingsStruct.value.staticIp !== useSettingsStore().network.staticIp;

  try {
    const response = await useSettingsStore().updateGeneralSettings(payload);
    useStateStore().showSnackbarMessage({ message: response.data.text || response.data, color: "success" });

    // Update the local settings cause the backend checked their validity. Assign is to deref value
    useSettingsStore().network = { ...useSettingsStore().network, ...Object.assign({}, tempSettingsStruct.value) };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    resetTempSettingsStruct();
    if (error.response) {
      useStateStore().showSnackbarMessage({
        color: "error",
        message: error.response.data.text || error.response.data
      });
    } else if (error.request) {
      useStateStore().showSnackbarMessage({
        color: "error",
        message: "Error while trying to process the request! The backend didn't respond."
      });
    } else {
      useStateStore().showSnackbarMessage({
        color: "error",
        message: "An error occurred while trying to process the request."
      });
    }
    return;
  }

  if (changingStaticIP) {
    const status = await statusCheck(5000, tempSettingsStruct.value.staticIp);

    if (!status) {
      useStateStore().showSnackbarMessage({
        message:
          "Warning: Unable to verify new static IP address! You may need to manually navigate to the new address: http://" +
          tempSettingsStruct.value.staticIp +
          ":5800",
        color: "warning"
      });
      return;
    }

    // Keep current hash route (e.g., #/settings)
    const hash = window.location.hash || "";
    const url = `http://${tempSettingsStruct.value.staticIp}:5800/${hash}`;
    setTimeout(() => {
      window.location.href = url;
    }, 1000);
  }
};

const currentNetworkInterface = computed<string>({
  get: () => useSettingsStore().network.networkManagerIface || "",
  set: (v) => {
    tempSettingsStruct.value.networkManagerIface = v;
  }
});

watchEffect(() => {
  // Reset temp settings on remote network settings change
  resetTempSettingsStruct();
});
</script>

<template>
  <pv-card padding="none" class="my-3">
    <div class="flex items-center justify-between p-5 pb-2">
      <div class="text-lg font-semibold">Global Settings</div>
      <pv-button
        variant="text"
        size="sm"
        :icon="IconPaletteOutline"
        @click="
          () => {
            loadCurrentColors();
            showThemeConfig = true;
          }
        "
      >
        Theme
      </pv-button>
    </div>
    <div class="p-5 pt-0">
      <div class="pb-10px text-base font-semibold">Networking</div>
      <div>
        <pv-input
          v-model="tempSettingsStruct.ntServerAddress"
          label="Team Number/NetworkTables Server Address"
          tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
          :label-cols="4"
          :disabled="tempSettingsStruct.runNTServer"
          :rules="[
            (v) =>
              (typeof v === 'string' && isValidNetworkTablesIP(v)) ||
              'The NetworkTables Server Address must be a valid Team Number, IP address, or Hostname'
          ]"
        />
        <pv-alert
          v-if="!isValidNetworkTablesIP(tempSettingsStruct.ntServerAddress) && !tempSettingsStruct.runNTServer"
          class="pt-3 pb-3"
          color="error"
          density="compact"
          text="The NetworkTables Server Address is not set or is invalid. NetworkTables is unable to connect."
          :icon="IconAlertCircleOutline"
        />
        <pv-radio
          v-show="!useSettingsStore().network.networkingDisabled"
          v-model="tempSettingsStruct.connectionType"
          label="IP Assignment Mode"
          tooltip="DHCP will make the radio (router) automatically assign an IP address; this may result in an IP address that changes across reboots. Static IP assignment means that you pick the IP address and it won't change."
          :input-cols="12 - 4"
          :list="['DHCP', 'Static']"
          :disabled="
            !tempSettingsStruct.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
          "
        />
        <pv-input
          v-show="!useSettingsStore().network.networkingDisabled"
          v-if="tempSettingsStruct.connectionType === NetworkConnectionType.Static"
          v-model="tempSettingsStruct.staticIp"
          :input-cols="12 - 4"
          label="Static IP"
          :rules="[(v) => (typeof v === 'string' && isValidIPv4(v)) || 'Invalid IPv4 address']"
          :disabled="
            !tempSettingsStruct.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
          "
        />
        <pv-input
          v-show="!useSettingsStore().network.networkingDisabled"
          v-model="tempSettingsStruct.hostname"
          label="Hostname"
          :input-cols="12 - 4"
          :rules="[(v) => (typeof v === 'string' && isValidHostname(v)) || 'Invalid hostname']"
          :disabled="
            !tempSettingsStruct.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
          "
        />
        <div class="pt-3 pb-10px text-base font-semibold">Advanced Networking</div>
        <pv-switch
          v-show="!useSettingsStore().network.networkingDisabled"
          v-model="tempSettingsStruct.shouldManage"
          :disabled="!useSettingsStore().network.canManage || useSettingsStore().network.networkingDisabled"
          label="Manage Device Networking"
          tooltip="If enabled, Photon will manage device hostname and network settings."
          :label-cols="4"
        />
        <pv-select
          v-show="!useSettingsStore().network.networkingDisabled"
          v-model="currentNetworkInterface"
          label="NetworkManager interface"
          :disabled="
            !tempSettingsStruct.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
          "
          :select-cols="12 - 4"
          tooltip="Name of the interface PhotonVision should manage the IP address of"
          :items="useSettingsStore().networkInterfaceNames"
        />
        <pv-alert
          v-if="
            !useSettingsStore().networkInterfaceNames.length &&
            tempSettingsStruct.shouldManage &&
            useSettingsStore().network.canManage &&
            !useSettingsStore().network.networkingDisabled
          "
          class="pt-3 pb-3"
          color="error"
          density="compact"
          text="Cannot detect any wired connections! Send program logs to the developers for help."
          :icon="IconAlertCircleOutline"
        />
        <pv-switch
          v-model="tempSettingsStruct.runNTServer"
          label="Run NetworkTables Server (Debugging Only)"
          tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
          :label-cols="4"
        />
        <pv-alert
          v-if="tempSettingsStruct.runNTServer"
          color="buttonActive"
          density="compact"
          text="This mode is intended for debugging and should be off for proper usage. PhotonLib will NOT work!"
          :icon="IconInformationOutline"
        />
        <div class="pt-3 pb-10px text-base font-semibold">Miscellaneous</div>
        <pv-switch
          v-model="tempSettingsStruct.shouldPublishProto"
          label="Also Publish Protobuf"
          tooltip="If enabled, Photon will publish all pipeline results in both the Packet and Protobuf formats. This is useful for visualizing pipeline results from NT viewers such as glass and logging software such as AdvantageScope. Note: photon-lib will ignore this value and is not recommended on the field for performance."
          :label-cols="4"
        />
        <pv-alert
          v-if="tempSettingsStruct.shouldPublishProto"
          color="buttonActive"
          density="compact"
          text="This mode is intended for debugging and may reduce performance; it should be off for field use."
          :icon="IconInformationOutline"
        />
      </div>
      <pv-button
        variant="primary"
        class="mt-3"
        block
        :disabled="!settingsValid || !settingsHaveChanged()"
        @click="saveGeneralSettings"
      >
        Save
      </pv-button>
    </div>
    <pv-dialog v-model="showThemeConfig" width="800">
      <pv-card padding="none" class="p-5">
        <div class="text-center text-lg font-semibold pb-3">Theme Configuration</div>
        <div class="pt-0 pb-10px">
          <div class="flex flex-wrap gap-4 justify-center">
            <div v-for="{ label, key } in themeColors" :key="key" class="flex flex-col items-center gap-2 w-45">
              <div class="flex items-center gap-2">
                <ColorSwatch
                  :color="getHexColor(key)"
                  class="w-5 h-5 rounded-sm border border-white/10"
                  :style="{ backgroundColor: 'var(--reka-color-swatch-color)' }"
                />
                <span class="text-sm font-medium">{{ label }}</span>
              </div>

              <!-- 2D Color Area (Saturation/Lightness) -->
              <ColorAreaRoot
                v-slot="{ style }"
                :model-value="colorObjects[key]"
                color-space="hsl"
                x-channel="saturation"
                y-channel="lightness"
                class="relative w-full"
                @update:color="(c: Color) => handleColorUpdate(key, c)"
              >
                <ColorAreaArea
                  class="relative w-full h-30 rounded-md overflow-hidden"
                  :style="style"
                >
                  <ColorAreaThumb class="block w-4 h-4 rounded-full bg-white border-2 border-white shadow-md cursor-pointer" />
                </ColorAreaArea>
              </ColorAreaRoot>

              <!-- Hue Slider -->
              <ColorSliderRoot
                :model-value="colorObjects[key]"
                channel="hue"
                color-space="hsl"
                class="relative flex items-center w-full h-4"
                @update:color="(c: Color) => handleColorUpdate(key, c)"
              >
                <ColorSliderTrack class="relative flex-1 rounded-full h-2">
                  <div class="absolute inset-0 rounded-full hue-gradient" />
                </ColorSliderTrack>
                <ColorSliderThumb class="block w-4 h-4 rounded-full bg-white border-2 border-white shadow-md cursor-pointer" />
              </ColorSliderRoot>

              <!-- Hex Input -->
              <ColorFieldRoot
                :model-value="getHexColor(key)"
                class="w-full"
                @update:model-value="(hex: string) => handleHexUpdate(key, hex)"
              >
                <ColorFieldInput
                  class="w-full px-2 py-1 text-sm border border-gray-600 bg-transparent rounded-md text-center font-mono"
                  placeholder="#000000"
                />
              </ColorFieldRoot>
            </div>
          </div>
        </div>
        <div class="flex flex-wrap justify-end gap-3 pt-0">
          <pv-button variant="passive" @click="showThemeConfig = false"> Close </pv-button>
          <pv-button
            variant="primary"
            @click="
              () => {
                resetTheme();
                loadCurrentColors();
              }
            "
          >
            Reset Default
          </pv-button>
        </div>
      </pv-card>
    </pv-dialog>
  </pv-card>
</template>

<style scoped>
.mt-10px {
  margin-top: 10px !important;
}

.hue-gradient {
  background: linear-gradient(
    to right,
    #ff0000 0%,
    #ffff00 17%,
    #00ff00 33%,
    #00ffff 50%,
    #0000ff 67%,
    #ff00ff 83%,
    #ff0000 100%
  );
}
</style>
