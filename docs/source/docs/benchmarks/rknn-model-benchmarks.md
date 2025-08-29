# RKNN Benchmarks

## Description
This benchmark compares the performance of four object detection models: YOLOv5, YOLOv5u, YOLOv8, and YOLOv11 on the [COCO 2017 Validation Set](http://images.cocodataset.org/zips/val2017.zip). The main purpose is to assess and compare the inference speed and detection accuracy of these models when deployed on the Orange Pi devices using the RKNN framework and int8 quantization.

## Methodology
- **Dataset**: [COCO 2017 Validation Set](http://images.cocodataset.org/zips/val2017.zip) (5,000 images)

- **Platform**: Orange Pi 5 with RK3588

- **Quantization**: int8 using 20 randomly selected images from the validation set

- **Framework**: RKNN Toolkit 2

## Operator-Level Benchmark Results

The following tables break down the average CPU time, NPU time, and total execution time (in microseconds) for each operator used by the models. Each value represents the mean ± standard deviation across 5,000 inferences.

### YOLOv5

| OpType          | CPU Time (μs)       | NPU Time (μs)         | Total Time (μs)        | Time Ratio (%)      | Number of Times Called |
|-----------------|---------------------|----------------------|-----------------------|---------------------|-----------------------|
| ConvExSwish     | 0.00 ± 0.00         | 10968.81 ± 1126.00   | 10968.81 ± 1126.00    | 73.06 ± 0.94        | 57                    |
| ConvSigmoid     | 0.00 ± 0.00         | 1243.49 ± 67.66      | 1243.49 ± 67.66       | 8.33 ± 0.57         | 3                     |
| Concat          | 0.00 ± 0.00         | 1080.68 ± 259.40     | 1080.68 ± 259.40      | 7.09 ± 0.87         | 13                    |
| Conv            | 0.00 ± 0.00         | 732.15 ± 29.42       | 732.15 ± 29.42        | 4.92 ± 0.42         | 1                     |
| Add             | 0.00 ± 0.00         | 473.71 ± 131.48      | 473.71 ± 131.48       | 3.10 ± 0.50         | 7                     |
| MaxPool         | 0.00 ± 0.00         | 272.40 ± 110.52      | 272.40 ± 110.52       | 1.76 ± 0.51         | 6                     |
| Resize          | 0.00 ± 0.00         | 147.61 ± 38.89       | 147.61 ± 38.89        | 0.97 ± 0.15         | 2                     |
| OutputOperator  | 106.60 ± 15.00      | 0.00 ± 0.00          | 106.60 ± 15.00        | 0.72 ± 0.13         | 3                     |
| InputOperator   | 8.64 ± 1.79         | 0.00 ± 0.00          | 8.64 ± 1.79           | 0.06 ± 0.02         | 1                     |
| **Total**       | **115.24 ± 16.16**  | **14918.85 ± 1735.45**| **15034.09 ± 1734.28**|                     | **93**                |

### YOLOv5u

| OpType          | CPU Time (μs)       | NPU Time (μs)         | Total Time (μs)        | Time Ratio (%)      | Number of Times Called |
|-----------------|---------------------|----------------------|-----------------------|---------------------|-----------------------|
| ConvExSwish     | 0.00 ± 0.00         | 16828.24 ± 1332.73   | 16828.24 ± 1332.73    | 83.04 ± 1.61        | 69                    |
| Concat          | 0.00 ± 0.00         | 1265.94 ± 250.24     | 1265.94 ± 250.24      | 6.17 ± 0.69         | 13                    |
| ConvSigmoid     | 0.00 ± 0.00         | 613.88 ± 62.97       | 613.88 ± 62.97        | 3.03 ± 0.15         | 3                     |
| Add             | 0.00 ± 0.00         | 553.75 ± 131.17      | 553.75 ± 131.17       | 2.69 ± 0.44         | 7                     |
| Conv            | 0.00 ± 0.00         | 298.61 ± 72.72       | 298.61 ± 72.72        | 1.45 ± 0.25         | 3                     |
| ConvClip        | 0.00 ± 0.00         | 256.02 ± 64.48       | 256.02 ± 64.48        | 1.24 ± 0.23         | 3                     |
| MaxPool         | 0.00 ± 0.00         | 178.68 ± 58.72       | 178.68 ± 58.72        | 0.86 ± 0.23         | 3                     |
| Resize          | 0.00 ± 0.00         | 170.87 ± 40.14       | 170.87 ± 40.14        | 0.83 ± 0.13         | 2                     |
| OutputOperator  | 126.89 ± 16.53      | 0.00 ± 0.00          | 126.89 ± 16.53        | 0.63 ± 0.10         | 9                     |
| InputOperator   | 8.69 ± 1.45         | 0.00 ± 0.00          | 8.69 ± 1.45           | 0.04 ± 0.01         | 1                     |
| **Total**       | **135.57 ± 17.51**  | **20165.99 ± 1963.70**| **20301.56 ± 1965.88**|                     | **113**               |

### YOLOv8

| OpType          | CPU Time (μs)       | NPU Time (μs)         | Total Time (μs)        | Time Ratio (%)      | Number of Times Called |
|-----------------|---------------------|----------------------|-----------------------|---------------------|-----------------------|
| ConvExSwish     | 0.00 ± 0.00         | 13017.04 ± 1165.76   | 13017.04 ± 1165.76    | 75.66 ± 1.96        | 57                    |
| Concat          | 0.00 ± 0.00         | 1489.94 ± 257.22     | 1489.94 ± 257.22      | 8.58 ± 0.53         | 13                    |
| Split           | 0.00 ± 0.00         | 681.47 ± 166.62      | 681.47 ± 166.62       | 3.89 ± 0.53         | 8                     |
| ConvSigmoid     | 0.00 ± 0.00         | 596.08 ± 75.01       | 596.08 ± 75.01        | 3.45 ± 0.18         | 3                     |
| Add             | 0.00 ± 0.00         | 443.60 ± 118.05      | 443.60 ± 118.05       | 2.53 ± 0.41         | 6                     |
| Conv            | 0.00 ± 0.00         | 269.61 ± 78.65       | 269.61 ± 78.65        | 1.54 ± 0.30         | 3                     |
| Resize          | 0.00 ± 0.00         | 236.79 ± 37.74       | 236.79 ± 37.74        | 1.37 ± 0.08         | 2                     |
| ConvClip        | 0.00 ± 0.00         | 231.82 ± 68.44       | 231.82 ± 68.44        | 1.32 ± 0.27         | 3                     |
| MaxPool         | 0.00 ± 0.00         | 156.85 ± 56.94       | 156.85 ± 56.94        | 0.89 ± 0.23         | 3                     |
| OutputOperator  | 124.86 ± 20.74      | 0.00 ± 0.00          | 124.86 ± 20.74        | 0.73 ± 0.15         | 9                     |
| InputOperator   | 8.47 ± 1.66         | 0.00 ± 0.00          | 8.47 ± 1.66           | 0.05 ± 0.01         | 1                     |
| **Total**       | **133.33 ± 21.95**  | **17123.19 ± 1985.72**| **17256.52 ± 1986.77**  |                     | **108**               |

---

### YOLOv11

| OpType          | CPU Time (μs)       | NPU Time (μs)         | Total Time (μs)        | Time Ratio (%)      | Number of Times Called |
|-----------------|---------------------|----------------------|-----------------------|---------------------|-----------------------|
| ConvExSwish     | 0.00 ± 0.00         | 16034.00 ± 1331.95   | 16034.00 ± 1331.95    | 69.90 ± 1.55        | 77                    |
| Concat          | 0.00 ± 0.00         | 1888.89 ± 293.99     | 1888.89 ± 293.99      | 8.17 ± 0.51         | 17                    |
| exSDPAttention  | 0.00 ± 0.00         | 1210.88 ± 17.73      | 1210.88 ± 17.73       | 5.32 ± 0.52         | 1                     |
| Split           | 0.00 ± 0.00         | 908.30 ± 183.92      | 908.30 ± 183.92       | 3.91 ± 0.45         | 10                    |
| Add             | 0.00 ± 0.00         | 871.64 ± 212.79      | 871.64 ± 212.79       | 3.73 ± 0.60         | 12                    |
| ConvSigmoid     | 0.00 ± 0.00         | 617.61 ± 59.61       | 617.61 ± 59.61        | 2.69 ± 0.16         | 3                     |
| Conv            | 0.00 ± 0.00         | 419.72 ± 89.88       | 419.72 ± 89.88        | 1.80 ± 0.24         | 5                     |
| Resize          | 0.00 ± 0.00         | 272.09 ± 49.91       | 272.09 ± 49.91        | 1.18 ± 0.12         | 2                     |
| ConvClip        | 0.00 ± 0.00         | 260.08 ± 59.12       | 260.08 ± 59.12        | 1.12 ± 0.18         | 3                     |
| MaxPool         | 0.00 ± 0.00         | 181.93 ± 53.32       | 181.93 ± 53.32        | 0.78 ± 0.18         | 3                     |
| OutputOperator  | 131.48 ± 22.93      | 0.00 ± 0.00          | 131.48 ± 22.93        | 0.58 ± 0.12         | 9                     |
| ConvAdd         | 0.00 ± 0.00         | 126.79 ± 35.28       | 126.79 ± 35.28        | 0.54 ± 0.11         | 2                     |
| Reshape         | 0.00 ± 0.00         | 56.61 ± 18.03        | 56.61 ± 18.03         | 0.24 ± 0.06         | 3                     |
| InputOperator   | 8.66 ± 1.59         | 0.00 ± 0.00          | 8.66 ± 1.59           | 0.04 ± 0.01         | 1                     |
| **Total**       | **140.14 ± 24.26**  | **22848.54 ± 2351.95**| **22988.68 ± 2355.97**|                     | **148**               |


## Model Summary and Accuracy Metrics

The table below summarizes the mean average precision (mAP) and total inference time for each model. These metrics provide a high-level view of how each model performs in terms of both detection accuracy and runtime efficiency.

### Mean Average Precision (mAP) by Model

| Metric | YOLOv5     | YOLOv5u    | YOLOv8     | YOLOv11    |
|--------|------------|------------|------------|------------|
| **mAP**    | 0.2243     | 0.2745     | 0.3051     | 0.3251     |
| **mAP50**  | 0.3538     | 0.3834     | 0.4145     | 0.4406     |
| **mAP75**  | 0.2432     | 0.2997     | 0.3349     | 0.3568     |
| **mAP85**  | 0.3054     | 0.3472     | 0.3867     | 0.4068     |
| **mAP95**  | 0.3708     | 0.4822     | 0.5483     | 0.5858     |

### Model Execution Time and Call Frequency

| Model   | Total Time (μs)         | Number of Processing Calls |
|---------|------------------------|----------------------------|
| **YOLOv5**  | 15034.09 ± 1734.28 | 93                         |
| **YOLOv5u** | 20301.56 ± 1965.88 | 113                        |
| **YOLOv8**  | 17256.52 ± 1986.77 | 108                        |
| **YOLOv11** | 22988.68 ± 2355.97 | 148                        |

## Conclusion

The benchmark reveals a clear performance trade-off between inference time and detection accuracy:

- **YOLOv5** is the fastest model with the lowest total inference time, making it well-suited for situations where speed is more important than high detection precision.

- **YOLOv11** achieves the highest accuracy (mAP) across all IoU thresholds but comes with the longest inference time, which may limit its use in real-time applications.

- **YOLOv8** offers a strong balance between speed and accuracy, making it a practical choice when both factors matter.

- **YOLOv5u** improves accuracy compared to YOLOv5 but falls behind YOLOv8 in both speed and detection quality.

When choosing a model for edge devices like the Orange Pi 5, it’s important to weigh how much latency your system can tolerate versus how much accuracy you need. A faster model may give quicker results, while a more accurate one may offer better detection reliability, but at the cost of speed.
