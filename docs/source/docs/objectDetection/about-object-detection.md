# About Object Detection

## How does it work?

PhotonVision supports object detection using neural network accelerator hardware built into Orange Pi 5/5+ coprocessors. Please note that the Orange Pi 5/5+ are the only coprocessors that are currently supported. The Neural Processing Unit, or NPU, is [used by PhotonVision](https://github.com/PhotonVision/rknn_jni/tree/main) to massively accelerate certain math operations like those needed for running ML-based object detection.

For the 2025 season, PhotonVision ships with a pretrained ALGAE model. A model to detect coral is not currently stable, and interested teams should ask in the Photonvision discord.

## Tracking Objects

Before you get started with object detection, ensure that you have followed the previous sections on installation, wiring, and networking. Next, open the Web UI, go to the top right card, and switch to the “Object Detection” type. You should see a screen similar to the image above.

Models are trained to detect one or more object "classes" (such as cars, stoplights) in an input image. For each detected object, the model outputs a bounding box around where in the image the object is located, what class the object belongs to, and a unitless confidence between 0 and 1.

:::{note}
This model output means that while its fairly easy to say that "this rectangle probably contains an object", we don't have any information about the object's orientation or location. Further math in user code would be required to make estimates about where an object is physically located relative to the camera.
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

:::{warning}
Power users only. This requires some setup, such as obtaining your own dataset and installing various tools. It's additionally advised to have a general knowledge of ML before attempting to train your own model. Additionally, this is not officially supported by Photonvision, and any problems that may arise are not attributable to Photonvision.
:::

Before beginning, it is necessary to install the [rknn-toolkit2](https://github.com/airockchip/rknn-toolkit2). Then, install the relevant [Ultralytics repository](https://github.com/airockchip?tab=repositories&q=yolo&type=&language=&sort=) from this list. After training your model, export it to `rknn`. This will give you an `onnx` file, formatted for conversion. Copy this file to the relevant folder in [rknn_model_zoo](https://github.com/airockchip/rknn_model_zoo), and use the conversion script located there to convert it. If necessary, modify the script to provide the path to your training database for quantization.

## Uploading Custom Models

:::{warning}
PhotonVision currently ONLY supports 640x640 Ultralytics YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 CPUs! Other models require different post-processing code and will NOT work. The model conversion process is also highly particular. Proceed with care.
:::

In the settings, under `Device Control`, there's an option to upload a new object detection model. Naming convention
should be `name-verticalResolution-horizontalResolution-yolovXXX`. The
`name` should only include alphanumeric characters, periods, and underscores. Additionally, the labels
file ought to have the same name as the RKNN file, with `-labels` appended to the end. For
example, if the RKNN file is named `Algae_1.03.2025-640-640-yolov5s.rknn`, the labels file should be
named `Algae_1.03.2025-640-640-yolov5s-labels.txt`.
