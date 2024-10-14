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

get_photonvision_releases() {
  if ! command -v curl > /dev/null 2>&1 ; then
    debug "./install --list-versions requires curl and it is not installed."

    read -p "Would you like to install curl? [y/N]: " response
    if [[ $response == [nN] || $response == [nN][oO] ]]; then
      die
    fi

    apt-get install --yes curl
  fi

  if [ -z "$PHOTON_VISION_RELEASES" ] ; then
    PHOTON_VISION_RELEASES="$(curl -sk https://api.github.com/repos/photonvision/photonvision/releases)"
  fi

 echo "$PHOTON_VISION_RELEASES"
}

get_versions() {
  if [ -z "$PHOTON_VISION_VERSIONS" ] ; then
    PHOTON_VISION_VERSIONS=$(get_photonvision_releases | \
      sed -En 's/\"tag_name\": \"v([0-9]+\.[0-9]+\.[0-9]+)(-(beta|alpha)(-[0-9])?(\.[0-9]+)?)?\",/\1\2/p' | \
      sed 's/^[[:space:]]*//')
  fi

  echo "$PHOTON_VISION_VERSIONS"
}

is_version_available() {
  local target_version="$1"

  # latest is a special case
  if [ "$target_version" = "latest" ]; then
    return
  fi

  # Check if the version is present
  if ! echo "$(get_versions)" | grep -qFx "$target_version"; then
    return 1
  fi

  # Check if multiple lines are match. You can only match 1.
  local line_count="$(echo "$versions" | grep -cFx "$target_string")"
  if [ $line_count -gt 1 ] ; then
    return 1
  fi

  return 0
}

help() {
  cat << EOF
This script installs Photonvision.
It must be run as root.

Syntax: sudo ./install.sh [options]
  options:
  -h, --help
      Display this help message.
  -l, --list-versions
      Lists all available versions of PhotonVision.
  -v <version>, --version=<version>
      Specifies which version of PhotonVision to install.
      If not specified, the latest stable release is installed.
      Ignores leading 'v's.
  -a <arch>, --arch=<arch>
      Install PhotonVision for the specified architecture.
      Supported values: aarch64, x86_64
  -m [option], --install-nm=[option]
      Whether or not to install NetworkManager. Only used on
      Ubuntu, and ignored for all other distros.
      Supported options are: "yes", "no", and "ask".
      "ask" prompts the user for installation of NetworkManager.
      If not specified, will fall back to "ask".
      If specified, "yes" is the default option.
  -n, --no-networking
      Disable networking. This will also prevent installation of
      NetworkManager (ignoring -m,--install-nm).
  -q, --quiet
      Silent install, automatically accepts all defaults. For
      non-interactive use. Forces --install-nm="no".

EOF
}

INSTALL_NETWORK_MANAGER="ask"
VERSION="latest"

while getopts "hlv:a:mnq-:" OPT; do
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
    l | list-versions)
      get_versions
      exit 0
      ;;
    v | version)
      needs_arg
      VERSION=$(echo "$OPTARG" | sed 's/^v//')  # drop leading 'v's
      ;;
    a | arch) needs_arg; ARCH=$OPTARG
      ;;
    m | install-nm)
      INSTALL_NETWORK_MANAGER="$(echo ${OPTARG:-'yes'} | tr '[:upper:]' '[:lower:]')"
      case "$INSTALL_NETWORK_MANAGER" in
        yes)
          ;;
        no)
          ;;
        ask)
          ;;
        * )
          die "Valid options for -m, --install-nm are: 'yes', 'no', and 'ask'"
          ;;
      esac
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

if [[ "$DISTRO" != "Ubuntu" || -n "$DISABLE_NETWORKING" || -n "$QUIET" ]] ; then
  INSTALL_NETWORK_MANAGER="no"
fi

if [[ "$INSTALL_NETWORK_MANAGER" == "ask" ]]; then
  debug ""
  debug "Photonvision uses NetworkManager to control networking on your device."
  read -p "Do you want this script to install and configure NetworkManager? [y/N]: " response
  if [[ $response == [yY] || $response == [yY][eE][sS] ]]; then
    INSTALL_NETWORK_MANAGER="yes"
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

if [[ "$INSTALL_NETWORK_MANAGER" == "yes" ]]; then
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

debug ""

if ! is_version_available "$VERSION" ; then
  die "PhotonVision v$VERSION is not available" \
      "See ./install --list-versions for a complete list of available versions."
fi

if [ "$VERSION" = "latest" ] ; then
  RELEASE_URL="https://api.github.com/repos/photonvision/photonvision/releases/latest"
  debug "Downloading PhotonVision (latest)..."
else
  RELEASE_URL="https://api.github.com/repos/photonvision/photonvision/releases/tags/v$VERSION"
  debug "Downloading PhotonVision (v$VERSION)..."
fi

mkdir -p /opt/photonvision
cd /opt/photonvision
curl -sk "$RELEASE_URL" |
    grep "browser_download_url.*$ARCH_NAME.jar" |
    cut -d : -f 2,3 |
    tr -d '"' |
    wget -qi - -O photonvision.jar
debug "Downloaded PhotonVision."

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
