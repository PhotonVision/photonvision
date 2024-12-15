/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

 package org.photonvision.vision.pipe.impl;

 import java.util.ArrayList;
 import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TrackedTarget;
 
 /**
  * A pipe that offsets the coordinates of TrackedTargets (from a ColoredShapePipeline)
  * back into the original (uncropped) image coordinate system.
  */
 public class UncropColoredShapePipe
         extends CVPipe<List<Contour>, List<Contour>, Rect> {
 
     public UncropColoredShapePipe(int width, int height) {
         this.params = new Rect(0, 0, width, height);
     }
 
     @Override
     protected List<Contour> process(List<Contour> in) {
         List<Contour> uncroppedTargets = new ArrayList<>();
        
        for (Contour target : in) {
            
            uncroppedTargets.add(
                new Contour(new MatOfPoint(target.mat.adjustROI(params.height,-params.height,params.width,-params.width)))
                );
        }
        
 
         return uncroppedTargets;
     }
 }
 