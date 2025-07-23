import subprocess
import sys
import random
import argparse
import os
from rknn.api import RKNN

image_extensions = (".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tiff", ".webp")
DEFAULT_PLATFORM = "rk3588"

def list_img_dir(img_dir):
    return [
        os.path.abspath(os.path.join(img_dir, f))
        for f in os.listdir(img_dir)
        if f.lower().endswith(image_extensions)
    ]


def sample_imgs(num, img_list):
    if len(img_list) < num:
        return img_list
    else:
        return random.sample(img_list, num)


def get_image_list_from_dataset(num_imgs, yaml_dir):
    print(f"Dataset detected with {yaml_dir} file")
    img_raw_paths = []

    with open(yaml_dir, "r") as yaml_file:
        for line in yaml_file:
            line = line.strip()
            if (
                line.startswith("train:")
                or line.startswith("val:")
                or line.startswith("test:")
            ):
                img_raw_paths.append(line.split(":", 1)[1].strip())

    no_yaml_dir = yaml_dir.replace(
        "data.yaml", "dummy_dir"
    )  # data.yaml sets dirs one level up
    img_set_paths = []

    for img_raw_path in img_raw_paths:
        p = (
            img_raw_path
            if os.path.isabs(img_raw_path)
            else os.path.realpath(os.path.join(no_yaml_dir, img_raw_path))
        )

        if os.path.exists(p):
            img_set_paths.append(p)

    if len(img_set_paths) < 1:
        return None

    all_imgs = [list_img_dir(path) for path in img_set_paths]

    for imgs in all_imgs:
        print(len(imgs))

    total_imgs = sum(len(group) for group in all_imgs)

    sampled_imgs = [
        sample_imgs(round((len(group) / total_imgs) * num_imgs), group)
        for group in all_imgs
    ]

    return [img for group in sampled_imgs for img in group]


def get_image_list_from_img_dir(num_imgs, img_dir):
    return sample_imgs(num_imgs, list_img_dir(img_dir))


def get_image_list(num_imgs, image_dir):
    yaml_path = os.path.join(image_dir, "data.yaml")

    if os.path.exists(yaml_path):
        return get_image_list_from_dataset(num_imgs, yaml_path)
    else:
        return get_image_list_from_img_dir(num_imgs, image_dir)


def run_rknn_conversion(
    img_list_txt, disable_quant, model_path, rknn_output, verbose_logging
):
    rknn = RKNN(verbose=verbose_logging, verbose_file=("rknn_convert.log" if verbose_logging else None))

    rknn.config(
        mean_values=[[0, 0, 0]],
        std_values=[[255, 255, 255]],
        target_platform=DEFAULT_PLATFORM,
    )

    print("Attempted RKNN load")
    ret = rknn.load_onnx(model=model_path)
    if ret != 0:
        print("Loading model failed!")
        exit(ret)

    print("Attempting RKNN build")
    ret = rknn.build(do_quantization=(not disable_quant), dataset=img_list_txt)
    if ret != 0:
        print("Building model failed!")
        exit(ret)

    print("Build succeeded! Starting export...")
    ret = rknn.export_rknn(rknn_output)
    if ret != 0:
        print("Exporting model failed!")
        exit(ret)
    print("Finished export!")

    # Release
    rknn.release()

    print(f'Your model is in "{rknn_output}" and ready to use!')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Generate valid ONNX file for yolo model"
    )

    parser.add_argument(
        "-ni",
        "--num_imgs",
        type=int,
        default=300,
        help="Number of images to use for calibration (default: 300)",
    )

    parser.add_argument(
        "-d",
        "--img_dir",
        required=True,
        help="Directory where your dataset is located (must have data.yaml), or images are located",
    )

    parser.add_argument(
        "-m",
        "--model_path",
        required=True,
        help=(f"Path to generated ONNX model"),
    )

    parser.add_argument(
        "-dq",
        "--disable_quantize",
        type=bool,
        default=False,
        help="Whether to skip quantization (default: False)",
    )

    parser.add_argument(
        "-o",
        "--rknn_output",
        default="out.rknn",
        help="Where the rknn model should be outputted (default: ./out.rknn)",
    )

    parser.add_argument(
        "-ds",
        "--img_dataset_txt",
        default="imgs.txt",
        help="Where the list of images used for quantization should be outputted (default: ./imgs.txt)",
    )

    parser.add_argument(
        "-vb",
        "--verbose",
        type=bool,
        default=False,
        help="Whether to enable verbose logging",
    )

    args = parser.parse_args()

    if not args.rknn_output.endswith(".rknn"):
        print("RKNN output path must end in .rknn!")
        sys.exit(1)

    if not args.disable_quantize:
        if args.img_dir == None or len(args.img_dir) < 1:
            print(f"Must specify list of images to use with --img_dir")
            sys.exit(1)

        img_dir_abs = os.path.abspath(args.img_dir)

        img_list = get_image_list(args.num_imgs, img_dir_abs)
        img_list_len = 0 if img_list is None else len(img_list)

        if img_list_len == 0:
            print(f"No images found in {img_dir_abs}")
            sys.exit(1)
        elif img_list_len < args.num_imgs:
            print(
                f"Not enough images in your dataset/directory, you have {img_list_len} images, but need {args.num_imgs}"
            )
            sys.exit(1)

        if not args.img_dataset_txt.endswith(".txt"):
            print(f"Image dataset text file path must end in .txt")
            sys.exit(1)

        with open(args.img_dataset_txt, "w") as set_file:
            set_file.writelines(f"{img}\n" for img in img_list)

    run_rknn_conversion(
        args.img_dataset_txt,
        args.disable_quantize,
        args.model_path,
        args.rknn_output,
        args.verbose,
    )
