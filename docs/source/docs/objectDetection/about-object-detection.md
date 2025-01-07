# About Object Detection

## How does it work?

PhotonVision supports object detection using neural network accelerator hardware built into Orange Pi 5/5+ coprocessors. The Neural Processing Unit, or NPU, is [used by PhotonVision](https://github.com/PhotonVision/rknn_jni/tree/main) to massively accelerate certain math operations like those needed for running ML-based object detection.

For the 2025 season, PhotonVision does not currently ship with a pre-trained detector.  If teams are interested in using object detection, they can follow the custom process outlined {ref}`below <docs/objectDetection/about-object-detection:Uploading Custom Models>`.

## Tracking Objects

Before you get started with object detection, ensure that you have followed the previous sections on installation, wiring, and networking. Next, open the Web UI, go to the top right card, and switch to the “Object Detection” type. You should see a screen similar to the image above.

PhotonVision currently ships with a NOTE detector based on a [YOLOv5 model](https://docs.ultralytics.com/yolov5/). This model is trained to detect one or more object "classes" (such as cars, stoplights, or in our case, NOTES) in an input image. For each detected object, the model outputs a bounding box around where in the image the object is located, what class the object belongs to, and a unitless confidence between 0 and 1.

:::{note}
This model output means that while its fairly easy to say that "this rectangle probably contains a NOTE", we don't have any information about the NOTE's orientation or location. Further math in user code would be required to make estimates about where an object is physically located relative to the camera.
:::

## Tuning and Filtering

Compared to other pipelines, object detection exposes very few tuning handles. The Confidence slider changes the minimum confidence that the model needs to have in a given detection to consider it valid, as a number between 0 and 1 (with 0 meaning completely uncertain and 1 meaning maximally certain).

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/objdetectFiltering.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

The same area, aspect ratio, and target orientation/sort parameters from {ref}`reflective pipelines <docs/reflectiveAndShape/contour-filtering:Reflective>` are also exposed in the object detection card.

## Letterboxing

Photonvision will letterbox your camera frame to 640x640. This means that if you select a resolution that is larger than 640 it will be scaled down to fit inside a 640x640 frame with black bars if needed. Smaller frames will be scaled up with black bars if needed.

## Training Custom Models

Coming soon!

## Uploading Custom Models

:::{warning}
PhotonVision currently ONLY supports YOLOv5 models trained and converted to `.rknn` format for RK3588 CPUs! Other models require different post-processing code and will NOT work. The model conversion process is also highly particular. Proceed with care.
:::

In the settings, under ``Device Control``, there's an option to upload a new object detection model. When uploading the files, ensure that that the labels file has the same name as the RKNN file, with ``-labels`` appended to the end. For example, if the RKNN file is named ``foo.rknn``, the labels file should be named ``foo-labels.txt``. The labels file should contain one line per label the model outputs with no training newline.
