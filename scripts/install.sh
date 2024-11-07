#!/bin/bash
# The install script is now in photon-image-modifier
# this downloads and runs that install script for people using the old short URL
wget -q https://raw.githubusercontent.com/PhotonVision/photon-image-modifier/master/install.sh -O ./real_install.sh
chmod +x ./real_install.sh
./real_install.sh "$@"
rm ./real_install.sh
