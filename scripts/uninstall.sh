#!/bin/bash

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

rm -rf /opt/photonvision/
echo "Uninstalling systemd service..."
systemctl stop photonvision
systemctl disable photonvision
rm /lib/systemd/system/photonvision.service
rm /etc/systemd/system/photonvision.service
systemctl dameon-relaod
systemctl reset-failed
