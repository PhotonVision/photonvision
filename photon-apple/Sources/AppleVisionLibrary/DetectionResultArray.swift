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

/// Array wrapper for detection results that can be accessed from Java
public class DetectionResultArray {
    private let results: [DetectionResult]

    public init(results: [DetectionResult]) {
        self.results = results
    }

    public func count() -> Int {
        return results.count
    }

    public func get(index: Int) -> DetectionResult {
        return results[index]
    }
}
