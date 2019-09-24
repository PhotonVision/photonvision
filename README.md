# Chameleon-Vision

a free software for FRC teams to use for vision proccesing on their robots

## getting started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

  

### Prerequisites

  

so in order to run this project we will need to install python in order to run the backend and node.js with vue.js in order to run the fronted

#### backend

- python 3.7 and above

- opencv 3.4.5

- tornado web framework

- robotpy-cscore

- pynetworktables

- pymq

  

#### frontend

- vue.js

- vuex

- vue-router

- less and less-loader

- iView

- vue-native-websocket

  

### installing

#### for the backend

1. sudo apt-get update

2. apt-get dist-upgrade

3. sudo apt-get upgrade

4. sudo apt-get install python3-pip python3-dev cmake zip unzip build-essential git libnss-mdns --fix-missing

5. sudo pip3 install numpy (if on raspberry pi do "sudo apt-get install python3-numpy")
6. sudo apt-get install python3-opencv
7. pip3 install robotpy-cscore 
8. pip3 install pyzmq
9. pip3 install tornado

  

to run the backend:

```

sudo python3 Main.py

```

if backed gets suck or no camera are recognized after a crash do:

```

sudo pkill -9 python3

```

#### compiling:

in order to compile the program for runtime run: (still needs to install dependencies)

```

python3 -m nuitka --follow-imports Main.py

```

#### for the frontend

1. sudo apt-get install nodejs npm

2. cd chameleon-client

3. sudo npm install

4. sudo npm install @vue/cli

  

to run the front end you can open the cli ui by:

```

vue ui

```

of you can auto serve the ui by

```

npm run serve

```

## Hardware

this is important when choosing your sbc it is more important to have a good usb controller that a good cpu

on the odroid xu4 which is very fast i have got many bottlenecks from the usb controller and many times making the program crach

#### networking

it is very important to install Bonjour

  
  

## docs

main docs can be found at [google docs](https://docs.google.com/document/d/1qDuwHtpIPJfyXGIL8PJG89LZwRWbn2J9f-5g19lWL9U/edit?usp=sharing)

  

## Authors

*  **Sagi Frimer** - *initial work* - websocket, settings manager, UI

*  **Ori Agranat** - *main coder* - vision loop , UI, websocket, networktables

  

## Acknowledgments

* the [robotpy project](https://github.com/robotpy) and mainly the cscore libs

* basically all of stackoverflow

##License
Copyright (C) 2019 Ori Agranat oriagranat9@gmail.com


* This file is part of Chameleon Vision.

Chameleon Vision can not be copied without the express permission of Ori Agranat
Chameleon Vision binaries may be distributed under [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/)
