## PhotonLib C++ Examples

### Building

Build photonvision and publish it locally with:

```
photonvision$ ./gradlew photon-lib:publishtomavenlocal
```

Now, cd into here, pull in the latest vendor json, and simulate the project of choice

```
photonvision/photonlib-cpp-exaples: ./gradlew copyPhotonlib
photonvision/photonlib-cpp-exaples: ./gradlew aimandrange:simulateNativeRelease
```
