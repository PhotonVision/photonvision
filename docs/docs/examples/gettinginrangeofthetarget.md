import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Getting in Range of the Target

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/master/photonlib-java-examples/getinrange)/[C++](https://github.com/PhotonVision/photonvision/tree/master/photonlib-cpp-examples/getinrange)).

## Knowledge and Equipment Needed

- Everything required in \{ref}`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>`.
- Large space where your robot can move around freely

## Code

In FRC, a mechanism usually has to be a certain distance away from its target in order to be effective and score. In the previous example, we showed how to aim your robot at the target. Now we will show how to move to a certain distance from the target.

For proper functionality of just this example, ensure that your robot is pointed towards the target.

While the operator holds down a button, the robot will drive towards the target and get in range.

This example uses P term of the PID loop and PhotonLib and the distance function of PhotonUtils.

:::warning
The PhotonLib utility to calculate distance depends on the camera being at a different vertical height than the target. If this is not the case, a different method for estimating distance, such as target width or area, should be used. In general, this method becomes more accurate as range decreases and as the height difference increases.
:::

:::note
There is no strict minimum delta-height necessary for this method to be applicable, just a requirement that a delta exists.
:::

<Tabs groupId="lang">
  <TabItem value="java" label="Java">
```java reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/getinrange/src/main/java/frc/robot/Robot.java#L42-L107
```
  </TabItem>
  <TabItem value="cpp" label="C++ (Header)">
```cpp reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/getinrange/src/main/include/Robot.h#L27-L67
```
  </TabItem>
  <TabItem value="cpp-src" label="C++ (Source)">
```cpp reference
https://github.com/PhotonVision/photonvision/blob/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/getinrange/src/main/cpp/Robot.cpp#L25-L58
```
  </TabItem>
</Tabs>

:::hint
The accuracy of the measurement of the camera's pitch (`CAMERA_PITCH_RADIANS` in the above example), as well as the camera's FOV, will determine the overall accuracy of this method.
:::
