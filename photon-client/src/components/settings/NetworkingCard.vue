<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, ref, watchEffect } from "vue";
import PvInput from "@/components/common/pv-input.vue";
import PvRadio from "@/components/common/pv-radio.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvSelect from "@/components/common/pv-select.vue";
import { type ConfigurableNetworkSettings, NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

// Copy object to remove reference to store
const tempSettingsStruct = ref<ConfigurableNetworkSettings>(Object.assign({}, useSettingsStore().network));
const resetTempSettingsStruct = () => {
  tempSettingsStruct.value = Object.assign({}, useSettingsStore().network);
};

const settingsValid = ref(true);

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

const saveGeneralSettings = () => {
  const changingStaticIp = useSettingsStore().network.connectionType === NetworkConnectionType.Static;

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

  useSettingsStore()
    .updateGeneralSettings(payload)
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });

      // Update the local settings cause the backend checked their validity. Assign is to deref value
      useSettingsStore().network = {
        ...useSettingsStore().network,
        ...Object.assign({}, tempSettingsStruct.value)
      };
    })
    .catch((error) => {
      resetTempSettingsStruct();
      if (error.response) {
        if (error.status === 504 || changingStaticIp) {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: `Connection lost! Try the new static IP at ${useSettingsStore().network.staticIp}:5800 or ${
              useSettingsStore().network.hostname
            }:5800?`
          });
        } else {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: error.response.data.text || error.response.data
          });
        }
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
    });
};

const currentNetworkInterfaceIndex = computed<number>({
  get: () => useSettingsStore().networkInterfaceNames.indexOf(useSettingsStore().network.networkManagerIface || ""),
  set: (v) => (tempSettingsStruct.value.networkManagerIface = useSettingsStore().networkInterfaceNames[v])
});

watchEffect(() => {
  // Reset temp settings on remote network settings change
  resetTempSettingsStruct();
});
</script>

<template>
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Global Settings</v-card-title>
    <div class="pa-6 pt-0">
      <v-divider class="pb-3" />
      <v-card-title class="pl-0 pt-3 pb-3">Networking</v-card-title>
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
        <v-banner
          v-if="!isValidNetworkTablesIP(tempSettingsStruct.ntServerAddress) && !tempSettingsStruct.runNTServer"
          rounded
          color="error"
          text-color="white"
          style="margin: 10px 0"
          icon="mdi-alert-circle-outline"
        >
          The NetworkTables Server Address is not set or is invalid. NetworkTables is unable to connect.
        </v-banner>
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
        <v-divider class="mt-3 pb-3" />
        <v-card-title class="pl-0 pt-3 pb-3">Advanced Networking</v-card-title>
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
        <v-banner
          v-if="
            !useSettingsStore().networkInterfaceNames.length &&
            tempSettingsStruct.shouldManage &&
            useSettingsStore().network.canManage &&
            !useSettingsStore().network.networkingDisabled
          "
          rounded
          color="error"
          text-color="white"
          icon="mdi-information-outline"
        >
          Photon cannot detect any wired connections! Please send program logs to the developers for help.
        </v-banner>
        <pv-switch
          v-model="tempSettingsStruct.runNTServer"
          label="Run NetworkTables Server (Debugging Only)"
          tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
          :label-cols="4"
        />
        <v-banner
          v-if="tempSettingsStruct.runNTServer"
          rounded
          color="error"
          text-color="white"
          icon="mdi-information-outline"
        >
          This mode is intended for debugging; it should be off for proper usage. PhotonLib will NOT work!
        </v-banner>
        <v-divider class="mt-3 pb-3" />
        <v-card-title class="pl-0 pt-3 pb-3">Miscellaneous</v-card-title>
        <pv-switch
          v-model="tempSettingsStruct.shouldPublishProto"
          label="Also Publish Protobuf"
          tooltip="If enabled, Photon will publish all pipeline results in both the Packet and Protobuf formats. This is useful for visualizing pipeline results from NT viewers such as glass and logging software such as AdvantageScope. Note: photon-lib will ignore this value and is not recommended on the field for performance."
          :label-cols="4"
        />
        <v-banner
          v-if="tempSettingsStruct.shouldPublishProto"
          rounded
          color="error"
          text-color="white"
          icon="mdi-information-outline"
        >
          This mode is intended for debugging; it should be off for field use. You may notice a performance hit by using
          this mode.
        </v-banner>
        <v-divider class="mt-3 mb-6" />
      </v-form>
      <v-btn
        color="accent"
        style="color: black; width: 100%"
        :disabled="!settingsValid || !settingsHaveChanged()"
        @click="saveGeneralSettings"
      >
        Save
      </v-btn>
    </div>
  </v-card>
</template>

<style>
.v-banner__wrapper {
  padding: 6px !important;
}
</style>
