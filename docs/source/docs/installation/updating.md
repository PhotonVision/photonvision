# Updating PhotonVision

PhotonVision provides many different files on a single release page. Each release contains JAR files for performing "offline updates" of a device with PhotonVision already installed, as well as full image files to "flash" to supported coprocessors.

```{image} images/release-page.png
:alt: Example GitHub release page
```

In the example release above, we see:

- Image files for flashing directly to supported coprocessors.

  - Raspberry Pi 3/4/5/CM4: follow our {ref}`Raspberry Pi flashing instructions<docs/installation/sw_install/raspberry-pi:raspberry pi installation>`.
  - For LimeLight devices: follow our {ref}`LimeLight flashing instructions<docs/installation/sw_install/limelight:imaging>`.
  - For Orange Pi 5 devices: follow our {ref}`Orange Pi flashing instructions<docs/installation/sw_install/orange-pi:orange pi installation>`.

- JAR files for the suite of supported operating systems for use with Offline Update. In general:

  - Raspberry Pi, Limelight, and Orange Pi: use images suffixed with -linuxarm64.jar. For example: {code}`photonvision-v2024.1.1-linuxarm64.jar`
  - Beelink and other Intel/AMD-based Mini-PCs: use images suffixed with -linuxx64.jar. For example: {code}`photonvision-v2024.1.1-linuxx64.jar`

## Offline Update

Unless noted in the release page, an offline update allows you to quickly upgrade the version of PhotonVision running on a coprocessor with PhotonVision already installed on it.

Unless otherwise noted on the release page, config files should be backward compatible with previous version of PhotonVision, and this offline update process should preserve any pipelines and calibrations previously performed. For paranoia, we suggest exporting settings from the Settings tab prior to performing an offline update.

:::{note}
Carefully review release notes to ensure that reflashing the device (for supported devices) or other installation steps are not required, as dependencies needed for PhotonVision may change between releases
:::

## Installing Pre-Release Versions

Pre-release/development version of PhotonVision can be tested by installing/downloading artifacts from Github Actions (see below), which are built automatically on commits to open pull requests and to PhotonVision's `master` branch, or by {ref}`compiling PhotonVision locally <docs/contributing/building-photon:Build Instructions>`.

:::{warning}
If testing a pre-release version of PhotonVision with a robot, PhotonLib must be updated to match the version downloaded! If not, packet schema definitions may not match and unexpected things will occur. To update PhotonLib, refer to {ref}`installing specific version of PhotonLib<docs/programming/photonlib/adding-vendordep:Install Specific Version - Java/C++>`.
:::

GitHub Actions builds pre-release version of PhotonVision automatically on PRs and on each commit merged to master. To test a particular commit to master, navigate to the [PhotonVision commit list](https://github.com/PhotonVision/photonvision/commits/master/) and click on the check mark (below). Scroll to "Build / Build fat JAR - PLATFORM", click details, and then summary. From here, JAR and image files can be downloaded to be flashed or uploaded using "Offline Update".

```{image} images/gh_actions_1.png
:alt: Github Actions Badge
```

```{image} images/gh_actions_2.png
:alt: Github Actions artifact list
```

Built JAR files (but not image files) can also be downloaded from PRs before they are merged. Navigate to the PR in GitHub, and select Checks at the top. Click on "Build" to display the same artifact list as above.

```{image} images/gh_actions_3.png
:alt: Github Actions artifacts from PR
```
