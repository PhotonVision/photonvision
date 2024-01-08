#!/bin/bash

package_is_installed(){
    dpkg-query -W -f='${Status}' "$1" 2>/dev/null | grep -q "ok installed"
}

help() {
  echo "This script installs Photonvision."
  echo "It must be run as root."
  echo
  echo "Syntax: sudo ./install.sh [-h|m|n|q]"
  echo "  options:"
  echo "  -h        Display this help message."
  echo "  -m        Install and configure NetworkManager (Ubuntu only)."
  echo "  -n        Disable networking. This will also prevent installation of NetworkManager."
  echo "  -q        Silent install, automatically accepts all defaults. For non-interactive use."
  echo
}

INSTALL_NETWORK_MANAGER="false"

while getopts ":hmnq" name; do
  case "$name" in
    h)
      help
      exit 0
      ;;
    m) INSTALL_NETWORK_MANAGER="true"
      ;;
    n) DISABLE_NETWORKING="true"
      ;;
    q) QUIET="true"
      ;;
    \?)
      echo "Error: Invalid option -- '$OPTARG'"
      echo "Try './install.sh -h' for more information."
      exit 1
  esac
done

shift $(($OPTIND -1))

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

DISTRO=$(lsb_release -is)
if [[ "$DISTRO" = "Ubuntu" && "$INSTALL_NETWORK_MANAGER" != "true" && -z "$QUIET" && -z "$DISABLE_NETWORKING" ]]; then
  echo ""
  echo "Photonvision uses NetworkManager to control networking on your device."
  read -p "Do you want this script to install and configure NetworkManager? [y/N]: " response
  if [[ $response == [yY] || $response == [yY][eE][sS] ]]; then
    INSTALL_NETWORK_MANAGER="true"
  fi
fi

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

if [[ "$INSTALL_NETWORK_MANAGER" == "true" ]]; then
  echo "Installing network-manager..."
  apt-get install --yes network-manager
  cat > /etc/netplan/00-default-nm-renderer.yaml <<EOF
network:
  renderer: NetworkManager
EOF
  echo "network-manager installation complete."
fi

echo "Installing the JRE..."
if ! package_is_installed openjdk-17-jre-headless
then
   apt-get update
   apt-get install --yes openjdk-17-jre-headless
fi
echo "JRE installation complete."

if [ "$ARCH" == "aarch64" ]
then
    if package_is_installed libopencv-core4.6
    then
        echo "libopencv-core4.6 already installed"
    else
        # libphotonlibcamera.so on raspberry pi has dep on libopencv_core
        echo "Installing libopencv-core4.6 on aarch64"
        apt-get install --yes libopencv-core4.6
    fi
fi

echo "Installing additional math packages"
apt-get install --yes libcholmod3 liblapack3 libsuitesparseconfig5

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

# service --status-all doesn't list photonvision on OrangePi use systemctl instead:
#if systemctl --quiet is-active photonvision; then
if service --status-all | grep -Fq 'photonvision'; then
  echo "PhotonVision is already running. Stopping service."
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
# AllowedCPUs=4-7

ExecStart=/usr/bin/java -Xmx512m -jar /opt/photonvision/photonvision.jar
ExecStop=/bin/systemctl kill photonvision
Type=simple
Restart=on-failure
RestartSec=1

[Install]
WantedBy=multi-user.target
EOF

if [ "$DISABLE_NETWORKING" = "true" ]; then
  sed -i "s/photonvision.jar/photonvision.jar -n/" /lib/systemd/system/photonvision.service
fi

cp /lib/systemd/system/photonvision.service /etc/systemd/system/photonvision.service
chmod 644 /etc/systemd/system/photonvision.service
systemctl daemon-reload
systemctl enable photonvision.service

echo "Created PhotonVision systemd service."

echo "PhotonVision installation successful."
