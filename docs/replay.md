# Recording, Replay, and AKit JSON Export

End-to-end developer reference for PhotonVision's recording and replay feature
and the JSON-export hand-off into AdvantageKit. Three things ship together:

1. **`FrameRecorder`** writes a per-camera-per-session recording to disk
   (JPEG image sequence + a `metadata.jsonl` sidecar of `(seq, capture_ns)`).
2. **`FileLogFrameProvider`** replays one of those recordings back through a
   `VisionModule` at original wall-clock pace.
3. **`JsonResultExporter`** tees the replay's pipeline results to a
   per-pipeline-hash `.jsonl` file so a robot project running AdvantageKit can
   feed re-tuned vision results into AKit's replay session alongside the
   match's original wpilog.

## Flow

```
match day                             dev box                           dev laptop
┌────────────────────┐        ┌──────────────────────────┐       ┌──────────────────────┐
│ coprocessor cam    │ record │ <recordingDir>/          │ ───►  │ replay PV against    │
│ pipeline running   │ ─────► │   frames/000000.jpg ...  │       │ recording, dump JSON │
│ NTDataPublisher    │        │   metadata.jsonl         │       │                      │
│ pushing results    │        │   tss.json               │       │ ┌──────────────────┐ │
│ on /photonvision/* │        │   strat                  │       │ │ results/<hash>   │ │
└────────────────────┘        └──────────────────────────┘       │ │   .jsonl per     │ │
        ▲                                  │                     │ │   tuning attempt │ │
        │ wpilog captures vision typed     │                     │ └──────────────────┘ │
        │ inputs via VisionIOPhoton         │                     │                      │
        ▼                                  ▼                     ▼
┌────────────────────┐               ┌──────────────────────────────────────────────┐
│ match wpilog       │ ───────────►  │ AKit replay session on dev laptop:           │
│ (joysticks, motors,│   feed both   │  - VisionIOReplay reads typed entries        │
│  sensors, vision)  │               │       from the wpilog (legacy path)          │
└────────────────────┘               │  - VisionIOJsonReplay reads our JSON         │
                                     │       and overrides vision inputs for the    │
                                     │       tuning under test                      │
                                     └──────────────────────────────────────────────┘
```

## Recording side

A recording is triggered when the per-camera NT boolean
`/photonvision/<nickname>/recordingRequest` flips true. `NTDataPublisher`
subscribes and calls `FrameRecorder.startRecording`. Each frame the source
captures is queued; a writer thread encodes each as a JPEG and appends a line
to `metadata.jsonl`.

### What gets written

```
<recordingsDir>/<camera-unique-name>/<recording-name>/
├── frames/
│   ├── 000000.jpg
│   ├── 000001.jpg
│   └── ...
├── metadata.jsonl              # {"seq":N,"capture_ns":T} per frame
├── tss.json                    # one-shot TSS snapshot (see below)
└── strat                       # marker for offline tooling
```

### `capture_ns` is local, not TSS-aligned

`capture_ns` is the coprocessor's `wpi::nt::Now()` epoch in nanoseconds, taken
straight from `cvSink.grabFrame()` (see `USBFrameProvider`). It is **not**
shifted into the Time Sync Server (= robot) time base — that shift only
happens at publish-time in `NTDataPublisher`.

`tss.json` records the TSS offset that was live at recording start so the
exporter (and any other consumer) can later add the same shift:

```json
{
  "tss_active_at_record": true,
  "tss_offset_at_record_ns": 12345000,
  "sampled_at_wpi_nt_now_ns": 1778580000000000000
}
```

A recording made when TSS is down still works for raw playback; the AKit
hand-off below refuses to load it because there's no way to align with a match
wpilog.

## Replay side

Replay is an **action on a live camera**, not a sibling assignment. The user
picks a recording on the Cameras tab (or the Recordings card), hits **Replay
with current pipeline**, and the live `VisionModule`'s `FrameProvider` is
swapped in place — `USBFrameProvider` / `LibcameraGpuFrameProvider` →
`FileLogFrameProvider` — for the duration of the replay. The pipeline,
calibration, and `NTDataPublisher` stay the same; only the frame source
changes. On EOF or cancel, the live provider is restored automatically.

The swap is owned by `VisionModule.startReplay(Path)` / `cancelReplay()`:

1. Open a `FileLogFrameProvider` against the recording dir. The provider
   pre-counts `frames/*.jpg` for progress reporting and lazily reads the
   metadata sidecar.
2. Wire `onProgress` (publishes `/photonvision/<cam>/replayProgress*` after
   every emitted frame) and `onEof` (dispatches the swap-back) **before**
   installing the provider on the source — a zero-frame recording fires EOF
   on the very first `getInputMat` call.
3. Save the live provider, atomically swap the source's volatile
   `FrameProvider` reference, set `isReplaying = true`, publish
   `/photonvision/<cam>/isReplaying`.
4. `VisionRunner` re-reads `frameSupplier.get()` once per loop tick, so it
   picks up the new provider on the next frame grab without restarting the
   thread.

On EOF or `cancelReplay`:

1. `FileLogFrameProvider.requestStop()` (or the natural-EOF path) fires
   `onEof` exactly once via a CAS on the stopped-state flag, then returns an
   empty `CapturedFrame` so the vision thread can pick up the swap-back on
   its next tick (instead of parking — that's the standalone-use semantics
   from before the swap).
2. The EOF callback dispatches `finishReplay` onto a dedicated
   single-thread executor (`ReplayWorker - <camera>`), never the vision
   thread.
3. `finishReplay` restores the live provider, then fences via
   `VisionRunner.runSynchronously(() -> {})` — the task drains on the
   vision thread at the top of its loop, *after* the supplier re-read, so
   when the future resolves the vision thread is no longer inside the old
   provider's `getInputMat`. The file-log provider is then released and the
   `JsonResultExporter` closed.
4. `isReplaying` flips false and `/photonvision/<cam>/isReplaying` is set
   to `false`.

`stop()` on the `VisionModule` waits up to 3s for an in-flight swap-back
before tearing the runner down, so a parallel stop-during-cancel can't race
teardown against the worker.

### REST endpoints

Two endpoints under `/api/recordings/` drive the state machine from the UI:

```http
POST /api/recordings/replay
Content-Type: application/json

{"cameraUniqueName": "<uniqueName>", "recording": "<recordingName>"}
```

Resolves `<recordingsDir>/<cameraUniqueName>/<recording>` via
`PathSafety.safeResolve`, validates the dir, calls `module.startReplay`.
Returns:

| Status | Meaning |
|---|---|
| 202 | replay accepted, running asynchronously |
| 400 | missing fields, malformed recording, unsafe path |
| 404 | no camera with that uniqueName, or recording not found |
| 409 | a replay is already active on that camera |

```http
POST /api/recordings/replay/cancel
Content-Type: application/json

{"cameraUniqueName": "<uniqueName>"}
```

Idempotent — 200 even if there was no replay to cancel.

### NT signals during replay

Per-camera under `/photonvision/<cameraNickname>/`:

| Topic | Type | Notes |
|---|---|---|
| `isReplaying` | boolean | flips true on startReplay, false on swap-back complete |
| `replayProgressCurrentFrame` | int64 | running 1-based emit count |
| `replayProgressTotalFrames` | int64 | discovered at construction; -1 if listing failed |
| `replayProgressRecordingName` | string | recording dir name |

Per-result topics (`result`, `targetPose`, `latencyMillis`, …) keep
publishing during replay — values reflect the replayed frames. Acceptable
because replay is a post-match / off-line analysis tool; no robot is
consuming NT then, and the JSON file is the canonical output.

### UI surfaces

- **Cameras tab → ReplayPanel** ([CameraSettingsView.vue](../photon-client/src/views/CameraSettingsView.vue)): per-camera recording dropdown (filtered to the current camera, newest-first default), Replay / Cancel / Download buttons. Sits between `CalibrationCard` and `CameraControlCard` so the iteration loop is tune → pick recording → Replay → Download.
- **Settings → Recordings card** ([RecordingsCard.vue](../photon-client/src/components/settings/RecordingsCard.vue)): per-row "▶ Replay Selected" button alongside Export / Delete. Same endpoint, target camera inferred from the row.

The cross-camera "replay in progress" banner and the results-list summary
(parsed `<recording>/results/*.jsonl` headers) are intentional follow-ups —
they need an NT→websocket bridge and a new server endpoint respectively.

### Live camera goes offline during replay — by design

Replay is exclusive: while it's running, `VisionRunner` is pulling from the
file-log provider, not the live camera. The live `CvSink` (or libcamera
pipe) stays open, so the swap-back is instant — but no live frames flow.
That's acceptable because replay is post-match / off-line; a competition
robot consuming NT during a match should never see `isReplaying = true`.
If a separate process *does* need to keep the live camera up during
replay, the fallback design is an "ephemeral parallel VisionModule"
that reuses the live module's pipeline + calibration *by reference* — not
done here because the user explicitly opted for the simpler in-place swap.

## JSON export

During replay, `VisionModule` tees every `CVPipelineResult` to a
`JsonResultExporter`. The tee gates on
`visionSource.getReplayRecordingDir().isPresent()` — non-empty iff the
current `FrameProvider` is a `FileLogFrameProvider`. The exporter is built
lazily on the first replayed result and closed by `finishReplay` on
swap-back, so live USB / CSI cameras never touch the disk for this path
and the next replay (potentially with a different pipeline hash) lands in a
fresh file.

### Where the file goes

```
<recordingDir>/results/<pipeline-hash>.jsonl
```

`<pipeline-hash>` is `Integer.toHexString(settings.hashCode())`. The
`hashCode` impl on `CVPipelineSettings` and its subclasses uses `Objects.hash`
over the settings fields, so it's deterministic across JVM runs — re-replay
with the same settings always lands in the same file; change one knob and
you land in a new file automatically.

The file is opened with `TRUNCATE_EXISTING`, so each replay session
overwrites whatever the previous run produced under that hash. To preserve
multiple tunings, change a setting between runs.

### File schema

One header line, then one line per pipeline-emitted result. Header:

```json
{
  "schema_version": 1,
  "camera_unique_name": "test-cam",
  "recording_name": "synthetic-apriltag-1778581416264",
  "pipeline_type": "AprilTag",
  "pipeline_hash": "1a2b3c4d",
  "tss_active_at_record": true,
  "tss_offset_at_record_ns": 12345000
}
```

Result lines:

```json
{"capture_ns": 1778580000033333333, "seq": 0, "packet_b64": "AAQAAAA..."}
{"capture_ns": 1778580000066666666, "seq": 1, "packet_b64": "AAQAAAA..."}
```

- **`capture_ns`** mirrors the recording's `metadata.jsonl` value verbatim
  (local-time-base). Apply `tss_offset_at_record_ns` if you want TSS time.
- **`seq`** echoes the recording's frame sequence number.
- **`packet_b64`** is `Base64.getEncoder().encodeToString(Packet.getWrittenDataCopy())`
  after `PhotonPipelineResult.photonStruct.pack(packet, result)`. The
  embedded `PhotonPipelineResult` has its `captureTimestampMicros` and
  `publishTimestampMicros` *already* shifted by the recorded offset, so a
  consumer can deserialize and use it interchangeably with a live NT
  publish.

### Header fields that are intentionally absent

- **`exported_at_ns`** — filesystem mtime serves the same purpose.
- **`schema_note`** — `schema_version` gates compatibility; freeform text
  rots faster than the code that referenced it.
- **`calibration_resolution`** — calibration belongs in the consumer's
  calibration JSON, not per-recording metadata. Revisit if calibration import
  ever gets wired into recordings.

## AKit consumption (robot side)

The robot-side reader is **not** shipped from PhotonVision — it lives in the
team's robot project. Reference shape:

```java
public class VisionIOJsonReplay implements VisionIO {
    private final List<JsonEntry> entries;   // sorted by capture_ns
    private final long tssOffsetNs;          // from header
    private int cursor = 0;

    public VisionIOJsonReplay(Path jsonl) throws IOException {
        // 1. Parse header line. If tss_active_at_record is false or null, refuse:
        //    the JSON capture_ns can't be aligned with an AKit wpilog timeline.
        // 2. Parse data lines into entries sorted by capture_ns.
    }

    @Override
    public void updateInputs(VisionIOInputs inputs) {
        // AKit replay drives Timer.getFPGATimestamp() from the wpilog. On the
        // original robot, FPGA time == wpi::nt::Now == TSS server time, so
        // capture_ns + tssOffsetNs lands in the same basis.
        long nowNs = (long)(Timer.getFPGATimestamp() * 1_000_000_000.0);

        List<PhotonPipelineResult> newResults = new ArrayList<>();
        while (cursor < entries.size()
                && entries.get(cursor).captureNsTssAligned() <= nowNs) {
            byte[] bytes = Base64.getDecoder().decode(entries.get(cursor).packetB64());
            newResults.add(PhotonPipelineResult.photonStruct.unpack(new Packet(bytes)));
            cursor++;
        }
        inputs.results = newResults.toArray(PhotonPipelineResult[]::new);
    }

    record JsonEntry(long captureNs, long seq, String packetB64) {
        long captureNsTssAligned() { /* captureNs + tssOffsetNs */ }
    }
}
```

### Sync with the wpilog

AKit's `LogReplayer` reads a wpilog and feeds it back through the robot loop.
Each IO subsystem's `updateInputs` is called once per tick; AKit makes the
recorded `Timer.getFPGATimestamp()` available via the wpilog's timestamp
column.

So sync is automatic:

- AKit's replayed FPGA clock drives `updateInputs(...)`.
- We advance our cursor past every entry whose **TSS-aligned** capture time
  is `<= now`.
- All entries past the cursor get emitted into `inputs.results`. If the wpilog
  is sampled at 50 Hz and the camera ran at 30 Hz, some ticks emit one entry,
  others zero, others two — same shape as the live NT subscriber.

### Why we don't just rewrite the wpilog

A previously-considered design merged re-tuned vision results back into the
match wpilog (strip the old vision entries, write new ones at the same
timestamps). Two reasons we didn't:

1. wpilog entry IDs and schema records would need to be remapped — non-trivial.
2. The team's `VisionIOPhoton` writes *typed* `@AutoLog` inputs into the
   wpilog, not raw NT topics, so regenerating them robot-side would need a
   harness that knows the team's IO layout. The JSON path keeps the contract
   at PhotonLib's wire format (which both sides already share) and pushes
   the typed-input regeneration into the team's `VisionIOJsonReplay`.

## Operational notes

### Where does a recording come from?

- **Match day**: a competition robot triggers `recordingRequest` from auto/
  teleop init. Memory budget: ~50–150 KB per frame at 1080p; ~6 GB for a
  full match at 30 FPS.
- **Synthetic**: `FrameRecorderIntegrationTest` shows the smallest possible
  recorder usage if you want to script a test fixture.

### How do I download a tuning's JSON?

Click **Download recording zip** on the Cameras-tab ReplayPanel, or
**Export Selected Recording** on the Recordings card. Both hit
`/api/recordings/exportIndividual`, which zips the whole recording dir, so
`results/*.jsonl` (every tuning you've tried) and `tss.json` ride along
automatically. There is no separate export endpoint.

### How do I cancel a running replay?

Hit **Cancel replay** on the ReplayPanel or POST
`/api/recordings/replay/cancel`. Idempotent. The actual swap-back runs
asynchronously on the replay worker thread; `isReplaying` flips false on
NT once it completes.

### Why is my `pipeline_hash` value 0?

Pipeline hash is `Integer.toHexString` of `CVPipelineSettings.hashCode()`. If
multiple fields net to zero the hex string is `"0"` — that's fine, just a
coincidence. Change any setting to land in a fresh file.

### Why is my header's `tss_active_at_record` `null`?

The recording predates the `tss.json` sidecar. The exporter logs a one-shot
warn and writes nulls in both TSS fields; on the robot side,
`VisionIOJsonReplay` should refuse to load such a JSON.

## Code map

| Concern | File |
|---|---|
| Per-frame recorder | `photon-core/.../vision/pipeline/FrameRecorder.java` |
| Replay frame provider + callbacks | `photon-core/.../vision/frame/provider/FileLogFrameProvider.java` |
| Swap state machine (startReplay / cancelReplay / finishReplay) | `photon-core/.../vision/processes/VisionModule.java` |
| Source-level provider swap (`setFrameProvider`, `getReplayRecordingDir`) | `photon-core/.../vision/processes/VisionSource.java` |
| Per-tick supplier indirection | `photon-core/.../vision/processes/VisionRunner.java` |
| JSON exporter | `photon-core/.../vision/pipeline/JsonResultExporter.java` |
| Tee wiring + close on swap-back | `VisionModule.teeToJsonResultExporter` + `finishReplay` |
| TSS snapshot read | `JsonResultExporter.readSnapshot` |
| NT replay topics (`isReplaying`, `replayProgress*`) | `photon-core/.../common/dataflow/networktables/NTDataPublisher.java` |
| REST endpoints | `photon-server/.../RequestHandler.onStartReplayRequest` / `onCancelReplayRequest` |
| Cameras-tab UI | `photon-client/src/components/cameras/ReplayPanel.vue` |
| Per-row Replay button | `photon-client/src/components/settings/RecordingsCard.vue` |
| Zip download | `photon-server/.../RequestHandler.onExportIndividualRecordingRequest` |

## Test coverage

| Property | Test |
|---|---|
| Recorder writes correct frames + sidecar | `FrameRecorderIntegrationTest` |
| Replay echoes `metadata.jsonl` verbatim | `FileLogFrameProviderTest.emitsFramesWithCaptureTimestamps` |
| Provider parks on EOF when no callback is wired (standalone use) | `FileLogFrameProviderTest.eofWithoutCallbackPreservesParkingBehaviour` |
| `setOnProgress` fires once per emitted frame with monotonic count | `FileLogFrameProviderTest.onProgressFiresOncePerEmittedFrame` |
| `setOnEof` is one-shot, fires only on natural exhaustion | `FileLogFrameProviderTest.onEofFiresOnceOnNaturalExhaustion` |
| `requestStop()` is synchronous, idempotent, short-circuits next read | `FileLogFrameProviderTest.requestStopShortCircuitsAndFiresEofOnce` |
| Total frame count discovered at construction | `FileLogFrameProviderTest.getTotalFramesMatchesFixture` |
| Provider swap visible through `getFrameProvider()` + bound `Supplier` | `VisionSourceFrameProviderSwapTest` (7 cases) |
| `getReplayRecordingDir()` flips on FileLog ↔ live swap | `VisionSourceFrameProviderSwapTest.replayRecordingDir*` |
| `tss.json` write/read round-trip | `FrameRecorderTssSnapshotTest` |
| Exporter line format + offset shift | `JsonResultExporterTest` |
| Record→export end-to-end determinism + tuning sensitivity | `JsonResultEndToEndTest` |

End-to-end `startReplay` → tee → swap-back integration is not unit-tested
here: instantiating `VisionModule` requires `photontargetingJNI`, which
isn't bundled into photon-core's resources on Linux (the WSL test host).
The contract is covered by the manual smoke against a real PV server plus
the seven swap-mechanism unit tests above, and will be retested via the
REST round-trip on a JNI-capable host before merging back to upstream.
