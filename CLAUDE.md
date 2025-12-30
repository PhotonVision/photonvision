# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

PhotonVision is a free, fast, and easy-to-use computer vision solution for the FIRST Robotics Competition. It consists of:
- **photon-core**: Core vision processing (Java) - pipelines, camera management, object detection
- **photon-server**: Web server and deployment (Java) - main entry point
- **photon-client**: Vue.js 3 web UI built with Vite and Vuetify
- **photon-lib**: C++/Java vendordep library for FRC robot code
- **photon-targeting**: Protocol buffer definitions for data serialization
- **photon-apple**: Apple Vision framework integration (macOS-only) - CoreML object detection

## Initial Setup

### Git Submodules

PhotonVision uses git submodules for the swift-java dependency (macOS-only). After cloning the repository:

```bash
git submodule update --init --recursive
```

**Note**: If you're only building for non-macOS platforms, you can skip the submodule initialization. The build system will automatically skip photon-apple on non-macOS platforms.

### macOS-Specific Setup

On macOS, you must manually publish the SwiftKit libraries to your local Maven repository before building:

```bash
cd photon-apple/swift-java
./gradlew publishToMavenLocal
cd ../..
```

This only needs to be done once after cloning, or when the swift-java submodule is updated.

## Common Commands

### Building

Build the entire project (skipping tests and formatting checks):
```bash
./gradlew build -x test -x spotlessCheck -x spotlessApply
```

Quick install (for running locally):
```bash
./gradlew installDist -x test -x spotlessCheck -x spotlessApply
```

Build only the Java backend JAR:
```bash
./gradlew photon-server:shadowJar
# Output: photon-server/build/libs/photonvision-<version>-<platform>.jar
```

Build for specific architecture (cross-compilation):
```bash
./gradlew build -PArchOverride=linuxarm64
# Valid overrides: winx32, winx64, winarm64, macx64, macarm64, linuxx64, linuxarm64, linuxathena
```

### Running

Run PhotonVision locally:
```bash
./gradlew photon-server:run
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

### Code Quality

Format all code (Java, Gradle, markdown):
```bash
./gradlew spotlessApply
```

Check formatting without modifying:
```bash
./gradlew spotlessCheck
```

### Frontend Development

Install dependencies and run dev server:
```bash
cd photon-client
pnpm install
pnpm dev
```

Build frontend for production:
```bash
cd photon-client
pnpm build
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
   - `ObjectDetectionPipeline`: Neural network object detection (RKNN, Rubik, Apple)

5. **Pipes** (`photon-core/src/main/java/org/photonvision/vision/pipe/`)
   - Atomic vision operations (threshold, contour, filter, etc.)
   - Pipelines compose pipes in sequence

### Object Detection

PhotonVision supports multiple object detection backends:

- **RKNN**: Rockchip NPU acceleration (Orange Pi 5 / RK3588)
- **Rubik**: Google Coral Edge TPU (Qualcomm platforms)
- **Apple**: CoreML + Vision framework (macOS, iOS via photon-apple)

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
  - Detects platform (Pi, Orange Pi, x86, macOS)
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

- **Requires Java 24** for all subprojects
- photon-apple requires Java 24 toolchain due to Foreign Function & Memory API usage in SwiftKit
- All subprojects compile with Java 24 (sourceCompatibility and targetCompatibility)
- SwiftKit uses FFM API in preview mode on Java 24 (stable in Java 25+)

### photon-apple Module (macOS-only)

The photon-apple module provides native Apple object detection using CoreML and the Vision framework.

#### Structure

- **Swift Package**: Located at `photon-apple/` (Package.swift)
  - `Sources/AppleVisionLibrary/`: Swift source code for Vision framework integration
    - `ObjectDetector.swift`: Main detector implementation using CoreML
    - `DetectionResult.swift`: Data structures for detection results
    - `swift-java.config`: Configuration for Java binding generation
  - Depends on swift-java submodule at `photon-apple/swift-java/`

- **Java Sources**: Located at `photon-apple/src/main/java/`
  - `com.photonvision.apple.ImageUtils`: Image format utilities
  - Generated Java bindings: `.build/plugins/outputs/.../src/generated/java/com/photonvision/apple/`
    - `ObjectDetector.java` (auto-generated from Swift)

- **Java Wrapper**: Located at `photon-core/src/main/java/org/photonvision/vision/objects/`
  - `AppleModel.java`: Model loading and management
  - `AppleObjectDetector.java`: Java wrapper around Swift detector

#### Dependencies

- **Swift-Java Submodule**: Vendored at `photon-apple/swift-java/`
  - Run `git submodule update --init --recursive` to initialize
  - Provides SwiftKit libraries for Java-Swift interop
  - **Manual publishing required**: `cd photon-apple/swift-java && ./gradlew publishToMavenLocal`
  - Publishes to `~/.m2/repository/org/swift/swiftkit/`
    - `swiftkit-core:1.0-SNAPSHOT`
    - `swiftkit-ffm:1.0-SNAPSHOT` (Foreign Function & Memory API)

- **Build Process**:
  1. `publishSwiftKitLocal` task is disabled (broken with parent gradle flags)
  2. SwiftKit must be manually published before building (see macOS-Specific Setup)
  3. `buildSwift` task compiles Swift code and generates Java bindings via JExtract plugin
  4. Generated Java classes are included in build via custom sourceSets configuration

#### Technical Details

- **Zero-copy optimization**:
  - Java: Passes image data directly via `MemorySegment` (Foreign Function & Memory API)
  - Swift: Wraps Java memory with `CVPixelBufferCreateWithBytes()` - no memcpy
  - Total: **Zero allocations and zero copies per frame** for image data transfer

- **Memory management**:
  - Uses `AllocatingSwiftArena` / `ClosableAllocatingSwiftArena` for confined memory arenas
  - Frame-scoped arenas for per-frame allocations (auto-cleanup)
  - Detector-scoped arena for detector lifetime (manual cleanup on release)

- **Image format**:
  - All images converted to BGRA format in Java before passing to Swift
  - Swift side receives BGRA pixel buffer directly

- **Vision framework integration**:
  - Automatic image preprocessing (resizing, cropping) handled by Vision framework
  - Built-in NMS (Non-Maximum Suppression) - NMS threshold passed but not used
  - Returns normalized coordinates (0-1) converted to pixels in Java

- **Supported models**:
  - `.mlmodel` (Core ML model files)
  - `.mlmodelc` (compiled Core ML models)
  - Models placed in `models/` directory

#### Platform Detection

- Module only included on macOS (`System.getProperty("os.name").toLowerCase().contains("mac")`)
- See `settings.gradle` and `photon-core/build.gradle` for conditional inclusion

### Code Style

- Java code uses Google Java Format with 4-space indentation (enforced by Spotless)
- Gradle files use 4-space indentation
- Tabs are converted to spaces in Java files
- All code must end with newline

### Testing Considerations

- Tests run from repository root (`workingDir = new File("${rootDir}")`)
- Headless tests exclude benchmark tests and run in headless mode
- Test resources are in `test-resources/` directory
- photon-apple tests are skipped by default (require macOS and CoreML model)

### Cross-Platform Concerns

- Native libraries are platform-specific:
  - **mrcal**: Linux/Windows only (excluded on macOS)
  - **libcamera**: Raspberry Pi only
  - **RKNN**: Orange Pi 5 / RK3588 only
  - **Rubik**: Qualcomm platforms only
  - **photon-apple**: macOS only
- Use `wpilibNativeName` and `jniPlatform` variables for platform detection

### Neural Network Models

- **NeuralNetworkModelManager** (`photon-core/src/main/java/org/photonvision/common/configuration/NeuralNetworkModelManager.java`)
  - Manages downloadable models
  - Supports three model families:
    - `RKNN`: Rockchip NPU models (.rknn files)
    - `RUBIK`: Coral Edge TPU models (.tflite files)
    - `APPLE`: CoreML models (.mlmodel / .mlmodelc files)
  - Model versions: YOLOv5, YOLOv8, YOLOv11

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
- Swift-Java interop (submodule): https://github.com/swiftlang/swift-java

## WPILib Integration

- Uses WPILib GradleRIO plugin (version 2025.3.2)
- Depends on WPILib libraries: cscore, ntcore, apriltag, wpimath, wpiunits
- PhotonLib is distributed as a vendordep JSON for robot projects
- Compatible with FRC year 2025

### rebuilding after modifying swift code

1. Rebuild Swift library and regenerate Java bindings
  ./gradlew :photon-apple:buildSwift --rerun-tasks

2. Clean and rebuild entire project
  ./gradlew clean :photon-server:build -x test -x spotlessCheck -x spotlessApply

3. Delete cached dylibs (critical step!)
  rm -rf photonvision_config/nativelibs/*.dylib
