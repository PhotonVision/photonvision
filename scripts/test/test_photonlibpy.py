from time import sleep
from .photonvision_wrapper import photonvision_wrapper


def test_photonlibpy():
    with photonvision_wrapper(
        "../../photon-server/build/libs/photonvision-dev-v2024.3.0-58-g30191e46-winx64.jar",
        # "photon.sqlite",
        java_exe="C:\\Users\\Public\\wpilib\\2024\\jdk\\bin\\java.exe",
    ):
        print("hi!")
        sleep(1)
        print("bye!")
