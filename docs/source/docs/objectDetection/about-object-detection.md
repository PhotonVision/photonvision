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

PhotonVision now ships with a [Python Notebook](https://github.com/PhotonVision/photonvision/blob/main/scripts/rknn-convert-tool/rknn_conversion.ipynb) that you can use in [Google Colab](https://colab.research.google.com) or in a local environment. In Google Colab, you can simply paste the PhotonVision GitHub URL into the "GitHub" tab and select the `rknn_conversion.ipynb` notebook without needing to manually download anything.

The notebook walks you through downloading and using the necessary scripts for RKNN conversion, making it easy to convert your model. While the scripts are used directly within the notebook with the full setup provided, advanced users may also choose to run them independently outside the notebook.

:::{note}
Running the scripts outside the notebook is entirely optional and intended for users who prefer working in their own environment without the notebook.
:::

## Managing Custom Models

:::{warning}
PhotonVision currently ONLY supports 640x640 Ultralytics YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 CPUs! Other models require different post-processing code and will NOT work. The model conversion process is also highly particular. Proceed with care.
:::

:::{warning}
Non-quantized models are not supported! If you have the option, make sure quantization is enabled when exporting to .rknn format. This will represent the weights and activations of the model as 8-bit integers, instead of 32-bit floats which PhotonVision doesn't support. Quantized models are also much faster for a negligible loss in accuracy.
:::

Custom models can now be managed from the Object Detection tab in settings. You can upload a custom model by clicking the "Upload Model" button, selecting your `.rknn` file, and filling out the property fields. Models can also be exported, both individually and in bulk. Models exported in bulk can be imported using the `import bulk` button. Models exported individually must be re-imported as an individual model, and all the relevant metadata is stored in the filename of the model.
