import { NT4_Client } from "/interfaces/nt4.js";

var nt4Client = new NT4_Client(window.location.hostname,
                               topicAnnounceHandler,
                               topicUnannounceHandler,
                               valueUpdateHandler,
                               onConnect,
                               onDisconnect
                               );


console.log("Starting connection...");
nt4Client.ws_connect();
console.log("Connection Triggered");

var subscription = null;

function topicAnnounceHandler( newTopic ) {

    // If topic is a photonvision camera stream, show it in a card.
    console.log("New topic announced: " + newTopic.name);

    if(isCamStreamTopic(newTopic.name)) {
        console.log("Camera stream topic detected: " + newTopic.name);
        
        // Create or update the tile with the MJPEG stream URL
        createOrUpdateTile(topicToCamName(newTopic.name), newTopic.value);
    }
}

function topicUnannounceHandler( removedTopic ) {
    if(isCamStreamTopic(removedTopic.name)) {
        removeTile(topicToCamName(removedTopic.name));
    }
}

function valueUpdateHandler(topic, timestamp_us, value) {
    // If topic is a photonvision camera stream, update the card with the new value.
    if(isCamStreamTopic(topic.name)) {
        console.log("Value update for topic: " + topic.name);
        console.log("Value: ", value);
        // Create or update the tile with the MJPEG stream URL
        createOrUpdateTile(topicToCamName(topic.name), value);
    }
}

function topicToCamName(topicName) {
    // Extract the camera name from the topic name
    // Strips prefix and suffix, and replaces _ with spaces
    // For example for the topic /CameraPublisher/photonvision_Port_1181_Input_MJPEG_Server/streams
    // The name shall be "Port 1181 Input"
    return topicName.split("/")[2].replace("photonvision_", "").replace("_MJPEG_Server", "").replace(/_/g, " ");
}

function isCamStreamTopic(topicName) {
    // Check if the topic is a photonvision camera stream
    return topicName.startsWith("/CameraPublisher/") && topicName.endsWith("/streams");
}

function onConnect() {

    document.getElementById("status").innerHTML = "Connected to Server";
    removeAllTiles();
    subscribeToCamerServer();
}

function onDisconnect() {
    document.getElementById("status").innerHTML = "Disconnected from Server";
    subscription = null;
}


function subscribeToCamerServer() {
    if(subscription == null){
        subscription = nt4Client.subscribePeriodic(["/CameraPublisher/"], 0.5);
    }
}

function createOrUpdateTile(camName, mjpegUrlList) {
    let tileId = camName + "-tile";

    const grid = document.getElementById('tileGrid');
    let tile = document.getElementById(tileId);

    if (!tile) {
        tile = document.createElement('div');
        tile.className = 'tile';
        tile.id = tileId;
        grid.appendChild(tile);
    }

    // Clear previous content
    tile.innerHTML = '';

    // Create a container for the cam name and image
    const content = document.createElement('div');
    content.className = 'tile-content';

    // Camera name element
    const camNameElement = document.createElement('div');
    camNameElement.className = 'cam-name';
    camNameElement.textContent = camName;

    // Filter and clean up the MJPEG URLs
    let urls = (Array.isArray(mjpegUrlList) ? mjpegUrlList : [])
        .filter(entry => typeof entry === "string" && entry.startsWith("mjpg:"))
        .map(entry => entry.replace("mjpg:", ""));

    // Prefer IPv4 addresses over DNS/mDNS
    const ipv4Regex = /https?:\/\/(\d{1,3}\.){3}\d{1,3}/;
    urls = urls.sort((a, b) => {
        const aIsIp = ipv4Regex.test(a);
        const bIsIp = ipv4Regex.test(b);
        return (aIsIp === bIsIp) ? 0 : aIsIp ? -1 : 1;
    });

    if (urls.length === 0) {
        tile.textContent = camName + ": No valid MJPEG stream found.";
        return;
    }

    // Create the img element
    const img = document.createElement('img');
    img.style.width = '100%';
    img.style.height = '100%';
    img.alt = 'Stream';

    let currentIdx = 0;
    img.src = urls[currentIdx];

    img.onerror = function () {
        currentIdx++;
        if (currentIdx < urls.length) {
            img.src = urls[currentIdx];
        } else {
            tile.textContent = camName + ": All MJPEG streams failed to load.";
        }
    };

    img.onclick = function () {
    if (img.requestFullscreen) {
        img.requestFullscreen();
    } else if (img.webkitRequestFullscreen) { // Safari
        img.webkitRequestFullscreen();
    } else if (img.msRequestFullscreen) { // IE11
        img.msRequestFullscreen();
    }
};

    content.appendChild(camNameElement);
    content.appendChild(img);
    tile.appendChild(content);
}


function removeTile(camName) {
    let tileId = camName + "-tile";
    const tile = document.getElementById(tileId);
    if (tile) {
        tile.remove();
    }
}

function removeAllTiles() {
    const grid = document.getElementById('tileGrid');
    while (grid.firstChild) {
        grid.removeChild(grid.firstChild);
    }
}