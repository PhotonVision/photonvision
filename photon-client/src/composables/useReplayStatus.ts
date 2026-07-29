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

// At the default 250ms poll interval this is ~5s of unreachable backend before
// the last-known replay state is considered stale and cleared.
const MAX_CONSECUTIVE_FAILURES = 20;

const active = ref<ActiveReplay[]>([]);

let backendHost: string | undefined;
let subscriberCount = 0;
let timer: number | undefined;
let consecutiveFailures = 0;

const poll = async () => {
  try {
    const res = await fetch(`http://${backendHost}/api/recordings/replay/status`, { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    active.value = (await res.json()) as ActiveReplay[];
    consecutiveFailures = 0;
  } catch {
    // Network blip; keep the last value rather than flapping the banner — but
    // if the backend stays unreachable, stop reporting a phantom replay.
    consecutiveFailures++;
    if (consecutiveFailures === MAX_CONSECUTIVE_FAILURES) {
      active.value = [];
    }
  }
};

// Must be called from a component setup() — uses inject() and onUnmounted().
// State and polling are shared across all subscribers: one interval runs while
// at least one subscribing component is mounted.
export function useReplayStatus(pollIntervalMs = 250) {
  const address = inject<string>("backendHost");

  if (!address) {
    return { active };
  }
  backendHost = address;

  subscriberCount++;
  if (subscriberCount === 1) {
    timer = window.setInterval(() => {
      void poll();
    }, pollIntervalMs);
    void poll();
  }

  onUnmounted(() => {
    subscriberCount--;
    if (subscriberCount === 0 && timer !== undefined) {
      window.clearInterval(timer);
      timer = undefined;
    }
  });

  return { active };
}
