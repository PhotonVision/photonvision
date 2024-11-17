# Other Debian-Based Co-Processor Installation

:::{warning}
Working with unsupported coprocessors requires some level of "know how" of your system. The install script has only been tested on Debian/Raspberry Pi OS Buster and Ubuntu Bionic. If any issues arise with your specific OS, please open an issue on our [issues page](https://github.com/PhotonVision/photonvision/issues).
:::

:::{note}
We'd love to have your input! If you get PhotonVision working on another coprocessor, consider documenting your steps and submitting a [docs issue](https://github.com/PhotonVision/photonvision-docs/issues)., [pull request](https://github.com/PhotonVision/photonvision-docs/pulls) , or [ping us on Discord](https://discord.com/invite/wYxTwym). For example, Limelight and Romi install instructions came about because someone spent the time to figure it out, and did a writeup.
:::

## Installing PhotonVision

We provide an [install script](https://git.io/JJrEP) for other Debian-based systems (with `apt`) that will automatically install PhotonVision and make sure that it runs on startup.

```bash
$ wget https://git.io/JJrEP -O install.sh
$ sudo chmod +x install.sh
$ sudo ./install.sh
$ sudo reboot now
```

:::{note}
Your co-processor will require an Internet connection for this process to work correctly.
:::

For installation on any other co-processors, we recommend reading the {ref}`advanced command line documentation <docs/advanced-installation/sw_install/advanced-cmd:Advanced Command Line Usage>`.

## Updating PhotonVision

PhotonVision can be updated by downloading the latest jar file, copying it onto the processor, and restarting the service.

For example, from another computer, run the following commands. Substitute the correct username for "\[user\]" ( Provided images use username "pi")

```bash
$ scp [jar name].jar [user]@photonvision.local:~/
$ ssh [user]@photonvision.local
$ sudo mv [jar name].jar /opt/photonvision/photonvision.jar
$ sudo systemctl restart photonvision.service
```
