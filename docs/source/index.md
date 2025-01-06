```{image} assets/PhotonVision-Header-onWhite.png
:alt: PhotonVision
```

Welcome to the official documentation of PhotonVision! PhotonVision is the free, fast, and easy-to-use vision processing solution for the _FIRST_ Robotics Competition. PhotonVision is designed to get vision working on your robot _quickly_, without the significant cost of other similar solutions. PhotonVision supports a variety of COTS hardware, including the Raspberry Pi 3, 4, and 5, the [SnakeEyes Pi hat](https://www.playingwithfusion.com/productview.php?pdid=133), and the Orange Pi 5.

# Content

```{eval-rst}
.. grid:: 2

    .. grid-item-card::  Quick Start
        :link: docs/quick-start/index
        :link-type: doc

        Quick start to using Photonvision.

    .. grid-item-card::  Advanced Installation
        :link: docs/advanced-installation/index
        :link-type: doc

        Get started with installing PhotonVision on non-supported hardware.

```

```{eval-rst}
.. grid:: 2

    .. grid-item-card::  Programming Reference and PhotonLib
        :link: docs/programming/index
        :link-type: doc

        Learn more about PhotonLib, our vendor dependency which makes it easier for teams to retrieve vision data, make various calculations, and more.

    .. grid-item-card::  Integration
        :link: docs/integration/index
        :link-type: doc

        Pick how to use vision processing results to control a physical robot.

```

```{eval-rst}
.. grid:: 2

    .. grid-item-card::  Code Examples
        :link: docs/examples/index
        :link-type: doc

        View various step by step guides on how to use data from PhotonVision in your code, along with game-specific examples.

    .. grid-item-card::  Hardware
        :link: docs/hardware/index
        :link-type: doc

        Select appropriate hardware for high-quality and easy vision target detection.
```

```{eval-rst}
.. grid:: 2

    .. grid-item-card::  Contributing
        :link: docs/contributing/index
        :link-type: doc

        Interested in helping with PhotonVision? Learn more about how to contribute to our main code base, documentation, and more.
```

# Source Code

The source code for all PhotonVision projects is available through our [GitHub organization](https://github.com/PhotonVision).

- [PhotonVision](https://github.com/PhotonVision/photonvision)

# Contact Us

To report a bug or submit a feature request in PhotonVision, please [submit an issue on the PhotonVision GitHub](https://github.com/PhotonVision/photonvision) or [contact the developers on Discord](https://discord.com/invite/KS76FrX).

If you find a problem in this documentation, please submit an issue on the [PhotonVision Documentation GitHub](https://github.com/PhotonVision/photonvision/tree/main/docs).

# License

PhotonVision is licensed under the [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html).

```{toctree}
:caption: Getting Started
:hidden: true
:maxdepth: 0

docs/description
docs/quick-start/index
docs/hardware/index
docs/advanced-installation/index
```

```{toctree}
:caption: Pipeline Tuning and Calibration
:hidden: true
:maxdepth: 0

docs/pipelines/index
docs/apriltag-pipelines/index
docs/reflectiveAndShape/index
docs/objectDetection/index
docs/calibration/calibration
```

```{toctree}
:caption: Programming Reference
:hidden: true
:maxdepth: 1

docs/programming/photonlib/index
docs/simulation/index
docs/integration/index
docs/examples/index
```

```{toctree}
:caption: Additional Resources
:hidden: true
:maxdepth: 1

docs/troubleshooting/index
docs/additional-resources/best-practices
docs/additional-resources/config
docs/additional-resources/nt-api
docs/contributing/index
```

```{toctree}
:caption: API Documentation
:hidden: true
:maxdepth: 1

 Java <https://javadocs.photonvision.org>

 C++ <https://cppdocs.photonvision.org/>
```
