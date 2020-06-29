package org.photonvision.vision.pipeline;

import org.opencv.core.Size;

public class Calibration3dPipelineSettings extends AdvancedPipelineSettings{
    public int boardHeight = 0;
    public int boardWidth= 0;
    public boolean isUsingChessboard = true;
    public double gridSize = 0;

    public Size resolution = new Size(640, 480);

}
