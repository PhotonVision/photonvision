#!/bin/bash

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

echo "This is the installation script for PhotonVision."

echo "Installing curl..."
apt-get install curl
echo "curl installation complete."

echo "Installing avahi-daemon..."
apt-get install avahi-daemon
echo "avahi-daemon installation complete."

echo "Installing cpufrequtils..."
apt-get install cpufrequtils
echo "cpufrequtils installation complete."

echo "Setting cpufrequtils to performance mode"
sed -i -e 's/^#\?GOVERNOR=.*$/GOVERNOR=performance/' /etc/default/cpufrequtils

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
curl -sk https://api.github.com/repos/photonvision/photonvision/releases/latest |
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
  systemctl daemon-reload
  systemctl reset-failed
fi

cat > /lib/systemd/system/photonvision.service <<EOF
[Unit]
Description=Service that runs PhotonVision

[Service]
WorkingDirectory=/opt/photonvision
Nice=-10
# for non-uniform CPUs, like big.LITTLE, you want to select the big cores
# look up the right values for your CPU
# AllowCPUs=4-7

ExecStart=/usr/bin/java -jar /opt/photonvision/photonvision.jar -Xmx512m
ExecStop=/bin/systemctl kill photonvision
Type=simple
Restart=on-failure
RestartSec=1

[Install]
WantedBy=multi-user.target
EOF

cp /lib/systemd/system/photonvision.service /etc/systemd/system/photonvision.service
chmod 644 /etc/systemd/system/photonvision.service
systemctl daemon-reload
systemctl enable photonvision.service

echo "Created PhotonVision systemd service."

echo "PhotonVision installation successful."
