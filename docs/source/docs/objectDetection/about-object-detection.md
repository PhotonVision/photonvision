# About Object Detection

## How does it work?

PhotonVision supports object detection using neural network accelerator hardware, commonly known as an NPU. The two coprocessors currently supported are the {ref}`Orange Pi 5 <docs/objectDetection/opi:Orange Pi 5 (and variants) Object Detection>` and the {ref}`Rubik Pi 3 <docs/objectDetection/rubik:Rubik Pi 3 Object Detection>`.

PhotonVision currently ships with a model trained on the [COCO dataset](https://cocodataset.org/) by [Ultralytics](https://github.com/ultralytics/ultralytics) (this model is licensed under [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html)). This model is meant to be used for testing and other miscellaneous purposes. It is not meant to be used in competition. For the 2025 post-season, PhotonVision also ships with a pretrained ALGAE model. A model to detect coral is available in the PhotonVision discord, but will not be distributed with PhotonVision.

## Tracking Objects

Before you get started with object detection, ensure that you have followed the previous sections on installation, wiring, and networking. Next, open the Web UI, go to the top right card, and switch to the “Object Detection” type. You should see a screen similar to the image above.

Models are trained to detect one or more object "classes" (such as cars, stoplights) in an input image. For each detected object, the model outputs a bounding box around where in the image the object is located, what class the object belongs to, and a unitless confidence between 0 and 1.

:::{note}
This model output means that while its fairly easy to say that "this rectangle probably contains an object", we don't have any information about the object's orientation or location. Further math in user code would be required to make estimates about where an object is physically located relative to the camera.
:::

## Tuning and Filtering

Compared to other pipelines, object detection exposes very few tuning handles. The Confidence slider changes the minimum confidence that the model needs to have in a given detection to consider it valid, as a number between 0 and 1 (with 0 meaning completely uncertain and 1 meaning maximally certain). The Non-Maximum Suppresion (NMS) Threshold slider is used to filter out overlapping detections. Higher values mean more detections are allowed through, but may result in false positives. It's generally recommended that teams leave this set at the default, unless they find they're unable to get usable results with solely the Confidence slider.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/objdetectFiltering.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

The same area, aspect ratio, and target orientation/sort parameters from {ref}`reflective pipelines <docs/reflectiveAndShape/contour-filtering:Reflective>` are also exposed in the object detection card.

## Letterboxing

Photonvision will letterbox your camera frame to 640x640. This means that if you select a resolution that is larger than 640 it will be scaled down to fit inside a 640x640 frame with black bars if needed. Smaller frames will be scaled up with black bars if needed.

It is recommended that you select a resolution that results in the smaller dimension being just greater than, or equal to, 640. Anything above this will not see any increased performance.

## Custom Models

For information regarding converting custom models and supported models for each platform, refer to the page detailing information about your specific coprocessor.

- {ref}`Orange Pi 5 <docs/objectDetection/opi:Orange Pi 5 (and variants) Object Detection>`
- {ref}`Rubik Pi 3 <docs/objectDetection/rubik:Rubik Pi 3 Object Detection>`

### Training Custom Models

PhotonVision does not offer any support for training custom models, only conversion. For information on which models are supported for a given coprocessor, use the links above.

### Managing Custom Models

Custom models can now be managed from the Object Detection tab in settings. You can upload a custom model by clicking the "Upload Model" button, selecting your model file, and filling out the property fields. Models can also be exported, both individually and in bulk. Models exported in bulk can be imported using the `import bulk` button. Models exported individually must be re-imported as an individual model, and all the relevant metadata is stored in the filename of the model.
