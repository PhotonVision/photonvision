//===----------------------------------------------------------------------===//
//
// This source file is part of the Swift.org open source project
//
// Copyright (c) 2024 Apple Inc. and the Swift.org project authors
// Licensed under Apache License v2.0
//
// See LICENSE.txt for license information
// See CONTRIBUTORS.txt for the list of Swift.org project authors
//
// SPDX-License-Identifier: Apache-2.0
//
//===----------------------------------------------------------------------===//

// This file requires macOS/iOS - no conditional compilation needed
// since this library is macOS-only
import Foundation
import Vision
import CoreML
import CoreImage
import CoreVideo
import Accelerate

// MARK: - Debug Logging

/// Print with source location for debugging
fileprivate func p(_ message: String, function: String = #function, line: Int = #line) {
    print("[swift][MySwiftLibrary/ObjectDetector.swift:\(line)](\(function)) \(message)")
}

// MARK: - Object Detector

/// Object detector using CoreML and Vision framework
/// Optimized for high-performance sequential frame processing using zero-copy CVPixelBuffer wrapping
public class ObjectDetector {
    // Use Any to avoid exposing Apple-specific types to JExtract
    private var mlModel: Any?
    private var vnModel: Any?
    private let modelPath: String

    /// Initialize the ObjectDetector with a CoreML model
    /// - Parameter modelPath: Absolute path to the .mlmodel or .mlmodelc file
    ///                         If .mlmodel is provided, it will be automatically compiled to .mlmodelc
    ///                         and cached in the system temp directory
    public init(modelPath: String) {
        self.modelPath = modelPath
        p("ObjectDetector created with model path: \(modelPath)")

        // Load model lazily on first detect() call
    }

    /// Load the CoreML model (called lazily on first use)
    /// Automatically compiles .mlmodel files to .mlmodelc format if needed
    private func ensureModelLoaded() -> Bool {
        guard mlModel == nil else { return true } // Already loaded

        p("Loading CoreML model from: \(modelPath)")
        let modelURL = URL(fileURLWithPath: modelPath)

        do {
            // Check if this is an uncompiled .mlmodel file
            let compiledURL: URL
            if modelPath.hasSuffix(".mlmodel") {
                // Compile the model to a temporary location
                p("Compiling .mlmodel to .mlmodelc format...")
                compiledURL = try MLModel.compileModel(at: modelURL)
                p("Model compiled to: \(compiledURL.path)")
            } else {
                // Already compiled (.mlmodelc) or compiled directory
                compiledURL = modelURL
            }

            let model = try MLModel(contentsOf: compiledURL)
            let vn = try VNCoreMLModel(for: model)
            self.mlModel = model
            self.vnModel = vn
            p("CoreML model loaded successfully")
            return true
        } catch {
            p("Failed to load CoreML model: \(error)")
            return false
        }
    }

    /// Detect objects in an image using zero-copy CVPixelBuffer wrapping
    /// - Parameters:
    ///   - imageData: Pointer to raw BGRA image bytes in Java memory (zero-copy wrapped)
    ///   - width: Image width in pixels
    ///   - height: Image height in pixels
    ///   - pixelFormat: Format of the pixel data (must be 2=BGRA, other values ignored)
    ///   - boxThreshold: Minimum confidence threshold for detections (0.0 - 1.0)
    ///   - nmsThreshold: Non-maximum suppression IoU threshold (0.0 - 1.0)
    ///   - Returns: Array of detection results
    /// - Note: Uses CVPixelBufferCreateWithBytes for zero-copy wrapping of Java memory
    public func detect(
        imageData: UnsafeRawPointer,
        width: Int,
        height: Int,
        pixelFormat: Int32,
        boxThreshold: Double,
        nmsThreshold: Double
    ) -> DetectionResultArray {
        let detectStart = CFAbsoluteTimeGetCurrent()

        // Ensure model is loaded
        guard ensureModelLoaded(), let vnModelAny = self.vnModel, let vnModel = vnModelAny as? VNCoreMLModel else {
            p("Model not loaded, returning empty results")
            return DetectionResultArray(results: [])
        }

        // Convert BGRA bytes to CVPixelBuffer (with copy for safety)
        // Java side converts all formats to BGRA before calling this method
        let pixelBufferStart = CFAbsoluteTimeGetCurrent()
        guard let pixelBuffer = createBGRAPixelBuffer(
            from: imageData,
            width: width,
            height: height
        ) else {
            p("Failed to create CVPixelBuffer")
            return DetectionResultArray(results: [])
        }
        let pixelBufferEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] CVPixelBuffer creation: %.3f ms", (pixelBufferEnd - pixelBufferStart) * 1000.0))

        // Create Vision request
        let requestStart = CFAbsoluteTimeGetCurrent()
        let request = VNCoreMLRequest(model: vnModel) { [weak self] request, error in
            if let error = error {
                p("Vision request failed: \(error)")
            }
        }

        // Set confidence threshold
        request.imageCropAndScaleOption = .scaleFill
        let requestEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] Vision request creation: %.3f ms", (requestEnd - requestStart) * 1000.0))

        // Perform detection
        let handlerStart = CFAbsoluteTimeGetCurrent()
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
        let handlerEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] Handler creation: %.3f ms", (handlerEnd - handlerStart) * 1000.0))

        let performStart = CFAbsoluteTimeGetCurrent()
        do {
            try handler.perform([request])
        } catch {
            p("Failed to perform detection: \(error)")
            return DetectionResultArray(results: [])
        }
        let performEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] handler.perform() (CoreML inference): %.3f ms", (performEnd - performStart) * 1000.0))

        // Process results
        let processStart = CFAbsoluteTimeGetCurrent()
        guard let observations = request.results as? [VNRecognizedObjectObservation] else {
            p("No results or unexpected result type")
            return DetectionResultArray(results: [])
        }

        // Debug: log all detections before filtering
        p("Vision detected \(observations.count) raw observations")
        for (idx, obs) in observations.prefix(5).enumerated() {
            p("  Observation \(idx): confidence=\(obs.confidence), bbox=\(obs.boundingBox)")
        }

        // Filter by confidence threshold
        let filterStart = CFAbsoluteTimeGetCurrent()
        p("Applying confidence threshold: \(boxThreshold)")
        let filteredObservations = observations.filter { $0.confidence >= Float(boxThreshold) }
        let filterEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] Confidence filtering: %.3f ms", (filterEnd - filterStart) * 1000.0))
        p("After confidence filtering: \(filteredObservations.count) observations")

        // Apply NMS if needed
        let nmsStart = CFAbsoluteTimeGetCurrent()
        p("Applying NMS with IoU threshold: \(nmsThreshold)")
        let nmsResults = applyNMS(observations: filteredObservations, iouThreshold: nmsThreshold)
        let nmsEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] NMS: %.3f ms", (nmsEnd - nmsStart) * 1000.0))
        p("After NMS: \(nmsResults.count) final detections")

        // Convert to DetectionResult array
        let convertStart = CFAbsoluteTimeGetCurrent()
        let results = nmsResults.map { observation -> DetectionResult in
            let boundingBox = observation.boundingBox

            // Get the top classification
            let topLabel = observation.labels.first
            if let label = topLabel {
                p("  Detection label: '\(label.identifier)' (confidence: \(label.confidence))")
            }
            let classId = topLabel?.identifier.split(separator: " ").first.flatMap { Int32($0) } ?? -1

            // Vision uses bottom-left origin, so y needs to be flipped
            return DetectionResult(
                x: Double(boundingBox.minX),
                y: Double(1.0 - boundingBox.maxY),  // Flip Y coordinate
                width: Double(boundingBox.width),
                height: Double(boundingBox.height),
                classId: classId,
                confidence: Double(observation.confidence)
            )
        }
        let convertEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] Result conversion: %.3f ms", (convertEnd - convertStart) * 1000.0))

        let processEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] Total post-processing: %.3f ms", (processEnd - processStart) * 1000.0))

        let detectEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT] ===== TOTAL SWIFT DETECT: %.3f ms =====", (detectEnd - detectStart) * 1000.0))

        return DetectionResultArray(results: results)
    }

    /// Fake detection method for testing - returns synthetic detection results
    /// This method is useful for testing the Swift→Java data passing without requiring a CoreML model
    /// - Parameters:
    ///   - imageData: Pointer to raw BGRA image bytes (not actually used)
    ///   - width: Image width in pixels
    ///   - height: Image height in pixels
    ///   - pixelFormat: Format of the pixel data (ignored)
    ///   - boxThreshold: Minimum confidence threshold (ignored)
    ///   - nmsThreshold: NMS threshold (ignored)
    /// - Returns: Array of synthetic detection results for testing
    public func detectFake(
        imageData: UnsafeRawPointer,
        width: Int,
        height: Int,
        pixelFormat: Int32,
        boxThreshold: Double,
        nmsThreshold: Double
    ) -> DetectionResultArray {
        // Return 3 fake detection results for testing
        let fakeResults = [
            DetectionResult(x: 0.1, y: 0.2, width: 0.3, height: 0.4, classId: 1, confidence: 0.95),
            DetectionResult(x: 0.5, y: 0.5, width: 0.2, height: 0.2, classId: 2, confidence: 0.87),
            DetectionResult(x: 0.7, y: 0.1, width: 0.15, height: 0.25, classId: 3, confidence: 0.72)
        ]

        return DetectionResultArray(results: fakeResults)
    }

    // MARK: - Private Helper Methods

    /// Create a CVPixelBuffer from BGRA image data (with copy for safety)
    /// - Parameters:
    ///   - imageData: Pointer to raw BGRA bytes from Java
    ///   - width: Image width in pixels
    ///   - height: Image height in pixels
    /// - Returns: CVPixelBuffer containing a copy of the image data
    private func createBGRAPixelBuffer(
        from imageData: UnsafeRawPointer,
        width: Int,
        height: Int
    ) -> CVPixelBuffer? {
        let createStart = CFAbsoluteTimeGetCurrent()

        var pixelBuffer: CVPixelBuffer?
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32BGRA,
            nil,
            &pixelBuffer
        )
        let createEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT]   CVPixelBufferCreate: %.3f ms", (createEnd - createStart) * 1000.0))

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else {
            p("Failed to create CVPixelBuffer: \(status)")
            return nil
        }

        let lockStart = CFAbsoluteTimeGetCurrent()
        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
        let lockEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT]   CVPixelBufferLockBaseAddress: %.3f ms", (lockEnd - lockStart) * 1000.0))

        guard let destData = CVPixelBufferGetBaseAddress(buffer) else {
            p("Failed to get CVPixelBuffer base address")
            return nil
        }

        let bytesPerRow = CVPixelBufferGetBytesPerRow(buffer)
        let srcBytesPerRow = width * 4  // BGRA = 4 channels

        // Debug: log key pointers and sizes to help correlate with Java-side logs
        p("createBGRAPixelBuffer: imageData=")
        p("  imageData(ptr)=\(UInt(bitPattern: imageData)) width=\(width) height=\(height) bytesPerRow=\(bytesPerRow) srcBytesPerRow=\(srcBytesPerRow)")

        // Defensive check: ensure destination stride is large enough for a direct copy
        if bytesPerRow < srcBytesPerRow {
            p("bytesPerRow (\(bytesPerRow)) < srcBytesPerRow (\(srcBytesPerRow)) - aborting copy to avoid OOB")
            return nil
        }

        // Direct copy of BGRA data row by row; print the first-row addresses to aid debugging
        let memcpyStart = CFAbsoluteTimeGetCurrent()
        for row in 0..<height {
            let srcRowPtr = imageData.advanced(by: row * srcBytesPerRow)
            let dstRowPtr = destData.advanced(by: row * bytesPerRow)
            if row == 0 {
                p("  first row: src=\(UInt(bitPattern: srcRowPtr)) dst=\(UInt(bitPattern: dstRowPtr)) copyLen=\(srcBytesPerRow)")
            }
            // use memcpy (calls memmove underneath on some platforms)
            memcpy(dstRowPtr, srcRowPtr, srcBytesPerRow)
        }
        let memcpyEnd = CFAbsoluteTimeGetCurrent()
        print(String(format: "[TIMING-SWIFT]   memcpy loop (%d rows): %.3f ms", height, (memcpyEnd - memcpyStart) * 1000.0))

        return buffer
    }

    /// Apply Non-Maximum Suppression to filter overlapping detections
    private func applyNMS(observations: [VNRecognizedObjectObservation], iouThreshold: Double) -> [VNRecognizedObjectObservation] {
        guard observations.count > 1 else { return observations }

        // Sort by confidence (descending)
        let sorted = observations.sorted { $0.confidence > $1.confidence }

        var selected: [VNRecognizedObjectObservation] = []
        var suppressed = Set<Int>()

        for (i, obs1) in sorted.enumerated() {
            if suppressed.contains(i) { continue }

            selected.append(obs1)

            // Suppress overlapping boxes
            for (j, obs2) in sorted.enumerated() {
                if j <= i || suppressed.contains(j) { continue }

                let iou = calculateIoU(box1: obs1.boundingBox, box2: obs2.boundingBox)
                if iou > iouThreshold {
                    suppressed.insert(j)
                }
            }
        }

        return selected
    }

    /// Calculate Intersection over Union (IoU) between two bounding boxes
    private func calculateIoU(box1: CGRect, box2: CGRect) -> Double {
        let intersection = box1.intersection(box2)

        if intersection.isNull || intersection.isEmpty {
            return 0.0
        }

        let intersectionArea = intersection.width * intersection.height
        let box1Area = box1.width * box1.height
        let box2Area = box2.width * box2.height
        let unionArea = box1Area + box2Area - intersectionArea

        guard unionArea > 0 else { return 0.0 }

        return Double(intersectionArea / unionArea)
    }
}
