# About PhotonVision

## Description

PhotonVision is a free, fast, and easy-to-use vision processing solution for the _FIRST_ Robotics Competition. PhotonVision is designed to get vision working on your robot _quickly_, but with lower cost than other solutions.
Using PhotonVision, teams can go from setting up a camera and coprocessor to detecting and tracking AprilTags and other targets by simply tuning sliders. With an easy to use interface, comprehensive documentation, and a feature rich vendor dependency, no experience is necessary to use PhotonVision. No matter your resources, using PhotonVision is easy compared to its alternatives.

## Advantages

PhotonVision has a myriad of advantages over similar solutions, including:

### Affordable

Compared to alternatives, PhotonVision is much cheaper to use (at the cost of your coprocessor and camera) compared to alternatives that cost \$400. This allows your team to save money while still being competitive.

### Easy to Use User Interface

The PhotonVision user interface is simple and modular, making things easier for the user. With a simpler interface, you can focus on what matters most, tracking targets, rather than how to use our UI. A major unique quality is that the PhotonVision UI includes an offline copy of our documentation for your ease of access at competitions.

### PhotonLib Vendor Dependency

The PhotonLib vendor dependency allows you to easily get necessary target data (without having to work directly with NetworkTables) while also providing utility methods to get distance and position on the field. A serialization strategy is used to guarantees data coherency, which is helpful for latency compensation. This helps your team focus less on getting data and more on using it to do cool things.

### User Calibration

Using PhotonVision allows the user to calibrate for their specific camera, which will get you the best tracking results. This is extremely important as every camera (even if it is the same model) will have it's own quirks and user calibration allows for those to be accounted for.

### Low Latency, High FPS Processing

PhotonVision exposes specialized hardware on select coprocessors to maximize processing speed. This allows for lower-latency detection of targets to ensure you aren't losing out on any performance.

### Fully Open Source and Active Developer Community

You can find all of our code on [GitHub](https://github.com/PhotonVision), including code for our main program, documentation, vendor dependency (PhotonLib), and more. This helps you see everything working behind the scenes and increases transparency. This also allows users to make pull requests for features that they want to add in to PhotonVision that will be reviewed by the development team. PhotonVision is licensed under the GNU General Public License (GPLv3) which you can learn more about [here](https://www.gnu.org/licenses/quick-guide-gplv3.html).

### Multi-Camera Support

You can use multiple cameras within PhotonVision, allowing you to see multiple angles without the need to buy multiple coprocessors. This makes vision processing more affordable and simpler for your team.

### Comprehensive Documentation

Using our comprehensive documentation, you will be able to easily start vision processing by following a series of simple steps.
