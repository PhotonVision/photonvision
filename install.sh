# Do before
# curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
# nvm install 18
# nvm use 18
# ./gradlew --stop

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

git config --add oh-my-zsh.hide-status 1
git config --add oh-my-zsh.hide-dirty 1

sudo apt install openjdk-17-jdk
sudo apt install openjdk-21-jdk

sudo apt install openjdk-17-jre
sudo apt install openjdk-21-jre

cd photon-client
npm install

cd ../
./gradlew buildAndCopyUI
./gradlew shadowJar

