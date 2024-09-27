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

## Letterboxing

Photonvision will letterbox your camera frame to 640x640. This means that if you select a resolution that is larger than 640 it will be scaled down to fit inside a 640x640 frame with black bars if needed. Smaller frames will be scaled up with black bars if needed.

## Training Custom Models

You can build your own YOLOv5 model for use with PhotonVision's Object Detection. However, this process is complex and requires a good understanding of machine learning, but it can be done. Here's an overview of the process.

### Step 1: Model Training

You can train the Object Detection model using [Ultralytics' YOLOv5 repository](https://github.com/ultralytics/yolov5), then use [airockchip's YOLOv5 fork](https://github.com/airockchip/yolov5) to export the model currectly before the export to a deployable RKNN file. The specific training setup will vary depending on your dataset and configuration, but here’s an example using `yolov5s`. Other models should work as well. You **will** need [PyTorch](https://pytorch.org/get-started/locally/). This example has been tested on Ubuntu 22.04.

#### Downloading nessessary files

```bash
git clone https://github.com/ultralytics/yolov5.git
git clone https://github.com/airockchip/yolov5.git airockchip-yolov5
wget https://gist.githubusercontent.com/Alex-idk/9a512ca7bd263892ff6991a856f1a458/raw/e8c0c9d8d5a1a60a2bbe72c065e04a261300baac/onnx2rknn.py # This is the onnx to rknn convertion script
```

#### Training Command

Please research what each of these parameters do and adjust them to fit your dataset and training needs. Make sure to change the number of classes in the `models/yolov5s.yaml` file to how many classes are in your dataset, otherwise you will run into problems with class labeling. Currently as of `September 2024` only YOLOv5s models have been tested.

```bash
python train.py --img 640 --batch 16 --epochs 10 --data path/to/dataset/data.yaml --cfg 'models/yolov5s.yaml' --weights '' --cache
```

### Step 2: Exporting the Model to ONNX

Once your model is trained, the next step is to export it to ONNX format using airockchip's YOLOv5 fork.

#### Export Command

```bash
cd /path/to/airockchip-yolov5 && python export.py --weights '/path/to/best.pt' --rknpu --include 'onnx'
```

### Step 3: Converting ONNX to RKNN

Using the `onnx2rknn.py` script, convert the ONNX model to an RKNN file. This script was downloaded in a previous step.

#### Conversion Command

Run the script, passing in the ONNX model and a text file containing paths to images from your dataset:

```bash
python onnx2rknn.py /path/to/best.onnx /path/to/export/best.rknn /path/to/imagePaths.txt
```

If you have any questions about this process feel free to mention `alex_idk` in the PhotonVision Discord server.

## Uploading Custom Models

:::{warning}
PhotonVision currently ONLY supports YOLOv5 models trained and converted to `.rknn` format for RK3588 CPUs! Other models require different post-processing code and will NOT work. The model conversion process is also highly particular. Proceed with care.
:::

Our [pre-trained NOTE model](https://github.com/PhotonVision/photonvision/blob/master/photon-server/src/main/resources/models/note-640-640-yolov5s.rknn) is automatically extracted from the JAR when PhotonVision starts, only if a file named “note-640-640-yolov5s.rknn” and "labels.txt" does not exist in the folder `photonvision_config/models/`. This technically allows power users to replace the model and label files with new ones without rebuilding Photon from source and uploading a new JAR.

Use a program like WinSCP or FileZilla to access your coprocessor's filesystem, and copy the new `.rknn` model file into /home/pi. Next, SSH into the coprocessor and `sudo mv /path/to/new/model.rknn /opt/photonvision/photonvision_config/models/note-640-640-yolov5s.rknn`. Repeat this process with the labels file, which should contain one line per label the model outputs with no training newline. Next, restart PhotonVision via the web UI.
