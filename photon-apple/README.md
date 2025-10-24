# photon-apple

Apple CoreML-based object detection integration for PhotonVision using Swift-Java interop.

## Overview

This subproject provides hardware-accelerated object detection on macOS and iOS devices using Apple's CoreML and Vision frameworks. It leverages the Foreign Function & Memory (FFM) API introduced in Java 24 to call Swift code that interfaces with CoreML models.

## Requirements

### Platform
- **macOS only** (CoreML and Vision frameworks are Apple-exclusive)
- macOS 15.0 or later
- Apple Silicon (M1/M2/M3) or Intel processors

### Software
- Java 24+ (required for FFM API)
- Swift 6.0+
- Xcode 16.0+ (for Swift toolchain)
- swift-java libraries (auto-published to mavenLocal)

### Environment
The `JAVA_HOME` environment variable must be set:
```bash
export JAVA_HOME="/Users/vasis/.sdkman/candidates/java/24.0.2-tem"
# or
export JAVA_HOME=$(/usr/libexec/java_home -v 24)
```

## Architecture

```
Swift Library (AppleVisionLibrary)
    ├── ObjectDetector.swift - CoreML/Vision integration
    ├── DetectionResult.swift - Detection result structure
    └── DetectionResultArray.swift - Array wrapper

        ↓ JExtract Plugin generates FFM bindings

Java Code (com.photonvision.apple)
    ├── Generated FFM bindings (auto-generated)
    ├── ImageUtils.java - OpenCV Mat conversion
    └── Test classes

        ↓ Used by photon-core

PhotonVision Integration (org.photonvision.vision.objects)
    ├── AppleModel.java - CoreML model wrapper
    └── AppleObjectDetector.java - Detector implementation
```

## Build Process

The build is conditionally executed only on macOS platforms (`osxarm64` or `osxx86-64`):

1. **Swift Package Build**: `swift build` compiles the Swift library and triggers JExtract plugin
2. **Java Binding Generation**: JExtract creates FFM-based Java wrappers for Swift classes
3. **Java Compilation**: Java code (including generated bindings) is compiled
4. **Native Library Packaging**: Swift `.dylib` files are embedded into the JAR
5. **JAR Creation**: Platform-specific JAR with classifier (e.g., `osx-aarch_64`)

### Build Commands

```bash
# Full build (from photonvision root)
./gradlew :photon-apple:build

# Run tests
./gradlew :photon-apple:test

# Generate Java bindings only
./gradlew :photon-apple:jextract

# Clean build
./gradlew :photon-apple:clean build
```

## Integration with PhotonVision

### Model Configuration

Place CoreML models (`.mlmodel` files) in PhotonVision's models directory. The models are automatically compiled to `.mlmodelc` format on first use.

Example model properties:
```json
{
  "family": "APPLE",
  "version": "YOLOV11",
  "nickname": "Coral Detection",
  "modelPath": "/path/to/coral-640-640-yolov11s.mlmodel",
  "labels": ["coral", "algae"],
  "resolutionWidth": 640,
  "resolutionHeight": 640
}
```

### Usage in PhotonVision Pipelines

The `AppleObjectDetector` implements the `ObjectDetector` interface and can be used interchangeably with RKNN and Rubik detectors:

```java
// Model is loaded by NeuralNetworkModelManager
AppleModel model = ...; // from config
ObjectDetector detector = model.load();

// Detect objects in a Mat
List<NeuralNetworkPipeResult> results = detector.detect(
    inputMat,
    nmsThreshold,  // e.g., 0.45
    boxThreshold   // e.g., 0.25
);

// Process results
for (NeuralNetworkPipeResult result : results) {
    Rect2d bbox = result.bbox();
    int classId = result.classIdx();
    double confidence = result.confidence();
    // ...
}

// Release when done
detector.release();
```

## Memory Management

- **Detector Lifecycle**: Managed by `AllocatingSwiftArena.ofAuto()` (GC-based cleanup)
- **Frame Data**: Each detection creates a temporary auto-managed arena for image data
- **Thread Safety**: The detector uses auto-arenas and is safe for concurrent access

## Testing

The project includes comprehensive test suites:

### Test Classes
- `CoreMLBaseTest` - Model loading, parameter validation, detector reuse
- `CoreMLDetectionTest` - Real detection with coral/algae models
- `CoreMLThreadSafetyTest` - Concurrent detection and stress testing
- `ObjectDetectorTest` - Integration tests with synthetic data

### Test Resources
- Located in `src/test/resources/2025/`
- `coral-640-640-yolov11s.mlmodel` (37.8 MB)
- `algae-640-640-yolov11s.mlmodel` (37.8 MB)
- Test images: `coral.jpeg`, `algae.jpeg`, `empty.png`

### Running Tests

```bash
# All tests
./gradlew :photon-apple:test

# Specific test class
./gradlew :photon-apple:test --tests "com.photonvision.apple.CoreMLDetectionTest"

# With output
./gradlew :photon-apple:test --info
```

## Getting CoreML Models

### Option 1: Export from PyTorch/TensorFlow
```python
import coremltools as ct

mlmodel = ct.convert(
    model,
    inputs=[ct.ImageType(shape=(1, 3, 640, 640))],
    minimum_deployment_target=ct.target.macOS15
)
mlmodel.save("model.mlmodel")
```

### Option 2: YOLOv8/YOLOv11 Export
```bash
pip install ultralytics
yolo export model=yolov11s.pt format=coreml imgsz=640
```

### Option 3: Download Pre-trained
- Apple's Core ML Model Gallery: https://developer.apple.com/machine-learning/models/
- Ultralytics Model Zoo: https://github.com/ultralytics/ultralytics

## Coordinate System

- **Input**: OpenCV uses top-left origin (standard)
- **Output**: DetectionResult coordinates are normalized (0.0-1.0) with top-left origin
- Vision framework internally uses bottom-left origin, but Y-coordinates are automatically flipped

## Troubleshooting

### Build Fails on Non-macOS Platforms
This is expected. The build is conditionally skipped on non-Apple platforms. A stub JAR is created instead.

### "JAVA_HOME not set"
```bash
export JAVA_HOME="/Users/vasis/.sdkman/candidates/java/24.0.2-tem"
```

### "Failed to load CoreML model"
- Ensure the model file exists and is a valid `.mlmodel`
- Check that you're running on macOS (CoreML requires Apple platforms)
- The first load compiles the model to `.mlmodelc` (may take a few seconds)

### "Class not found: com.photonvision.apple.ObjectDetector"
- Ensure you're running on macOS
- Run `./gradlew :photon-apple:jextract` to regenerate bindings
- Check that swift-java libraries are published to mavenLocal

### Tests Failing
- Verify Java 24 is being used: `java -version`
- Ensure `JAVA_HOME` points to Java 24
- Check that test resources (models and images) are present in `src/test/resources/2025/`

## Dependencies

### Swift Dependencies
- `SwiftKitSwift` - Core Swift-to-Java interop
- `JExtractSwiftPlugin` - Code generation plugin
- Apple Frameworks (automatic):
  - `Vision` - Object detection
  - `CoreML` - ML model execution
  - `CoreImage` - Image processing
  - `CoreVideo` - Pixel buffer handling

### Java Dependencies
- `SwiftKitCore` - Core Java runtime support
- `SwiftKitFFM` - FFM utilities and arena management
- `org.openpnp:opencv` - OpenCV for Java
- JUnit 5 - Testing

## License

GPL-3.0 - See PhotonVision LICENSE file

## Contributing

This module is part of PhotonVision. See main project for contribution guidelines.

## Credits

Built using the swift-java project (https://github.com/swiftlang/swift-java) for Swift-Java interoperability.
