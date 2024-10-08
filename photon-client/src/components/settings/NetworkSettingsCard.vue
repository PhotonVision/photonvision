<script setup lang="ts">
import { NetworkConnectionType, NetworkSettings } from "@/types/SettingTypes";
import { useServerStore } from "@/stores/ServerStore";
import { computed, ref, watch } from "vue";
import _ from "lodash";
import PvRadio from "@/components/common/pv-radio.vue";
import PvTextbox from "@/components/common/pv-textbox.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";

const serverStore = useServerStore();

const networkSettingsBuffer = ref<NetworkSettings>();

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

const settingsHaveChanged = computed<boolean>(() => !_.isEqual(networkSettingsBuffer.value, serverStore.settings!.network));

const saveChanges = () => {
  if (!settingsValid.value || !networkSettingsBuffer.value) return;

  serverStore.updateNetworkSettings(networkSettingsBuffer.value);
};

watch(() => serverStore.settings?.network, (newVal) => {
  networkSettingsBuffer.value = newVal;
}, { deep: true });
</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">Network Settings</v-card-title>
    <v-card-text v-if="!networkSettingsBuffer">No Network Settings Found</v-card-text>
    <v-form v-else v-model="settingsValid" class="pl-4 pr-4" @submit.prevent="saveChanges">
      <pv-textbox
        v-model="networkSettingsBuffer.ntServerAddress"
        :disabled="networkSettingsBuffer.runNTServer"
        label="Team Number/NetworkTables Server Address"
        :label-cols="4"
        :rules="[
          (v: string) =>
            isValidNetworkTablesIP(v) ||
            'The NetworkTables Server Address must be a valid Team Number, IP address, or Hostname'
        ]"
        tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
      />
      <pv-radio
        v-if="serverStore.settings?.network.networkingDisabled"
        v-model="networkSettingsBuffer.connectionType"
        :disabled="
          !networkSettingsBuffer.shouldManage ||
            !serverStore.settings?.network.canManage ||
            serverStore.settings?.network.networkingDisabled
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
          networkSettingsBuffer.connectionType === NetworkConnectionType.Static &&
            !serverStore.settings?.network.networkingDisabled
        "
        v-model="networkSettingsBuffer.staticIp"
        :disabled="
          !networkSettingsBuffer.shouldManage ||
            !serverStore.settings?.network.canManage ||
            serverStore.settings?.network.networkingDisabled
        "
        label="Static IP Address"
        :label-cols="4"
        :rules="[(v: string) => isValidIPv4(v) || 'Invalid IPv4 address']"
      />
      <pv-textbox
        v-if="!serverStore.settings?.network.networkingDisabled"
        v-model="networkSettingsBuffer.hostname"
        :disabled="
          !networkSettingsBuffer.shouldManage ||
            !serverStore.settings?.network.canManage ||
            serverStore.settings?.network.networkingDisabled
        "
        label="Hostname"
        :label-cols="4"
        :rules="[(v) => isValidHostname(v) || 'Invalid hostname']"
      />
      <v-expansion-panels v-model="advancedNetworkSettingsOpen">
        <v-expansion-panel title="Advanced Networking">
          <v-expansion-panel-text eager>
            <pv-switch
              v-if="!serverStore.settings?.network.networkingDisabled"
              v-model="networkSettingsBuffer.shouldManage"
              :disabled="!serverStore.settings?.network.canManage || serverStore.settings?.network.networkingDisabled"
              label="Manage Device Networking"
              :label-cols="4"
              tooltip="If enabled, Photon will manage device hostname and network settings."
            />
            <pv-dropdown
              v-if="!serverStore.settings?.network.networkingDisabled"
              v-model="networkSettingsBuffer.networkManagerIface"
              :disabled="
                !networkSettingsBuffer.shouldManage ||
                  !serverStore.settings?.network.canManage ||
                  serverStore.settings?.network.networkingDisabled
              "
              :items="serverStore.networkInterfaceNames.map((v: string) => ({ name: v, value: v }))"
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
                !serverStore.networkInterfaceNames.length &&
                  networkSettingsBuffer.shouldManage &&
                  serverStore.settings?.network.canManage &&
                  !serverStore.settings?.network.networkingDisabled
              "
              density="compact"
              rounded
              text="Photon cannot detect any wired connections! Please send program logs to the developers for help."
              type="error"
            />
            <pv-switch
              v-model="networkSettingsBuffer.runNTServer"
              class="mt-3 mb-2"
              label="Run NetworkTables Server (Debugging Only)"
              :label-cols="4"
              tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
            />
            <v-alert
              v-show="networkSettingsBuffer.runNTServer"
              density="compact"
              rounded
              text="This mode is intended for debugging; it should be off for proper usage. PhotonLib will NOT work!"
              type="info"
            />
            <pv-switch
              v-model="networkSettingsBuffer.shouldPublishProto"
              class="mt-3 mb-2"
              label="Also Publish Protobuf"
              :label-cols="4"
              tooltip="If enabled, Photon will publish all pipeline results in both the Packet and Protobuf formats. This is useful for visualizing pipeline results from NT viewers such as glass and logging software such as AdvantageScope. Note: photon-lib will ignore this value and is not recommended on the field for performance."
            />
            <v-alert
              v-show="networkSettingsBuffer.shouldPublishProto"
              density="compact"
              rounded
              text="This mode is intended for debugging; it should be off for field use. You may notice a performance hit by using this mode."
              type="info"
            />
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
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
