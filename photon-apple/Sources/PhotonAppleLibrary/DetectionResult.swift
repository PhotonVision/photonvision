import Foundation

/// Represents a single object detection result
public class DetectionResult {
    public let x: Double          // Center x coordinate (normalized 0-1)
    public let y: Double          // Center y coordinate (normalized 0-1)
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
