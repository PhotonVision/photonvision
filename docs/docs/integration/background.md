# Vision - Robot Integration Background

## Vision Processing's Purpose

Each year, the FRC game requires a fundamental operation: **Align the Robot to a Goal**.

Regardless of whether that alignment point is for picking up gamepieces, or for scoring, fast and effective robots must be able to align to them quickly and repeatably.

Software strategies can be used to help augment the ability of a human operator, or step in when a human operator is not allowed to control the robot.

*Vision Processing* is one key *input* to these software strategies. However, the inputs your coprocessor provides must be interpreted and converted (ultimately) to motor voltage commands.

There are many valid strategies for doing this transformation. Picking a strategy is a balancing act between:

> 1. Available team resources (time, programming skills, previous experience)
> 2. Precision of alignment required
> 3. Team willingness to take on risk

Simple strategies are low-risk - they require comparatively little effort to implement and tune, but have hard limits on the complexity of motion they can control on the robot. Advanced methods allow for more complex and precise movement, but take more effort to implement and tune. For this reason, it is more risky to attempt to use them.
