# Build Instructions

This section contains the build instructions from the source code available at [our GitHub page](https://github.com/PhotonVision/photonvision).

## Development Setup

### Prerequisites

**Java Development Kit:**

 This project requires Java Development Kit (JDK) 17 to be compiled. This is the same Java version that comes with WPILib for 2025+. **Windows Users must use the JDK that ships with WPILib.** For other platforms, you can follow the instructions to install JDK 17 for your platform [here](https://bell-sw.com/pages/downloads/#jdk-17-lts).

**Node JS:**

 The UI is written in Node JS. To compile the UI, Node 18.20.4 to Node 20.0.0 is required. To install Node JS follow the instructions for your platform [on the official Node JS website](https://nodejs.org/en/download/).  However, modify this line

```bash
nvm install 20
```

so that it instead reads

```javascript
nvm install 18.20.4
```

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
npm install
```

### Build and Copy UI to Java Source

In the root directory:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux

      ``./gradlew buildAndCopyUI``

   .. tab-item:: macOS

      ``./gradlew buildAndCopyUI``

   .. tab-item:: Windows (cmd)

      ``gradlew buildAndCopyUI``
```

### Build and Run PhotonVision

To compile and run the project, issue the following command in the root directory:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux

      ``./gradlew run``

   .. tab-item:: macOS

      ``./gradlew run``

   .. tab-item:: Windows (cmd)

      ``gradlew run``
```

Running the following command under the root directory will build the jar under `photon-server/build/libs`:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux

      ``./gradlew shadowJar``

   .. tab-item:: macOS

      ``./gradlew shadowJar``

   .. tab-item:: Windows (cmd)

      ``gradlew shadowJar``
```

### Build and Run PhotonVision on a Raspberry Pi Coprocessor

As a convenience, the build has a built-in `deploy` command which builds, deploys, and starts the current source code on a coprocessor.

An architecture override is required to specify the deploy target's architecture.

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux

      ``./gradlew clean``

      ``./gradlew deploy -PArchOverride=linuxarm64``

   .. tab-item:: macOS

      ``./gradlew clean``

      ``./gradlew deploy -PArchOverride=linuxarm64``

   .. tab-item:: Windows (cmd)

      ``gradlew clean``

      ``gradlew deploy -PArchOverride=linuxarm64``
```

The `deploy` command is tested against Raspberry Pi coprocessors. Other similar coprocessors may work too.

### Using PhotonLib Builds

The build process automatically generates a vendordep JSON of your local build at `photon-lib/build/generated/vendordeps/photonlib.json`.

The photonlib source can be published to your local maven repository after building:

```{eval-rst}
.. tab-set::

   .. tab-item:: Linux

      ``./gradlew publishToMavenLocal``

   .. tab-item:: macOS

      ``./gradlew publishToMavenLocal``

   .. tab-item:: Windows (cmd)

      ``gradlew publishToMavenLocal``
```

After adding the generated vendordep to your project, add the following to your project's `build.gradle` under the `plugins {}` block.

```Java
repositories {
    mavenLocal()
}
```

### VSCode Test Runner Extension

With the VSCode [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack), you can get the Test Runner for Java and Gradle for Java extensions. This lets you easily run specific tests through the IDE:

```{image} assets/vscode-runner-tests.png
:alt: An image showing how unit tests can be ran in VSCode through the Test Runner for Java extension.
```

To correctly run PhotonVision tests this way, you must [delegate the tests to Gradle](https://code.visualstudio.com/docs/java/java-build#_delegate-tests-to-gradle). Debugging tests like this will [**not** currently](https://github.com/microsoft/build-server-for-gradle/issues/119) collect outputs.

### Debugging PhotonVision Running Locally

Unit tests can instead be debugged through the ``test`` Gradle task for a specific subproject in VSCode, found in the Gradle tab:

```{image} assets/vscode-gradle-tests.png
:alt: An image showing how unit tests can be debugged in VSCode through the Gradle for Java extension.
```

However, this will run all tests in a subproject.

Similarly, a local instance of PhotonVision can be debugged in the same way using the Gradle ``run`` task. In both cases, additional arguments can be specified:

```{image} assets/vscode-gradle-args.png
:alt: An image showing how VSCode gradle tasks can specify additional arguments.
```

### Debugging PhotonVision Running on a CoProcessor

Set up a VSCode configuration in {code}`launch.json`

```
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

Stop any existing instance of PhotonVision.

Launch the program with the following additional argument to the JVM: {code}`java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5801 photonvision.jar`

Once the program says it is listening on port 5801, launch the debug configuration in VSCode.

The program will wait for the VSCode debugger to attach before proceeding.

### Running examples

You can run one of the many built in examples straight from the command line, too! They contain a fully featured robot project, and some include simulation support. The projects can be found inside the photonlib-*-examples subdirectories for each language.

#### Running C++/Java

PhotonLib must first be published to your local maven repository. This will also copy the generated vendordep json file into each example. After that, the simulateJava/simulateNative task can be used like a normal robot project. Robot simulation with attached debugger is technically possible by using simulateExternalJava and modifying the launch script it exports, though not yet supported.

```
~/photonvision$ ./gradlew publishToMavenLocal

~/photonvision$ cd photonlib-java-examples
~/photonvision/photonlib-java-examples$ ./gradlew <example-name>:simulateJava

~/photonvision$ cd photonlib-cpp-examples
~/photonvision/photonlib-cpp-examples$ ./gradlew <example-name>:simulateNative
```

#### Running Python

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

#### Downloading Pipeline Artifacts

Using the [GitHub CLI](https://cli.github.com/), we can download artifacts from pipelines by run ID and name:

```
~/photonvision$ gh run download 11759699679 -n jar-Linux
```
