package org.photonvision.vision.apriltag;

import java.util.Objects;

public class AprilTagDetectorParams {
  public static AprilTagDetectorParams DEFAULT_36H11 = new AprilTagDetectorParams(
          "tag36h11", 1.0, 0.0, 1, false, false);

  public final String tagFamily;
  public final double decimate;
  public final double blur;
  public final int threads;
  public final boolean debug;
  public final boolean refineEdges;

  public AprilTagDetectorParams(String tagFamily, double decimate, double blur, int threads, boolean debug,
                                boolean refineEdges) {
    this.tagFamily = tagFamily;
    this.decimate = decimate;
    this.blur = blur;
    this.threads = threads;
    this.debug = debug;
    this.refineEdges = refineEdges;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AprilTagDetectorParams that = (AprilTagDetectorParams) o;
    return Objects.equals(tagFamily, that.tagFamily)
            && Double.compare(decimate, that.decimate) == 0
            && Double.compare(blur, that.blur) == 0
            && threads == that.threads
            && debug == that.debug
            && refineEdges == that.refineEdges;
  }

}