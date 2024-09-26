import os
from setuptools import setup, find_packages
import subprocess, re

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

# must be in sync with the rest of the project to avoid ABI breaks
wpilibVersion = "2024.3.2.1"

from wheel.bdist_wheel import bdist_wheel as _bdist_wheel


# source: https://github.com/Yelp/dumb-init/blob/48db0c0d0ecb4598d1a6400710445b85d67616bf/setup.py#L11-L27
# Licensed under the MIT License
class bdist_wheel(_bdist_wheel):

    def finalize_options(self):
        _bdist_wheel.finalize_options(self)
        # Mark us as not a pure python package
        self.root_is_pure = False


script_path = os.path.dirname(os.path.realpath(__file__))
if not os.path.exists(f"{script_path}/photonlibpy/lib/_photonlibpy.pyi"):
    print("Generating typehints")
    try:
        from create_photonlib_pyi import write_stubgen
        write_stubgen()
    except Exception as e:
        print(e)
        exit(1)

setup(
    name="photonlibpy",
    packages=find_packages(),
    version=versionString,
    install_requires=[
        f"wpilib~={wpilibVersion}",
        f"robotpy-wpimath~={wpilibVersion}",
        f"robotpy-apriltag~={wpilibVersion}",
        f"pyntcore~={wpilibVersion}",
    ],
    description=descriptionStr,
    url="https://photonvision.org",
    author="Photonvision Development Team",
    long_description="A Pure-python implementation of PhotonLib",
    long_description_content_type="text/markdown",
    package_data={
        "photonlibpy": [
            "lib/*.so*",
            "lib/*.dylib*",
            "lib/*.dll*",
            "lib/*.pyi",
            "lib/*.pyd",
        ]
    },
    include_package_data=True,
    cmdclass={"bdist_wheel": bdist_wheel},
)
