# Installing Pre-Release Versions

Pre-release/development version of PhotonVision can be tested by installing/downloading artifacts from Github Actions (see below), which are built automatically on commits to open pull requests and to PhotonVision's `main` branch, or by {ref}`compiling PhotonVision locally <docs/contributing/building-photon:Build Instructions>`.

:::{warning}
If testing a pre-release version of PhotonVision with a robot, PhotonLib must be updated to match the version downloaded! If not, packet schema definitions may not match and unexpected things will occur. To update PhotonLib, refer to {ref}`installing specific version of PhotonLib<docs/programming/photonlib/adding-vendordep:Install Specific Version - Java/C++>`.
:::

GitHub Actions builds pre-release version of PhotonVision automatically on PRs and on each commit merged to main. To test a particular commit to main, navigate to the [PhotonVision commit list](https://github.com/PhotonVision/photonvision/commits/main/) and click on the check mark (below). Scroll to "Build / Build fat JAR - PLATFORM", click details, and then summary. From here, JAR and image files can be downloaded to be flashed or uploaded using "Offline Update".

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
