#include "test.h"

#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs.hpp>

#include <stdio.h>

int main() {
    
    cv::Mat mat = cv::imread("/home/matt/Documents/GitHub/photonvision/test-resources/testimages/2022/WPI/FarLaunchpad13ft10in.png");

    printf("DevMain! mat size %i %i\n", mat.rows, mat.cols);

    return 1;
}