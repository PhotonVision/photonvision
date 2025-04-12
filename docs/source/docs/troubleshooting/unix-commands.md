Useful Unix Commands
====================

SSH
---

[SSH (Secure Shell)](https://www.mankier.com/1/ssh) is used to securely connect from a local to a remote system (ex. from a laptop to a coprocessor). Unlike other commands on this page, ssh is not Unix specific and can be done on Windows and MacOS from their respective terminals.

:::{note}
    You may see a warning similar to "The authenticity of host 'xxx' can't be established..." or "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!", in most cases this can be safely ignored if you have confirmed that you are connecting to the correct host over a secure connection. You can read more about it `here <https://superuser.com/questions/421997/what-is-a-ssh-key-fingerprint-and-how-is-it-generated>`_.
:::

Example:

:::{code-block}
    ssh username@hostname
:::

ip
--

Run `ip address <https://www.mankier.com/8/ip>`_ with your coprocessor connected to a monitor in order to see its IP address and other network configuration information.


SCP
---

`SCP (Secure Copy) <https://www.mankier.com/1/scp>`_ is used to securely transfer files between local and remote systems.

Example:

.. code-block:: bash

    scp [file] username@hostname:/path/to/destination

v4l2-ctl
--------

`v4l2-ctl <https://www.mankier.com/1/v4l2-ctl>`_ is a command-line tool for controlling video devices.

List available video devices (used to verify the device recognized a connected camera):

.. code-block:: bash

    v4l2-ctl --list-devices

List supported formats and resolutions for a specific video device:

.. code-block:: bash

    v4l2-ctl --list-formats-ext --device /path/to/video_device

List all video device's controls and their values:

.. code-block:: bash

    v4l2-ctl --list-ctrls --device path/to/video_device

.. note::

    This command is especially useful in helping to debug when certain camera controls, like exposure, aren't behaving as expected. If you see an error in the logs similar to "WARNING 30: failed to set property [property name] (UsbCameraImpl.cpp:646)", that means that PhotonVision is trying to use a control that doesn't exist or has a different name on your hardware. If you encounter this issue, please `file an issue <https://github.com/PhotonVision/photonvision/issues>`_ with the necessary logs and output of the v4l2-ctl --list-ctrls command.
