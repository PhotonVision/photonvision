<script setup lang="ts">
import { computed } from "vue";
import { axiosPost } from "@/lib/PhotonUtils";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useReplayStatus } from "@/composables/useReplayStatus";

const { active } = useReplayStatus();
const cameraStore = useCameraSettingsStore();

interface BannerRow {
  cameraUniqueName: string;
  nickname: string;
  recordingName: string;
  currentFrame: number;
  totalFrames: number;
  progress: number;
}

const ROW_HEIGHT_PX = 64;

const rows = computed<BannerRow[]>(() =>
  active.value.map((entry) => {
    const camera = cameraStore.cameras[entry.cameraUniqueName];
    const total = entry.totalFrames > 0 ? entry.totalFrames : 1;
    return {
      cameraUniqueName: entry.cameraUniqueName,
      nickname: camera?.nickname ?? entry.cameraUniqueName,
      recordingName: entry.recordingName,
      currentFrame: entry.currentFrame,
      totalFrames: entry.totalFrames,
      progress: Math.min(100, Math.round((entry.currentFrame / total) * 100))
    };
  })
);

const cancelReplay = (cameraUniqueName: string) => {
  void axiosPost("/recordings/replay/cancel", "cancel replay on " + cameraUniqueName, { cameraUniqueName });
};
</script>

<template>
  <v-app-bar
    v-if="rows.length > 0"
    location="top"
    :height="rows.length * ROW_HEIGHT_PX"
    color="info"
    flat
    role="status"
    aria-live="polite"
  >
    <div class="replay-banner-stack">
      <div v-for="row in rows" :key="row.cameraUniqueName" class="replay-banner-row">
        <div class="replay-banner-content">
          <div class="replay-banner-text">
            <strong>Replaying</strong>
            <span class="replay-banner-recording">{{ row.recordingName }}</span>
            <span>on</span>
            <strong>{{ row.nickname }}</strong>
            <span class="replay-banner-counter">— frame {{ row.currentFrame }} / {{ row.totalFrames }}</span>
          </div>
          <v-btn size="small" color="error" variant="flat" @click="cancelReplay(row.cameraUniqueName)">
            <v-icon start>mdi-stop</v-icon>
            Cancel
          </v-btn>
        </div>
        <v-progress-linear :model-value="row.progress" color="white" bg-color="rgba(255, 255, 255, 0.2)" height="3" />
      </div>
    </div>
  </v-app-bar>
</template>

<style scoped>
.replay-banner-stack {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.replay-banner-row {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.replay-banner-row:last-child {
  border-bottom: none;
}

.replay-banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.replay-banner-text {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  align-items: baseline;
}

.replay-banner-recording {
  font-family: monospace;
}

.replay-banner-counter {
  opacity: 0.85;
}
</style>
