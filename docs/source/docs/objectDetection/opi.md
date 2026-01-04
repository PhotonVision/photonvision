# Orange Pi 5 (and variants) Object Detection

## How it works

PhotonVision runs object detection on the Orange Pi 5 by use of the RKNN model architecture, and [this JNI code](https://github.com/PhotonVision/rknn_jni).

## Supported models

PhotonVision currently ONLY supports 640x640 Ultralytics YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 SOCs! Other models require different post-processing code and will NOT work.

## Converting Custom Models

:::{warning}
Only quantized models are supported, so take care when exporting to select the option for quantization.
:::

PhotonVision now ships with a {{ '[Python Notebook](https://github.com/PhotonVision/photonvision/blob/{}/scripts/rknn_conversion.ipynb)'.format(git_tag_ref) }} that you can use in [Google Colab](https://colab.research.google.com) or in a local **Linux** environment (since `rknn-toolkit2` only supports Linux). In Google Colab, you can simply paste the PhotonVision GitHub URL into the "GitHub" tab and select the `rknn_conversion.ipynb` notebook without needing to manually download anything.

Please ensure that the model you are attempting to convert is among the {ref}`supported models <docs/objectDetection/opi:Supported Models>` and using the PyTorch format.
