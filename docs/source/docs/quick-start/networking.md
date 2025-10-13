# Networking

## Physical Networking

:::{warning}
When using PhotonVision off robot, you _MUST_ plug the coprocessor into a physical router/radio. You can then connect your laptop/device used to view the webdashboard to the same network. Any other networking setup will not work and will not be supported in any capacity.
:::

::::{tab-set}

:::{tab-item} New Radio (2025 - present)

```{danger}
Ensure that the radio's DIP switches 1 and 2 are turned off; otherwise, the radio PoE feature may electrically destroy your coprocessor. [More info.](https://frc-radio.vivid-hosting.net/overview/wiring-your-radio#power-over-ethernet-poe-for-downstream-devices)
```

```{image} images/networking-diagram-vividhosting.png
:alt: Wiring using a network switch and the new vivid hosting radio
```

:::

:::{tab-item} Old Radio (pre 2025)

PhotonVision _STRONGLY_ recommends the usage of a network switch on your robot. This is because the second radio port on the old FRC radios is known to be buggy and cause frequent connection issues that are detrimental during competition. An in-depth guide on how to install a network switch can be found [on FRC 900's website](https://zebracorns.org/blog/ZebraSwitch/).

```{image} images/networking-diagram.png
:alt: Wiring using a network switch and the old open mesh radio
```

:::
::::

## Network Hostname

Rename each device from the default "photonvision" to a unique hostname (e.g., "Photon-OrangePi-Left" or "Photon-RPi5-Back"). This helps differentiate multiple coprocessors on your network, making it easier to manage them. Navigate to the settings page and scroll down to the network section. You will find the hostname is set to "photonvision" by default, this can only contain letters (A-Z), numeric characters (0-9), and the minus sign (-).

```{image} images/editHostname.png
:alt: The hostname can be edited in the settings page under the network section.
```

## Robot Networking

PhotonVision _STRONGLY_ recommends the usage of Static IPs as it increases reliability on the field and when using PhotonVision in general. To properly set up your static IP, follow the steps below:

:::{warning}
Only use a static IP when connected to the **robot radio**, and never when testing at home, unless you are well versed in networking or have the relevant "know how".
:::

1. Ensure your robot is on and you are connected to the robot network.
2. Navigate to `photonvision.local:5800`in your browser.
3. Open the settings tab on the left pane.
4. Under the Networking section, set your team number.
5. Change your IP to Static.
6. Set your coprocessor's IP address to “10.TE.AM.11”. More information on IP format can be found [here](https://docs.wpilib.org/en/stable/docs/networking/networking-introduction/ip-configurations.html#on-the-field-static-configuration).
7. Click the “Save” button.

Power-cycle your robot and then you will now be access the PhotonVision dashboard at `10.TE.AM.11:5800`.

```{image} images/static.png
:alt: Correctly set static IP
```

The "team number" field will accept (in addition to a team number) an IP address or hostname. This is useful for testing PhotonVision on the same computer as a simulated robot program;
you can set the team number to "localhost", and PhotonVision will send data to the network tables in the simulated robot.

## Port Forwarding

:::{note}
If you are using a VH-109 radio (2025 and later, excluding China and Taiwan), you should not use port forwarding. Instead, tether to the dedicated DS ethernet port on the VH-109. The VH-109 does not exhibit the issues found in the OM5P radio with multiple ports, and with a dedicated DS port, it provides more realistic match conditions and removes the need to tether over USB.
:::

If you would like to access your Ethernet-connected vision device from a computer when tethered to the USB port on the roboRIO, you can use [WPILib's](https://docs.wpilib.org/en/stable/docs/networking/networking-utilities/portforwarding.html) `PortForwarder`.

```{eval-rst}
.. tab-set-code::

    .. code-block:: java

        PortForwarder.add(5800, "photonvision.local", 5800);

    .. code-block:: c++

        wpi::PortForwarder::GetInstance().Add(5800, "photonvision.local", 5800);

    .. code-block:: python

        # Coming Soon!
```

:::{note}
The address in the code above (`photonvision.local`) is the hostname of the coprocessor. This can be different depending on your hardware, and can be checked in the settings tab under "hostname".
:::

## Camera Stream Ports

The camera streams start at 1181 with two ports for each camera (ex. 1181 and 1182 for camera one, 1183 and 1184 for camera two, etc.). The easiest way to identify the port of the camera that you want is by double clicking on the stream, which opens it in a separate page. The port will be listed below the stream.

:::{warning}
If your camera stream isn't sent to the same port as it's originally found on, its stream will not be visible in the UI.
:::

## SSH Access

For advanced users, SSH access is available for coprocessors running PhotonVision. This allows you to perform advanced configurations and troubleshooting. The default credentials are: `photon:vision` for all devices using an image of `v2026.0.3` or later. The legacy credentials of `pi:raspberry` will still work, but it's recommended to switch to the new credentials as the old ones will be deprecated in a future release.
