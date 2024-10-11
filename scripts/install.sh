#!/bin/bash

needs_arg() {
    if [ -z "$OPTARG" ]; then
      die "Argument is required for --$OPT option" \
          "See './install.sh -h' for more information."
    fi;
}

die() {
  for arg in "$@"; do
    echo "$arg" 1>&2
  done
  exit 1
}

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

install_if_missing() {
  if package_is_installed "$1" ; then
    debug "Found existing $1. Skipping..."
    return
  fi

  debug "Installing $1..."
  apt-get install --yes $1
  debug "$1 installation complete."
}

help() {
  cat << EOF
This script installs Photonvision.
It must be run as root.

Syntax: sudo ./install.sh [options]
  options:
  -h, --help
      Display this help message.
  -a <arch>, --arch=<arch>
      Install PhotonVision for the specified architecture.
      Supported values: aarch64, x86_64
  -m, --install-nm
      Install and configure NetworkManager (Ubuntu only).
  -n, --no-networking
      Disable networking. This will also prevent installation of
      NetworkManager.
  -q, --quiet
      Silent install, automatically accepts all defaults. For
      non-interactive use.

EOF
}

INSTALL_NETWORK_MANAGER="false"

while getopts "ha:mnq-:" OPT; do
  if [ "$OPT" = "-" ]; then
    OPT="${OPTARG%%=*}"       # extract long option name
    OPTARG="${OPTARG#"$OPT"}" # extract long option argument (may be empty)
    OPTARG="${OPTARG#=}"      # if long option argument, remove assigning `=`
  fi

  case "$OPT" in
    h | help)
      help
      exit 0
      ;;
    a | arch) needs_arg; ARCH=$OPTARG
      ;;
    m | install-nm) INSTALL_NETWORK_MANAGER="true"
      ;;
    n | no-networking) DISABLE_NETWORKING="true"
      ;;
    q | quiet) QUIET="true"
      ;;
    \?)  # Handle invalid short options
      die "Error: Invalid option -$OPTARG" \
          "See './install.sh -h' for more information."
      ;;
    * )  # Handle invalid long options
      die "Error: Invalid option --$OPT" \
          "See './install.sh -h' for more information."
      ;;
  esac
done

shift $(($OPTIND -1))

if [ "$(id -u)" != "0" ]; then
   die "This script must be run as root"
fi

if [[ -z "$ARCH" ]]; then
  debug "Arch was not specified. Inferring..."
  ARCH=$(uname -m)
  debug "Arch was inferred to be $ARCH"
fi

ARCH_NAME=""
if [ "$ARCH" = "aarch64" ]; then
  ARCH_NAME="linuxarm64"
elif [ "$ARCH" = "armv7l" ]; then
  die "ARM32 is not supported by PhotonVision. Exiting."
elif [ "$ARCH" = "x86_64" ]; then
  ARCH_NAME="linuxx64"
else
  die "Unsupported or unknown architecture: '$ARCH'." \
  "Please specify your architecture using: ./install.sh -a <arch> " \
  "Run './install.sh -h' for more information."
fi

debug "This is the installation script for PhotonVision."
debug "Installing for platform $ARCH"

DISTRO=$(lsb_release -is)
if [[ "$DISTRO" = "Ubuntu" && "$INSTALL_NETWORK_MANAGER" != "true" && -z "$QUIET" && -z "$DISABLE_NETWORKING" ]]; then
  debug ""
  debug "Photonvision uses NetworkManager to control networking on your device."
  read -p "Do you want this script to install and configure NetworkManager? [y/N]: " response
  if [[ $response == [yY] || $response == [yY][eE][sS] ]]; then
    INSTALL_NETWORK_MANAGER="true"
  fi
fi

debug "Updating package list..."
apt-get update
debug "Updated package list."

install_if_missing curl
install_if_missing avahi-daemon
install_if_missing cpufrequtils
install_if_missing libatomic1
install_if_missing v4l-utils
install_if_missing sqlite3
install_if_missing openjdk-17-jre-headless

debug "Setting cpufrequtils to performance mode"
if [ -f /etc/default/cpufrequtils ]; then
    sed -i -e 's/^#\?GOVERNOR=.*$/GOVERNOR=performance/' /etc/default/cpufrequtils
else
    echo 'GOVERNOR=performance' > /etc/default/cpufrequtils
fi

if [[ "$INSTALL_NETWORK_MANAGER" == "true" ]]; then
  debug "NetworkManager installation specified. Installing components..."
  install_if_missing network-manager
  install_if_missing net-tools

  debug "Configuring..."
  systemctl disable systemd-networkd-wait-online.service
  cat > /etc/netplan/00-default-nm-renderer.yaml <<EOF
network:
  renderer: NetworkManager
EOF
  debug "network-manager installation complete."
fi

debug ""
debug "Installing additional math packages"
if [[ "$DISTRO" = "Ubuntu" && -z $(apt-cache search libcholmod3) ]]; then
  debug "Adding jammy to list of apt sources"
  add-apt-repository -y -S 'deb http://ports.ubuntu.com/ubuntu-ports jammy main universe'
fi

install_if_missing libcholmod3
install_if_missing liblapack3
install_if_missing libsuitesparseconfig5

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
