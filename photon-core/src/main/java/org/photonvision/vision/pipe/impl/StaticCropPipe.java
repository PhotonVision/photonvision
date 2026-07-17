package org.photonvision.vision.pipe.impl;

import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;

public class StaticCropPipe extends CVPipe<CVMat, CVMat, Rect> {
    private Rect cropArea = null;

    public StaticCropPipe() {
        super();
    }

    @Override
    protected CVMat process(CVMat in) {
        if (in.getMat().empty()) {
            return null;
        }

        if (cropArea == null) {
            return null;
        }

        return new CVMat(in.getMat().submat(cropArea));
    }

    @Override
    public void setParams(Rect cropArea) {
        if (this.cropArea == null || !this.cropArea.equals(cropArea)) {
            this.cropArea = cropArea;
        }

        super.setParams(cropArea);
    }
}
