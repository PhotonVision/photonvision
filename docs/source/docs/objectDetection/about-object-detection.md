# About Object Detection

## How does it work?

PhotonVision supports object detection using neural network accelerator hardware built into Orange Pi 5/5+ coprocessors. The Neural Processing Unit, or NPU, is [used by PhotonVision](https://github.com/PhotonVision/rknn_jni/tree/main) to massively accelerate certain math operations like those needed for running ML-based object detection.

For the 2024 season, PhotonVision ships with a **pre-trained NOTE detector** (shown above), as well as a mechanism for swapping in custom models. Future development will focus on enabling lower friction management of multiple custom models.

```{image} images/notes-ui.png
```

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

## Training Custom Models

Coming soon!

## Uploading Custom Models

:::{warning}
PhotonVision currently ONLY supports YOLOv5 models trained and converted to `.rknn` format for RK3588 CPUs! Other models require different post-processing code and will NOT work. The model conversion process is also highly particular. Proceed with care.
:::

Our [pre-trained NOTE model](https://github.com/PhotonVision/photonvision/blob/master/photon-server/src/main/resources/models/note-640-640-yolov5s.rknn) is automatically extracted from the JAR when PhotonVision starts, only if a file named “note-640-640-yolov5s.rknn” and "labels.txt" does not exist in the folder `photonvision_config/models/`. This technically allows power users to replace the model and label files with new ones without rebuilding Photon from source and uploading a new JAR.

Use a program like WinSCP or FileZilla to access your coprocessor's filesystem, and copy the new `.rknn` model file into /home/pi. Next, SSH into the coprocessor and `sudo mv /path/to/new/model.rknn /opt/photonvision/photonvision_config/models/note-640-640-yolov5s.rknn`. Repeat this process with the labels file, which should contain one line per label the model outputs with no training newline. Next, restart PhotonVision via the web UI.
