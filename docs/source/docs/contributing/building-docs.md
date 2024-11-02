# Building the PhotonVision Documentation

To build the PhotonVision documentation, you will require [Git](https://git-scm.com) and [Python 3.6 or greater](https://www.python.org).

## Cloning the Documentation Repository

Documentation lives within the main PhotonVision repository within the `docs` sub-folder. If you are planning on contributing, it is recommended to create a fork of the [PhotonVision repository](https://github.com/PhotonVision/photonvision). To clone this fork, run the following command in a terminal window:

`git clone https://github.com/[your username]/photonvision`

## Installing Python Dependencies

You must install a set of Python dependencies in order to build the documentation. To do so, you can run the following command in the docs sub-folder:

`~/photonvision/docs$ python -m pip install -r requirements.txt`

## Building the Documentation

In order to build the documentation, you can run the following command in the docs sub-folder. This will automatically build docs every time a file changes, and serves them locally at `localhost:8000` by default.

`~/photonvision/docs$ sphinx-autobuild --open-browser source/_build/html`

## Opening the Documentation

The built documentation is located at `docs/build/html/index.html` relative to the root project directory, or can be accessed via the local web server if using sphinx-autobuild.

## Docs Builds on Pull Requests

Pre-merge builds of docs can be found at: `https://photonvision-docs--PRNUMBER.org.readthedocs.build/en/PRNUMBER/index.html`. These docs are republished on every commit to a pull request made to PhotonVision/photonvision-docs. For example, PR 325 would have pre-merge documentation published to `https://photonvision-docs--325.org.readthedocs.build/en/325/index.html`. Additionally, the pull request will have a link directly to the pre-release build of the docs. This build only runs when there is a change to files in the docs sub-folder.

## Style Guide

PhotonVision follows the frc-docs style guide which can be found [here](https://docs.wpilib.org/en/stable/docs/contributing/style-guide.html). In order to run the linter locally (which builds on doc8 and checks for compliance with the style guide), follow the instructions [on GitHub](https://github.com/wpilibsuite/ohnoyoudidnt).
