import shutil
import os

def build_hook(metadata):

    target = "linuxx86-64" # TODO: pull the target from gradle

    gradle_lib = f"../build/libs/photonlib/shared/{target}/release/libphotonlib.so"
    dest = "src/native/photonlib"

    gradle_include = "../src/main/native/include"

    dest = "src/native/photonlib-native"

    # clean
    if os.path.exists(dest):
        shutil.rmtree(dest)

    os.makedirs(f"{dest}/lib", exist_ok=True)
    os.makedirs(f"{dest}/include", exist_ok=True)

    shutil.copy2(gradle_lib, f"{dest}/lib/")
    shutil.copytree(gradle_include, f"{dest}/include/", dirs_exist_ok=True)