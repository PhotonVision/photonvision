###
# Alternative ARM Runner installer to setup PhotonVision JAR
# for ARM based builds such as Raspberry Pi, Orange Pi, etc.
# This assumes that the image provided to arm-runner-action contains
# the servicefile needed to auto-launch PhotonVision.
###
set -x

NEW_JAR=$(realpath $(find . -name photonvision\*-linuxarm64.jar))
echo "Using jar: " $(basename $NEW_JAR)

# This is for debugging purposes
echo "Current working directory: $(pwd)"
echo "Contents:"
ls -la

DEST_PV_LOCATION=/opt/photonvision
sudo mkdir -p $DEST_PV_LOCATION
sudo cp $NEW_JAR ${DEST_PV_LOCATION}/photonvision.jar
