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
curl -sk $1 | grep "browser_download_url.*xz" | cut -d : -f 2,3 | tr -d '"' | wget -qi -
ls
FILE_NAME=$(ls | grep image_*.xz)

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
echo "Image mounted! Copying jar..."
sudo mount ${LOOP}p2 $TMP
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
