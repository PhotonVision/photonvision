# Rubik Pi 3 Object Detection

## How it works

PhotonVision runs object detection on the Rubik Pi 3 by use of [TensorflowLite](https://github.com/tensorflow/tensorflow), and [this JNI code](https://github.com/PhotonVision/rubik_jni).

## Supported models

PhotonVision currently ONLY supports 640x640 Ultralytics YOLOv8 and YOLOv11 models trained and converted to `.tflite` format for QCS6490 SOCs! Other models require different post-processing code and will NOT work.

## Converting Custom Models

:::{warning}
Only quantized models are supported, so take care when exporting to select the option for quantization.
:::

PhotonVision now ships with a [Python Notebook](https://github.com/PhotonVision/photonvision/blob/main/scripts/rubik_conversion.ipynb) that you can use in [Google Colab](https://colab.research.google.com) or in a local environment. In Google Colab, you can simply paste the PhotonVision GitHub URL into the "GitHub" tab and select the `rubik_conversion.ipynb` notebook without needing to manually download anything.

Please ensure that the model you are attempting to convert is among the {ref}`supported models <docs/objectDetection/rubik:Supported Models>` and using the PyTorch format.
