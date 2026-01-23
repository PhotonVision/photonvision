# Rubik Pi 3 Object Detection

## How it works

PhotonVision runs object detection on the Rubik Pi 3 by use of [TensorflowLite](https://github.com/tensorflow/tensorflow), and [this JNI code](https://github.com/PhotonVision/rubik_jni).

## Supported models

PhotonVision currently ONLY supports 640x640 Ultralytics YOLOv8 and YOLOv11 models trained and converted to `.tflite` format for QCS6490 SOCs! Other models require different post-processing code and will NOT work.

## Converting Custom Models

:::{warning}
Only quantized models are supported, so take care when exporting to select the option for quantization.
:::

PhotonVision now ships with a {{ '[Python Notebook](https://github.com/PhotonVision/photonvision/blob/{}/scripts/rubik_conversion.ipynb)'.format(git_tag_ref) }} that you can use in [Google Colab](https://colab.research.google.com), [Kaggle](https://kaggle.com/code), or in a local environment. In Google Colab, you can simply paste the PhotonVision GitHub URL into the "GitHub" tab and select the `rubik_conversion.ipynb` notebook without needing to manually download anything.

Please ensure that the model you are attempting to convert is among the {ref}`supported models <docs/objectDetection/rubik:Supported Models>` and using the PyTorch format.

## Benchmarking

Before you can perform benchmarking, it's necessary to install `tensorflow-lite-qcom-apps` with apt.

By SSHing into your Rubik Pi and running this command, replacing `PATH/TO/MODEL` with the path to your model, `benchmark_model --graph=src/test/resources/yolov8nCoco.tflite --external_delegate_path=/usr/lib/libQnnTFLiteDelegate.so --external_delegate_options=backend_type:htp --external_delegate_options=htp_use_conv_hmx:1 --external_delegate_options=htp_performance_mode:2` you can determine how long it takes for inference to be performed with your model.
