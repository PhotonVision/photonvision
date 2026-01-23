# Installing PhotonLib

## What is PhotonLib?

PhotonLib is the C++ and Java vendor dependency that accompanies PhotonVision. We created this vendor dependency to make it easier for teams to retrieve vision data from their integrated vision system.

PhotonLibPy is a minimal, pure-python implementation of PhotonLib.

## Online Install - Java/C++

Click on the WPILib logo in the activity bar to access the Vendor Dependencies interface.

```{image} images/wpilib-vendor-dependencies.png
:scale: 50%
:align: center
:alt: WPILib Vendor Dependencies
```

Select the install button for the "PhotonLib" dependency.

```{image} images/photonlib-install.png
:scale: 50%
:align: center
:alt: PhotonLib Install Button
```

:::{note}
The Dependency Manager will automatically build your program when it loses focus. This allows you to use the changed dependencies.
:::

When an update is available for PhotonLib, a "To Latest" button will become available. This will update the vendordep to the latest version of PhotonLib.

```{image} images/photonlib-to-latest.png
:align: center
:alt: PhotonLib Update Button
```

Refer to [The WPILib docs](https://docs.wpilib.org/en/stable/docs/software/vscode-overview/3rd-party-libraries.html#installing-libraries) for more details on installing vendor libraries.

## Offline Install - Java/C++

Download the latest PhotonLib release from our [GitHub releases page](https://github.com/PhotonVision/photonvision/releases) (named in the format `photonlib-VERSION.zip`), and extract the contents to `~/wpilib/YYYY/vendordeps` (where YYYY is the year and ~ is `C:\Users\Public` on Windows). This adds PhotonLib maven artifacts to your local maven repository. PhotonLib will now also appear available in the "install vendor libraries (offline)" menu in WPILib VSCode. Refer to [the WPILib docs](https://docs.wpilib.org/en/stable/docs/software/vscode-overview/3rd-party-libraries.html#how-does-it-work) for more details on installing vendor libraries offline.

## Install - Python

Add photonlibpy to `pyproject.toml`.

```toml
# Other pip packages to install
requires = [
    "photonlibpy",
]
```

See [The WPILib/RobotPy docs](https://docs.wpilib.org/en/stable/docs/software/python/pyproject_toml.html) for more information on using `pyproject.toml.`

## Install Specific Version - Java/C++

In cases where you want to test a specific version of PhotonLib, make sure you have finished the steps in Online Install - Java/C++ and then manually change the version string in the PhotonLib vendordep json file(at ``/path/to/your/project/vendordep/photonlib.json``) to your desired version.

```{image} images/photonlib-vendordep-json.jpg
```
