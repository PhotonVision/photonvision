

# Chameleon-Vision

[![CircleCI](https://img.shields.io/circleci/build/github/Chameleon-Vision/chameleon-vision/dev?label=dev&logo=name)](https://circleci.com/gh/Chameleon-Vision/workflows/chameleon-vision/tree/dev)
[![CircleCI](https://img.shields.io/circleci/build/github/Chameleon-Vision/chameleon-vision/master?label=master&logo=name)](https://circleci.com/gh/Chameleon-Vision/workflows/chameleon-vision/tree/master)

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

- Java Development Kit 12: 
Follow the correct instructions for your platform from [BellSoft](https://bell-sw.com/pages/liberica_install_guide-12.0.2/)
- Chameleon-vision source code
Clone via a git client or download as zip and extract the source code into a empty folder
#### For the co-processor(Linux system)
- Avahi Daemon:
`sudo apt-get install avahi-daemon avahi-discover avahi-utils libnss-mdns mdns-scan`

#### For the driver station

- Bonjour
Download and install Bonjour [from here](https://support.apple.com/kb/DL999?locale=en_US)
- VC++ Redistributable  (Windows only)
Download and install [this](https://aka.ms/vs/16/release/vc_redist.x64.exe) 

## Importing to IDEA
We recommend the use of [Intellij Idea](https://www.jetbrains.com/idea/) for running the source-code

1. Import Project 

2. Choose the path to `chameleon-server` inside the copy of Chameleon-Vision that you cloned or downloaded

![](https://i.vgy.me/KmrzCV.png)

3. Import the project as a `Maven` project

![](https://i.vgy.me/2ltb7B.png)

4. Under `JDK for importer` choose the JDK 12 you downloaded earlier
5. Maven will automatically download the necessary dependencies 
6. Run `Main` under `src/main/java/com/chameleonvision/`
 
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

* [Google](https://github.com/google) - Specifically [Gson](https://github.com/google/gson)

## License  
Usage of Chameleon Vision must fall under all terms of [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)
