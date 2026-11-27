# Photon Calibration Tool

A standalone CLI tool for producing new calibration data from existing images.
Performing calibration on-camera should be preferred since the lens/sensor construction can shift over time.

## Building

```bash
./gradlew photon-calibration-tool:install-dist
```

The resulting executable is in a subdirectory of `photon-calibration-tool/build/install`:

## Usage

Run it through the wrapper (Linux/Mac only):

```bash
./scripts/calibration_tool.sh -h
```

Run it through Gradle (slow, since it rebuilds):

```bash
./gradlew photon-calibration-tool:run --args="-h"
```

Example of rerunning detection and calibration:

```bash
./scripts/calibration_tool.sh redetect ./test-resources/calibrationCharucoImg/lifecam/2024-05-07_lifecam_1280 ./test-resources/calibration/lifecam_1280.json 68.5 --squareSize 0.0254 --markerSize 0.01905 --width 8 --height 8 --tagFamily=Dict_4X4_1000
```

Example of just rerunning calibration

```bash
./scripts/calibration_tool.sh recalibrate ./test-resources/calibrationCharucoImg/lifecam/2024-05-07_lifecam_1280 68.5
```
