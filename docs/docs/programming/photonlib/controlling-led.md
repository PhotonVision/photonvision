import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Controlling LEDs

You can control the vision LEDs of supported hardware via PhotonLib using the `setLED()` method on a `PhotonCamera` instance. In Java and C++, an `VisionLEDMode` enum class is provided to choose values from. These values include, `kOff`, `kOn`, `kBlink`, and `kDefault`. `kDefault` uses the default LED value from the selected pipeline.

<Tabs groupId="lang">
  <TabItem value="java" label="Java">
```java
// Blink the LEDs.
camera.setLED(VisionLEDMode.kBlink);
```
  </TabItem>
  <TabItem value="cpp" label="C++">
```cpp
// Blink the LEDs.
camera.SetLED(photonlib::VisionLEDMode::kBlink);
```
  </TabItem>
</Tabs>
