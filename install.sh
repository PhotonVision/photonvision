#!/bin/bash
apt-get update
apt-get dist-upgrade
apt-get upgrade
apt-get install python3-pip python3-dev cmake zip unzip build-essential git libnss-mdns --fix-missing
apt-get install python3-numpy
apt-get install python3-opencv
pip3 install robotpy-cscore
pip3 install pyzmq
pip3 install tornado
