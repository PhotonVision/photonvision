# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

PhotonVision is a free, fast, and easy-to-use computer vision solution for the FIRST Robotics Competition. It consists of:
- **photon-core**: Core vision processing (Java) - pipelines, camera management, object detection
- **photon-server**: Web server and deployment (Java) - main entry point
- **photon-client**: Vue.js 3 web UI built with Vite and Vuetify
- **photon-lib**: C++/Java vendordep library for FRC robot code
- **photon-targeting**: Protocol buffer definitions for data serialization

## Initial Setup

### Git Submodules

PhotonVision uses git submodules for the swift-java dependency (macOS-only). After cloning the repository:

```bash
git submodule update --init --recursive
```

**Note**: If you're only building for non-macOS platforms, you can skip the submodule initialization. The build system will automatically skip photon-apple on non-macOS platforms.

## Common Commands

### Building

Build the entire project (Java backend + Vue frontend), skipping tests:
```bash
./gradlew build -x test
```

**First-time macOS builds**: The build will automatically publish swift-java SwiftKit libraries to your local Maven repository (~/.m2). This adds ~30 seconds to the first build but is cached for subsequent builds.

Build with tests (note: photon-apple tests are skipped by default):
```bash
./gradlew build
```

Build only the Java backend (faster, skips UI):
```bash
./gradlew photon-server:shadowJar
```

Build for specific architecture (cross-compilation):
```bash
./gradlew build -PArchOverride=linuxarm64
# Valid overrides: winx32, winx64, winarm64, macx64, macarm64, linuxx64, linuxarm64, linuxathena
```

Build and package platform-specific JAR:
```bash
./gradlew photon-server:shadowJar
# Output: photon-server/build/libs/photonvision-<version>-<platform>.jar
```

### Running

Run PhotonVision locally:
```bash
./gradlew photon-server:run
```

Run with JVM profiling enabled:
```bash
./gradlew photon-server:run -Pprofile
```

### Testing

Run all Java tests:
```bash
./gradlew test
```

Run headless tests (for CI):
```bash
./gradlew testHeadless
```

Run C++ tests:
```bash
./gradlew runCpp
```

### Code Quality

Format all code (Java, Gradle, markdown):
```bash
./gradlew spotlessApply
```

Check formatting without modifying:
```bash
./gradlew spotlessCheck
```

Lint and format Vue frontend:
```bash
cd photon-client
pnpm lint
pnpm format
```

### Frontend Development

Install frontend dependencies:
```bash
cd photon-client
pnpm install
```

Run frontend dev server (hot reload):
```bash
cd photon-client
pnpm dev
```

Build frontend for production:
```bash
cd photon-client
pnpm build
```

### Deployment

Deploy to Raspberry Pi/Orange Pi:
```bash
./gradlew photon-server:deploy -PtgtIP=photonvision.local -PtgtUser=pi -PtgtPw=raspberry
```

### Publishing

Publish PhotonLib to local Maven:
```bash
./gradlew photon-lib:publish
```

## Architecture

### Vision Processing Pipeline

The core architecture follows a modular pipeline design:

1. **VisionModuleManager** (`photon-core/src/main/java/org/photonvision/vision/processes/VisionModuleManager.java`)
   - Manages multiple VisionModule instances (one per camera)
   - Coordinates camera configuration and lifecycle

2. **VisionModule** (`photon-core/src/main/java/org/photonvision/vision/processes/VisionModule.java`)
   - Owns a VisionSource (camera) and PipelineManager
   - Runs vision processing in its own thread via VisionRunner

3. **PipelineManager** (`photon-core/src/main/java/org/photonvision/vision/processes/PipelineManager.java`)
   - Manages multiple pipeline configurations per camera
   - Handles switching between pipeline types (AprilTag, Reflective, ColoredShape, Object Detection)

4. **Pipeline Types** (`photon-core/src/main/java/org/photonvision/vision/pipeline/`)
   - `AprilTagPipeline`: Fiducial marker detection using WPILib apriltag
   - `ArucoPipeline`: ArUco marker detection
   - `ReflectivePipeline`: Retroreflective tape detection
   - `ColoredShapePipeline`: Shape and color-based detection
   - `ObjectDetectionPipeline`: Neural network object detection (RKNN, Rubik)

5. **Pipes** (`photon-core/src/main/java/org/photonvision/vision/pipe/`)
   - Atomic vision operations (threshold, contour, filter, etc.)
   - Pipelines compose pipes in sequence

### Camera Management

- **VisionSource** (`photon-core/src/main/java/org/photonvision/vision/processes/VisionSource.java`)
  - Abstract camera interface
  - Implementations include USB cameras, CSI cameras (Pi/libcamera), network cameras

- **VisionSourceManager** (`photon-core/src/main/java/org/photonvision/vision/processes/VisionSourceManager.java`)
  - Detects and manages available cameras
  - Handles camera connection/disconnection

### Configuration & Storage

- **ConfigManager** (`photon-core/src/main/java/org/photonvision/common/configuration/ConfigManager.java`)
  - Persists camera and pipeline configurations to disk
  - Uses Jackson for JSON serialization

- **HardwareManager** (`photon-core/src/main/java/org/photonvision/common/hardware/HardwareManager.java`)
  - Detects platform (Pi, Orange Pi, x86)
  - Manages platform-specific features (GPIO, LED control)

### Web Server & API

- **Server** (`photon-server/src/main/java/org/photonvision/server/Server.java`)
  - Javalin-based REST API and WebSocket server
  - Serves Vue.js frontend from resources

- **NetworkTablesManager** (`photon-core/src/main/java/org/photonvision/common/dataflow/networktables/NetworkTablesManager.java`)
  - Publishes vision results to NetworkTables for FRC robot consumption

### Frontend (photon-client)

- Vue 3 with Composition API
- Vuetify 3 component library
- Pinia for state management
- Axios for REST API communication
- WebSocket for real-time updates

## Important Implementation Notes

### Java Version

- **Requires Java 24** due to the photon-apple subproject using Foreign Function & Memory API
- All subprojects compile with Java 24 (sourceCompatibility and targetCompatibility)

### photon-apple Module (macOS-only)

- **Git Submodule**: Depends on [swift-java](https://github.com/swiftlang/swift-java) vendored as a git submodule at `photon-apple/swift-java/`
  - Run `git submodule update --init --recursive` to initialize after cloning
  - Build system automatically publishes SwiftKit libraries to mavenLocal before building
  - Submodule pinned to specific commit for stability
- **Direct imports only**: Uses direct imports from `com.photonvision.apple.*` and `org.swift.swiftkit.*` - no reflection
- **Zero-copy optimization**:
  - Java: Uses `Mat.dataAddr()` for direct pointer passing - no byte[] allocation per frame
  - Swift: Uses `CVPixelBufferCreateWithBytes()` to wrap Java memory - **no memcpy at all**
  - Total: **Zero allocations and zero copies per frame** for image data transfer
- **Resource reuse**: Java side reuses `bgraMat` across frames for BGRA conversion
- **Performance**: Optimized for ~100 FPS sequential frame processing
- **BGRA conversion**: All images are converted to BGRA format in Java before passing to Swift
- **No deprecated code**: ImageUtils contains only pixel format constants and helpers

### Code Style

- Java code uses Google Java Format with 4-space indentation (enforced by Spotless)
- Gradle files use 4-space indentation
- Tabs are converted to spaces in Java files
- All code must end with newline

### Testing Considerations

- Tests run from repository root (`workingDir = new File("${rootDir}")`)
- Headless tests exclude benchmark tests and run in headless mode
- Test resources are in `test-resources/` directory

### Cross-Platform Concerns

- Native libraries are platform-specific (mrcal, libcamera, RKNN only on certain platforms)
- macOS builds conditionally include photon-apple module for AVFoundation camera support
- Use `wpilibNativeName` and `jniPlatform` variables for platform detection

### Neural Network Models

- **NeuralNetworkModelManager** manages downloadable models
- RKNN (Rockchip NPU) models for Orange Pi 5
- Coral Edge TPU support via Rubik

### Version Management

- Version generated from git via `versioningHelper.gradle`
- PhotonVersion.java/.cpp generated at build time
- Dev builds use "dev-" prefix, releases use semantic versioning

## Project Dependencies

PhotonVision depends on several out-of-source repositories:
- Custom OpenCV build with GStreamer: https://github.com/PhotonVision/thirdparty-opencv
- mrcal Java bindings: https://github.com/PhotonVision/mrcal-java
- libcamera driver for Pi CSI: https://github.com/PhotonVision/photon-libcamera-gl-driver
- ArUco Nano JNI: https://github.com/PhotonVision/aruconano-jni

## WPILib Integration

- Uses WPILib GradleRIO plugin (version 2025.3.2)
- Depends on WPILib libraries: cscore, ntcore, apriltag, wpimath, wpiunits
- PhotonLib is distributed as a vendordep JSON for robot projects
- Compatible with FRC year 2025
