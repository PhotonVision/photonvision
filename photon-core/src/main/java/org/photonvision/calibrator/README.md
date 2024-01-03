# Java Camera Calibrator
Program guides user to pose the camera to the ChArUcoBoard for most efficient and accurate calibration.

**Run from a terminal window either the Windows jar or Linux arm64 jar (RPi, etc.)**

**don't double click the jar file name**

`java -jar the_right_jar_file [options]`

Run log is in the directory with the jar file.

User specified Runtime Options are listed with `-help`, for example, on a Windows PC:

`java -jar calib-photonvision-dev-<version number>-winx64.jar -help`

This is a minimal set of options from the many options set in the program at compile time.

On a computer with an internal camera the camera ids on Windows typically are:
internal and external cameras connected at boot up: external=0, internal=1
internal camera connected at boot up and external plugged in later: internal=0, external=1
It may be convenient to disable the internal camera for extended calibration testing to make the camera id for the external constant.

Start the program (successfully) to make a ChArUcoBoard.png file that can be printed. Alternatively the ChArUcoBoard file can be displayed in another frame on the computer screen or on a second screen. Use of a computer flat screen display can improve the camera calibration, however, there may be restrictions compared to using a paper board. Computer displays must be flat and visible at steep (nearly parallel) angles. For the smallest guidance target some displays may not have enough resolution or refresh rate to display the guidance (although it may look fine to the human eye).

Run the program and aim the camera at the printed board in the pose that matches the guidance board on the computer screen. It may be easier to align a fixed camera and hold and move the board.

The top line of the guidance display has movement information that isn't very useful. Below that are the guidance rotations and translations (r{x, y, z} t{x, y, z}). The camera should be aimed such that its rotations and translations should fairly closely match the guidance. The small insert of the outline of the guidance and the current camera pose indicate how clsoe the camera is to matching the guidance.

The first two guidance poses are always the same.

The first pose at a fairly steep angle to the board's left (camera's right) sets the initial camera matrix. I advise getting the angles correct but not at precisely the correct distance so the images do not align yet. Then carefully move closer or further to get the precise size alignment. The capture should be automatic. “Well begun is half done.” - Aristotle. [Rotated poses are good to calibrate the camera matrix.] The guidance board is always intended to be able to be duplicated by a camera pose. An occaisonal distorted board that is bent, broken or split indicates previously captured poses weren't close enough to the guidance. Start over.

Occaisionally the rotated first pose "jumps" to a different size. That is NOT a capture and is the program trying to use the latest estimate of the camera matrix for that first pose. That pose is captured when the second pose - the head-on pose - appears.

The second pose is straight head-on and sets the initial distortion. Similarly, get the straight-on correct, move to align the images but not yet the matching sizes then move closer or further to match. The capture should be automatic. [Straight-on poses are good to calibrate the distortion coefficients.]

If the guidance board and the camera image match, the program should auto-capture that information. Rarely, but if the auto capture is not happening, the user can force capture by pressing "c" and Enter on the computer terminal window that was used to start the program. The black and white insert shows what the poses are of the exact guidance board and the estimated camera view. The similar numbers are the Jaccard index between the poses and the minimum acceptable value for auto capture.

The nine red camera intrinsic parameters turn green when the poses provide enough information. Usually after about 10 poses. That information might not be good unless you have carefully aligned to the guidance and especially autocapture yields good information. (You can point incorrectly at the target and randomly hit the "c" and "enter" several times to complete a bad calibration; it doesn't care, although it has been known to throw an "unknown exception" and keep going as if nothing bad happened.)

The terminal (keyboard) inputs are 'c' for force capture, 'm' for mirror view if that helps you align the camera to the guidance, and 'q' to quit. Rarely if ever are those needed.

The display of the guidance board and camera view are on a browser's port 1185 to the computer running the program. For example, 127.0.0.1:1185?action=stream.

The camera server is standard WPILib camera server stuff so you can adust many camera parameters on the automatically assigned port - 1181 or higher. For example 127.0.0.1:1181 to see the camera parameters.

If you run this on a system with PhotonVision running, then stop PhotonVision. (linux command is `sudo service photonvision stop`)

Camera calibration data is written to a json file suitable for import into PhotonVision. (Note the camera name and platform are unknown so if you needed those, edit the file.)
References:

https://arxiv.org/pdf/1907.04096.pdf

https://www.calibdb.net/#

https://github.com/paroj/pose_calib
