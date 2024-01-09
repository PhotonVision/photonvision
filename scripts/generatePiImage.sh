if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters -- expected (Image release URL) (image suffix)"
    exit 1
fi

# 1st arg should be the release to download the image template from. The release ought to only have one
# artifact for a "xz" image.

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

echo "Unziped image: " $IMAGE_FILE " -- mounting"
TMP=$(mktemp -d)
LOOP=$(sudo losetup --show -fP "${IMAGE_FILE}")
PARTITION="${LOOP}p2"

echo "Confirming that loop partition exists"
if ! lsblk | grep -q "$(basename $PARTITION)"; then
    echo "Loop device was not found in lsblk output."
    sudo parted $LOOP mklabel msdos

    sudo parted $LOOP mkpart primary ext4 0% 50%
    sudo mkfs.ext4 "${LOOP}p1"

    sudo parted $LOOP mkpart primary ext4 50% 100%
    sudo mkfs.ext4 $PARTITION

    if ! lsblk | grep -q "$(basename $PARTITION)"; then
        echo "Failed to create partition. Exiting."
	    exit 1
    fi

    echo "Created loop device partition"
fi

echo "Image mounted! Copying jar..."
sudo mount $PARTITION $TMP
pushd .
cd $TMP/opt/photonvision
sudo cp $NEW_JAR photonvision.jar

echo "Jar updated! Creating service..."

cd $TMP/etc/systemd/system/multi-user.target.wants
sudo bash -c "printf \
\"[Unit]
Description=Service that runs PhotonVision

[Service]
WorkingDirectory=/opt/photonvision
ExecStart=/usr/bin/java -Xmx512m -jar /opt/photonvision/photonvision.jar
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
