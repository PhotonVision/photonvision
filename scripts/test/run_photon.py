import os
from pathlib import Path
import subprocess
import tempfile
from time import sleep
from typing import Union
import shutil


def start_photon(
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
    print(config_dir)

    if golden_config_sqlite is not None:
        golden_config_sqlite = os.path.abspath(golden_config_sqlite)
        print(f"Running from input config db {golden_config_sqlite}")
        shutil.copy(golden_config_sqlite, Path(config_dir) / "photon.sqlite")

    print("Config copied! starting photon in test mode")

    p = subprocess.Popen(
        [java_exe, "-jar", jar_path, "-n", "--test-mode", "--config-dir", config_dir]
    )

    # TODO: replace this with a real check to spin up photonlib or something
    sleep(10)

    print("Killing photon")

    p.terminate()
    ret = p.wait()
    print(ret)


if __name__ == "__main__":
    start_photon(
        "./photon-server/build/libs/photonvision-dev-v2024.3.0-70-gf20e10fc-linuxx64.jar",
        "photon.sqlite",
    )
