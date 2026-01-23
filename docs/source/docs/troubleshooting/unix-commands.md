# Useful Unix Commands

## Networking

### SSH

[SSH (Secure Shell)](https://www.mankier.com/1/ssh) is used to securely connect from a local to a remote system (ex. from a laptop to a coprocessor). Unlike other commands on this page, ssh is not Unix specific and can be done on Windows and MacOS from their respective terminals.

:::{note}
You may see a warning similar to `The authenticity of host 'xxx' can't be established...` or `WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!`, in most cases this can be safely ignored if you have confirmed that you are connecting to the correct host over a secure connection, and the fingerprint will change when your operating system is reinstalled or PhotonVision's coprocessor image is re-flashed. This can also occur if you have multiple coprocessors with the same hostname on your network. You can read more about it [here](https://superuser.com/questions/421997/what-is-a-ssh-key-fingerprint-and-how-is-it-generated)
:::

Example:

```
ssh pi@hostname
```

For PhotonVision, the username will be `pi` and the password will be `raspberry`.

### ip

Run [ip address](https://www.mankier.com/8/ip) with your coprocessor connected to a monitor in order to see its IP address and other network configuration information.

Your output might look something like this:

```
2: end1: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP group default qlen 1000
    link/ether de:9a:8f:7d:31:aa brd ff:ff:ff:ff:ff:ff
    inet 10.88.47.12/24 brd 10.88.47.255 scope global dynamic noprefixroute end1
        valid_lft 27367sec preferred_lft 27367sec
```

In this example, the numbers following `inet` (10.88.47.12) are your IP address.

### ping

[ping](https://www.mankier.com/8/ping) is a command-line utility used to test the reachability of a host on an IP network. It also measures the round-trip time for messages sent from the originating host to a destination computer. It can be used to determine if a network interface is available, which can be helpful when debugging.

## File Transfer

All files under `/opt/photonvision` are owned by the root user. This means that if you want to modify them, the commands to do so must be ran as sudo.

### SCP

[SCP (Secure Copy)](https://www.mankier.com/1/scp) is used to securely transfer files between local and remote systems.

Example:

```
scp [file] pi@hostname:/path/to/destination
```

### SFTP

[SFTP (SSH File Transfer Protocol)](https://www.mankier.com/1/sftp#) is another option for transferring files between local and remote systems.

### Filezilla

[Filezilla](https://filezilla-project.org/) is a GUI alternative to SCP and SFTP. It is available for Windows, MacOS, and Linux.

## Miscellaneous

### v4l2-ctl

[v4l2-ctl](https://www.mankier.com/1/v4l2-ctl) is a command-line tool for controlling video devices.

List available video devices (used to verify the device recognized a connected camera):

```
v4l2-ctl --list-devices
```

List supported formats and resolutions for a specific video device:

```
v4l2-ctl --list-formats-ext --device /path/to/video_device
```

List all video device's controls and their values:

```
v4l2-ctl --list-ctrls --device path/to/video_device
```

:::{note}
This command is especially useful in helping to debug when certain camera controls, like exposure, aren't behaving as expected. If you see an error in the logs similar to `WARNING 30: failed to set property [property name] (UsbCameraImpl.cpp:646)`, that means that PhotonVision is trying to use a control that doesn't exist or has a different name on your hardware. If you encounter this issue, please [file an issue](https://github.com/PhotonVision/photonvision/issues) with the necessary logs and output of the `v4l2-ctl --list-ctrls` command.
:::

### systemctl

[systemctl](https://www.mankier.com/1/systemctl) is a command that controls the `systemd` system and service manager.

Start PhotonVision:

```
systemctl start photonvision
```

Stop PhotonVision:

```
systemctl stop photonvision
```

Restart PhotonVision:

```
systemctl restart photonvision
```

Check the status of PhotonVision:

```
systemctl status photonvision
```

### journalctl

[journalctl](https://www.mankier.com/1/journalctl) is a command that queries the systemd journal, which is a logging system used by many Linux distributions.

View the PhotonVision logs:

```
journalctl --output cat -u photonvision
```

View the PhotonVision logs in real-time:

```
journalctl --output cat -u photonvision -f
```

`--output cat` is used to prevent journalctl from printing its own timestamps, because we log our own timestamps.
