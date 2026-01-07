<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, ref, watchEffect } from "vue";
import PvInput from "@/components/common/pv-input.vue";
import PvRadio from "@/components/common/pv-radio.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvSelect from "@/components/common/pv-select.vue";
import { type ConfigurableNetworkSettings, NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "vuetify";
import { getThemeColor, setThemeColor, resetTheme } from "@/lib/ThemeManager";
import { statusCheck } from "@/lib/PhotonUtils";

const theme = useTheme();

// Copy object to remove reference to store
const tempSettingsStruct = ref<ConfigurableNetworkSettings>(Object.assign({}, useSettingsStore().network));
const resetTempSettingsStruct = () => {
  tempSettingsStruct.value = Object.assign({}, useSettingsStore().network);
};

const settingsValid = ref(true);

const showThemeConfig = ref(false);
const backgroundColor = ref("");
const primaryColor = ref("");
const secondaryColor = ref("");
const surfaceColor = ref("");

const loadCurrentColors = () => {
  backgroundColor.value = getThemeColor(theme, "background");
  primaryColor.value = getThemeColor(theme, "primary");
  secondaryColor.value = getThemeColor(theme, "secondary");
  surfaceColor.value = getThemeColor(theme, "surface");
};

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

const currentNetworkInterfaceIndex = computed<number | undefined>({
  get: () => {
    const index = useSettingsStore().networkInterfaceNames.indexOf(
      useSettingsStore().network.networkManagerIface || ""
    );
    return index === -1 ? undefined : index;
  },
  set: (v) => v && (tempSettingsStruct.value.networkManagerIface = useSettingsStore().networkInterfaceNames[v])
});

watchEffect(() => {
  // Reset temp settings on remote network settings change
  resetTempSettingsStruct();
});
</script>

<template>
  <v-card class="mb-3 rounded-12" color="surface">
    <v-card-title style="display: flex; justify-content: space-between">
      <span>Global Settings</span>
      <v-btn
        variant="text"
        @click="
          () => {
            loadCurrentColors();
            showThemeConfig = true;
          }
        "
      >
        <v-icon size="x-large">mdi-palette-outline</v-icon>
        Theme
      </v-btn>
    </v-card-title>
    <div class="pa-5 pt-0">
      <v-card-title class="pl-0 pt-0 pb-10px">Networking</v-card-title>
      <v-form ref="form" v-model="settingsValid">
        <pv-input
          v-model="tempSettingsStruct.ntServerAddress"
          label="Team Number/NetworkTables Server Address"
          tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
          :label-cols="4"
          :disabled="tempSettingsStruct.runNTServer"
          :rules="[
            (v) =>
              isValidNetworkTablesIP(v) ||
              'The NetworkTables Server Address must be a valid Team Number, IP address, or Hostname'
          ]"
        />
        <v-alert
          v-if="!isValidNetworkTablesIP(tempSettingsStruct.ntServerAddress) && !tempSettingsStruct.runNTServer"
          class="pt-3 pb-3"
          color="error"
          density="compact"
          text="The NetworkTables Server Address is not set or is invalid. NetworkTables is unable to connect."
          icon="mdi-alert-circle-outline"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
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
          :rules="[(v) => isValidIPv4(v) || 'Invalid IPv4 address']"
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
          :rules="[(v) => isValidHostname(v) || 'Invalid hostname']"
          :disabled="
            !tempSettingsStruct.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
          "
        />
        <v-card-title class="pl-0 pt-3 pb-10px">Advanced Networking</v-card-title>
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
          v-model="currentNetworkInterfaceIndex"
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
        <v-alert
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
          icon="mdi-alert-circle-outline"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
        />
        <pv-switch
          v-model="tempSettingsStruct.runNTServer"
          label="Run NetworkTables Server (Debugging Only)"
          tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
          :label-cols="4"
        />
        <v-alert
          v-if="tempSettingsStruct.runNTServer"
          color="buttonActive"
          density="compact"
          text="This mode is intended for debugging and should be off for proper usage. PhotonLib will NOT work!"
          icon="mdi-information-outline"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
        />
        <v-card-title class="pl-0 pt-3 pb-10px">Miscellaneous</v-card-title>
        <pv-switch
          v-model="tempSettingsStruct.shouldPublishProto"
          label="Also Publish Protobuf"
          tooltip="If enabled, Photon will publish all pipeline results in both the Packet and Protobuf formats. This is useful for visualizing pipeline results from NT viewers such as glass and logging software such as AdvantageScope. Note: photon-lib will ignore this value and is not recommended on the field for performance."
          :label-cols="4"
        />
        <v-alert
          v-if="tempSettingsStruct.shouldPublishProto"
          color="buttonActive"
          density="compact"
          text="This mode is intended for debugging and may reduce performance; it should be off for field use."
          icon="mdi-information-outline"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
        />
      </v-form>
      <v-btn
        color="primary"
        class="mt-3"
        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
        style="color: black; width: 100%"
        :disabled="!settingsValid || !settingsHaveChanged()"
        @click="saveGeneralSettings"
      >
        Save
      </v-btn>
    </div>
    <v-dialog v-model="showThemeConfig" width="800" dark>
      <v-card color="surface" flat>
        <v-card-title class="text-center">Theme Configuration</v-card-title>
        <v-card-text class="pt-0 pb-10px">
          <v-row>
            <v-col class="text-center">
              Background
              <v-color-picker
                v-model:model-value="backgroundColor"
                class="ma-auto pt-3"
                elevation="0"
                mode="hex"
                :modes="['hex']"
                @update:model-value="(hex) => setThemeColor(theme, 'background', hex)"
              ></v-color-picker>
            </v-col>
            <v-col class="text-center">
              Surface
              <v-color-picker
                v-model:model-value="surfaceColor"
                class="ma-auto pt-3"
                elevation="0"
                mode="hex"
                :modes="['hex']"
                @update:model-value="(hex) => setThemeColor(theme, 'surface', hex)"
              ></v-color-picker>
            </v-col>
          </v-row>
          <v-row>
            <v-col class="text-center">
              Primary
              <v-color-picker
                v-model:model-value="primaryColor"
                class="ma-auto pt-3"
                elevation="0"
                mode="hex"
                :modes="['hex']"
                @update:model-value="(hex) => setThemeColor(theme, 'primary', hex)"
              ></v-color-picker>
            </v-col>
            <v-col class="text-center">
              Secondary
              <v-color-picker
                v-model:model-value="secondaryColor"
                class="ma-auto pt-3"
                elevation="0"
                mode="hex"
                :modes="['hex']"
                @update:model-value="(hex) => setThemeColor(theme, 'secondary', hex)"
              ></v-color-picker>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="pa-5 pt-0">
          <v-btn
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            color="buttonPassive"
            class="text-black"
            @click="showThemeConfig = false"
          >
            Close
          </v-btn>
          <v-btn
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            color="buttonActive"
            class="text-black"
            @click="
              () => {
                resetTheme(theme);
                loadCurrentColors();
              }
            "
          >
            Reset Default
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style>
.mt-10px {
  margin-top: 10px !important;
}
</style>
