import shutil
import sys
import platform
import urllib.request
import subprocess
from urllib.parse import urlparse
import os

CHUNK_SIZE = 8192

wheel_versions = {
    "arm64": {
        "3.6": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp36-cp36m-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.7": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp37-cp37m-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.8": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.9": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp39-cp39-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.10": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp310-cp310-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.11": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp311-cp311-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
        "3.12": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/arm64/rknn_toolkit2-2.3.2-cp312-cp312-manylinux_2_17_aarch64.manylinux2014_aarch64.whl",
    },
    "x86_64": {
        "3.6": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp36-cp36m-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.7": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp37-cp37m-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.8": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp38-cp38-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.9": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp39-cp39-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.10": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp310-cp310-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.11": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp311-cp311-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
        "3.12": "https://github.com/airockchip/rknn-toolkit2/raw/refs/heads/master/rknn-toolkit2/packages/x86_64/rknn_toolkit2-2.3.2-cp312-cp312-manylinux_2_17_x86_64.manylinux2014_x86_64.whl",
    },
}

supported_arch = list(wheel_versions.keys())

def get_filename_from_url(url):
    parsed = urlparse(url)
    filename = os.path.basename(parsed.path)
    if not filename:
        filename = "" # never gonna get here
    return filename

if __name__ == "__main__":
    arch = platform.machine()

    if not arch in supported_arch:
        print(f"Unsupported architecture {arch}. Must be one of the following: {supported_arch}")

    current_version = f"{sys.version_info.major}.{sys.version_info.minor}"
    supported_versions = list(wheel_versions[arch])
    
    if sys.version_info.major < 3:
        print(f"Must have at least python version {supported_versions[0]}")
    elif not current_version in supported_versions:
        print(f"Unsupported python version {current_version}, supported python versions are: {supported_versions}")
    
    
    download_url = wheel_versions[arch][current_version]
    wheel_name = get_filename_from_url(download_url)
    
    print(f"Downloading RKNN Toolkit2 wheel: {wheel_name}")
    with urllib.request.urlopen(download_url) as response, open(wheel_name, 'wb') as out_file:
            while True:
                chunk = response.read(CHUNK_SIZE)
                if not chunk:
                    break
                out_file.write(chunk)
    print("Download completed, now running pip install")    

    try:
        subprocess.run(["pip", "install", wheel_name]).check_returncode()
    except subprocess.CalledProcessError as e:
        print("Failed to run pip install, see output below")
        print(e.output)
        sys.exit(1)

    print("Python RKNN Toolkit2 installed successfully!")