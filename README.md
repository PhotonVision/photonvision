# Chameleon-Vision
a free software for FRC teams to use for vision proccesing on their robots 
## getting started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

so in order to run this project we will need to install python in order to run the backend and node.js with vue.js in order to run the fronted
#### backend
- python 3.6 and above
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
####for the backend
1. sudo apt-get update 
2. apt-get dist-upgrade
3. sudo apt-get upgrade 
4. sudo apt-get install python3-pip python3-dev cmake zip unzip build-essential git --fix-missing
5. sudo pip3 install numpy
6. OPENCV_VERSION=3.4.5
7. wget -O opencv.zip https://github.com/opencv/opencv/archive/${OPENCV_VERSION}.zip
8. unzip opencv.zip
9. cd ~/opencv-${OPENCV_VERSION}/
10. mkdir build
11. cd build
12. cmake -D BUILD_SHARED_LIBS=ON -D BUILD_opencv_python3=ON ..
13. make -j $(python3 -c 'import multiprocessing as mp; print(int(mp.cpu_count() * 1.5))')
14. make install
15. ldconfig
16. pip3 install robotpy-cscore pyzmq tornado  

to run the backend:
```
sudo python3 Main.py
```
if backed gets suck or no camera are recognized after a crash do:
```
sudo pkill -9 python3
```

####for the frontend
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



## docs
main docs can be found at [google docs](https://docs.google.com/document/d/1qDuwHtpIPJfyXGIL8PJG89LZwRWbn2J9f-5g19lWL9U/edit?usp=sharing)

## Authors
* **Sagi Frimer** - *initial work* - websocket, settings manager, UI
* **Ori Agranat** - *main coder* - vision loop , UI, websocket, networktables

## Acknowledgments
* the [robotpy project](https://github.com/robotpy) and mainly the cscore libs
* basically all of stackoverflow
