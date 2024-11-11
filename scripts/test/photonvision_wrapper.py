import contextlib
import os
from pathlib import Path
import subprocess
import tempfile
from time import sleep
from typing import Union
import shutil

from distutils.dir_util import copy_tree


@contextlib.contextmanager
def test_resources_wrapper():
    print("Copying test-resources folder over (ew ew ew)")
    copy_tree("..\\..\\test-resources", "test-resources")

    try:
        yield
    finally:
        print("Not deleting files (too lazy to implement cleaning these up)")


@contextlib.contextmanager
def photonvision_wrapper(
    jar_path: Union[str, os.PathLike],
    golden_config_sqlite: Union[str, os.PathLike, None] = None,
    java_exe="java",
):
    """
    Launch Photon from a jar with a optional starting sqlite config. This runs with the config file set to a temporary directory

    Args:
    jar_path: Where to find photonvision.jar
    golden_config_sqlite: Optional config file to copy into the new empty config directory
    java_exe: The command to run to invoke Java
    """

    jar_path = os.path.abspath(jar_path)

    config_dir = tempfile.mkdtemp()
    print(f"Running from config_dir {config_dir}")

    if golden_config_sqlite is not None:
        golden_config_sqlite = os.path.abspath(golden_config_sqlite)
        print(f"Running from input config db {golden_config_sqlite}")
        shutil.copy(golden_config_sqlite, Path(config_dir) / "photon.sqlite")

    print("Config copied! starting photon in test mode")

    process = subprocess.Popen(
        [
            java_exe,
            "-jar",
            jar_path,
            "-n",
            "--test-mode",
            "--config-dir",
            config_dir,
        ]
    )

    try:
        yield
    finally:
        print("Killing photon")

        process.terminate()
        ret = process.wait()
        print(ret)
