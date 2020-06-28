package org.photonvision.vision.opencv;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.imgproc.Imgproc;

public class CVShape {
    public final Contour contour;
    public final ContourShape shape;

    private MatOfPoint3f customTarget = null;

    private MatOfPoint2f approxCurve = new MatOfPoint2f();

    public CVShape(Contour contour, ContourShape shape) {
        this.contour = contour;
        this.shape = shape;
    }

    public CVShape(Contour contour, MatOfPoint3f targetPoints) {
        this.contour = contour;
        this.shape = ContourShape.Custom;
        customTarget = targetPoints;
    }

    public Contour getContour() {
        return contour;
    }

    public MatOfPoint2f getApproxPolyDp(double epsilon, boolean closed) {
        approxCurve.release();
        approxCurve = new MatOfPoint2f();

        Imgproc.approxPolyDP(contour.getMat2f(), approxCurve, epsilon, closed);
        return approxCurve;
    }

    public MatOfPoint2f getApproxPolyDpConvex(double epsilon, boolean closed) {
        approxCurve.release();
        approxCurve = new MatOfPoint2f();

        Imgproc.approxPolyDP(contour.getConvexHull(), approxCurve, epsilon, closed);
        return approxCurve;
    }

    boolean approxPolyMatchesShape() {
        var pointList = approxCurve.toList();

        // TODO: @Matt
        switch (shape) {
            case Custom:
                break;
            case Circle:
                break;
            case Triangle:
                break;
            case Quadrilateral:
                break;
        }
        return true;
    }
}
