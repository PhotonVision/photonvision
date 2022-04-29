package org.photonvision.vision.apriltag;

public enum AprilTagFamily {
    kTag36h11,
    kTag25h9,
    kTag16h5,
    kTagCircle21h7,
    kTagCircle49h12,
    kTagStandard41h12,
    kTagStandard52h13,
    kTagCustom48h11;

    public String getNativeName() {
        // We wanna strip the leading kT and replace with "t"
        return this.name().replaceFirst("kT", "t");
    }
}
