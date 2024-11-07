# Quick Configure

## Variables to configure

### Network Hostname

Rename each device from the default "Photonvision" to a unique hostname (e.g., "Photon-OrangePi-Left" or "Photon-RPi5-Back"). This helps differentiate multiple coprocessors on your network, making it easier to manage them.

### Pipeline Settings

#### Apriltag

When using an Orange pi 5 with an OV9281 teams will usually change the following settings.

- Resolution:
  - 1280x800
- Decimate:
  - 2
- Mode:
  - 3D
- Exposure and Gain:
  - Adjust these to achieve good brightness without flicker. This may vary based on lighting conditions in your competition environment.

#### Object Detection

- Resolution:
  - Resolutions larger than 640x640 may not result in any more accurate detection and may lower performance.
- Confidence:
  - 0.75 - 0.95 Depending on if you want detection of warn game pieces or low false positives.
- White Balance Temperature:
  - Adjust this to achieve better color accuracy. This may be needed to increase confidence.
