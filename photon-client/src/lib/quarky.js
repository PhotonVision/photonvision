import IDLEGIF from "@/assets/images/idle.gif";
import GROWGIF from "@/assets/images/grow.gif";
import BLINKGIF from "@/assets/images/blink.gif";
import WAVEGIF from "@/assets/images/wave.gif";
import SPEAKGIF from "@/assets/images/speak.gif";
import SHRINKGIF from "@/assets/images/shrink.gif";
import POINTGIF from "@/assets/images/point.gif";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

const ANIMATIONS = {
  idle: IDLEGIF,
  grow: GROWGIF,
  blink: BLINKGIF,
  wave: WAVEGIF,
  speak: SPEAKGIF,
  shrink: SHRINKGIF,
  point: POINTGIF
};

// Extended animation sequence
const SEQUENCE = [
  { animation: "grow", loops: 1 },
  { animation: "idle", loops: 2 },
  { animation: "blink", loops: 1 },
  { animation: "idle", loops: 1 },
  { animation: "speak", loops: 1 },
  { animation: "idle", loops: 1 },
  { animation: "point", loops: 1 },
  { animation: "idle", loops: 1 },
  { animation: "wave", loops: 1 },
  { animation: "idle", loops: 1 }
];

let quarkyImage;
let quarkyContainer;
let speechBubble;
let currentSequenceIndex = 0;
let currentLoopCount = 0;
let isMovingDemo = false;
let hasPlayedGrow = false;

// Speech bubble text (configurable)
let quarkySpeechText = "Hello from Quarky!";

// Turbo-encabulator style nonsense phrases
const quarkyPhrases = [
  "Reverse phase oscillation detected!",
  "Initializing hyperflux capacitor...",
  "Reticulating splines in progress.",
  "Quantum entanglement buffer overflowzomg",
  "Did you remember the turbo-encabulator?",
  "Engaging magnetic flux inverter.",
  "Calibrating photon resonance field.",
  "Deploying recursive feedback loop.",
  "wow.",
  "I applaud your pseudo-random bitstream.",
  "Rebooting quantum foam stabilizer.",
  "Analyzing subspace harmonics.",
  "Transmitting encrypted flux packets.",
  "Verifying entropic phase alignment.",
  "Reconfiguring nano-particle array.",
  "pew pew. pew pew.",
  "I can't parse the synthetic logic matrix.",
  "Don't forget to make the holographic interface.",
  "You should generate more stochastic resonance.",
  "Greetings",
  "You look like you need some help!",
  "Set this slider to 25",
  "Set this slider to 67. HAHA 67!!!!",
  "That's a horrible choice!",
  "Fun is a core value! Is that a fun choice?",
  "If your grandma saw that choice, would she be proud?",
  "Chute Door?",
  "Yes, Chute Door!",
  "That's a bold strategy, Cotton.",
  "asdflkjaslkdflklnf2222",
  "00110101? That's just gibberish!",
  "Three is my favorite number too!",
  "Robots should not quit, but yours did!",
  "Don’t forget to disable auto-exposure! Or enable it. I'm not sure. ",
  "Have you glued your lenses to keep them in focus?",
  "Don’t put spaces in your camera names — it makes the robot very sad",
  "Upgrade to Photon Pro for gtsam support 👍",
  "Did you forget to take off the lense covers? It’s dark in here…"
];

// State-specific humorous phrases
const cameraNeedsSetupPhrases = [
  "These cameras are just standing there... menacingly",
  "Are your cameras plugged in? Trick question -- they aren't!",
  "Have you hot-glued your USB cameras?"
];

const backendNotConnectedPhrases = [
  "Um, is this thing even on?",
  "Anyone home? Bulldozer? Bulldozer?",
  "Have you tried turning the NI™ RoboRIO™ off and on again?"
];

const ntDisconnectedPhrases = [
  "NetworkTables? More like Network'(; DROP TABLE websockets;--",
  "Robots shouldn't quit, but I sure can't talk to yours!",
  "Are you an OM5P? Because I can't talk to you over the LAN!",
  "I'm a sentient subatomic particle, not a networking engineer."
];

/**
 * Get list of applicable phrase categories based on current UI state
 */
function getApplicablePhraseLists() {
  const cameraStore = useCameraSettingsStore();
  const stateStore = useStateStore();

  // Build list of applicable phrase categories
  const applicableLists = [quarkyPhrases];

  // Add state-specific categories (additive, not replacing)
  if (cameraStore?.needsCameraConfiguration) {
    applicableLists.push(cameraNeedsSetupPhrases);
  }

  if (!stateStore?.backendConnected) {
    applicableLists.push(backendNotConnectedPhrases);
  }

  if (!stateStore?.ntConnectionStatus?.connected) {
    applicableLists.push(ntDisconnectedPhrases);
  }

  return applicableLists;
}

/**
 * Pick a random phrase from applicable categories
 */
function pickRandomPhrase() {
  const applicableLists = getApplicablePhraseLists();
  const randomList = applicableLists[Math.floor(Math.random() * applicableLists.length)];
  return randomList[Math.floor(Math.random() * randomList.length)];
}

/**
 * Get the duration of an animation in milliseconds
 */
function getAnimationDuration(animation) {
  if (!animation) return 500; // Default 0.5s for empty state

  // Animation durations (in seconds) based on quarky_generator.py
  const durations = {
    idle: 0.5,
    grow: 2.0,
    blink: 0.3,
    wave: 1.8,
    speak: 2.0,
    shrink: 2.0,
    point: 1.5
  };

  return (durations[animation] || 1.0) * 1000; // Convert to ms
}

/**
 * Play an animation
 */
function playAnimation(animation) {
  if (!animation) {
    quarkyImage.src = "";
    quarkyImage.style.display = "none";
    return;
  }
  quarkyImage.style.display = "block";
  quarkyImage.src = ANIMATIONS[animation];
  if (animation === "speak") {
    // Pick random phrase from applicable categories
    quarkySpeechText = pickRandomPhrase();
    speechBubble.textContent = quarkySpeechText;
    speechBubble.style.display = "block";
    speechBubble.style.opacity = 1;
    setTimeout(() => {
      speechBubble.style.opacity = 0;
      setTimeout(() => {
        speechBubble.style.display = "none";
      }, 700);
    }, 3000);
  }
}

// Mini Quarky management
let miniQuarkies = [];
const MAX_MINI_QUARKIES = 20;
const MINI_QUARKY_SIZE = 120;
const MINI_QUARKY_Z_INDEX = 999;
const MINI_QUARKY_VELOCITY_MULTIPLIER = 12;
const MINI_QUARKY_VELOCITY_CENTER = 0.5;
const DIRECTION_CHANGE_PROBABILITY = 0.02;
const DIRECTION_CHANGE_VELOCITY_MULTIPLIER = 4;
const MINI_QUARKY_ANIMATION_INTERVAL_MS = 50;
const MINI_QUARKY_SPAWN_BASE_DELAY_MS = 4000;
const MINI_QUARKY_SPAWN_DELAY_RANGE_MS = 8000;

// Random movement every few cycles
let mouseX = window.innerWidth / 2;
let mouseY = window.innerHeight / 2;
let mouseMoving = false;
let mouseMoveTimeout = null;

/**
 * Clamp Quarky's position to stay within the viewport, accounting for the sidebar
 */
function clampQuarkyPosition(x, y) {
  const rect = quarkyContainer.getBoundingClientRect();

  // Get sidebar width (account for both expanded and compact modes)
  const sidebar = document.querySelector(".v-navigation-drawer");
  const sidebarWidth = sidebar ? sidebar.offsetWidth : 0;

  // Clamp to viewport, starting after the sidebar
  const clampedX = Math.max(sidebarWidth, Math.min(x, window.innerWidth - rect.width));
  const clampedY = Math.max(0, Math.min(y, window.innerHeight - rect.height));
  return { x: clampedX, y: clampedY };
}

function mouseMoveHandler(e) {
  mouseX = e.clientX + window.scrollX;
  mouseY = e.clientY + window.scrollY;
  mouseMoving = true;
  clearTimeout(mouseMoveTimeout);
  // After 1.5s of no movement, return Quarky to home and resume nonsense
  mouseMoveTimeout = setTimeout(() => {
    mouseMoving = false;
    quarkyContainer.style.left = "calc(100vw - 550px)";
    quarkyContainer.style.top = "calc(100vh - 550px)";
    isMovingDemo = false;
    playNextAnimation();
  }, 1500);
  // Actively track mouse: update Quarky's position every mouse move (clamped to viewport)
  const clamped = clampQuarkyPosition(mouseX, mouseY);
  quarkyContainer.style.left = `${clamped.x}px`;
  quarkyContainer.style.top = `${clamped.y}px`;
  // Immediately trigger Quarky to point at cursor
  if (!isMovingDemo) {
    isMovingDemo = true;
    playAnimation("point");
    setTimeout(() => {
      playAnimation("idle");
    }, getAnimationDuration("point"));
  }
}

/**
 * Advance to the next animation in the sequence
 */
function playNextAnimation() {
  if (isMovingDemo) return;
  let currentStep = SEQUENCE[currentSequenceIndex];

  // On loop, skip grow after first time
  if (hasPlayedGrow && currentSequenceIndex === 0 && currentStep.animation === "grow") {
    currentSequenceIndex = 1;
    currentStep = SEQUENCE[currentSequenceIndex];
  }

  // If mouse is moving, don't do normal cycle
  if (mouseMoving) {
    // Quarky will point at cursor via mousemove handler
    return;
  }

  // Show speech bubble before speak
  if (currentStep.animation === "speak") {
    quarkySpeechText = pickRandomPhrase();
    speechBubble.textContent = quarkySpeechText;
    speechBubble.style.display = "block";
    speechBubble.style.opacity = 1;
    setTimeout(() => {
      speechBubble.style.opacity = 0;
      setTimeout(() => {
        speechBubble.style.display = "none";
      }, 700);
    }, 3000);
  }

  // Fade out speech bubble after speak (during idle)
  if (SEQUENCE[currentSequenceIndex - 1]?.animation === "speak" && currentStep.animation === "idle") {
    // Speech bubble already has its own timeout from above
  }

  // Always return to corner when idle
  quarkyContainer.style.left = "calc(100vw - 550px)";
  quarkyContainer.style.top = "calc(100vh - 550px)";

  // Play the animation
  playAnimation(currentStep.animation);

  // Calculate duration and schedule next animation
  const duration = getAnimationDuration(currentStep.animation);

  setTimeout(() => {
    currentLoopCount++;

    // Check if we've completed all loops for this step
    if (currentLoopCount >= currentStep.loops) {
      // Move to next step
      currentLoopCount = 0;
      currentSequenceIndex++;

      // Loop back to start if we've completed the sequence
      if (currentSequenceIndex >= SEQUENCE.length) {
        currentSequenceIndex = 0;
        hasPlayedGrow = true;
      }
    }

    // Play the next animation
    playNextAnimation();
  }, duration);
}

function clickToPoint(e) {
  // Ignore clicks on the button
  if (e.target.id === "moveDemoBtn") return;
  isMovingDemo = true;
  const clickX = e.clientX + window.scrollX;
  const clickY = e.clientY + window.scrollY;
  const clamped = clampQuarkyPosition(clickX, clickY);
  quarkyContainer.style.left = `${clamped.x}px`;
  quarkyContainer.style.top = `${clamped.y}px`;
  setTimeout(() => {
    playAnimation("point");
    setTimeout(() => {
      playAnimation("idle");
      quarkyContainer.style.left = "calc(100vw - 550px)";
      quarkyContainer.style.top = "calc(100vh - 550px)";
      setTimeout(() => {
        isMovingDemo = false;
        playNextAnimation();
      }, 1000);
    }, getAnimationDuration("point"));
  }, 1000);
}

/**
 * Spawn a mini Quarky that moves randomly around the screen
 */
function spawnMiniQuarky() {
  console.log("SPAWNING A QUARKY");

  // If at max, don't spawn
  if (miniQuarkies.length >= MAX_MINI_QUARKIES) {
    return;
  }

  const miniContainer = document.createElement("div");
  miniContainer.style.position = "fixed";
  miniContainer.style.width = `${MINI_QUARKY_SIZE}px`;
  miniContainer.style.height = `${MINI_QUARKY_SIZE}px`;
  miniContainer.style.pointerEvents = "none";
  miniContainer.style.zIndex = MINI_QUARKY_Z_INDEX.toString();

  // Spawn from main quarky's actual position
  const rect = quarkyContainer.getBoundingClientRect();
  const startX = rect.left + window.scrollX + rect.width / 2;
  const startY = rect.top + window.scrollY + rect.height / 2;
  miniContainer.style.left = startX + "px";
  miniContainer.style.top = startY + "px";

  const miniImage = document.createElement("img");
  miniImage.src = ANIMATIONS["idle"];
  miniImage.style.width = "100%";
  miniImage.style.height = "100%";
  miniImage.style.objectFit = "contain";

  miniContainer.appendChild(miniImage);
  document.body.appendChild(miniContainer);

  const miniQuarky = {
    container: miniContainer,
    image: miniImage,
    x: startX,
    y: startY,
    vx: (Math.random() - MINI_QUARKY_VELOCITY_CENTER) * MINI_QUARKY_VELOCITY_MULTIPLIER,
    vy: (Math.random() - MINI_QUARKY_VELOCITY_CENTER) * MINI_QUARKY_VELOCITY_MULTIPLIER,
    animationInterval: null
  };

  miniQuarkies.push(miniQuarky);

  // Start movement loop
  miniQuarky.animationInterval = setInterval(() => {
    miniQuarky.x += miniQuarky.vx;
    miniQuarky.y += miniQuarky.vy;

    // Bounce off edges
    if (miniQuarky.x <= 0 || miniQuarky.x >= window.innerWidth - MINI_QUARKY_SIZE) {
      miniQuarky.vx *= -1;
      miniQuarky.x = Math.max(0, Math.min(miniQuarky.x, window.innerWidth - MINI_QUARKY_SIZE));
    }
    if (miniQuarky.y <= 0 || miniQuarky.y >= window.innerHeight - MINI_QUARKY_SIZE) {
      miniQuarky.vy *= -1;
      miniQuarky.y = Math.max(0, Math.min(miniQuarky.y, window.innerHeight - MINI_QUARKY_SIZE));
    }

    // Occasionally change direction randomly
    if (Math.random() < DIRECTION_CHANGE_PROBABILITY) {
      miniQuarky.vx = (Math.random() - MINI_QUARKY_VELOCITY_CENTER) * DIRECTION_CHANGE_VELOCITY_MULTIPLIER;
      miniQuarky.vy = (Math.random() - MINI_QUARKY_VELOCITY_CENTER) * DIRECTION_CHANGE_VELOCITY_MULTIPLIER;
    }

    miniContainer.style.left = miniQuarky.x + "px";
    miniContainer.style.top = miniQuarky.y + "px";
  }, MINI_QUARKY_ANIMATION_INTERVAL_MS);
}

/**
 * Clean up all mini Quarkies
 */
function cleanupMiniQuarkies() {
  // eslint-disable-line @typescript-eslint/no-unused-vars
  miniQuarkies.forEach((mini) => {
    clearInterval(mini.animationInterval);
    mini.container.remove();
  });
  miniQuarkies = [];
}

export default function setup() {
  quarkyImage = document.getElementById("quarkyImage");
  quarkyContainer = document.getElementById("quarkyContainer");
  speechBubble = document.getElementById("quarkySpeechBubble");

  // Start the animation sequence
  playNextAnimation();

  //Install mouse move handler
  window.addEventListener("mousemove", mouseMoveHandler);

  // Click-to-point feature installation
  window.addEventListener("click", (e) => {
    clickToPoint(e);
  });

  // Spawn mini Quarkies on a random timer
  function scheduleNextMiniQuarkySpawn() {
    const delayMs = MINI_QUARKY_SPAWN_BASE_DELAY_MS + Math.random() * MINI_QUARKY_SPAWN_DELAY_RANGE_MS;
    setTimeout(() => {
      spawnMiniQuarky();
      scheduleNextMiniQuarkySpawn();
    }, delayMs);
  }
  scheduleNextMiniQuarkySpawn();
}
