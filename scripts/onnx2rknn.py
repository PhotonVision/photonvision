import argparse
import cv2
import numpy as np
from rknn.api import RKNN
import os

def collect_images_from_directory(directory_path):
    """Collect all image paths from the specified directory."""
    image_extensions = ['.jpg', '.jpeg', '.png']
    image_paths = []

    for root, dirs, files in os.walk(directory_path):
        for file in files:
            if any(file.lower().endswith(ext) for ext in image_extensions):
                image_paths.append(os.path.join(root, file))
    
    return image_paths

def save_dataset_to_file(dataset, output_path):
    """Save the list of image paths to a text file."""
    with open(output_path, 'w') as f:
        for image_path in dataset:
            f.write(image_path + '\n')
    return output_path

def convert(srcFileName, dstFilename, dataset_file):
    platform = "rk3588"

    print('--> Source file name: ' + srcFileName)
    print('--> RKNN file name: ' + dstFilename)

    rknn = RKNN()

    rknn.config(mean_values=[[0, 0, 0]], std_values=[[255, 255, 255]], target_platform=platform)

    # Load model
    print('--> Loading model')
    ret = rknn.load_onnx(srcFileName)
    if ret != 0:
        print('load model failed!')
        exit(ret)
    print('done')

    # Build model with quantization
    print('--> Building model')
    ret = rknn.build(do_quantization=True, dataset=dataset_file)
    if ret != 0:
        print('build model failed.')
        exit(ret)
    print('done')

    # Export model to rknn format for Rockchip NPU
    print('--> Export rknn model')
    ret = rknn.export_rknn(dstFilename)
    if ret != 0:
        print('Export rknn model failed!')
        return ret

    print('export done')

    rknn.release()

def main():
    parser = argparse.ArgumentParser(description='Transform to RKNN model')
    parser.add_argument('source_file', help='Path to the ONNX model file')
    parser.add_argument('description_file', help='Output path for the RKNN model file')
    parser.add_argument('quant_dir', help='Directory containing images for quantization')
    args = parser.parse_args()

    dataset = collect_images_from_directory(args.quant_dir)
    if not dataset:
        print(f"No images found in directory: {args.quant_dir}")
        exit(1)

    dataset_file = save_dataset_to_file(dataset, 'dataset.txt')

    convert(args.source_file, args.description_file, dataset_file)

if __name__ == '__main__':
    main()
