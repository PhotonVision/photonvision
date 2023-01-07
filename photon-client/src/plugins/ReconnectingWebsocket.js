/**
 * Auto-reconnecting Websocket, a stripped down version of the NT4 client from
 * https://raw.githubusercontent.com/wpilibsuite/NetworkTablesClients/2f8d378ac08d5ca703d590cfb019fc4af062db89/nt4/js/src/nt4.js
 */
export class ReconnectingWebsocket {
    constructor(serverAddr,
        onDataIn_in,
        onConnect_in,
        onDisconnect_in) {

        this.onDataIn = onDataIn_in;
        this.onConnect = onConnect_in;
        this.onDisconnect = onDisconnect_in;

        // WS Connection State (with defaults)
        this.serverAddr = serverAddr;
        this.serverConnectionActive = false;

        //Trigger the websocket to connect automatically
        this.ws_connect();
    }

    //////////////////////////////////////////////////////////////
    // Websocket connection Maintenance

    ws_onOpen() {
        // Set the flag allowing general server communication
        this.serverConnectionActive = true;

        console.log("[WebSocket] Connected!");

        // User connection-opened hook
        this.onConnect();
    }

    ws_onClose(e) {
        //Clear flags to stop server communication
        this.ws = null;
        this.serverConnectionActive = false;

        // User connection-closed hook
        this.onDisconnect();

        console.log('[WebSocket] Socket is closed. Reconnect will be attempted in 0.5 second.', e.reason);
        setTimeout(this.ws_connect.bind(this), 500);

        if (!e.wasClean) {
            console.error('Socket encountered error!');
        }

    }

    ws_onError(e) {
        console.log("[WebSocket] Websocket error - " + e.toString());
        this.ws.close();
    }

    ws_onMessage(e) {
       this.onDataIn(e);
    }

    ws_connect() {
        this.ws = new WebSocket(this.serverAddr);
        this.ws.binaryType = "arraybuffer";
        this.ws.onopen = this.ws_onOpen.bind(this);
        this.ws.onmessage = this.ws_onMessage.bind(this);
        this.ws.onclose = this.ws_onClose.bind(this);
        this.ws.onerror = this.ws_onError.bind(this);

        console.log("[WebSocket] Starting...");
    }
}

export default { ReconnectingWebsocket }
