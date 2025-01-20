# Filesystem Directory

PhotonVision stores and loads settings in the {code}`photonvision_config` directory, in the same folder as the PhotonVision JAR is stored. On supported hardware, this is in the {code}`/opt/photonvision` directory. The contents of this directory can be exported as a zip archive from the settings page of the interface, under "export settings". This export will contain everything detailed below. These settings can later be uploaded using "import settings", to restore configurations from previous backups.

## Directory Structure

The directory structure is outlined below.

```{image} images/configDir.png
:alt: Config directory structure
:width: 600
```

- calibImgs
  - Images saved from the last run of the calibration routine
- cameras
  - Contains a subfolder for each camera. This folder contains the following files:
    - pipelines folder, which contains a {code}`json` file for each user-created pipeline.
    - config.json, which contains all camera-specific configuration. This includes FOV, pitch, current pipeline index, and calibration data
    - drivermode.json, which contains settings for the driver mode pipeline
- imgSaves
  - Contains images saved with the input/output save commands.
- logs
  - Contains timestamped logs in the format {code}`photonvision-YYYY-MM-D_HH-MM-SS.log`. These timestamps will likely be significantly behind the real time. Coprocessors on the robot have no way to get current time.
- hardwareSettings.json
  - Contains hardware settings. Currently this includes only the LED brightness.
- networkSettings.json
  - Contains network settings, including team number (or remote network tables address), static/dynamic settings, and hostname.

## Importing and Exporting Settings

The entire settings directory can be exported as a ZIP archive from the settings page.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/import-export-settings.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

A variety of files can be imported back into PhotonVision:

- ZIP Archive ({code}`.zip`)
  - Useful for restoring a full configuration from a different PhotonVision instance.
- Single Config File
  - Currently-supported Files
    - {code}`hardwareConfig.json`
    - {code}`hardwareSettings.json`
    - {code}`networkSettings.json`
  - Useful for simple hardware or network configuration tasks without overwriting all settings.



