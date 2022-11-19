// Circular buffer storage. Externally-apparent 'length' increases indefinitely
// while any items with indexes below length-n will be forgotten (undefined
// will be returned if you try to get them, trying to set is an exception).
// n represents the initial length of the array, not a maximum
class StatsHistoryBuffer{
    constructor (){ 
        this.windowLen = 15; //eeh guess
        this._array= new Array(this.windowLen);
        this.headPtr = 0;
        this.frameCount = 0;
        this.bitAvgAccum = 0;
        
        //calculated vals
        this.bitRate_mbps = 0;
        this.framerate_fps = 0;
    }

    putAndPop(v){
        this.headPtr++;
        var idx = (this.headPtr)%this._array.length;
        var poppedVal = this._array[idx];
        this._array[idx] = v;
        return poppedVal;
    }

    addSample(time, frameSize_bits, dispFrame_count) {
        var oldVal = this.putAndPop([time, frameSize_bits, dispFrame_count]);
        var oldTime = oldVal[0];
        var oldFrameSize = oldVal[1];
        var oldFrameCount = oldVal[2];

        var deltaTime_s = (time - oldTime);

        this.bitAvgAccum += frameSize_bits;
        this.bitAvgAccum -= oldFrameSize;

        this.bitRate_mbps = (this.bitAvgAccum / deltaTime_s) * (1.0/1048576.0);
        this.framerate_fps = (dispFrame_count - oldFrameCount) / deltaTime_s;
    }

}


export class WebsocketVideoStream{


    constructor(drawDiv, streamPort, host) {
        console.log("host " + host + " port " + streamPort)

        this.drawDiv = drawDiv;
        this.image = document.getElementById(this.drawDiv);
        this.streamPort = streamPort;
        this.newStreamPortReq = null;
        this.serverAddr = "ws://" + host + "/websocket_cameras";
        this.dispNoStream();
        this.ws_connect();
        this.imgData = null;
        this.imgDataTime = -1;
        this.imgObjURL = null;
        this.frameRxCount = 0;
        this.dispFrameCount = 0;
        this.stats = null;

        //Set up div for stats overlay
        this.statsTextDiv = this.image.parentNode.appendChild(document.createElement("div"));


        //Display state machine
        this.DSM_DISCONNECTED = "DISCONNECTED";
        this.DSM_WAIT_FOR_VALID_PORT = "WAIT_FOR_VALID_PORT";
        this.DSM_SUBSCRIBE = "SUBSCRIBE";
        this.DSM_WAIT_FOR_FIRST_FRAME = "WAIT_FOR_FIRST_FRAME";
        this.DSM_SHOWING = "SHOWING";
        this.DSM_RESTART_UNSUBSCRIBE = "UNSUBSCRIBE";
        this.DSM_RESTART_WAIT = "WAIT_BEFORE_SUBSCRIBE";

        this.dsm_cur_state = this.DSM_DISCONNECTED;
        this.dsm_prev_state = this.DSM_DISCONNECTED;
        this.dsm_restart_start_time = window.performance.now();

        requestAnimationFrame(()=>this.animationLoop());

    }

    dispImageData(){
        //From https://stackoverflow.com/questions/67507616/set-image-src-from-image-blob/67507685#67507685
        if(this.imgObjURL != null){
            URL.revokeObjectURL(this.imgObjURL)
        }
        this.imgObjURL = URL.createObjectURL(this.imgData);

        //Update the image with the new mimetype and image
        this.image.src = this.imgObjURL;

        this.dispFrameCount++;
    }

    dispNoStream() {
        this.image.src = require("../assets/loading.gif");
    }

    animationLoop(){
        // Update time metrics
        var now = window.performance.now();
        var timeInState  = now - this.dsm_restart_start_time;

        // Save previous state
        this.dsm_prev_state = this.dsm_cur_state;

        // Evaluate state transitions
        if(this.serverConnectionActive == false){
            //Any state - if the server connection goes false, always transition to disconnected
            this.dsm_cur_state = this.DSM_DISCONNECTED;
        } else {
            //Conditional transitions
            switch(this.dsm_cur_state) {
                case this.DSM_DISCONNECTED:
                    //Immediately transition to waiting for the first frame
                    this.dsm_cur_state = this.DSM_WAIT_FOR_VALID_PORT;
                    break;
                case this.DSM_WAIT_FOR_VALID_PORT:
                    // Wait until the user has configured a valid port
                    if(this.streamPort > 0){
                        this.dsm_cur_state = this.DSM_SUBSCRIBE;
                    } else {
                        this.dsm_cur_state = this.DSM_WAIT_FOR_VALID_PORT;
                    }
                    break;
                case this.DSM_SUBSCRIBE:
                    // Immediately transition after subscriptions is sent
                    this.dsm_cur_state = this.DSM_WAIT_FOR_FIRST_FRAME;
                    break;
                case this.DSM_WAIT_FOR_FIRST_FRAME:
                    if(this.imgData != null){
                        //we got some image data, start showing it
                        this.dsm_cur_state = this.DSM_SHOWING;
                    } else if (this.newStreamPortReq != null){
                        //Stream port requested changed, unsubscribe and restart
                        this.dsm_cur_state = this.DSM_RESTART_UNSUBSCRIBE;
                    } else {
                        this.dsm_cur_state = this.DSM_WAIT_FOR_FIRST_FRAME;
                    }
                    break;
                case this.DSM_SHOWING:
                    if((now - this.imgDataTime) > 2500){
                        //timeout, begin the restart sequence
                        this.dsm_cur_state = this.DSM_RESTART_UNSUBSCRIBE;
                    } else if (this.newStreamPortReq != null){
                        //Stream port requested changed, unsubscribe and restart
                        this.dsm_cur_state = this.DSM_RESTART_UNSUBSCRIBE;
                    }  else {
                        //stay in this state.
                        this.dsm_cur_state = this.DSM_SHOWING;
                    }
                    break;
                case this.DSM_RESTART_UNSUBSCRIBE:
                    //Only should spend one loop in Unsubscribe, immediately transition
                    this.dsm_cur_state = this.DSM_RESTART_WAIT;
                    break;
                case this.DSM_RESTART_WAIT:
                    if (timeInState > 250) {
                        //we've waited long enough, go to try to re-subscribe
                        this.dsm_cur_state = this.DSM_WAIT_FOR_VALID_PORT;
                    } else {
                        //stay in this state.
                        this.dsm_cur_state = this.DSM_RESTART_WAIT;
                    }
                    break;
                default:
                    // Shouldn't get here, default back to init
                    this.dsm_cur_state = this.DSM_DISCONNECTED;
              }
        }

        //take current-state or state-transition actions

        if(this.dsm_cur_state != this.dsm_prev_state){
            //Any state transition
            console.log("State Change: " + this.dsm_prev_state + " -> " + this.dsm_cur_state);
        }

        if(this.dsm_cur_state == this.DSM_SHOWING){
            // Currently in SHOWING
            this.dispImageData();
        }

        if(this.dsm_cur_state != this.DSM_SHOWING && this.dsm_prev_state == this.DSM_SHOWING ){
            //Any transition out of showing - no stream
            this.dispNoStream();
        }

        if(this.dsm_cur_state == this.DSM_RESTART_UNSUBSCRIBE){
            // Currently in UNSUBSCRIBE, do the unsubscribe actions
            this.stopStream();
            this.dsm_restart_start_time = now;
        }

        if(this.dsm_cur_state == this.DSM_SUBSCRIBE){
            // Currently in SUBSCRIBE, do the subscribe actions
            this.startStream();
            this.dsm_restart_start_time = now;
        }

        if(this.dsm_cur_state == this.DSM_WAIT_FOR_VALID_PORT){
            // Currently waiting for a vaild port to be requested
            if(this.newStreamPortReq != null){
                this.streamPort = this.newStreamPortReq;
                this.newStreamPortReq = null;
            }
        }

        requestAnimationFrame(()=>this.animationLoop());
    }

    startStream() {
        console.log("Subscribing to port " + this.streamPort);
        this.imgData = null;
        this.ws.send(JSON.stringify({"cmd": "subscribe", "port":this.streamPort}));
    }

    stopStream() {
        console.log("Unsubscribing");
        this.ws.send(JSON.stringify({"cmd": "unsubscribe"}));
        this.imgData = null;
    }

    setPort(streamPort){
        console.log("Port set to " + streamPort);
        this.newStreamPortReq = streamPort;
    }

    ws_onOpen() {
        // Set the flag allowing general server communication
        this.serverConnectionActive = true;
        console.log("Camera Websockets Connected!");

        // New websocket connection, reset stats
        this.frameRxCount = 0;
        this.dispFrameCount = 0;
        this.stats = new StatsHistoryBuffer();
    }

    ws_onClose(e) {
        //Clear flags to stop server communication
        this.ws = null;
        this.serverConnectionActive = false;

        console.log('Camera Socket is closed. Reconnect will be attempted in 0.5 second.', e.reason);
        setTimeout(this.ws_connect.bind(this), 500);

        if(!e.wasClean){
            console.error('Socket encountered error!');
        }

    }

    ws_onError(e){
        e; //prevent unused failure
        this.ws.close();
    }

    ws_onMessage(e){
        //console.log("Got message from " + this.serverAddr)
        var msgTime = window.performance.now();
        if(typeof e.data === 'string'){
            //string data from host
            //TODO - anything to receive info here? Maybe "available streams?"
        } else {
            if(e.data.size > 0){
                //binary data - a frame!
                //Save frame data for display in the next animation thread
                this.imgData = e.data;
                this.imgDataTime = msgTime;

                //Count the incoming frame
                this.frameRxCount++;

                //keep the stats up to date
                this.stats.addSample(msgTime,this.imgData.size(),this.dispFrameCount);
            } else {
                //TODO - server is sending empty frames?
                console.log("WS Stream Error: Server sent empty frame!");
            }
        }

    }

    ws_connect() {
        this.serverConnectionActive = false;
        this.ws = new WebSocket(this.serverAddr);
        this.ws.binaryType = "blob";
        this.ws.onopen = this.ws_onOpen.bind(this);
        this.ws.onmessage = this.ws_onMessage.bind(this);
        this.ws.onclose = this.ws_onClose.bind(this);
        this.ws.onerror = this.ws_onError.bind(this);
        console.log("Connecting to server " + this.serverAddr);
    }

    ws_close(){
        this.ws.close();
    }

}


export default {WebsocketVideoStream}
