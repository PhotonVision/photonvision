# Romi Installation

The [Romi](https://docs.wpilib.org/en/latest/docs/romi-robot/index.html) is a small robot that can be controlled with the WPILib software. The main controller is a Raspberry Pi that must be imaged with [WPILibPi](https://docs.wpilib.org/en/latest/docs/romi-robot/imaging-romi.html) .

## Installation

The WPILibPi image includes FRCVision, which reserves USB cameras; to use PhotonVision, we need to edit the `/home/pi/runCamera` script to disable it. First we will need to make the file system writeable; the easiest way to do this is to go to `10.0.0.2` and choose "Writable" at the top.

SSH into the Raspberry Pi (using Windows command line, or a tool like [Putty](https://www.chiark.greenend.org.uk/~sgtatham/putty/) ) at the Romi's default address `10.0.0.2`. The default user is `pi`, and the password is `raspberry`.

:::.. The following paragraph can be restored when WPILibPi becomes compatible with the current version of PhotonVision.
:::.. Follow the process for installing PhotonVision on {ref}`"Other Debian-Based Co-Processor Installation" <docs/advanced-installation/sw_install/other-coprocessors:Other Debian-Based Co-Processor Installation>`. As it mentions, this will require an internet connection so connecting the Raspberry Pi to an internet-connected router via an Ethernet cable will be the easiest solution. The pi must remain writable while you are following these steps!

:::..Temporary instructions explaining how to install the older version of PhotonVision on a Romi. Remove when no longer needed.
:::{attention}
The version of WPILibPi for the Romi is 2023.2.1, which is not compatible with the current version of PhotonVision. **If you are using WPILibPi 2023.2.1 on your Romi, you must install PhotonVision v2023.4.2 or earlier!**

To install a compatible version of PhotonVision, enter these commands in the SSH terminal connected to the Raspberry Pi. This will download and run the install script, which will install PhotonVision on your Raspberry Pi and configure it to run at startup.

```bash
$ wget https://git.io/JJrEP -O install.sh
$ sudo chmod +x install.sh
$ sudo ./install.sh -v v2023.4.2
```
The install script requires an internet connection, so connecting the Raspberry Pi to an internet-connected router via an Ethernet cable will be the easiest solution. The pi must remain writable while you are following these steps!
:::
:::..End of temporary instructions.

Next, from the SSH terminal, run `sudo nano /home/pi/runCamera` then arrow down to the start of the exec line and press "Enter" to add a new line. Then add `#` before the exec command to comment it out. Then, arrow up to the new line and type `sleep 10000`. Hit "Ctrl + O" and then "Enter" to save the file. Finally press "Ctrl + X" to exit nano. Now, reboot the Romi by typing `sudo reboot now`.

```{image} images/nano.png

```

After the Romi reboots, you should be able to open the PhotonVision UI at: [`http://10.0.0.2:5800/`](http://10.0.0.2:5800/). From here, you can adjust settings and configure {ref}`Pipelines <docs/pipelines/index:Pipelines>`.

:::{warning}
In order for settings, logs, etc. to be saved / take effect, ensure that PhotonVision is in writable mode.
:::

:::{attention}
When using an older version of PhotonVision, the user interface and features may be different than what appears in the online documentation. The [Documentation](http://10.0.0.2:5800/#/docs) link in the User Interface will open a bundled version of the documentation that matches the PhotonVision version running on your coprocessor.
:::
