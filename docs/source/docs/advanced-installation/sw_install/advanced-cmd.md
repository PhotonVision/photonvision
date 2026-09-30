# Advanced Command Line Usage

PhotonVision exposes some command line options which may be useful for customizing execution on Debian-based installations.

## Running a JAR File

Assuming `java` has been installed, and the appropriate environment variables have been set upon installation (a package manager like `apt` should automatically set these), you can use `java -jar` to run a JAR file. If you downloaded the latest stable JAR of PhotonVision from the [GitHub releases page](https://github.com/PhotonVision/photonvision/releases), you can run the following to start the program:

```bash
java -jar /path/to/photonvision/photonvision.jar
```

## Disabling Networking

PhotonVision normally manages the network configuration of the machine it runs on. On a coprocessor that is what makes the device reachable at `photonvision.local` and at a fixed IP, but it also rewrites the hostname and the static IP addresses of the host, which is usually not what you want when you are running the JAR on your own computer for development or testing.

Pass `-n` (or `--disable-networking`) to leave the host network configuration alone:

```bash
java -jar /path/to/photonvision/photonvision.jar -n
```

With this flag set, PhotonVision skips network management entirely at startup and logs `Network management is disabled.` It does not change the hostname, it does not write static IP configuration, and it does not monitor the network interfaces. Since there is nothing left to configure, the networking controls in the web dashboard are disabled as well.

:::{note}
This does not turn the networking stack off. The web dashboard is still served on port 5800, so you can reach it at `http://localhost:5800`, and camera streams keep working.
:::

:::{note}
Network management is also skipped automatically when PhotonVision is not running on Linux, when it is not running as `root`, or when `nmcli` is not installed.
:::

## Updating a JAR File

When you need to update your JAR file, run the following:

```bash
wget https://git.io/JqkQ9 -O update.sh
sudo chmod +x update.sh
sudo ./update.sh
sudo reboot now
```

## Creating a `systemd` Service

You can also create a systemd service that will automatically run on startup. To do so, first navigate to `/lib/systemd/system`. Create a file called `photonvision.service` (or name it whatever you want) using `touch photonvision.service`. Then open this file in the editor of your choice and paste the following text:

```
[Unit]
Description=Service that runs PhotonVision

[Service]
WorkingDirectory=/path/to/photonvision
# Optional: run photonvision at "nice" -10, which is higher priority than standard
# Nice=-10
ExecStart=/usr/bin/java -jar /path/to/photonvision/photonvision.jar

[Install]
WantedBy=multi-user.target
```

Then copy the `.service` file to `/etc/systemd/system/` using `cp photonvision.service /etc/systemd/system/photonvision.service`. Then modify the file to have `644` permissions using `chmod 644 /etc/systemd/system/photonvision.service`.

:::{note}
Many ARM processors have a big.LITTLE architecture where some of the CPU cores are more powerful than others. On this type of architecture, you may get more consistent performance by limiting which cores PhotonVision can use. To do this, add the parameter `AllowedCPUs` to the systemd service file in the `[Service]` section.

For instance, for an Orange Pi 5, cores 4 through 7 are the fast ones, and you can target those cores with the line `AllowedCPUs=4-7`.
:::

## Installing the `systemd` Service

To install the service, simply run `systemctl enable photonvision.service`.

:::{note}
It is recommended to reload configurations by running `systemctl daemon-reload`.
:::
