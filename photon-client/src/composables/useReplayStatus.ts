import { inject, onUnmounted, ref } from "vue";

// Mirrors VisionModule.ReplayStatus on the server. currentFrame / totalFrames
// are Java `long`s on the wire; safe to treat as `number` here — a recording
// would need >2^53 frames (~285 million years at 1000 fps) to overflow.
export interface ActiveReplay {
  cameraUniqueName: string;
  recordingName: string;
  currentFrame: number;
  totalFrames: number;
}

// Must be called from a component setup() — uses inject() and onUnmounted().
export function useReplayStatus(pollIntervalMs = 250) {
  const address = inject<string>("backendHost");
  const active = ref<ActiveReplay[]>([]);

  if (!address) {
    return { active };
  }

  const poll = async () => {
    try {
      const res = await fetch(`http://${address}/api/recordings/replay/status`, { cache: "no-store" });
      if (res.ok) {
        active.value = (await res.json()) as ActiveReplay[];
      }
    } catch {
      // Network blip; keep the last value rather than flapping the banner.
    }
  };

  const timer = window.setInterval(() => {
    void poll();
  }, pollIntervalMs);
  void poll();
  onUnmounted(() => window.clearInterval(timer));

  return { active };
}
