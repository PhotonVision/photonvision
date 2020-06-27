package org.photonvision.common.vision.pipe.impl;

import org.photonvision.common.vision.opencv.Contour;
import org.photonvision.common.vision.pipe.CVPipe;

import java.util.ArrayList;
import java.util.List;

public class SpeckleRejectPipe
        extends CVPipe<List<Contour>, List<Contour>, SpeckleRejectPipe.SpeckleRejectParams> {

    private List<Contour> m_despeckledContours = new ArrayList<>();

    @Override
    protected List<Contour> process(List<Contour> in) {
        for (var c : m_despeckledContours) {
            c.mat.release();
        }
        m_despeckledContours.clear();

        if (in.size() > 0) {
            double averageArea = 0.0;
            for (Contour c : in) {
                averageArea += c.getArea();
            }
            averageArea /= in.size();

            double minAllowedArea = params.getMinPercentOfAvg() / 100.0 * averageArea;
            for (Contour c : in) {
                if (c.getArea() >= minAllowedArea) {
                    m_despeckledContours.add(c);
                }
            }
        }

        return m_despeckledContours;
    }

    public static class SpeckleRejectParams {
        private double m_minPercentOfAvg;

        public SpeckleRejectParams(double minPercentOfAvg) {
            m_minPercentOfAvg = minPercentOfAvg;
        }

        public double getMinPercentOfAvg() {
            return m_minPercentOfAvg;
        }
    }
}
