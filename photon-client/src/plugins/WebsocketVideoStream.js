

export class WebsocketVideoStream{


    constructor(drawDiv, streamPort, host) {

        this.drawDiv = drawDiv;
        this.image = document.getElementById(this.drawDiv);
        this.streamPort = streamPort;
        this.serverAddr = "ws://" + host + "/websocket_cameras";
        this.noStream = false;
        this.noStreamPrev = false;
        this.setNoStream();
        this.ws_connect();
        this.imgData = null;
        this.imgDataTime = -1;
        this.imgObjURL = null;
        this.frameRxCount = 0;

        requestAnimationFrame(()=>this.animationLoop());

    }

    animationLoop(){
        var now = window.performance.now();

        if((now - this.imgDataTime) > 2500 && this.imgData != null){
            //Handle websocket send timeouts by restarting
            this.setNoStream();
            this.stopStream();
            setTimeout(this.startStream.bind(this), 1000); //restart stream one second later
        } else {
            if(this.streamPort == null){
                this.setNoStream();
            } else if (this.imgData != null) {
                //From https://stackoverflow.com/questions/67507616/set-image-src-from-image-blob/67507685#67507685
                if(this.imgObjURL != null){
                    URL.revokeObjectURL(this.imgObjURL)
                }
                this.imgObjURL = URL.createObjectURL(this.imgData);

                //Update the image with the new mimetype and image
                this.image.src = this.imgObjURL;
                this.noStream = false;

            } else {
                //Nothing, hold previous image while waiting for next frame
            }
        }


        requestAnimationFrame(()=>this.animationLoop());
    }

    setNoStream() {
        this.noStreamPrev = this.noStream;
        this.noStream = true;
        if(this.noStreamPrev == false && this.noStream == true){
            //One-shot background change to preserve animation
            this.image.src = require("../assets/loading.gif");
        }
    }

    startStream() {
        if(this.serverConnectionActive == true && this.streamPort > 0){
            this.ws.send(JSON.stringify({"cmd": "subscribe", "port":this.streamPort}));
            this.noStream = false;
        }
    }

    stopStream() {
        if(this.serverConnectionActive == true && this.streamPort > 0){
            this.ws.send(JSON.stringify({"cmd": "unsubscribe"}));
            this.noStream = true;
        }
    }

    setPort(streamPort){
        this.stopStream();
        this.frameRxCount = 0;
        this.streamPort = streamPort;
        this.startStream();
    }

    ws_onOpen() {
        // Set the flag allowing general server communication
        this.serverConnectionActive = true;
        console.log("Connected!");
        this.startStream();
    }

    ws_onClose(e) {
        this.setNoStream();

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
        if(typeof e.data === 'string'){
            //string data from host
            //TODO - anything to receive info here? Maybe "available streams?"
        } else {
            if(e.data.size > 0){
                //binary data - a frame
                this.imgData = e.data;
                this.imgDataTime = window.performance.now();
                this.frameRxCount++;
            } else {
                //TODO - server is sending empty frames?
            }
        }

    }

    ws_connect() {
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
