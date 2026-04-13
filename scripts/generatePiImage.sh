if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters -- expected (Image release URL) (image suffix)"
    echo "Optional environment overrides:"
    echo "  PV_ROOT_PARTITION    Root filesystem partition number (default: 2)"
    echo "  PV_PHOTON_DIR        Photon install directory inside the image (default: /opt/photonvision)"
    echo "  PV_SYSTEMD_UNIT_DIR  systemd wants directory inside the image (default: /etc/systemd/system/multi-user.target.wants)"
    exit 1
fi

# 1st arg should be the release to download the image template from. The release ought to only have one
# artifact for a "xz" image.

ROOT_PARTITION="${PV_ROOT_PARTITION:-2}"
PHOTON_DIR="${PV_PHOTON_DIR:-/opt/photonvision}"
SYSTEMD_UNIT_DIR="${PV_SYSTEMD_UNIT_DIR:-/etc/systemd/system/multi-user.target.wants}"

NEW_JAR=$(realpath $(find . -name photonvision\*-linuxarm64.jar))
echo "Using jar: " $NEW_JAR
echo "Downloading image from" $1
sudo apt-get install -y xz-utils
wget -q $1
ls
FILE_NAME=$(ls | grep *.xz)

if [ -z "$FILE_NAME" ]
then
    echo "Could not locate image archive!"
    exit 1
fi

echo "Downloaded " $FILE_NAME " -- decompressing now..."
xz -T0 -v --decompress $FILE_NAME
IMAGE_FILE=$(ls | grep *.img)
ls

if [ -z "$FILE_NAME" ]
then
    echo "Could not locate unzipped image!"
    exit 1
fi

echo "Unzipped image: " $IMAGE_FILE " -- mounting"
TMP=$(mktemp -d)
LOOP=$(sudo losetup --show -fP "${IMAGE_FILE}")
echo "Image mounted! Copying jar..."
sudo mount ${LOOP}p${ROOT_PARTITION} $TMP
pushd .
cd "$TMP$PHOTON_DIR"
sudo cp $NEW_JAR photonvision.jar

echo "Jar updated! Creating service..."

cd "$TMP$SYSTEMD_UNIT_DIR"
sudo bash -c "printf \
\"[Unit]
Description=Service that runs PhotonVision

[Service]
WorkingDirectory=${PHOTON_DIR}
ExecStart=/usr/bin/java -Xmx512m -jar ${PHOTON_DIR}/photonvision.jar
ExecStop=/bin/systemctl kill photonvision
Type=simple
Restart=on-failure
RestartSec=1

[Install]
WantedBy=multi-user.target\" > photonvision.service"

popd

echo "Service created!"

sudo umount ${TMP}
sudo rmdir ${TMP}
NEW_IMAGE=$(basename "${NEW_JAR/.jar/_$2.img}")
echo "Renaming image " $IMAGE_FILE " -> " $NEW_IMAGE
mv $IMAGE_FILE $NEW_IMAGE
xz -T0 -v -z $NEW_IMAGE
mv $NEW_IMAGE.xz $(basename "${NEW_JAR/.jar/-image_$2.xz}")
