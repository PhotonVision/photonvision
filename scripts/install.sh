#!/bin/bash

debug() {
  if [ -z "$QUIET" ] ; then
    for arg in "$@"; do
      echo "$arg"
    done
  fi
}

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
  echo "ARM32 is not supported by PhotonVision. Exiting."
  exit 1
elif [ "$ARCH" = "x86_64" ]; then
  ARCH_NAME="linuxx64"
else
  if [ "$#" -ne 1 ]; then
      echo "Can't determine current arch; please provide it (one of):"
      echo ""
      echo "- linuxarm64 (64-bit Linux ARM)"
      echo "- linuxx64   (64-bit Linux)"
      exit 1
  else
    debug "Can't detect arch (got $ARCH) -- using user-provided $1"
    ARCH_NAME=$1
  fi
fi

debug "This is the installation script for PhotonVision."
debug "Installing for platform $ARCH_NAME"

DISTRO=$(lsb_release -is)
if [[ "$DISTRO" = "Ubuntu" && "$INSTALL_NETWORK_MANAGER" != "true" && -z "$QUIET" && -z "$DISABLE_NETWORKING" ]]; then
  debug ""
  debug "Photonvision uses NetworkManager to control networking on your device."
  read -p "Do you want this script to install and configure NetworkManager? [y/N]: " response
  if [[ $response == [yY] || $response == [yY][eE][sS] ]]; then
    INSTALL_NETWORK_MANAGER="true"
  fi
fi

debug "Update package list"
apt-get update

debug "Installing curl..."
apt-get install --yes curl
debug "curl installation complete."

debug "Installing avahi-daemon..."
apt-get install --yes avahi-daemon
debug "avahi-daemon installation complete."

debug "Installing cpufrequtils..."
apt-get install --yes cpufrequtils
debug "cpufrequtils installation complete."

debug "Setting cpufrequtils to performance mode"
if [ -f /etc/default/cpufrequtils ]; then
    sed -i -e 's/^#\?GOVERNOR=.*$/GOVERNOR=performance/' /etc/default/cpufrequtils
else
    echo 'GOVERNOR=performance' > /etc/default/cpufrequtils
fi

debug "Installing libatomic"
apt-get install --yes libatomic1
debug "libatomic installation complete."

if [[ "$INSTALL_NETWORK_MANAGER" == "true" ]]; then
  debug "Installing network-manager..."
  apt-get install --yes network-manager net-tools
  systemctl disable systemd-networkd-wait-online.service
  cat > /etc/netplan/00-default-nm-renderer.yaml <<EOF
network:
  renderer: NetworkManager
EOF
  debug "network-manager installation complete."
fi

debug "Installing the JRE..."
if ! package_is_installed openjdk-17-jre-headless
then
   apt-get update
   apt-get install --yes openjdk-17-jre-headless
fi
debug "JRE installation complete."

debug "Installing additional math packages"
if [[ "$DISTRO" = "Ubuntu" && -z $(apt-cache search libcholmod3) ]]; then
  debug "Adding jammy to list of apt sources"
  add-apt-repository -y -S 'deb http://ports.ubuntu.com/ubuntu-ports jammy main universe'
fi
apt-get install --yes libcholmod3 liblapack3 libsuitesparseconfig5

debug "Installing v4l-utils..."
apt-get install --yes v4l-utils
debug "v4l-utils installation complete."

debug "Installing sqlite3"
apt-get install --yes sqlite3

debug "Downloading latest stable release of PhotonVision..."
mkdir -p /opt/photonvision
cd /opt/photonvision
curl -sk https://api.github.com/repos/photonvision/photonvision/releases/latest |
    grep "browser_download_url.*$ARCH_NAME.jar" |
    cut -d : -f 2,3 |
    tr -d '"' |
    wget -qi - -O photonvision.jar
debug "Downloaded latest stable release of PhotonVision."

debug "Creating the PhotonVision systemd service..."

# service --status-all doesn't list photonvision on OrangePi use systemctl instead:
if systemctl --quiet is-active photonvision; then
  debug "PhotonVision is already running. Stopping service."
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

if [[ -n $(cat /proc/cpuinfo | grep "RK3588") ]]; then
  debug "This has a Rockchip RK3588, enabling all cores"
  sed -i 's/# AllowedCPUs=4-7/AllowedCPUs=0-7/g' /lib/systemd/system/photonvision.service
fi

cp /lib/systemd/system/photonvision.service /etc/systemd/system/photonvision.service
chmod 644 /etc/systemd/system/photonvision.service
systemctl daemon-reload
systemctl enable photonvision.service

debug "Created PhotonVision systemd service."

debug "PhotonVision installation successful."
