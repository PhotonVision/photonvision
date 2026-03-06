import XCTest
@testable import PhotonAppleLibrary

final class ObjectDetectorTests: XCTestCase {
    func testDetectFake() {
        // Test the fake detection method
        let detector = ObjectDetector(modelPath: "/fake/path/model.mlmodel")

        // Create a dummy image buffer
        let width = 640
        let height = 480
        let buffer = UnsafeMutableRawPointer.allocate(byteCount: width * height * 4, alignment: 1)
        defer { buffer.deallocate() }

        // Call detectFake
        let results = detector.detectFake(
            imageData: buffer,
            width: width,
            height: height,
            pixelFormat: 2, // BGRA
            boxThreshold: 0.5,
            nmsThreshold: 0.4
        )

        // Verify we get 3 fake results
        XCTAssertEqual(results.count(), 3, "Should return 3 fake detections")

        // Verify first result
        let det0 = results.get(index: 0)
        XCTAssertEqual(det0.x, 0.1, accuracy: 0.001)
        XCTAssertEqual(det0.y, 0.2, accuracy: 0.001)
        XCTAssertEqual(det0.width, 0.3, accuracy: 0.001)
        XCTAssertEqual(det0.height, 0.4, accuracy: 0.001)
        XCTAssertEqual(det0.classId, 1)
        XCTAssertEqual(det0.confidence, 0.95, accuracy: 0.001)
    }

    func testDetectionResultCreation() {
        let result = DetectionResult(
            x: 0.5,
            y: 0.5,
            width: 0.2,
            height: 0.3,
            classId: 1,
            confidence: 0.9
        )

        XCTAssertEqual(result.x, 0.5)
        XCTAssertEqual(result.y, 0.5)
        XCTAssertEqual(result.width, 0.2)
        XCTAssertEqual(result.height, 0.3)
        XCTAssertEqual(result.classId, 1)
        XCTAssertEqual(result.confidence, 0.9)
    }

    func testDetectionResultArrayCreation() {
        let results = [
            DetectionResult(x: 0.1, y: 0.2, width: 0.3, height: 0.4, classId: 1, confidence: 0.9),
            DetectionResult(x: 0.5, y: 0.6, width: 0.2, height: 0.2, classId: 2, confidence: 0.8),
        ]

        let array = DetectionResultArray(results: results)
        XCTAssertEqual(array.count(), 2)

        let first = array.get(index: 0)
        XCTAssertEqual(first.x, 0.1, accuracy: 0.001)
        XCTAssertEqual(first.classId, 1)
    }
}
