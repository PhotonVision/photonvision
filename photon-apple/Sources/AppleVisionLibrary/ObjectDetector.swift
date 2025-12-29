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

import Foundation
import Vision
import CoreML
import CoreImage
import CoreVideo

// MARK: - Debug Logging

/// Print with source location for debugging
fileprivate func p(_ message: String, function: String = #function, line: Int = #line) {
    print("[ObjectDetector.swift:\(line)](\(function)) \(message)")
}

// MARK: - Object Detector

/// Object detector using Apple's Vision framework
/// Vision framework automatically handles image resizing and cropping for the model
public class ObjectDetector {
    private let modelPath: String
    private var vnModel: VNCoreMLModel?
    private var request: VNCoreMLRequest?

    /// Initialize the ObjectDetector with a CoreML model
    /// - Parameter modelPath: Absolute path to the .mlmodel or .mlmodelc file
    ///                         Vision framework will handle all image preprocessing
    public init(modelPath: String) {
        self.modelPath = modelPath
        p("ObjectDetector created with model path: \(modelPath)")
    }

    /// Load the CoreML model (called lazily on first use)
    /// Automatically compiles .mlmodel files to .mlmodelc format if needed
    private func ensureModelLoaded() -> Bool {
        guard vnModel == nil else { return true } // Already loaded

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

            let config = MLModelConfiguration()
            config.computeUnits = .all  // Use all available compute units (CPU, GPU, Neural Engine)
            let model = try MLModel(contentsOf: compiledURL, configuration: config)
            let vn = try VNCoreMLModel(for: model)
            self.vnModel = vn

            // Create Vision request with completion handler
            self.request = VNCoreMLRequest(model: vn) { request, error in
                if let error = error {
                    p("Vision request failed: \(error)")
                }
            }

            // Vision framework will handle image scaling automatically
            // scaleFill will resize and crop the image to fit the model's expected input size
            request!.imageCropAndScaleOption = .scaleFill

            p("CoreML model loaded successfully")
            return true
        } catch {
            p("Failed to load CoreML model: \(error)")
            return false
        }
    }

    /// Detect objects in an image using Vision framework
    /// Vision framework automatically handles image resizing and cropping
    /// - Parameters:
    ///   - imageData: Pointer to raw BGRA image bytes in Java memory
    ///   - width: Image width in pixels
    ///   - height: Image height in pixels
    ///   - pixelFormat: Format of the pixel data (must be 2=BGRA)
    ///   - boxThreshold: Minimum confidence threshold for detections (0.0 - 1.0)
    ///   - nmsThreshold: Non-maximum suppression IoU threshold (0.0 - 1.0)
    /// - Returns: Array of detection results
    public func detect(
        imageData: UnsafeRawPointer,
        width: Int,
        height: Int,
        pixelFormat: Int32,
        boxThreshold: Double,
        nmsThreshold: Double
    ) -> DetectionResultArray {
        // Ensure model is loaded
        guard ensureModelLoaded(), self.vnModel != nil, let request = self.request else {
            p("Model not loaded, returning empty results")
            return DetectionResultArray(results: [])
        }

        // Create CVPixelBuffer from BGRA data
        // Vision framework will handle resizing this to the model's input size
        guard let pixelBuffer = createBGRAPixelBuffer(
            from: imageData,
            width: width,
            height: height
        ) else {
            p("Failed to create CVPixelBuffer")
            return DetectionResultArray(results: [])
        }

        // Perform detection using Vision framework
        // Vision automatically resizes and crops the image to fit the model
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
        do {
            try handler.perform([request])
        } catch {
            p("Failed to perform detection: \(error)")
            return DetectionResultArray(results: [])
        }

        // Process results
        guard let observations = request.results as? [VNRecognizedObjectObservation] else {
            p("No results or unexpected result type")
            return DetectionResultArray(results: [])
        }

        // Filter by confidence threshold
        // Note: Vision framework already performs NMS internally, so we don't need to do it manually
        let filteredObservations = observations.filter { $0.confidence >= Float(boxThreshold) }
        p("Detected \(observations.count) objects, \(filteredObservations.count) above threshold \(boxThreshold)")

        // Convert to DetectionResult array
        let results = filteredObservations.map { observation -> DetectionResult in
            let boundingBox = observation.boundingBox

            // Get the top classification
            let topLabel = observation.labels.first
            let classId = topLabel?.identifier.split(separator: " ").first.flatMap { Int32($0) } ?? -1

            // Vision uses bottom-left origin, use midX/midY for center and flip Y coordinate
            return DetectionResult(
                x: Double(boundingBox.midX),
                y: Double(1.0 - boundingBox.midY),
                width: Double(boundingBox.width),
                height: Double(boundingBox.height),
                classId: classId,
                confidence: Double(observation.confidence)
            )
        }

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

    /// Create a CVPixelBuffer from BGRA image data
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
        var pixelBuffer: CVPixelBuffer?
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32BGRA,
            nil,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else {
            p("Failed to create CVPixelBuffer: \(status)")
            return nil
        }

        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

        guard let destData = CVPixelBufferGetBaseAddress(buffer) else {
            p("Failed to get CVPixelBuffer base address")
            return nil
        }

        let bytesPerRow = CVPixelBufferGetBytesPerRow(buffer)
        let srcBytesPerRow = width * 4  // BGRA = 4 channels

        // Copy BGRA data row by row
        for row in 0..<height {
            let srcRowPtr = imageData.advanced(by: row * srcBytesPerRow)
            let dstRowPtr = destData.advanced(by: row * bytesPerRow)
            memcpy(dstRowPtr, srcRowPtr, srcBytesPerRow)
        }

        return buffer
    }
}
