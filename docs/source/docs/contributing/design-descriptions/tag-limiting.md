# AprilTag Rejection

With the introduction of AprilTag's and full field localisation becoming more common rejecting tags that aren't relevant to the robot has become more important for maintaining a useful pose estimation. Including tags that aren't relevant to your objective can harm localisation, causing results to become more noisy and eventually skewed over the course of an event. In order to prevent this we can reject tags that aren't related to our current objective. There are a couple of things to consider when it comes to rejecting tags:
- why can't this be done in user code
- accessing rejected tag info
- synchronisation between coprocessor and robot code

## Why not do this in user code?

The first thing that may come to mind when talking about tag rejection is, why not just do this in user code? After all we provide the tag IDs to the user. The main issue with doing this in user code is it means that you have to reject entire multitag results, when in reality you still want a multitag estimate just without the rejected tag. One way to work around this would be running all pose estimations in user code but this is a significant performance to robot code, particularly on the roboRIO.

## Accessing rejected tag info

Accessing rejected tag info is still important, as tags can serve other information not just localisation. For example in the 2025-2026 FTC Game, Decode, there was a tag utilised to determine different patterns of scoring elements. Mechanisms like this make it desirable to still have access to detection info even when we reject a tag for localisation purposes.

## Synchronisation between coprocessor and robot code

In order to be able to use Photon standalone without any robot code we must be able to change the tags to reject on the coprocessor. This unfortunately doesn't scale well with more complex robot code usecases, for example rejecting tags based on current scoring location or alliance, making it beneficial to be able to set the tags to reject from robot code as well. This raises a couple of questions:
- How do we keep the coprocessor and robot code in sync?
- What do we do if the robot code and coprocessor have conflicting values?
