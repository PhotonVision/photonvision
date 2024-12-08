package org.photonvision.vision.pipe.impl;

import org.opencv.core.Rect;
import org.photonvision.vision.apriltag.AprilTagFamily;

import edu.wpi.first.apriltag.AprilTagDetector;

public class CropPipeParams {
     public Rect staticCrop;
     public Rect dynamicCrop;
     
 
     public CropPipeParams(Rect staticRect, Rect dynamiRect) {
         this.staticCrop = staticRect;
         this.dynamicCrop = dynamiRect;
         
     }
 
     @Override
     public int hashCode() {
         return 0;
     }
 
     @Override
     public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         CropPipeParams other = (CropPipeParams) obj;
         return staticCrop.equals(other.staticCrop) &&
                dynamicCrop.equals(other.dynamicCrop);
         
     }
}
