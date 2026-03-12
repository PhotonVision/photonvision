import IDLEGIF from '@/assets/images/idle.gif';
import GROWGIF from '@/assets/images/grow.gif';
import BLINKGIF from '@/assets/images/blink.gif';
import WAVEGIF from '@/assets/images/wave.gif';
import SPEAKGIF from '@/assets/images/speak.gif';
import SHRINKGIF from '@/assets/images/shrink.gif';
import POINTGIF from '@/assets/images/point.gif';

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
    { animation: 'grow', loops: 1 },
    { animation: 'idle', loops: 2 },
    { animation: 'blink', loops: 1 },
    { animation: 'idle', loops: 1 },
    { animation: 'speak', loops: 1 },
    { animation: 'idle', loops: 1 },
    { animation: 'point', loops: 1 },
    { animation: 'idle', loops: 1 },
    { animation: 'wave', loops: 1 },
    { animation: 'idle', loops: 1 },
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
    "Quantum entanglement buffer overflow.",
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
    "That's a horrible choice!",
    "Fun is a core value! Is that a fun choice?",
    "If your grandma saw that choice, would she be proud?",
    "Chute Door?",
    "Yes, Chute Door!",
    "That's a bold strategy, Cotton.",
    "asdflkjaslkdflklnf2222",
    "00110101? That's just gibberish!",
    "Three is my favorite number too!",
];

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
        quarkyImage.src = '';
        quarkyImage.style.display = 'none';
        return;
    }
    quarkyImage.style.display = 'block';
    quarkyImage.src = ANIMATIONS[animation];
    if (animation === 'speak') {
        // Pick random phrase
        quarkySpeechText = quarkyPhrases[Math.floor(Math.random() * quarkyPhrases.length)];
        speechBubble.textContent = quarkySpeechText;
        speechBubble.style.display = 'block';
        setTimeout(() => { speechBubble.style.opacity = 1; }, 10);
    }
}

// Random movement every few cycles
let randomMoveCounter = 0;
let mouseX = window.innerWidth / 2;
let mouseY = window.innerHeight / 2;
let mouseMoving = false;
let mouseMoveTimeout = null;

function mouseMoveHandler(e){
    mouseX = e.clientX + window.scrollX;
    mouseY = e.clientY + window.scrollY;
    mouseMoving = true;
    clearTimeout(mouseMoveTimeout);
    // After 1.5s of no movement, return Quarky to home and resume nonsense
    mouseMoveTimeout = setTimeout(() => {
        mouseMoving = false;
        quarkyContainer.style.left = 'calc(100vw - 550px)';
        quarkyContainer.style.top = 'calc(100vh - 550px)';
        isMovingDemo = false;
        playNextAnimation();
    }, 1500);
    // Actively track mouse: update Quarky's position every mouse move
    quarkyContainer.style.left = `${mouseX}px`;
    quarkyContainer.style.top = `${mouseY}px`;
    // Immediately trigger Quarky to point at cursor
    if (!isMovingDemo) {
        isMovingDemo = true;
        playAnimation('point');
        setTimeout(() => {
            playAnimation('idle');
        }, getAnimationDuration('point'));
    }
}

/**
 * Advance to the next animation in the sequence
 */
function playNextAnimation() {
    if (isMovingDemo) return;
    let currentStep = SEQUENCE[currentSequenceIndex];

    // On loop, skip grow after first time
    if (hasPlayedGrow && currentSequenceIndex === 0 && currentStep.animation === 'grow') {
        currentSequenceIndex = 1;
        currentStep = SEQUENCE[currentSequenceIndex];
    }

    // If mouse is moving, don't do normal cycle
    if (mouseMoving) {
        // Quarky will point at cursor via mousemove handler
        return;
    }

    // Show speech bubble before speak
    if (currentStep.animation === 'speak') {
        quarkySpeechText = quarkyPhrases[Math.floor(Math.random() * quarkyPhrases.length)];
        speechBubble.textContent = quarkySpeechText;
        speechBubble.style.display = 'block';
        setTimeout(() => { speechBubble.style.opacity = 1; }, 10);
    }

    // Fade out speech bubble after speak (during idle)
    if (SEQUENCE[currentSequenceIndex - 1]?.animation === 'speak' && currentStep.animation === 'idle') {
        speechBubble.style.opacity = 0;
        setTimeout(() => { speechBubble.style.display = 'none'; }, 700);
    }

    // Always return to corner when idle
    quarkyContainer.style.left = 'calc(100vw - 550px)';
    quarkyContainer.style.top = 'calc(100vh - 550px)';

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


function clickToPoint(e){
   // Ignore clicks on the button
    if (e.target.id === 'moveDemoBtn') return;
    isMovingDemo = true;
    const clickX = e.clientX + window.scrollX;
    const clickY = e.clientY + window.scrollY;
    quarkyContainer.style.left = `${clickX}px`;
    quarkyContainer.style.top = `${clickY}px`;
    setTimeout(() => {
        playAnimation('point');
        setTimeout(() => {
            playAnimation('idle');
            quarkyContainer.style.left = 'calc(100vw - 550px)';
            quarkyContainer.style.top = 'calc(100vh - 550px)';
            setTimeout(() => {
                isMovingDemo = false;
                playNextAnimation();
            }, 1000);
        }, getAnimationDuration('point'));
    }, 1000);
}



export default function setup() {
    quarkyImage = document.getElementById('quarkyImage');
    quarkyContainer = document.getElementById('quarkyContainer');
    speechBubble = document.getElementById('quarkySpeechBubble');

    // Start the animation sequence
    playNextAnimation();

    //Install mouse move handler
    window.addEventListener('mousemove', mouseMoveHandler);

    // Click-to-point feature installation
    window.addEventListener('click', (e) => {
        clickToPoint(e);
    });
}