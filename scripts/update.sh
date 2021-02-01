#!/bin/bash

# Check for root
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# Check for internet connection
wget -q --spider http://google.com
if [ $? -eq 0 ]; then
    echo "Internet connection OK"
else
    echo "This script requires an Internet connection! Exiting"
    exit 1
fi

echo "Stopping PhotonVision service"
systemctl stop photonvision

echo "Downloading and installing latest stable release of PhotonVision..."
cd /opt/photonvision
rm photonvision.jar
curl -sk https://api.github.com/repos/photonvision/photonvision/releases/latest |
    grep "browser_download_url.*jar" |
    cut -d : -f 2,3 |
    tr -d '"' |
    wget -qi - -O photonvision.jar

echo "Starting PhotonVision service"
systemctl start photonvision

echo "PhotonVision update succesful!"
