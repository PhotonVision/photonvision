# Camera Calibration

:::{important} In order to detect AprilTags and use 3D mode, your camera must be calibrated at the desired resolution! Inaccurate calibration will lead to poor performance.

:::

If you’re not using cameras in 3D mode, calibration is optional, but it can still offer benefits. Calibrating cameras helps refine the pitch and yaw values, leading to more accurate positional data in every mode. {ref}`For a more in-depth view<docs/calibration/calibration:Calibrating Your Camera>`.

## Print the Calibration Target

- Downloaded from our [demo site](http://photonvision.global/#/cameras), or directly from your coprocessors cameras tab.
- Use the Charuco calibration board:
  - Board Type: Charuco
  - Tag Family: 4x4
  - Pattern Spacing: 1.00in
  - Marker Size: 0.75in
  - Board Height : 8
  - Board Width : 8

## Prepare the Calibration Target

- Measure Accurately: Use calipers to measure the actual size of the squares and markers. Accurate measurements are crucial for effective calibration.
- Ensure Flatness: The calibration board must be perfectly flat, without any wrinkles or bends, to avoid introducing errors into the calibration process.

## Calibrate your Camera

- Take lots of photos: It's recommended to capture more than 50 images to properly calibrate your camera for accuracy. 12 is the bare minimum and may not provide good results.
- Other Tips
  - Move the board not the camera.
  - Take photos of lots of angles: The more angles the more better (up to 45 deg).
  - A couple of up close images is good.
  - Cover the entire cameras fov.
  - Avoid images with the board facing straight towards the camera.

## Interactive Camera Transformation

Below is an interactive demo to visualize multiple cameras' positions and orientations relative to the robot chassis. Use the table to adjust each camera's transformation parameters (position and rotation).

<div id="camera-demo" style="width: 800px; height: 400px; border: 1px solid #ccc; margin: auto;"></div>
<table id="camera-table" style="table-layout: fixed; width: 100%;">
  <thead>
    <tr style="height: 35px;">
      <th>Camera</th>
      <th>Position X</th>
      <th>Position Y</th>
      <th>Position Z</th>
      <th>Roll (deg)</th>
      <th>Pitch (deg)</th>
      <th>Yaw (deg)</th>
    </tr>
  </thead>
  <tbody>
  </tbody>
</table>
<button id="add-camera" style="margin-top: 10px;">Add Camera</button>

<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r134/three.min.js"></script>
<script>
  const scene = new THREE.Scene();
  const aspect = 800 / 400; // Aspect ratio based on the renderer size
  const orthoSize = 1.5; // Size of the orthographic view
  const camera = new THREE.OrthographicCamera(
    -orthoSize * aspect, // Left
    orthoSize * aspect,  // Right
    orthoSize,           // Top
    -orthoSize,          // Bottom
    0.01,                // Near
    2000                 // Far
  );
  const renderer = new THREE.WebGLRenderer();
  renderer.setSize(800, 400); // Fixed size to ensure proper rendering
  renderer.setPixelRatio(window.devicePixelRatio); // Ensure proper scaling on high-DPI displays
  document.getElementById('camera-demo').appendChild(renderer.domElement);

  // Update grid to align with the XY axis and increase its size
  const gridHelper = new THREE.GridHelper(40, 40); // Increase size to 40x40
  gridHelper.rotation.x = -Math.PI / 2; // Rotate to align with the XY plane
  gridHelper.position.set(0, 0, 0); // Align grid with the bottom of the camera's frustum
  scene.add(gridHelper);

  // Replace axes helper with custom origin marker
  function createThickOriginMarker(size, thickness) {
    const originGroup = new THREE.Group();

    const createAxis = (color, start, end) => {
      const direction = new THREE.Vector3().subVectors(end, start).normalize();
      const length = start.distanceTo(end);
      const cylinderGeometry = new THREE.CylinderGeometry(thickness, thickness, length, 16);
      const material = new THREE.MeshBasicMaterial({ color });
      const cylinder = new THREE.Mesh(cylinderGeometry, material);

      // Position and rotate the cylinder
      cylinder.position.copy(start).addScaledVector(direction, length / 2);
      cylinder.lookAt(end);

      // Align cylinder with the Z-axis
      cylinder.rotateX(Math.PI / 2);
      originGroup.add(cylinder);
    };

    createAxis(0xff0000, new THREE.Vector3(0, 0, 0), new THREE.Vector3(size, 0, 0)); // X-axis (red)
    createAxis(0x00ff00, new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, size, 0)); // Y-axis (green)
    createAxis(0x0000ff, new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 0, size)); // Z-axis (blue)

    return originGroup;
  }

  const thickOriginMarker = createThickOriginMarker(0.5, 0.02); // Size of 0.5 units, thickness of 0.02
  scene.add(thickOriginMarker);

  const cameras = [];
  const fovs = [];

  function createCamera(index) {

    const table = document.getElementById('camera-table').getElementsByTagName('tbody')[0];
    const row = document.createElement('tr');
    row.id = `camera-row-${index}`;
    row.style = "height: 35px;";
    row.innerHTML = `
      <td>Camera ${index}</td>
      <td><input id="posX-${index}" type="number" min="-10" max="10" step="0.01" value="0"></td>
      <td><input id="posY-${index}" type="number" min="-10" max="10" step="0.01" value="0"></td>
      <td><input id="posZ-${index}" type="number" min="-10" max="10" step="0.01" value="0"></td>
      <td><input id="roll-${index}" type="number" min="-180" max="180" step="0.5" value="0"></td>
      <td><input id="pitch-${index}" type="number" min="-180" max="180" step="0.5" value="0"></td>
      <td><input id="yaw-${index}" type="number" min="-180" max="180" step="0.5" value="0"></td>
    `;
    table.appendChild(row);

    ['posX', 'posY', 'posZ', 'roll', 'pitch', 'yaw'].forEach(param => {
      document.getElementById(`${param}-${index}`).addEventListener('input', () => updateTransformation(index));
    });

    const geometry = new THREE.BoxGeometry(0.04445, 0.04445, 0.0254);
    const material = new THREE.MeshBasicMaterial({ color: 0x00ff00 });
    const cameraCube = new THREE.Mesh(geometry, material);
    scene.add(cameraCube);

    const fovGeometry = new THREE.BufferGeometry();
    const fovMaterial = new THREE.LineBasicMaterial({ color: new THREE.Color(`hsl(${index * 60}, 100%, 50%)`) }); // Unique color per camera
    const fov = new THREE.LineSegments(fovGeometry, fovMaterial);
    scene.add(fov);

    cameras.push(cameraCube);
    fovs.push(fov);

    updateFOV(index);
  }

  function updateFOV(index) {
    const horizontalFOV = THREE.MathUtils.degToRad(70 / 2); // Half of 70 degrees
    const verticalFOV = THREE.MathUtils.degToRad(55 / 2); // Half of 55 degrees
    const depth = .5; // Depth of the FOV visualization

    const fovVertices = new Float32Array([
      0, 0, 0, depth, depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV),
      0, 0, 0, depth, -depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV),
      0, 0, 0, depth, depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV),
      0, 0, 0, depth, -depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV),
      depth, depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV), depth, -depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV),
      depth, -depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV), depth, -depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV),
      depth, -depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV), depth, depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV),
      depth, depth * Math.tan(horizontalFOV), depth * Math.tan(verticalFOV), depth, depth * Math.tan(horizontalFOV), -depth * Math.tan(verticalFOV),
    ]);
    fovs[index].geometry.setAttribute('position', new THREE.BufferAttribute(fovVertices, 3));
  }

  function updateTransformation(index) {
    const posX = parseFloat(document.getElementById(`posX-${index}`).value);
    const posY = parseFloat(document.getElementById(`posY-${index}`).value);
    const posZ = parseFloat(document.getElementById(`posZ-${index}`).value);
    const roll = parseFloat(document.getElementById(`roll-${index}`).value) * (Math.PI / 180); // Roll (rotation around X-axis)
    const pitch = parseFloat(document.getElementById(`pitch-${index}`).value) * (Math.PI / 180); // Pitch (rotation around Y-axis)
    const yaw = parseFloat(document.getElementById(`yaw-${index}`).value) * (Math.PI / 180); // Yaw (rotation around Z-axis, inverted for NWU)

    // NWU convention: X (North), Y (West), Z (Up)
    cameras[index].position.set(posX, posY, posZ);
    cameras[index].rotation.set(roll, -pitch, yaw); // Invert pitch for NWU

    fovs[index].position.set(posX, posY, posZ); // Invert Y for NWU
    fovs[index].rotation.set(roll, pitch, yaw); // Invert pitch for NWU
  }

  function drawFixedRobotBumpers() {
    // Remove existing bumpers if any
    const existingBumpers = scene.getObjectByName('robotBumpers');
    if (existingBumpers) {
      scene.remove(existingBumpers);
    }

    const bumpersGroup = new THREE.Group();
    bumpersGroup.name = 'robotBumpers';

    const bumperThickness = 3.25 * 0.0254;
    const width = 30 * 0.0254; // 30 inches to meters
    const length = 30 * 0.0254; // 30 inches to meters
    const height = 6 * 0.0254; // 6 inches to meters
    const cornerRadius = 2 * 0.0254; // 2 inches to meters
    const material = new THREE.MeshBasicMaterial({ color: 0xffa500, side: THREE.DoubleSide }); // Orange color for bumpers

    // Create rounded bumper segments
    const createRoundedBumper = (x, y, z, w, h, d, radius) => {
      const shape = new THREE.Shape();
      shape.moveTo(-w / 2 + radius, -h / 2);
      shape.lineTo(w / 2 - radius, -h / 2);
      shape.quadraticCurveTo(w / 2, -h / 2, w / 2, -h / 2 + radius);
      shape.lineTo(w / 2, h / 2 - radius);
      shape.quadraticCurveTo(w / 2, h / 2, w / 2 - radius, h / 2);
      shape.lineTo(-w / 2 + radius, h / 2);
      shape.quadraticCurveTo(-w / 2, h / 2, -w / 2, h / 2 - radius);
      shape.lineTo(-w / 2, -h / 2 + radius);
      shape.quadraticCurveTo(-w / 2, -h / 2, -w / 2 + radius, -h / 2);

      const extrudeSettings = { depth: d, bevelEnabled: false };
      const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      const mesh = new THREE.Mesh(geometry, material);
      mesh.position.set(x, y, z - d / 2); // Center the bumper
      bumpersGroup.add(mesh);
    };

    // Bottom bumper
    createRoundedBumper(0, -length / 2, height / 2, width, bumperThickness, height, cornerRadius);
    // Top bumper
    createRoundedBumper(0, length / 2, height / 2, width, bumperThickness, height, cornerRadius);
    // Left bumper
    createRoundedBumper(-width / 2, 0, height / 2, bumperThickness, length, height, cornerRadius);
    // Right bumper
    createRoundedBumper(width / 2, 0, height / 2, bumperThickness, length, height, cornerRadius);

    // Adjust bumpers to make the bottom at z = 0
    bumpersGroup.position.set(0, 0, 0);

    // Add bumpers to the scene
    scene.add(bumpersGroup);
  }

  document.getElementById('add-camera').addEventListener('click', () => {
    const index = cameras.length;
    createCamera(index);
  });

  createCamera(0);

  // Add buttons for camera views below the 3D view
  const viewButtons = document.createElement('div');
  viewButtons.style.marginTop = '10px';
  viewButtons.style.textAlign = 'center';
  viewButtons.innerHTML = `
    <button onclick="setCameraView('front')">Front</button>
    <button onclick="setCameraView('back')">Back</button>
    <button onclick="setCameraView('left')">Left</button>
    <button onclick="setCameraView('right')">Right</button>
    <button onclick="setCameraView('top')">Top</button>
    <button onclick="setCameraView('bottom')">Bottom</button>
    <button onclick="setCameraView('isometric')">Isometric</button>
  `;
  document.getElementById('camera-demo').parentNode.appendChild(viewButtons);

  // Function to set camera views
  function setCameraView(view) {
    const distance = 1.5; // Distance from the origin
    switch (view) {
      case 'front':
        camera.position.set(distance, 0, 0); // Positive X-axis
        camera.up.set(0, 0, 1); // Z+ is up
        break;
      case 'back':
        camera.position.set(-distance, 0, 0); // Negative X-axis
        camera.up.set(0, 0, 1); // Z+ is up
        break;
      case 'left':
        camera.position.set(0, distance, 0); // Positive Y-axis
        camera.up.set(0, 0, 1); // Z+ is up
        break;
      case 'right':
        camera.position.set(0, -distance, 0); // Negative Y-axis
        camera.up.set(0, 0, 1); // Z+ is up
        break;
      case 'top':
        camera.position.set(0, 0, distance); // Positive Z-axis
        camera.up.set(1, 0, 0); // X+ is up
        break;
      case 'bottom':
        camera.position.set(0, 0, -distance); // Negative Z-axis
        camera.up.set(1, 0, 0); // X+ is up
        break;
      case 'isometric':
        camera.position.set(-1.5, -1.5, 1.5); // Same as the default view
        camera.up.set(0, 0, 1); // Z+ is up
        break;
    }
    camera.lookAt(0, 0, 0); // Look at the origin
  }

  setCameraView('isometric');

  // Draw the fixed bumpers once
  drawFixedRobotBumpers();

  function animate() {
    requestAnimationFrame(animate);
    renderer.render(scene, camera);
  }
  animate();
</script>
