## RKNN Benchmark

### Description
This benchmark compares the performance of four object detection models—YOLOv5, YOLOv5u, YOLOv8, and YOLOv11—on the [COCO 2017 Validation Set](http://images.cocodataset.org/zips/val2017.zip). The primary goal is to evaluate and compare their inference speeds and detection accuracy when deployed on the Orange Pi 5 with RK3588 using int8 quantization.

### Methodology
- **Dataset**: [COCO 2017 Validation Set](http://images.cocodataset.org/zips/val2017.zip) (5,000 images)

- **Platform**: Orange Pi 5 with RK3588

- **Quantization**: int8 using 20 randomly selected images from the validation set

- **Framework**: RKNN Toolkit 2

- All models were benchmarked using consistent preprocessing and runtime environments to ensure fair comparison.

### Results

#### YOLOv5

| OpType          | CPU Time (μs)           | NPU Time (μs)            | Total Time (μs)           | Time Ratio (%)         | Number of Times Called |
|----------------|-------------------------|---------------------------|----------------------------|-------------------------|-------------|
| ConvExSwish    | 0.0000 ± 0.0000         | 10968.8144 ± 1125.9973    | 10968.8144 ± 1125.9973     | 73.0624 ± 0.9429        | 57          |
| ConvSigmoid    | 0.0000 ± 0.0000         | 1243.4860 ± 67.6628       | 1243.4860 ± 67.6628        | 8.3324 ± 0.5668         | 3           |
| Concat         | 0.0000 ± 0.0000         | 1080.6754 ± 259.4036      | 1080.6754 ± 259.4036       | 7.0912 ± 0.8707         | 13          |
| Conv           | 0.0000 ± 0.0000         | 732.1466 ± 29.4241        | 732.1466 ± 29.4241         | 4.9159 ± 0.4179         | 1           |
| Add            | 0.0000 ± 0.0000         | 473.7118 ± 131.4796       | 473.7118 ± 131.4796        | 3.0972 ± 0.5030         | 7           |
| MaxPool        | 0.0000 ± 0.0000         | 272.4042 ± 110.5195       | 272.4042 ± 110.5195        | 1.7567 ± 0.5131         | 6           |
| Resize         | 0.0000 ± 0.0000         | 147.6092 ± 38.8864        | 147.6092 ± 38.8864         | 0.9666 ± 0.1450         | 2           |
| OutputOperator | 106.6022 ± 15.0000      | 0.0000 ± 0.0000           | 106.6022 ± 15.0000         | 0.7190 ± 0.1313         | 3           |
| InputOperator  | 8.6406 ± 1.7897         | 0.0000 ± 0.0000           | 8.6406 ± 1.7897            | 0.0596 ± 0.0150         | 1           |
| **Total**      | **115.2428 ± 16.1637**  | **14918.8476 ± 1735.4529**| **15034.0904 ± 1734.2830** |                         | **93**       |

#### YOLOv5u
| OpType          | CPU Time (μs)           | NPU Time (μs)            | Total Time (μs)           | Time Ratio (%)         | Number of Times Called |
|----------------|-------------------------|---------------------------|----------------------------|-------------------------|-------------|
| ConvExSwish    | 0.0000 ± 0.0000         | 16828.2438 ± 1332.7291    | 16828.2438 ± 1332.7291     | 83.0440 ± 1.6108        | 69          |
| Concat         | 0.0000 ± 0.0000         | 1265.9360 ± 250.2417      | 1265.9360 ± 250.2417       | 6.1722 ± 0.6867         | 13          |
| ConvSigmoid    | 0.0000 ± 0.0000         | 613.8768 ± 62.9660        | 613.8768 ± 62.9660         | 3.0258 ± 0.1510         | 3           |
| Add            | 0.0000 ± 0.0000         | 553.7464 ± 131.1744       | 553.7464 ± 131.1744        | 2.6927 ± 0.4369         | 7           |
| Conv           | 0.0000 ± 0.0000         | 298.6142 ± 72.7158        | 298.6142 ± 72.7158         | 1.4536 ± 0.2530         | 3           |
| ConvClip       | 0.0000 ± 0.0000         | 256.0216 ± 64.4750        | 256.0216 ± 64.4750         | 1.2446 ± 0.2283         | 3           |
| MaxPool        | 0.0000 ± 0.0000         | 178.6820 ± 58.7247        | 178.6820 ± 58.7247         | 0.8625 ± 0.2261         | 3           |
| Resize         | 0.0000 ± 0.0000         | 170.8654 ± 40.1374        | 170.8654 ± 40.1374         | 0.8311 ± 0.1313         | 2           |
| OutputOperator | 126.8850 ± 16.5305      | 0.0000 ± 0.0000           | 126.8850 ± 16.5305         | 0.6303 ± 0.1023         | 9           |
| InputOperator  | 8.6872 ± 1.4511         | 0.0000 ± 0.0000           | 8.6872 ± 1.4511            | 0.0432 ± 0.0084         | 1           |
| **Total**      | **135.5722 ± 17.5109**  | **20165.9862 ± 1963.7014**| **20301.5584 ± 1965.8794** |                         | **113**     |

### YOLOv8

| OpType          | CPUTime (μs)           | NPUTime (μs)               | TotalTime (μs)             | TimeRatio (%)    | Number of Times Called |
|-----------------|------------------------|----------------------------|----------------------------|------------------|------------|
| ConvExSwish     | 0.0000 ± 0.0000        | 13017.0354 ± 1165.7590     | 13017.0354 ± 1165.7590     | 75.6553 ± 1.9562 | 57         |
| Concat          | 0.0000 ± 0.0000        | 1489.9438 ± 257.2214       | 1489.9438 ± 257.2214       | 8.5790 ± 0.5314  | 13         |
| Split           | 0.0000 ± 0.0000        | 681.4716 ± 166.6193        | 681.4716 ± 166.6193        | 3.8923 ± 0.5252  | 8          |
| ConvSigmoid     | 0.0000 ± 0.0000        | 596.0784 ± 75.0117         | 596.0784 ± 75.0117         | 3.4547 ± 0.1831  | 3          |
| Add             | 0.0000 ± 0.0000        | 443.5986 ± 118.0517        | 443.5986 ± 118.0517        | 2.5299 ± 0.4119  | 6          |
| Conv            | 0.0000 ± 0.0000        | 269.6090 ± 78.6457         | 269.6090 ± 78.6457         | 1.5352 ± 0.2962  | 3          |
| Resize          | 0.0000 ± 0.0000        | 236.7884 ± 37.7401         | 236.7884 ± 37.7401         | 1.3662 ± 0.0818  | 2          |
| ConvClip        | 0.0000 ± 0.0000        | 231.8182 ± 68.4416         | 231.8182 ± 68.4416         | 1.3197 ± 0.2663  | 3          |
| MaxPool         | 0.0000 ± 0.0000        | 156.8480 ± 56.9400         | 156.8480 ± 56.9400         | 0.8852 ± 0.2333  | 3          |
| OutputOperator  | 124.8610 ± 20.7386     | 0.0000 ± 0.0000            | 124.8610 ± 20.7386         | 0.7326 ± 0.1464  | 9          |
| InputOperator   | 8.4654 ± 1.6569        | 0.0000 ± 0.0000            | 8.4654 ± 1.6569            | 0.0502 ± 0.0122  | 1          |
| **Total**           | **133.3264 ± 21.9546** | **17123.1914 ± 1985.7238** | **17256.5178 ± 1986.7732** |                  | **108**        |

---

### YOLOv11

| OpType          | CPUTime (μs)           | NPUTime (μs)               | TotalTime (μs)             | TimeRatio (%)    | Number of Times Called |
|-----------------|------------------------|----------------------------|----------------------------|------------------|------------|
| ConvExSwish     | 0.0000 ± 0.0000        | 16034.0000 ± 1331.9527     | 16034.0000 ± 1331.9527     | 69.9019 ± 1.5469 | 77         |
| Concat          | 0.0000 ± 0.0000        | 1888.8872 ± 293.9925       | 1888.8872 ± 293.9925       | 8.1686 ± 0.5069  | 17         |
| exSDPAttention  | 0.0000 ± 0.0000        | 1210.8758 ± 17.7283        | 1210.8758 ± 17.7283        | 5.3206 ± 0.5233  | 1          |
| Split           | 0.0000 ± 0.0000        | 908.3042 ± 183.9160        | 908.3042 ± 183.9160        | 3.9086 ± 0.4507  | 10         |
| Add             | 0.0000 ± 0.0000        | 871.6440 ± 212.7915        | 871.6440 ± 212.7915        | 3.7324 ± 0.6026  | 12         |
| ConvSigmoid     | 0.0000 ± 0.0000        | 617.6050 ± 59.6144         | 617.6050 ± 59.6144         | 2.6932 ± 0.1581  | 3          |
| Conv            | 0.0000 ± 0.0000        | 419.7188 ± 89.8841         | 419.7188 ± 89.8841         | 1.8045 ± 0.2375  | 5          |
| Resize          | 0.0000 ± 0.0000        | 272.0918 ± 49.9142         | 272.0918 ± 49.9142         | 1.1758 ± 0.1240  | 2          |
| ConvClip        | 0.0000 ± 0.0000        | 260.0816 ± 59.1174         | 260.0816 ± 59.1174         | 1.1197 ± 0.1809  | 3          |
| MaxPool         | 0.0000 ± 0.0000        | 181.9318 ± 53.3246         | 181.9318 ± 53.3246         | 0.7768 ± 0.1754  | 3          |
| OutputOperator  | 131.4758 ± 22.9268     | 0.0000 ± 0.0000            | 131.4758 ± 22.9268         | 0.5769 ± 0.1180  | 9          |
| ConvAdd         | 0.0000 ± 0.0000        | 126.7852 ± 35.2763         | 126.7852 ± 35.2763         | 0.5421 ± 0.1132  | 2          |
| Reshape         | 0.0000 ± 0.0000        | 56.6096 ± 18.0317          | 56.6096 ± 18.0317          | 0.2412 ± 0.0613  | 3          |
| InputOperator   | 8.6646 ± 1.5914        | 0.0000 ± 0.0000            | 8.6646 ± 1.5914            | 0.0391 ± 0.0085  | 1          |
| **Total**           | **140.1404 ± 24.2614** | **22848.5350 ± 2351.9458** | **22988.6754 ± 2355.9731** |                  | **148**        |

### Overall Results

mAP of each model:

| Metric | YOLOv5     | YOLOv5u    | YOLOv8     | YOLOv11    |
|--------|------------|------------|------------|------------|
| mAP    | 0.2243     | 0.2745     | 0.3051     | 0.3251     |
| mAP50  | 0.3538     | 0.3834     | 0.4145     | 0.4406     |
| mAP75  | 0.2432     | 0.2997     | 0.3349     | 0.3568     |
| mAP85  | 0.3054     | 0.3472     | 0.3867     | 0.4068     |
| mAP95  | 0.3708     | 0.4822     | 0.5483     | 0.5858     |

Model run times and number of calls:

| Model   | TotalTime (μs)         | Number of Processing Calls |
|---------|------------------------|----------------------------|
| YOLOv5  | 15034.0904 ± 1734.2830 | 93                         |
| YOLOv5u | 20301.5584 ± 1965.8794 | 113                        |
| YOLOv8  | 17256.5178 ± 1986.7732 | 108                        |
| YOLOv11 | 22988.6754 ± 2355.9731 | 148                        |


### Conclusion

The benchmark reveals a clear performance trade-off between inference time and detection accuracy:

- **YOLOv5** is the fastest model with the lowest total inference time, making it ideal for real-time applications where speed is critical but accuracy isn't critical.

- **YOLOv11** achieves the highest mAP across all IoU thresholds, demonstrating improved detection precision at the cost of the longest inference time.

- **YOLOv8** offers a strong balance between speed and accuracy, outperforming YOLOv5u in both metrics.

- **YOLOv5u**, while slightly slower than YOLOv8, still offers a noticeable accuracy boost over the original YOLOv5.

For deployment on edge compute devices like the Orange Pi 5, its best to consider the tradeoffs between speed and accuracy based on your requirements.