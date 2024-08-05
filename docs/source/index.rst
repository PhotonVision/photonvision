.. image:: assets/PhotonVision-Header-onWhite.png
  :alt: PhotonVision

Welcome to the official documentation of PhotonVision! PhotonVision is the free, fast, and easy-to-use vision processing solution for the *FIRST*\  Robotics Competition. PhotonVision is designed to get vision working on your robot *quickly*, without the significant cost of other similar solutions. PhotonVision supports a variety of COTS hardware, including the Raspberry Pi 3 and 4, the `Gloworm smart camera <https://photonvision.github.io/gloworm-docs/docs/quickstart/#finding-gloworm>`_, the `SnakeEyes Pi hat <https://www.playingwithfusion.com/productview.php?pdid=133>`_, and the Orange Pi 5.

Content
-------

.. grid:: 2

    .. grid-item-card::  Getting Started
        :link: docs/installation/index
        :link-type: doc

        Get started with installing PhotonVision, creating a pipeline, and tuning it for usage in competitions.

    .. grid-item-card::  Programming Reference and PhotonLib
        :link: docs/programming/index
        :link-type: doc

        Learn more about PhotonLib, our vendor dependency which makes it easier for teams to retrieve vision data, make various calculations, and more.

.. grid:: 2

    .. grid-item-card::  Integration
        :link: docs/integration/index
        :link-type: doc

        Pick how to use vision processing results to control a physical robot.

    .. grid-item-card::  Code Examples
        :link: docs/examples/index
        :link-type: doc

        View various step by step guides on how to use data from PhotonVision in your code, along with game-specific examples.

.. grid:: 2

    .. grid-item-card::  Hardware
        :link: docs/hardware/index
        :link-type: doc

        Select appropriate hardware for high-quality and  easy vision target detection.

    .. grid-item-card::  Contributing
        :link: docs/contributing/index
        :link-type: doc

        Interested in helping with PhotonVision? Learn more about how to contribute to our main code base, documentation, and more.

Source Code
-----------

The source code for all PhotonVision projects is available through our `GitHub organization <https://github.com/PhotonVision>`_.

* `PhotonVision <https://github.com/PhotonVision/photonvision>`_
* `PhotonVision ReadTheDocs <https://github.com/PhotonVision/photonvision-docs/>`_

Contact Us
----------

To report a bug or submit a feature request in PhotonVision, please `submit an issue on the PhotonVision GitHub <https://github.com/PhotonVision/photonvision>`_ or `contact the developers on Discord <https://discord.com/invite/KS76FrX>`_.

If you find a problem in this documentation, please submit an issue on the `PhotonVision Documentation GitHub <https://github.com/PhotonVision/photonvision-docs>`_.

License
-------

PhotonVision is licensed under the `GNU GPL v3 <https://www.gnu.org/licenses/gpl-3.0.en.html>`_.


.. toctree::
   :maxdepth: 0
   :caption: Getting Started
   :hidden:

   docs/description
   docs/hardware/index
   docs/installation/index
   docs/settings

.. toctree::
   :maxdepth: 0
   :caption: Pipeline Tuning and Calibration
   :hidden:

   docs/pipelines/index
   docs/apriltag-pipelines/index
   docs/reflectiveAndShape/index
   docs/objectDetection/index
   docs/calibration/calibration

.. toctree::
   :maxdepth: 1
   :caption: Programming Reference
   :hidden:

   docs/programming/photonlib/index
   docs/simulation/index
   docs/integration/index
   docs/examples/index

.. toctree::
   :maxdepth: 1
   :caption: Additional Resources
   :hidden:

   docs/troubleshooting/index
   docs/additional-resources/best-practices
   docs/additional-resources/config
   docs/additional-resources/nt-api
   docs/contributing/index

.. toctree::
 :maxdepth: 1
 :caption: API Documentation
 :hidden:

  Java <https://javadocs.photonvision.org>

  C++ <https://cppdocs.photonvision.org/>
