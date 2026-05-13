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

Replay assigns a recording dir to a vision source via the camera-matching UI.
A `FileLogFrameProvider` reads frames out of `frames/`, paces them by the
deltas in `metadata.jsonl`, and emits `CapturedFrame`s whose
`captureTimestamp` is set to the recorded `capture_ns` verbatim.

The pipeline runs end-to-end as normal — same `NTDataPublisher`, same UI feed,
same target processing. When the recording hits EOF the provider parks the
vision thread; deactivate-and-reactivate the camera to restart.

## JSON export

For File Log Camera sources only, `VisionModule` tees every
`CVPipelineResult` to a `JsonResultExporter`. The exporter is built lazily on
the first result so non-File-Log cameras pay no cost.

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

Click **Export Selected Recording** on the Recordings card. The existing
`/api/recordings/exportIndividual` endpoint already zips the whole
recording dir, so `results/*.jsonl` (every tuning you've tried) and `tss.json`
ride along automatically. There is no separate export endpoint.

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
| Recording replay | `photon-core/.../vision/frame/provider/FileLogFrameProvider.java` |
| JSON exporter | `photon-core/.../vision/pipeline/JsonResultExporter.java` |
| Tee wiring + close | `photon-core/.../vision/processes/VisionModule.java` (`teeToJsonResultExporter`) |
| TSS snapshot read | `JsonResultExporter.readSnapshot` |
| Camera-info gate | `photon-core/.../vision/camera/PVCameraInfo.PVFileLogCameraInfo` |
| Zip download | `photon-server/.../RequestHandler.onExportIndividualRecordingRequest` |

## Test coverage

| Property | Test |
|---|---|
| Recorder writes correct frames + sidecar | `FrameRecorderIntegrationTest` |
| Replay echoes `metadata.jsonl` verbatim | `FileLogFrameProviderTest.emitsFramesWithCaptureTimestamps` |
| Replay parks on EOF | `FileLogFrameProviderTest.parksTheVisionThreadAtEof` |
| `tss.json` write/read round-trip | `FrameRecorderTssSnapshotTest` |
| Exporter line format + offset shift | `JsonResultExporterTest` |
| Record→export end-to-end determinism + tuning sensitivity | `JsonResultEndToEndTest` |
