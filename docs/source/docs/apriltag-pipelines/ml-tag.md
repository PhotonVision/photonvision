# ML-Tag (ML AprilTag Acceleration)

## How does it work?

ML-Tag is an acceleration mode within the AprilTag pipeline. Instead of running the AprilTag detector across the full camera frame, an ML model running on the coprocessor's NPU first proposes bounding-box regions where AprilTags appear to be in the image, and the standard AprilTag decoder then runs only inside those regions. On supported hardware this reduces the work the classical detector has to do, leaving more headroom for higher resolutions, higher framerates, and lower latency.

The ML model is purely a localizer: it answers "is there an AprilTag here?" and nothing else. It does not read the tag's ID, identify its family, recover its orientation, or estimate its pose — all of that is still done by the same classical detector used in the standard AprilTag pipeline. ML-Tag is therefore transparent to your robot code: detections come out as the same targets, and {ref}`3D Tracking <docs/apriltag-pipelines/3D-tracking:3D Tracking>` and {ref}`MultiTag <docs/apriltag-pipelines/multitag:MultiTag Localization>` work without any changes.

## Hardware Requirements

ML-Tag runs on the same NPUs PhotonVision uses for object detection:

- RK3588 boards — Orange Pi 5 / 5 Plus, Rock 5C, CoolPi 4B (RKNN backend)
- QCS6490 boards — Rubik Pi 3 (TFLite backend)

For installation, model conversion, and other platform-specific notes, see the coprocessor pages used for object detection: {ref}`Orange Pi 5 <docs/objectDetection/opi:Orange Pi 5 (and variants) Object Detection>` and {ref}`Rubik Pi 3 <docs/objectDetection/rubik:Rubik Pi 3 Object Detection>`.

:::{note}
The ML-Tag controls only appear in the AprilTag pipeline tab when a supported NPU backend is detected. On other coprocessors the AprilTag pipeline runs in its standard CPU-only configuration.
:::

## Enabling ML-Tag

In the AprilTag pipeline tab, scroll past the standard AprilTag tuning settings to the "ML-Tag (ML AprilTag Acceleration)" section. Toggle **Enable ML-Tag**, and the remaining ML-Tag controls will appear below.

## Tuning ML-Tag

The standard AprilTag tuning parameters described in {ref}`2D AprilTag Tuning / Tracking <docs/apriltag-pipelines/2D-tracking-tuning:Tuning AprilTags>` still apply — they control how the detector decodes each region the ML model hands it. The settings below only control the ML localizer stage.

### Model

Selects which ML model the NPU uses to locate AprilTag regions in the image. PhotonVision ships with a YoloV11 AprilTag model for each supported NPU; any compatible models you have uploaded will also appear here. Only models compatible with the detected NPU backend are listed.

### Confidence Threshold

The minimum confidence score (between 0 and 1) the ML model must report for a proposed region before it is passed to the classical decoder. Higher values reject more weak proposals — good for cutting false positives, but at the risk of missing tags the model is less sure about. The default of 0.5 is a reasonable starting point; lower it if tags are being missed and the ROI overlay confirms the model is hesitating on them.

### NMS Threshold

The non-maximum suppression overlap cutoff (between 0 and 1) used to merge overlapping proposals of the same tag. Higher values allow more overlapping boxes through; lower values are stricter about merging duplicates. The default of 0.45 works well in most cases.

### ROI Padding (px)

The number of pixels of padding added around each proposed region before the classical decoder runs on it. Padding is applied in pixels, so it naturally adapts to tag size in the image: small, far-away tags receive proportionally more expansion (helping the decoder see their borders), while large, nearby tags receive less. Raise this if tag corners are being clipped at the edges of the ROIs; lower it if neighboring tags are being merged into a single region.

### Show ROI Boxes

When enabled, draws the ML model's proposed bounding boxes onto the processed stream. This is valuable when tuning Confidence Threshold and ROI Padding — you can see exactly which regions the model is finding and how tightly they fit each tag. Turn this off in competition for a cleaner stream.

## Limitations

- ML-Tag is only available on the supported NPU coprocessors listed above. On any other hardware the AprilTag pipeline behaves in the standard manner.
- When several overlapping ROIs decode the same tag, only the detection with the highest decision margin is kept. In practice this is the desired behavior, but it does mean alternate solutions from the same tag are not surfaced.
- The ML model is purely a localizer. If a tag is in the frame but the model fails to propose a region around it, the classical decoder will never see it and the tag will not be detected. When tuning, use **Show ROI Boxes** to confirm the model is finding the tags you care about, and lower **Confidence Threshold** if it is not.
- The ML model has shown in testing to be able to identify AprilTag at distances where there is not enough pixel density from the region of image to makeout what specific Tag the detector is finding.
