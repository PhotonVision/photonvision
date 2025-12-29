import Foundation

/// Array wrapper for detection results that can be accessed from Java
/// SwiftJava FFM doesn't support arrays
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
