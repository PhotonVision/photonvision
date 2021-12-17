# We need to look for a JAR with the "-raspi" suffix so we don't accidentally bundle the big jar
# Not that it really matters, but it'll save us 50 megs or so
NEW_JAR=$(realpath $(find . -name photonvision\*-raspi.jar))
sudo apt install unzip zip
curl -sk https://api.github.com/repos/photonvision/photon-pi-gen/releases/tags/v2021.1.4 | grep "browser_download_url.*zip" | cut -d : -f 2,3 | tr -d '"' | wget -qi -
FILE_NAME=$(ls | grep image_*.zip)
unzip $FILE_NAME
IMAGE_FILE=$(ls | grep *.img)
TMP=$(mktemp -d)
LOOP=$(sudo losetup --show -fP "${IMAGE_FILE}")
sudo mount ${LOOP}p2 $TMP
pushd .
cd $TMP/opt/photonvision
ls
sudo cp $NEW_JAR photonvision.jar
popd
sudo umount ${TMP}
sudo rmdir ${TMP}
rm $FILE_NAME
NEW_IMAGE=$(basename "${NEW_JAR/jar/img}")
mv $IMAGE_FILE $NEW_IMAGE
zip -r $(basename "${NEW_JAR/.jar/-image.zip}") $NEW_IMAGE
rm $NEW_IMAGE
