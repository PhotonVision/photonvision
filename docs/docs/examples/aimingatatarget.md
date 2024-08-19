import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Aiming at a Target

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/master/photonlib-java-examples/aimattarget)/[C++](https://github.com/PhotonVision/photonvision/tree/master/photonlib-cpp-examples/aimattarget)).

## Knowledge and Equipment Needed

- Robot with a vision system running PhotonVision
- Target
- Ability to track a target by properly tuning a pipeline

## Code

Now that you have properly set up your vision system and have tuned a pipeline, you can now aim your robot/turret at the target using the data from PhotonVision. This data is reported over NetworkTables and includes: latency, whether there is a target detected or not, pitch, yaw, area, skew, and target pose relative to the robot. This data will be used/manipulated by our vendor dependency, PhotonLib. The documentation for the Network Tables API can be found \{ref}`here <docs/additional-resources/nt-api:Getting Target Information>` and the documentation for PhotonLib \{ref}`here <docs/programming/photonlib/adding-vendordep:What is PhotonLib?>`.

For this simple example, only yaw is needed.

In this example, while the operator holds a button down, the robot will turn towards the goal using the P term of a PID loop. To learn more about how PID loops work, how WPILib implements them, and more, visit [Advanced Controls (PID)](https://docs.wpilib.org/en/stable/docs/software/advanced-control/introduction/index.html) and [PID Control in WPILib](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/pidcontroller.html#pid-control-in-wpilib).

<Tabs groupId="lang">
  <TabItem value="java" label="Java">
```java reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/aimattarget/src/main/java/frc/robot/Robot.java#L41-L98
```
  </TabItem>
  <TabItem value="cpp" label="C++ (Header)">
```cpp reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/aimattarget/src/main/include/Robot.h#L27-L53
```
  </TabItem>
  <TabItem value="cpp-src" label="C++ (Source)">
```cpp reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/aimattarget/src/main/cpp/Robot.cpp#L25-L52
```
  </TabItem>
</Tabs>


