

# Chameleon-Vision

[![CircleCI](https://img.shields.io/circleci/build/github/Chameleon-Vision/chameleon-vision/dev?label=dev&logo=name)](https://circleci.com/gh/Chameleon-Vision/chameleon-vision/tree/dev)
[![CircleCI](https://img.shields.io/circleci/build/github/Chameleon-Vision/chameleon-vision/master?label=master&logo=name)](https://circleci.com/gh/Chameleon-Vision/chameleon-vision/tree/master)

Chameleon Vision is free open-source software for FRC teams to use for vision proccesing on their robots.


There instructions are for compiling (contributing) and running the source-code of the project.
This is NOT intended for the co-processor setup or your testing PC. 
To run the program normally (from a build .jar file), take a look at our ReadTheDocs documentation for installation [here](https://chameleon-vision.readthedocs.io/en/latest/installation/coprocessor-setup.html)


These instruction are for the Chameleon Vision's backend/server in Java

To run the UI's sourcecode (optional) see the UI's [readme](https://github.com/Chameleon-Vision/chameleon-vision/blob/master/chameleon-client/README.md)

## Hardware
Currently any 64-Bit devices (Windows, Linux and Mac OS) are supported.  
32 Bit devices are not supported.

At least one USB camera ([supported](https://chameleon-vision.readthedocs.io/en/latest/hardware/supported-hardware.html#supported-cameras) one is recommended)

## Development setup

### Prerequisites

- Java Development Kit 11: 
Follow the correct instructions for your platform from [AdoptOpenJDK](https://adoptopenjdk.net/)
	-When running the installer, follow the given instructions and ensure that you select Add to PATH, Associate .jar, and Set JAVA_HOME variable.
- Chameleon-Vision source code
Clone via a git client or download as zip and extract the source code into a empty folder.
`git clone -b 3.0 https://github.com/Chameleon-Vision/chameleon-vision.git`
#### For the co-processor(Linux system)
- Avahi Daemon:
`sudo apt-get install avahi-daemon avahi-discover avahi-utils libnss-mdns mdns-scan`

#### For the Driver Station

- Bonjour
Download and install Bonjour [from here](https://support.apple.com/kb/DL999?locale=en_US)
- VC++ Redistributable  (Windows only)
Download and install [this](https://aka.ms/vs/16/release/vc_redist.x64.exe) 

## Importing to IDEA
We recommend the use of [IntelliJ IDEA](https://www.jetbrains.com/idea/) for running the source-code

1. Import Project 

2. Choose the path to `chameleon-server` inside the copy of chameleon-vision that you cloned or downloaded

![](https://i.vgy.me/KmrzCV.png)

3. Click Ok, go to File -> Project Structure -> Project -> Project SDK, and then choose JDK 11.

4. Gradle will automatically download the necessary dependencies 

Note: At this time, the program is not in a runnable state.
 
## Authors

*  **Sagi Frimer** - *initial work* - websocket, settings manager, UI

*  **Ori Agranat** - *main coder* - project manager, vision loop, UI, websocket, networktables

*  **Omer Zipory** - *developer* - vision loop, websocket, networking, documentation, UI

*  **Banks Troutman** - *developer* - vision loop, websocket, networking, project structue

*  **Matt Morley** - *developer* - vision loop, project structue, documentation, solvePNP


## Acknowledgments

* [WPILib](https://github.com/wpilibsuite) - Specifically [cscore](https://github.com/wpilibsuite/allwpilib/tree/master/cscore), [CameraServer](https://github.com/wpilibsuite/allwpilib/tree/master/cameraserver), [NTCore](https://github.com/wpilibsuite/allwpilib/tree/master/ntcore), and [OpenCV](https://github.com/wpilibsuite/thirdparty-opencv). 

* [Apache Commons](https://commons.apache.org/) - Specifically [Commons Math](https://commons.apache.org/proper/commons-math/), and [Commons Lang](https://commons.apache.org/proper/commons-lang/)

* [Javalin](https://javalin.io/)

* [JSON](https://json.org)

* [FasterXML](https://github.com/FasterXML) - Specifically [jackson](https://github.com/FasterXML/jackson)

## License  
Usage of Chameleon Vision must fall under all terms of [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.html)
