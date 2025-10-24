package org.photonvision.vision.pipe.impl;

import org.opencv.core.Mat;
import org.photonvision.vision.pipe.MutatingPipe;
import org.teamdeadbolts.basler.BaslerJNI;

public class PixelBinPipe extends MutatingPipe<Mat, PixelBinPipe.PixelBinParams> {

    @Override
    protected Void process(Mat in) {
        switch (params.mode) {
            case AVERAGE:
                BaslerJNI.avgBin(in, params.binHorz(), params.binVert());
                break;
            case SUM:
                BaslerJNI.sumBin(in, params.binHorz(), params.binVert());
                break;
            case NONE:
                break;
        }

        return null;
    }

    public static record PixelBinParams(BinMode mode, int binHorz, int binVert) {
        public enum BinMode {
            SUM,
            AVERAGE,
            NONE,
        }
    }
}
