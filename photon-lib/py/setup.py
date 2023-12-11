from setuptools import setup, find_packages
import subprocess

versionStr = subprocess.check_output(['git', 'describe', '--tags', "--match=v*", "--always"]).decode('utf-8').strip()
print(f"Building version {versionStr}")

setup(
    name='photonlibpy',
    packages=find_packages(),
    version="0.0.2",
    install_requires=[
        "wpilib<2025,>=2024.0.0b2", 
    ],
    description=f"Pure-python implementation of PhotonLib for interfacing with PhotonVision on coprocessors. Goes with photonvision version {versionStr}",
    url="https://photonvision.org",
    author="Photonvision Development Team",
)
