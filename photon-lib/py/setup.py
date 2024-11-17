import re
import subprocess

from setuptools import find_packages, setup

gitDescribeResult = (
    subprocess.check_output(["git", "describe", "--tags", "--match=v*", "--always"])
    .decode("utf-8")
    .strip()
)

m = re.search(
    r"(v[0-9]{4}\.[0-9]{1}\.[0-9]{1})-?((?:beta)?(?:alpha)?)-?([0-9\.]*)",
    gitDescribeResult,
)

# Extract the first portion of the git describe result
# which should be PEP440 compliant
if m:
    versionString = m.group(0)
    # Hack -- for strings like v2024.1.1, do NOT add matruity/suffix
    if len(m.group(2)) > 0:
        print("using beta group matcher")
        prefix = m.group(1)
        maturity = m.group(2)
        suffix = m.group(3).replace(".", "")
        versionString = f"{prefix}.{maturity}.{suffix}"
    else:
        split = gitDescribeResult.split("-")
        if len(split) == 3:
            year, commits, sha = split
            # Chop off leading v from "v2024.1.2", and use "post" for commits to master since
            versionString = f"{year[1:]}post{commits}"
            print("using dev release " + versionString)
        else:
            year = gitDescribeResult
            versionString = year[1:]
            print("using full release " + versionString)


else:
    print("Warning, no valid version found")
    versionString = gitDescribeResult

print(f"Building version {versionString}")

# Put the version info into a python file for runtime access
with open("photonlibpy/version.py", "w", encoding="utf-8") as fp:
    fp.write(f'PHOTONLIB_VERSION="{versionString}"\n')
    fp.write(f'PHOTONVISION_VERSION="{gitDescribeResult}"\n')


descriptionStr = f"Pure-python implementation of PhotonLib for interfacing with PhotonVision on coprocessors. Implemented with PhotonVision version {gitDescribeResult} ."

setup(
    name="photonlibpy",
    packages=find_packages(),
    version=versionString,
    install_requires=[
        "numpy~=2.1",
        "wpilib<2026,>=2025.0.0b1",
        "robotpy-wpimath<2026,>=2025.0.0b1",
        "robotpy-apriltag<2026,>=2025.0.0b1",
        "robotpy-cscore<2026,>=2025.0.0b1",
        "pyntcore<2026,>=2025.0.0b1",
        "opencv-python;platform_machine!='roborio'",
    ],
    description=descriptionStr,
    url="https://photonvision.org",
    author="Photonvision Development Team",
    long_description="A Pure-python implementation of PhotonLib",
    long_description_content_type="text/markdown",
)
