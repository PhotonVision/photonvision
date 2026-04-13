# Build Instructions

This section contains the build instructions from the source code available at [our GitHub page](https://github.com/PhotonVision/photonvision).

## Development Setup

### Prerequisites

**Java Development Kit:**

 This project requires Java Development Kit (JDK) 17 to be compiled. This is the same Java version that comes with WPILib for 2026+. **Windows Users must use the JDK that ships with WPILib.** For other platforms, you can follow the instructions to install JDK 17 for your platform [here](https://bell-sw.com/pages/downloads/#jdk-17-lts).

**Node JS:**

 The UI is written in Node JS. To compile the UI, Node 22 or later is required. To install Node JS, follow the instructions for your platform [on the official Node JS website](https://nodejs.org/en/download/).

**pnpm:**

 [pnpm](https://pnpm.io/) is the package manager used to download dependencies for the UI. To install pnpm, follow [the instructions on the official pnpm website](https://pnpm.io/installation).

**Cross-Compilation Toolchains (Optional):**

 If you plan to deploy PhotonVision to a coprocessor like a Raspberry Pi, you will need to install the appropriate cross-compilation toolchain for your platform. For `linuxarm64` devices, this can be accomplished by running `./gradlew installArm64Toolchain` in the root folder of the project.

## Compiling Instructions

### Getting the Source Code

Get the source code from git:

```bash
git clone https://github.com/PhotonVision/photonvision
```

or alternatively download the source code from GitHub and extract the zip:

```{image} assets/git-download.png
:alt: Download source code from git
:width: 600
```

### Install Necessary Node JS Dependencies

In the photon-client directory:

```bash
pnpm install
```

### Using hot reload on the UI

In the photon-client directory:

```bash
pnpm run dev
```

This allows you to make UI changes quickly without having to spend time rebuilding the jar. Hot reload is enabled, so changes that you make and save are reflected in the UI immediately. Running this command will give you the URL for accessing the UI, which is on a different port than normal. You must use the printed URL to use hot reload.

### Build and Run PhotonVision

To compile and run the project, issue the following command in the root directory:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew run``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew run``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew run``
```

Running the following command under the root directory will build the jar under `photon-server/build/libs`:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew shadowJar``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew shadowJar``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew shadowJar``
```

### Build and Run PhotonVision with the Optional NVIDIA AprilTag Backend

The CUDA AprilTag backend is optional and only builds when `-PenableNvidiaAprilTag` is set and the required SDK pieces are present. Runtime selection still falls back to the existing CPU detector when the NVIDIA path cannot be used.

Current requirements for the optional backend:

- Linux host with a supported NVIDIA GPU and driver
- JDK 17
- Node 22+
- `NVIDIA_APRILTAG_SDK_ROOT` pointing at a directory that contains `libcuapriltags.a`
- `CUDA_HOME` pointing at a CUDA install that contains `lib64/libcudart.so`

Example build and run flow on Linux x86_64:

```bash
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:/path/to/node-v22/bin:$PATH"
export NVIDIA_APRILTAG_SDK_ROOT=/path/to/cuapriltags-root
export CUDA_HOME=/path/to/cuda

./gradlew :photon-targeting:nvidiaapriltagJNILinuxx86-64ReleaseSharedLibrary \
  :photon-server:run \
  -PenableNvidiaAprilTag \
  -Dphotonvision.apriltag.backend=auto
```

Useful backend selection modes:

- `-Dphotonvision.apriltag.backend=auto`: prefer NVIDIA when supported, otherwise fall back to CPU
- `-Dphotonvision.apriltag.backend=cpu`: force the existing CPU path
- `-Dphotonvision.apriltag.backend=nvidia`: request the CUDA path and log a fallback reason if it cannot be used

The current NVIDIA implementation only applies to `tag36h11`. Other tag families continue to run on the CPU detector.

### Build and Run PhotonVision on a Raspberry Pi Coprocessor

As a convenience, the build has a built-in `deploy` command which builds, deploys, and starts the current source code on a coprocessor. It uses [deploy-utils](https://github.com/wpilibsuite/deploy-utils/blob/main/README.md), so it works very similarly to deploys on robot projects.

An architecture override is required to specify the deploy target's architecture.

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew clean``

      ``./gradlew deploy -PArchOverride=linuxarm64``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew clean``

      ``./gradlew deploy -PArchOverride=linuxarm64``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew clean``

      ``gradlew deploy -PArchOverride=linuxarm64``
```

The `deploy` command is tested against Raspberry Pi coprocessors. Other similar coprocessors may work too.

### Generate a Device Image Locally

PhotonVision coprocessor releases are delivered as board images such as `.img.xz` or `.tar.xz`. This repo does not produce a desktop installer ISO.

If you want to inject a locally built Linux ARM64 jar into an existing PhotonVision base image, use the helper script in `scripts/generatePiImage.sh`.

1. Build a Linux ARM64 jar:

```bash
./gradlew :photon-server:shadowJar -PArchOverride=linuxarm64
```

2. Download and repack a board image by passing the base image URL and the desired artifact suffix:

```bash
./scripts/generatePiImage.sh <base-image-url> <image-suffix>
```

Example for Raspberry Pi:

```bash
./scripts/generatePiImage.sh \
  https://github.com/PhotonVision/photon-image-modifier/releases/download/<image-version>/photonvision_raspi.img.xz \
  RaspberryPi
```

What the script does:

- downloads the base `.img.xz`
- decompresses it
- mounts the root filesystem with `losetup`
- replaces `/opt/photonvision/photonvision.jar`
- rewrites the `photonvision.service` systemd unit
- recompresses the result as `photonvision*-image_<suffix>.xz`

This flow requires a Linux host with `sudo`, loop-device support, `wget`, and `xz-utils`.

Example for a custom Jetson-style image:

```bash
./gradlew :photon-server:shadowJar -PArchOverride=linuxarm64
PV_ROOT_PARTITION=1 \
./scripts/generatePiImage.sh \
  https://example.com/your-jetson-photon-base.img.xz \
  JetsonOrinNano
```

The Jetson example above assumes:

- the base image is already a PhotonVision-ready Linux ARM64 image
- the root filesystem is on partition `1`
- PhotonVision should live at `/opt/photonvision`

If your Jetson image uses different paths, the helper also supports:

- `PV_ROOT_PARTITION`
- `PV_PHOTON_DIR`
- `PV_SYSTEMD_UNIT_DIR`

This repo does not currently publish an official Jetson base image in CI, so you must provide the base `.img.xz` yourself.

### Using PhotonLib Builds

The build process automatically generates a vendordep JSON of your local build at `photon-lib/build/generated/vendordeps/photonlib.json`.

The photonlib source can be published to your local maven repository after building:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew publishToMavenLocal``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew publishToMavenLocal``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew publishToMavenLocal``
```

After adding the generated vendordep to your project, add the following to your project's `build.gradle` under the `plugins {}` block.

```Java
repositories {
    mavenLocal()
}
```

### Debugging PhotonVision Running on a CoProcessor

We can use Java's remote debug capabilities to run the PhotonVision JAR file on a Coprocessor, and attach a debugger running on a desktop/laptop to the process remotely. Set up a VSCode configuration in {code}`launch.json`

```json
{
   // Use IntelliSense to learn about possible attributes.
   // Hover to view descriptions of existing attributes.
   // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
   "version": "0.2.0",
   "configurations": [
     {
         "type": "java",
         "name": "Attach to CoProcessor",
         "request": "attach",
         "hostName": "photonvision.local",
         "port": "5801",
         "projectName": "photon-core"
     },
   ]
}
```

Stop any existing instance of PhotonVision by running {code}`systemctl stop photonvision`.

Launch the program with the following additional argument to the JVM: {code}`java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5801 photonvision.jar` -- the JVM will wait for a debugger to connect before running `main`.

Once the program says it is listening on port 5801, launch the debug configuration in VSCode.

The program will wait for the VSCode debugger to attach before proceeding.


## Running Tests

### Running Default Tests

Most unit tests [run as "headless" tests](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering) (i.e have no UI component during the test) by default.
To run a test, pass the test name(s):

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew test --tests <Test Name>``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew test --tests <Test Name>``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew test --tests <Test Name>``
```

### Debugging PhotonVision Tests Locally

Unit tests can also be debugged through the ``test`` Gradle task for a specific subproject in VSCode, found in the Gradle tab:

```{image} assets/vscode-gradle-tests.png
:alt: An image showing how unit tests can be debugged in VSCode through the Gradle for Java extension.
```

However, this will run all tests in a subproject.

Similarly, a local instance of PhotonVision can be debugged in the same way using the Gradle ``run`` task. In both cases, additional arguments can be specified:

```{image} assets/vscode-gradle-args.png
:alt: An image showing how VSCode gradle tasks can specify additional arguments.
```

### Running Tests With UI

By default, tests are run with UI disabled so they are not obtrusive during a build. All tests should be useful when the UI is disabled. However, if a particular test would benefit from having UI access (i.e. for debugging info), the UI can be enabled by passing the `enableTestUi` project property to Gradle. This will run all tests by default, but the Gradle `--tests` option can be used to [filter for specific tests](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering).

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux
      :sync: linux

      ``./gradlew test -PenableTestUi``

   .. tab-item:: macOS
      :sync: macos

      ``./gradlew test -PenableTestUi``

   .. tab-item:: Windows (cmd)
      :sync: windows

      ``gradlew test -PenableTestUi``
```

### VSCode Test Runner Extension

With the VSCode [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack), you can get the Test Runner for Java and Gradle for Java extensions. This lets you easily run specific tests through the IDE:

```{image} assets/vscode-runner-tests.png
:alt: An image showing how unit tests can be ran in VSCode through the Test Runner for Java extension.
```

To correctly run PhotonVision tests this way, you must [delegate the tests to Gradle](https://code.visualstudio.com/docs/java/java-build#_delegate-tests-to-gradle). Debugging tests like this will [**not** currently](https://github.com/microsoft/build-server-for-gradle/issues/119) collect outputs.

## Running examples

You can run one of the many built in examples straight from the command line, too! They contain a fully featured robot project, and some include simulation support. The projects can be found inside the photonlib-*-examples subdirectories for each language.

### Running C++/Java

PhotonLib must first be published to your local maven repository. This will also copy the generated vendordep json file into each example. After that, the simulateJava/simulateNative task can be used like a normal robot project. Robot simulation with attached debugger is technically possible by using simulateExternalJava and modifying the launch script it exports, though not yet supported.

```
~/photonvision$ ./gradlew publishToMavenLocal

~/photonvision$ cd photonlib-java-examples
~/photonvision/photonlib-java-examples$ ./gradlew <example-name>:simulateJava

~/photonvision$ cd photonlib-cpp-examples
~/photonvision/photonlib-cpp-examples$ ./gradlew <example-name>:simulateNative
```

### Running Python

PhotonLibPy must first be built into a wheel.

```
> cd photon-lib/py
> buildAndTest.bat
```

Then, you must enable using the development wheels. robotpy will use pip behind the scenes, and this bat file tells pip about your development artifacts.

Note: This is best done in a virtual environment.

```
> enableUsingDevBuilds.bat
```

Then, run the examples:

```
> cd photonlib-python-examples
> run.bat <example name>
```

### Downloading Pipeline Artifacts

Using the [GitHub CLI](https://cli.github.com/), we can download artifacts from pipelines by run ID and name:

```
~/photonvision$ gh run download 11759699679 -n jar-Linux
```

### MacOS Builds

MacOS builds are not published to releases as MacOS is not an officially
supported platform. However, MacOS builds are still available from the MacOS
build action, which can be found [here](https://github.com/PhotonVision/photonvision/actions/workflows/build.yml).

### Forcing Object Detection in the UI

In order to force the Object Detection interface to be visible, it's necessary to hardcode the platform that `Platform.java` returns. This can be done by changing the function that detects the RK3588S/QCS6490 platform to always return true, and changing the `getCurrentPlatform()` function to always return the RK3588S/QCS6490 architecture.
Alternatively, it's possible to modify the frontend code by changing all instances of `useSettingsStore().general.supportedBackends.length > 0` to `true`, which will force the card to render.
Make sure to revert these changes before submitting a Pull Request.
