import os

from pathlib import Path

sw_root_dir = Path(__file__).resolve().parent.parent

pkgconfig_dirs = [
    sw_root_dir / "photonlib-native" / "src",
    sw_root_dir / "photontargeting-native" / "src"
]

for pkgconfig_dir in pkgconfig_dirs:
    existing = os.environ.get("PKG_CONFIG_PATH", "")

    os.environ["PKG_CONFIG_PATH"] = (
        f"{str(pkgconfig_dir)}:{existing}"
        if existing
        else str(pkgconfig_dir)
    )

import subprocess

subprocess.call([
    "python",
    "-m",
    "semiwrap",
    "update-yaml",
    "--write"
])