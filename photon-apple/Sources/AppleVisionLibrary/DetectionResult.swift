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

/// Represents a single object detection result
/// JExtract doesn't support structs well, so we use a class
public class DetectionResult {
    public let x: Double          // Top-left x coordinate (normalized 0-1)
    public let y: Double          // Top-left y coordinate (normalized 0-1)
    public let width: Double      // Width (normalized 0-1)
    public let height: Double     // Height (normalized 0-1)
    public let classId: Int32     // Class identifier
    public let confidence: Double // Confidence score (0-1)

    public init(x: Double, y: Double, width: Double, height: Double, classId: Int32, confidence: Double) {
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.classId = classId
        self.confidence = confidence
    }
}
