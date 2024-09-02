<script setup lang="ts">
import { computed, ref } from "vue";
import { NetworkConnectionType, NetworkSettings } from "@/types/SettingTypes";
import _ from "lodash";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import PvTextbox from "@/components/common/pv-textbox.vue";
import PvRadio from "@/components/common/pv-radio.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";

const settingsBuffer = ref<NetworkSettings>(Object.assign({}, useSettingsStore().network));
const settingsValid = ref<boolean | null>(true);
const advancedNetworkSettingsOpen = ref<number | undefined>();

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

const settingsHaveChanged = computed<boolean>(() => !_.isEqual(settingsBuffer.value, useSettingsStore().network));

const saveChanges = () => {
  if (!settingsValid.value) return;

  // replace undefined members with empty strings for backend
  const payload = {
    connectionType: settingsBuffer.value.connectionType,
    hostname: settingsBuffer.value.hostname,
    networkManagerIface: settingsBuffer.value.networkManagerIface || "",
    ntServerAddress: settingsBuffer.value.ntServerAddress,
    runNTServer: settingsBuffer.value.runNTServer,
    setDHCPcommand: settingsBuffer.value.setDHCPcommand || "",
    setStaticCommand: settingsBuffer.value.setStaticCommand || "",
    shouldManage: settingsBuffer.value.shouldManage,
    shouldPublishProto: settingsBuffer.value.shouldPublishProto,
    matchCamerasOnlyByPath: settingsBuffer.value.matchCamerasOnlyByPath,
    staticIp: settingsBuffer.value.staticIp
  };

  useSettingsStore()
    .updateGeneralSettings(payload)
    .then((response) => {
      // TODO
      // useStateStore().showSnackbarMessage({
      //   message: response.data.text || response.data,
      //   color: "success"
      // });

      if (
        settingsBuffer.value.connectionType === NetworkConnectionType.Static &&
        settingsBuffer.value.staticIp !== useSettingsStore().network.staticIp
      ) {
        const newIpAddress = settingsBuffer.value.staticIp;

        // TODO snackbar message about changing IP address

        setTimeout(() => {
          window.open(`http://${newIpAddress}:${window.location.port}/`);
        }, 2000);
      } else {
        // Update the local settings cause the backend checked their validity. Assign is to deref value
        useSettingsStore().network = {
          ...useSettingsStore().network,
          ...Object.assign({}, settingsBuffer.value)
        };
        settingsBuffer.value = Object.assign({}, useSettingsStore().network);
      }
    })
    .catch((error) => {
      // TODO
      // resetTempSettingsStruct();
      // if (error.response) {
      //   if (error.status === 504 || changingStaticIp) {
      //     useStateStore().showSnackbarMessage({
      //       color: "error",
      //       message: `Connection lost! Try the new static IP at ${useSettingsStore().network.staticIp}:5800 or ${
      //         useSettingsStore().network.hostname
      //       }:5800?`
      //     });
      //   } else {
      //     useStateStore().showSnackbarMessage({
      //       color: "error",
      //       message: error.response.data.text || error.response.data
      //     });
      //   }
      // } else if (error.request) {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "Error while trying to process the request! The backend didn't respond."
      //   });
      // } else {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "An error occurred while trying to process the request."
      //   });
      // }
    });
};
</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">General Settings</v-card-title>
    <v-form v-model="settingsValid" class="pl-4 pr-4" @submit.prevent="saveChanges">
      <div class="mb-2">Networking</div>
      <pv-textbox
        v-model="settingsBuffer.ntServerAddress"
        :disabled="settingsBuffer.runNTServer"
        label="Team Number/NetworkTables Server Address"
        :label-cols="4"
        :rules="[
          (v) =>
            isValidNetworkTablesIP(v) ||
            'The NetworkTables Server Address must be a valid Team Number, IP address, or Hostname'
        ]"
        tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
      />
      <pv-radio
        v-if="!useSettingsStore().network.networkingDisabled"
        v-model="settingsBuffer.connectionType"
        :disabled="
          !settingsBuffer.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
        "
        inline
        :items="[
          {
            name: 'DHCP',
            value: NetworkConnectionType.DHCP,
            tooltip:
              'Make the radio (router) automatically assign an IP address; this may result in an IP address that changes across reboots so isn\'t recommended for competition use.'
          },
          {
            name: 'Static',
            value: NetworkConnectionType.Static,
            tooltip: 'Manually assigns a fixed IP address to a device. Recommended for competition use.'
          }
        ]"
        label="IP Assignment Mode"
        :label-cols="4"
      />
      <pv-textbox
        v-if="
          settingsBuffer.connectionType === NetworkConnectionType.Static &&
            !useSettingsStore().network.networkingDisabled
        "
        v-model="settingsBuffer.staticIp"
        :disabled="
          !settingsBuffer.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
        "
        label="Static IP Address"
        :label-cols="4"
        :rules="[(v) => isValidIPv4(v) || 'Invalid IPv4 address']"
      />
      <pv-textbox
        v-if="!useSettingsStore().network.networkingDisabled"
        v-model="settingsBuffer.hostname"
        :disabled="
          !settingsBuffer.shouldManage ||
            !useSettingsStore().network.canManage ||
            useSettingsStore().network.networkingDisabled
        "
        label="Hostname"
        :label-cols="4"
        :rules="[(v) => isValidHostname(v) || 'Invalid hostname']"
      />
      <v-expansion-panels v-model="advancedNetworkSettingsOpen">
        <v-expansion-panel title="Advanced Networking">
          <v-expansion-panel-text eager>
            <pv-switch
              v-if="!useSettingsStore().network.networkingDisabled"
              v-model="settingsBuffer.shouldManage"
              :disabled="!useSettingsStore().network.canManage || useSettingsStore().network.networkingDisabled"
              label="Manage Device Networking"
              :label-cols="4"
              tooltip="If enabled, Photon will manage device hostname and network settings."
            />
            <pv-dropdown
              v-if="!useSettingsStore().network.networkingDisabled"
              v-model="settingsBuffer.networkManagerIface"
              :disabled="
                !settingsBuffer.shouldManage ||
                  !useSettingsStore().network.canManage ||
                  useSettingsStore().network.networkingDisabled
              "
              :items="useSettingsStore().networkInterfaceNames.map((v) => ({ name: v, value: v }))"
              label="NetworkManager Interface"
              :label-cols="4"
              :rules="[
                (v) => {
                  if (v === undefined || v === null) {
                    advancedNetworkSettingsOpen = 0;
                    return 'Select a NetworkInterface';
                  }
                  return true;
                }
              ]"
              tooltip="Name of the interface PhotonVision should manage the IP address of"
            />
            <v-alert
              v-show="
                !useSettingsStore().networkInterfaceNames.length &&
                  settingsBuffer.shouldManage &&
                  useSettingsStore().network.canManage &&
                  !useSettingsStore().network.networkingDisabled
              "
              density="compact"
              rounded
              text="Photon cannot detect any wired connections! Please send program logs to the developers for help."
              type="error"
            />
            <pv-switch
              v-model="settingsBuffer.runNTServer"
              class="mt-3 mb-2"
              label="Run NetworkTables Server (Debugging Only)"
              :label-cols="4"
              tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
            />
            <v-alert
              v-show="settingsBuffer.runNTServer"
              density="compact"
              rounded
              text="This mode is intended for debugging; it should be off for proper usage. PhotonLib will NOT work!"
              type="info"
            />
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-divider class="mt-3 mb-3" />

      <div class="mb-2">Miscellaneous</div>
      <pv-switch
        v-model="settingsBuffer.shouldPublishProto"
        class="mt-3 mb-2"
        label="Also Publish Protobuf"
        :label-cols="4"
        tooltip="If enabled, Photon will publish all pipeline results in both the Packet and Protobuf formats. This is useful for visualizing pipeline results from NT viewers such as glass and logging software such as AdvantageScope. Note: photon-lib will ignore this value and is not recommended on the field for performance."
      />
      <v-alert
        v-show="settingsBuffer.shouldPublishProto"
        density="compact"
        rounded
        text="This mode is intended for debugging; it should be off for field use. You may notice a performance hit by using this mode."
        type="info"
      />
      <pv-switch
        v-model="settingsBuffer.matchCamerasOnlyByPath"
        label="Strictly match ONLY known cameras"
        :label-cols="4"
        tooltip="ONLY match cameras by the USB port they're plugged into + (basename or USB VID/PID), and never only by the device product string. Also disables automatic detection of new cameras."
      />
      <v-alert v-show="settingsBuffer.matchCamerasOnlyByPath" density="compact" rounded type="info">
        <ul>
          <li>
            Physical cameras will be strictly matched to camera configurations using physical USB port they are plugged
            into, in addition to device name and other USB metadata. Additionally, no new cameras are allowed to be
            added. This setting is useful for guaranteeing that an already known and configured camera can never be
            matched as an "unknown"/"new" camera, which resets pipelines and calibration data.
          </li>
          <li>
            Cameras will NOT be matched if they change USB ports, and new cameras plugged into this coprocessor will NOT
            be automatically recognized or configured for vision processing.
          </li>
          <li>To add a new camera to this coprocessor, disable this setting, connect the camera, and re-enable.</li>
        </ul>
      </v-alert>
      <v-card-actions class="pl-0 pr-0">
        <v-btn
          class="w-100"
          color="accent"
          :disabled="!settingsHaveChanged"
          text="Save"
          type="submit"
          variant="elevated"
        />
      </v-card-actions>
    </v-form>
  </v-card>
</template>
