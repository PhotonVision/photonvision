import os
import shutil
import subprocess
from time import sleep

from distutils.dir_util import copy_tree
from .photonvision_wrapper import photonvision_wrapper, test_resources_wrapper


def test_photonlibpy():
    jar_folder = "../../photon-server/build/libs/"
    jar = [jar for jar in os.listdir(jar_folder) if jar.endswith(".jar")][0]

    with photonvision_wrapper(
        f"{jar_folder}/{jar}",
        golden_config_sqlite=os.path.abspath("photon_nt_servermode.sqlite"),
        # java_exe="C:\\Users\\Public\\wpilib\\2024\\jdk\\bin\\java.exe",
    ), test_resources_wrapper():
        # assumes we have already installed the test binary via gradle via 
        # checkPhotonlibIntegrationTestWindowsx86-64DebugGoogleTestExe
        ret = subprocess.run(
            [os.path.abspath("..\\..\\photon-lib\\build\\install\\photonlibIntegrationTest\\linuxx86-64\\debug\\photonlibIntegrationTest.bat")],
            cwd=os.path.abspath("..\\..\\"),
            shell=True,
        )
        print(ret)
        assert ret.returncode == 0
