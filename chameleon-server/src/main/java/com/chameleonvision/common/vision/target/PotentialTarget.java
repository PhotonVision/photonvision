package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.vision.opencv.Contour;
import java.util.ArrayList;
import java.util.List;

public class PotentialTarget {

    final Contour m_mainContour;
    final List<Contour> m_subContours;

    public PotentialTarget(Contour inputContour) {
        m_mainContour = inputContour;
        m_subContours = new ArrayList<>(); // empty
    }

    public PotentialTarget(Contour inputContour, List<Contour> subContours) {
        m_mainContour = inputContour;
        m_subContours = subContours;
    }
}
