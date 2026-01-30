# Composite Pipeline Plan (AprilTag + Object Detection)

## Goals
- Run AprilTag and Object Detection on the same frame, per camera, without switching pipelines.
- Publish independent results for each detector in the same output format as today.
- Keep the main camera result as a combined list (tags + objects).
- Draw both AprilTag overlays and object detection overlays on the output stream.
- Avoid pre-optimization; match existing object detection behavior on Orange Pi.

## Outputs (NetworkTables)
- `/<camera>/result`: combined list (AprilTag + object detections)
- `/<camera>-tags/result`: AprilTag-only list
- `/<camera>-objects/result`: object-only list

## Design Overview
- Add a new `Composite` pipeline type.
- Add `CompositePipelineSettings` with:
  - AprilTag-specific fields (tagFamily, decimate, blur, threads, refineEdges, decisionMargin, etc.)
  - Object detection-specific fields (confidence, nms, model)
  - Two toggles: `enableAprilTag`, `enableObjectDetection`
  - Shared advanced settings (exposure, solvePNPEnabled, targetModel, output drawing, etc.)
- Implement `CompositePipeline` that:
  - Requests `FrameThresholdType.NONE` and uses the color frame directly.
  - Builds a reusable grayscale buffer ring for AprilTag detection.
  - Runs AprilTag detection and pose estimation.
  - Runs object detection.
  - Combines targets (tags first, then objects) for the main result.
  - Preserves `multiTagResult` and `objectDetectionClassNames`.
- Implement `CompositePipelineResult` to carry split target lists.
- Update NT publishing to publish three `PhotonPipelineResult` streams.
- Update OutputStream drawing to render AprilTags and object detections together.
- Update UI to allow selecting Composite and configuring both AprilTag + Object Detection.

## Code Touch Points
- `photon-core`
  - `vision/pipeline/PipelineType.java` (add Composite)
  - `vision/pipeline/CompositePipelineSettings.java` (new)
  - `vision/pipeline/CompositePipeline.java` (new)
  - `vision/pipeline/result/CompositePipelineResult.java` (new)
  - `vision/processes/PipelineManager.java` (create/switch/clone)
  - `common/dataflow/networktables/NTDataPublisher.java` (publish combined + split results)
  - `vision/pipeline/OutputStreamPipeline.java` (draw both overlays)
- `photon-server`
  - `server/DataSocketHandler.java` (pipeline type mapping)
- `photon-client`
  - `types/PipelineTypes.ts` (Composite settings + defaults)
  - `types/WebsocketDataTypes.ts` (Composite pipeline enum)
  - UI components/tabs to show Composite config

## Risks / Notes
- Combined list changes “best target” semantics on `/<camera>/result` (tags first by default).
- Composite pipeline runs two detectors per frame; FPS may drop on Orange Pi.

## Implementation Steps
1. Add Composite pipeline type + settings (Java + TS).
2. Implement CompositePipeline + CompositePipelineResult.
3. Publish split results to `/<camera>-tags` and `/<camera>-objects`.
4. Update output stream drawing to show both overlays.
5. Update UI to select and configure Composite.
6. Add minimal tests (if feasible) for pipeline creation + NT publishing paths.
