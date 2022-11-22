# Photon Vision

[![CI](https://github.com/PhotonVision/photonvision/workflows/CI/badge.svg)](https://github.com/PhotonVision/photonvision/actions?query=workflow%3ACI) [![codecov](https://codecov.io/gh/PhotonVision/photonvision/branch/master/graph/badge.svg)](https://codecov.io/gh/PhotonVision/photonvision) [![Discord](https://img.shields.io/discord/725836368059826228?color=%23738ADB&label=Join%20our%20Discord&logo=discord&logoColor=white)](https://discord.gg/wYxTwym)

PhotonVision is the free, fast, and easy-to-use computer vision solution for the *FIRST* Robotics Competition. You can read an overview of our features [on our website](https://photonvision.org). You can find our comprehensive documentation [here](https://docs.photonvision.org).

A copy of the latest Raspberry Pi image is available [here](https://github.com/PhotonVision/photon-pi-gen/releases). A copy of the latest standalone JAR is available [here](https://github.com/PhotonVision/photonvision/releases). If you are a Gloworm user you can find the latest Gloworm image [here](https://github.com/gloworm-vision/pi-gen/releases).

If you are interested in contributing code or documentation to the project, please [read our getting started page for contributors](https://docs.photonvision.org/en/latest/docs/contributing/index.html) and **[join the Discord](https://discord.gg/wYxTwym) to introduce yourself!** We hope to provide a welcoming community to anyone who is interested in helping.

## Authors

<a href="https://github.com/PhotonVision/photonvision/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=PhotonVision/photonvision" />
</a>

## Gradle Arguments

Note that these are case sensitive!

* `-Ppionly`: only builds for `linuxraspbian`, which reduces JAR size. The JAR name will have "-raspi" appended.
- `-PtgtIp`: deploys (builds and copies the JAR) to the coprocessor at the specified IP
- `-Pprofile`: enables JVM profiling

## Acknowledgments
PhotonVision was forked from [Chameleon Vision](https://github.com/Chameleon-Vision/chameleon-vision/). Thank you to everyone who worked on the original project.


* [WPILib](https://github.com/wpilibsuite) - Specifically [cscore](https://github.com/wpilibsuite/allwpilib/tree/master/cscore), [CameraServer](https://github.com/wpilibsuite/allwpilib/tree/master/cameraserver), [NTCore](https://github.com/wpilibsuite/allwpilib/tree/master/ntcore), and [OpenCV](https://github.com/wpilibsuite/thirdparty-opencv).

* [Apache Commons](https://commons.apache.org/) - Specifically [Commons Math](https://commons.apache.org/proper/commons-math/), and [Commons Lang](https://commons.apache.org/proper/commons-lang/)

* [Javalin](https://javalin.io/)

* [JSON](https://json.org)

* [FasterXML](https://github.com/FasterXML) - Specifically [jackson](https://github.com/FasterXML/jackson)

## License
PhotonVision is licensed under the [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.html)

## Meeting Notes
Our meeting notes can be found in the wiki section of this repository.

* [2020 Meeting Notes](https://github.com/PhotonVision/photonvision/wiki/2020-Meeting-Notes)
* [2021 Meeting Notes](https://github.com/PhotonVision/photonvision/wiki/2021-Meeting-Notes)
