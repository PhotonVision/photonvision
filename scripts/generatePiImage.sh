if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters -- expected (Image release URL) (image suffix)"
    exit 1
fi

# 1st arg should be the release to download the image template from. The release ought to only have one
# artifact for a "xz" image.

# These are workarounds for the OrangePi. The current image does not use `pushd`, `popd` or `dirs`.
DIR_STACK=""

opi_pushd() {
    if [ $# -eq 0 ]; then
        echo "Directory stack:"
	    for dir in $DIR_STACK; do
	        echo "- $dir"
	    done
    else
        DIR_STACK="$DIR_STACK $PWD"
	    cd "$1" || return
        # debugging only
        echo "Added ${1} to the directory stack"
    fi
}

opi_popd() {
    if [ -z $DIR_STACK ]; then
        echo "No directories"
    else
        LAST_ITEM=$(echo "$my_list" | awk '{print $NF}')
	    POPPED_LIST="${DIR_STACK% *}"

        # debugging only
        echo "Removed ${LAST_ITEM} from the directory stack."
        echo "Reassigning the DIR_STACK to `$POPPED_LIST`"

        cd "$LAST_ITEM"
	    DIR_STACK=$POPPED_LIST
    fi
}

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

# echo "Confirming that loop partition exists"
# if ! lsblk | grep -q "$(basename $PARTITION)"; then
#     echo "Loop device was not found in lsblk output. Creating it now."
#     sudo parted $LOOP mklabel msdos

#     sudo parted $LOOP mkpart primary ext4 0% 50% > /dev/null 2>&1
#     sudo mkfs.ext4 "${LOOP}p1" > /dev/null 2>&1

#     sudo parted $LOOP mkpart primary ext4 50% 100% > /dev/null 2>&1
#     sudo mkfs.ext4 $PARTITION > /dev/null 2>&1

#     if ! lsblk | grep -q "$(basename $PARTITION)"; then
#         echo "Failed to create partition. Exiting."
#         exit 1
#     fi

#     echo "Created loop device partition"
# fi

echo "Image mounted! Copying jar..."
sudo mount $PARTITION $TMP


# if ! command -v pushd > /dev/null 2>&1; then
#     echo "Overwriting pushd because it doesn't exist."
#     alias pushd='opi_pushd'
# fi

# if ! command -v popd > /dev/null 2>&1; then
#     echo "Overwriting popd because it doesn't exist."
#     alias popd='opi_popd'
# fi

pushd .

DEST_PV_LOCATION=$TMP/opt/photonvision

sudo mkdir -p $DEST_PV_LOCATION
cd $DEST_PV_LOCATION
sudo cp $NEW_JAR photonvision.jar

echo "Jar updated! Creating service..."

DEST_TARGET_WANTS=$TMP/etc/systemd/system/multi-user.target.wants
sudo mkdir -p $DEST_TARGET_WANTS
cd $DEST_TARGET_WANTS
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