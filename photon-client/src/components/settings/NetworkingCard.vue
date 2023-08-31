<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { ref } from "vue";
import CvInput from "@/components/common/cv-input.vue";
import CvRadio from "@/components/common/cv-radio.vue";
import CvSwitch from "@/components/common/cv-switch.vue";
import { NetworkConnectionType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

const settingsValid = ref(true);
const isValidNetworkTablesIP = (v: string | undefined): boolean => {
  // Check if it is a valid team number between 1-9999
  const teamNumberRegex = /^[1-9][0-9]{0,3}$/;
  // Check if it is a team number longer than 5 digits
  const badTeamNumberRegex = /^[0-9]{5,}$/;

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

const saveGeneralSettings = () => {
  const changingStaticIp = useSettingsStore().network.connectionType === NetworkConnectionType.Static;

  useSettingsStore()
    .saveGeneralSettings()
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
    })
    .catch((error) => {
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
</script>

<template>
  <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
    <v-card-title>Networking</v-card-title>
    <div class="ml-5">
      <v-form ref="form" v-model="settingsValid">
        <cv-input
          v-model="useSettingsStore().network.ntServerAddress"
          label="Team Number/NetworkTables Server Address"
          tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
          :label-cols="3"
          :disabled="useSettingsStore().network.runNTServer"
          :rules="[
            (v) =>
              isValidNetworkTablesIP(v) ||
              'The NetworkTables Server Address must be a valid Team Number, IP address, or Hostname'
          ]"
        />
        <v-banner
          v-show="
            !isValidNetworkTablesIP(useSettingsStore().network.ntServerAddress) &&
            !useSettingsStore().network.runNTServer
          "
          rounded
          color="red"
          text-color="white"
          style="margin: 10px 0"
          icon="mdi-alert-circle-outline"
        >
          The NetworkTables Server Address is not set or is invalid. NetworkTables is unable to connect.
        </v-banner>
        <cv-radio
          v-show="useSettingsStore().network.shouldManage"
          v-model="useSettingsStore().network.connectionType"
          label="IP Assignment Mode"
          tooltip="DHCP will make the radio (router) automatically assign an IP address; this may result in an IP address that changes across reboots. Static IP assignment means that you pick the IP address and it won't change."
          :input-cols="12 - 3"
          :list="['DHCP', 'Static']"
        />
        <cv-input
          v-if="useSettingsStore().network.connectionType === NetworkConnectionType.Static"
          v-model="useSettingsStore().network.staticIp"
          :input-cols="12 - 3"
          label="Static IP"
          :rules="[(v) => isValidIPv4(v) || 'Invalid IPv4 address']"
        />
        <cv-input
          v-show="useSettingsStore().network.shouldManage"
          v-model="useSettingsStore().network.hostname"
          label="Hostname"
          :input-cols="12 - 3"
          :rules="[(v) => isValidHostname(v) || 'Invalid hostname']"
        />
        <cv-switch
          v-model="useSettingsStore().network.runNTServer"
          label="Run NetworkTables Server (Debugging Only)"
          tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
          class="mt-3 mb-3"
          :label-cols="3"
        />
        <v-banner
          v-show="useSettingsStore().network.runNTServer"
          rounded
          color="red"
          text-color="white"
          icon="mdi-information-outline"
        >
          This mode is intended for debugging; it should be off for proper usage. PhotonLib will NOT work!
        </v-banner>
      </v-form>
      <v-btn
        color="accent"
        :class="useSettingsStore().network.runNTServer ? 'mt-3' : ''"
        style="color: black; width: 100%"
        :disabled="!settingsValid && !useSettingsStore().network.runNTServer"
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
