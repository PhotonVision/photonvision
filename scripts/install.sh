#!/bin/bash

package_is_installed(){
    dpkg-query -W -f='${Status}' "$1" 2>/dev/null | grep -q "ok installed"
}

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

ARCH=$(uname -m)
ARCH_NAME=""
if [ "$ARCH" = "aarch64" ]; then
  ARCH_NAME="linuxarm64"
elif [ "$ARCH" = "armv7l" ]; then
  ARCH_NAME="linuxarm32"
elif [ "$ARCH" = "x86_64" ]; then
  ARCH_NAME="linuxx64"
else
  if [ "$#" -ne 1 ]; then
      echo "Can't determine current arch; please provide it (one of):"
      echo ""
      echo "- linuxarm32 (32-bit Linux ARM)"
      echo "- linuxarm64 (64-bit Linux ARM)"
      echo "- linuxx64   (64-bit Linux)"
      exit 1
  else
    echo "Can't detect arch (got $ARCH) -- using user-provided $1"
    ARCH_NAME=$1
  fi
fi

echo "This is the installation script for PhotonVision."
echo "Installing for platform $ARCH_NAME"

echo "Installing curl..."
apt-get install --yes curl
echo "curl installation complete."

echo "Installing avahi-daemon..."
apt-get install --yes avahi-daemon
echo "avahi-daemon installation complete."

echo "Installing cpufrequtils..."
apt-get install --yes cpufrequtils
echo "cpufrequtils installation complete."

echo "Setting cpufrequtils to performance mode"
if [ -f /etc/default/cpufrequtils ]; then
    sed -i -e 's/^#\?GOVERNOR=.*$/GOVERNOR=performance/' /etc/default/cpufrequtils
else
    echo 'GOVERNOR=performance' > /etc/default/cpufrequtils
fi

echo "Installing the JDK..."
if ! package_is_installed openjdk-11-jdk-headless
then
   apt-get update
   apt-get install --yes openjdk-11-jdk-headless
fi
echo "JDK installation complete."

if [ "$ARCH" == "aarch64" ]
then
    if package_is_installed libopencv-core4.5
    then
        echo "libopencv-core4.5 already installed"
    else
        # libphotonlibcamera.so on raspberry pi has dep on libopencv_core
        echo "Installing libopencv-core4.5 on aarch64"
        apt-get install --yes libopencv-core4.5
    fi
fi

echo "Downloading latest stable release of PhotonVision..."
mkdir -p /opt/photonvision
cd /opt/photonvision
curl -sk https://api.github.com/repos/photonvision/photonvision/releases/latest |
    grep "browser_download_url.*$ARCH_NAME.jar" |
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
# Run photonvision at "nice" -10, which is higher priority than standard
Nice=-10
# for non-uniform CPUs, like big.LITTLE, you want to select the big cores
# look up the right values for your CPU
# AllowCPUs=4-7

ExecStart=/usr/bin/java -Xmx512m -jar /opt/photonvision/photonvision.jar
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
