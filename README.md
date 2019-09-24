# Chameleon-Vision

Chameleon Vision is free open-source software for FRC teams to use for vision proccesing on their robots.

## Getting started

See Deployment for notes on how to deploy the project on a live system.

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.  
(Coming soon!)  

### Prerequisites
---
#### For the co-processor

- Java 12 Runtime
- Avahi Daemon

#### For the driver station

- Bonjour


## Deployment
Deploying is as simple as uploading the chameleon-vision-1.xx.jar file to your target device.  
Run the program with `java -jar chameleon-vision-1.xx.jar`

## Hardware

### ARM Co-processors
Currently only Raspberry Pi 3 or 4 models with at least 1GB of RAM are tested and supported.  
Additional ARM-based single board computers (Odroid, Nvidia Jetson, etc.) will be supported in the near future.


### x86 Computers
Currently any 64-Bit devices (Windows, Linux and Mac OS) are supported.  
32 Bit devices are not supported.

## Authors

*  **Sagi Frimer** - *initial work* - websocket, settings manager, UI

*  **Ori Agranat** - *main coder* - vision loop, UI, websocket, networktables

*  **Omer Zipory** - *developer* - vision loop, websocket, networking

*  **Banks Troutman** - *developer* - vision loop, websocket, networking

*  **Matt Morley** - *developer* - documentation


## Acknowledgments

* [WPILib](https://github.com/wpilibsuite) - Specifically [cscore](https://github.com/wpilibsuite/allwpilib/tree/master/cscore), [CameraServer](https://github.com/wpilibsuite/allwpilib/tree/master/cameraserver), [NTCore](https://github.com/wpilibsuite/allwpilib/tree/master/ntcore), and [OpenCV](https://github.com/wpilibsuite/thirdparty-opencv). 

* [Apache Commons](https://commons.apache.org/) - Specifically [Commons Math](https://commons.apache.org/proper/commons-math/), and [Commons Lang](https://commons.apache.org/proper/commons-lang/)

* [Javalin](https://javalin.io/)

* [Spring Framework](https://spring.io/)

* [JSON](https://json.org)

* [Google](https://github.com/google) - Specifically [Gson](https://github.com/google/gson)

## License  
Usage of Chameleon Vision must fall under all terms of [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)
