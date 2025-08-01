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
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.NullModel;
import org.photonvision.vision.objects.ObjectDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;
import jni.NerualNetwork;

public class ObjectDetectionPipe
    extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, ObjectDetectionPipe.ObjectDetectionPipeParams>
    implements Releasable {
  // private ObjectDetector detector;
  private long model;

  public ObjectDetectionPipe() {
    model = NerualNetwork.initModel("models/yolo11n.engine");
  }

  @Override
  protected List<NeuralNetworkPipeResult> process(CVMat in) {
    // Check if the model has changed
    Mat frame = in.getMat();
    if (frame.empty()) {
      System.exit(0);
      return List.of();
    }
    float[] detections = NerualNetwork.runModel(model, frame.nativeObj);
    ArrayList<NeuralNetworkPipeResult> output = new ArrayList<>();
    for (int i = 0; i < 10; i++){
      if (detections[i * 6 + 5] != 0){
        output.add(new NeuralNetworkPipeResult(new Rect2d(new Point(detections[i * 6 + 0], detections[i * 6 + 1]), new Point(detections[i * 6 + 2], detections[i * 6 + 3])), 10, detections[i * 6 + 4]));
      }
    }

    System.out.println(detections);
    System.out.println("giving dumb result");
    return output;

  }

  public static record ObjectDetectionPipeParams(double confidence, double nms, Model model) {
  }

  public List<String> getClassNames() {
    return new ArrayList<String>();
  }

  @Override
  public void release() {
  }
}
