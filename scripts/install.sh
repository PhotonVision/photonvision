#!/bin/bash

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

echo "This is the installation script for PhotonVision."

echo "Installing the JDK..."
if [ $(dpkg-query -W -f='${Status}' openjdk-11-jdk-headless 2>/dev/null | grep -c "ok installed") -eq 0 ];
then
   apt update
   apt-get install openjdk-11-jdk-headless;
fi
echo "JDK installation complete."


echo "Downloading latest stable release of PhotonVision..."
mkdir -p /opt/photonvision
cd /opt/photonvision
curl -s https://api.github.com/repos/photonvision/photonvision/releases/latest | 
    grep "browser_download_url.*jar" | 
    cut -d : -f 2,3 | 
    tr -d '"' | 
    wget -qi - -O photonvision.jar
echo "Downloaded latest stable release of PhotonVision."

echo "Creating the PhotonVision systemd service..."

if service --status-all | grep -Fq 'photonvision'; then    
  systemctl stop photonvision
  systemctl disable photonvision
  rm /lib/systemd/system/photonvision.service
  rm /etc/systemd/system/photonvision.service
  systemctl dameon-reload
  systemctl reset-failed
fi

cd /lib/systemd/system/
touch photonvision.service
printf \
"[Unit]
Description=Service that runs PhotonVision

[Service]
WorkingDirectory=/opt/photonvision
ExecStart=/usr/bin/java -jar /opt/photonvision/photonvision.jar
    
[Install]
WantedBy=multi-user.target" >> photonvision.service
cp photonvision.service /etc/systemd/system/photonvision.service
chmod 644 /etc/systemd/system/photonvision.service
systemctl daemon-reload
systemctl enable photonvision.service

echo "Created PhotonVision systemd service."

echo "PhotonVision installation successful."
