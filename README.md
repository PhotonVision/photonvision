# PhotonVision

[![CI](https://github.com/PhotonVision/photonvision/workflows/CI/badge.svg)](https://github.com/PhotonVision/photonvision/actions?query=workflow%3ACI) [![codecov](https://codecov.io/gh/PhotonVision/photonvision/branch/master/graph/badge.svg)](https://codecov.io/gh/PhotonVision/photonvision) [![Discord](https://img.shields.io/discord/725836368059826228?color=%23738ADB&label=Join%20our%20Discord&logo=discord&logoColor=white)](https://discord.gg/wYxTwym)

PhotonVision is the free, fast, and easy-to-use computer vision solution for the *FIRST* Robotics Competition. You can read an overview of our features [on our website](https://photonvision.org). You can find our comprehensive documentation [here](https://docs.photonvision.org).

The latest release of platform-specific jars and images is found [here](https://github.com/PhotonVision/photonvision/releases).

If you are interested in contributing code or documentation to the project, please [read our getting started page for contributors](https://docs.photonvision.org/en/latest/docs/contributing/index.html) and **[join the Discord](https://discord.gg/wYxTwym) to introduce yourself!** We hope to provide a welcoming community to anyone who is interested in helping.

## Authors

<a href="https://github.com/PhotonVision/photonvision/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=PhotonVision/photonvision" />
</a>

## Documentation

- Our main documentation page: [docs.photonvision.org](https://docs.photonvision.org)
- Photon UI demo: [demo.photonvision.org](https://demo.photonvision.org) (or [manual link](https://photonvision.github.io/photonvision/built-client/))
- Javadocs: [javadocs.photonvision.org](https://javadocs.photonvision.org) (or [manual link](https://photonvision.github.io/photonvision/built-docs/javadoc/))
- C++ Doxygen  [cppdocs.photonvision.org](https://cppdocs.photonvision.org) (or [manual link](https://photonvision.github.io/photonvision/built-docs/doxygen/html/))

## Building

Gradle is used for all C++ and Java code, and NPM is used for the web UI. Instructions to compile PhotonVision yourself can be found [in our docs](https://docs.photonvision.org/en/latest/docs/contributing/building-photon.html#compiling-instructions).

You can run one of the many built in examples straight from the command line, too! They contain a fully featured robot project, and some include simulation support. The projects can be found inside the [`photonlib-java-examples`](photonlib-java-examples) and [`photonlib-cpp-examples`](photonlib-cpp-examples) subdirectories, respectively. Instructions for running these examples directly from the repo are found [in the docs](https://docs.photonvision.org/en/latest/docs/contributing/building-photon.html#running-examples).

## Gradle Arguments

Note that these are case sensitive!

* `-PArchOverride=foobar`: builds for a target system other than your current architecture. [Valid overrides](https://github.com/wpilibsuite/wpilib-tool-plugin/blob/main/src/main/java/edu/wpi/first/tools/NativePlatforms.java) are:
    * winx32
    * winx64
    * winarm64
    * macx64
    * macarm64
    * linuxx64
    * linuxarm64
    * linuxathena
- `-PtgtIP`: Specifies where `./gradlew deploy` should try to copy the fat JAR to
- `-Pprofile`: enables JVM profiling

If you're cross-compiling, you'll need the wpilib toolchain installed. This can be done via Gradle: for example `./gradlew installArm64Toolchain` or `./gradlew installRoboRioToolchain`

## Out-of-Source Dependencies

PhotonVision uses the following additional out-of-source repositories for building code.

- Base system images for Raspberry Pi & Orange Pi: https://github.com/PhotonVision/photon-image-modifier
- C++ driver for Raspberry Pi CSI cameras: https://github.com/PhotonVision/photon-libcamera-gl-driver
- JNI code for [mrcal](https://mrcal.secretsauce.net/): https://github.com/PhotonVision/mrcal-java
- Custom build of OpenCV with GStreamer/Protobuf/other custom flags: https://github.com/PhotonVision/thirdparty-opencv
- JNI code for aruco-nano: https://github.com/PhotonVision/aruconano-jni

## Additional packages

For now, using mrcal requires installing these additional packages on Linux systems:

```
sudo apt install libcholmod3 liblapack3 libsuitesparseconfig5
```

## Acknowledgments

PhotonVision was forked from [Chameleon Vision](https://github.com/Chameleon-Vision/chameleon-vision/). Thank you to everyone who worked on the original project.

* [WPILib](https://github.com/wpilibsuite) - Specifically [cscore](https://github.com/wpilibsuite/allwpilib/tree/master/cscore), [CameraServer](https://github.com/wpilibsuite/allwpilib/tree/master/cameraserver), [NTCore](https://github.com/wpilibsuite/allwpilib/tree/master/ntcore), and [OpenCV](https://github.com/wpilibsuite/thirdparty-opencv).

* [Apache Commons](https://commons.apache.org/) - Specifically [Commons Math](https://commons.apache.org/proper/commons-math/), and [Commons Lang](https://commons.apache.org/proper/commons-lang/)

* [Javalin](https://javalin.io/)

* [JSON](https://json.org)

* [FasterXML](https://github.com/FasterXML) - Specifically [jackson](https://github.com/FasterXML/jackson)

## License

PhotonVision is licensed under the [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.html).

## Meeting Notes

Our [meeting notes](https://github.com/PhotonVision/photonvision/wiki/PhotonVision-Meeting-Notes) can be found in the wiki section of this repository.
