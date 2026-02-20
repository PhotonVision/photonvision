import "./msgpack/msgpack.js";

var typestrIdxLookup = {
    NT4_TYPESTR: 0,
    "double": 1,
    "int": 2,
    "float": 3,
    "string": 4,
    "json": 4,
    "raw": 5,
    "rpc": 5,
    "msgpack": 5,
    "protobuf": 5,
    "boolean[]": 16,
    "double[]": 17,
    "int[]": 18,
    "float[]": 19,
    "string[]": 20
}

class NT4_TYPESTR {
    static BOOL = "boolean";
    static FLOAT_64 = "double";
    static INT = "int";
    static FLOAT_32 = "float";
    static STR = "string";
    static JSON = "json";
    static BIN_RAW = "raw";
    static BIN_RPC = "rpc";
    static BIN_MSGPACK = "msgpack";
    static BIN_PROTOBUF = "protobuf";
    static BOOL_ARR = "boolean[]";
    static FLOAT_64_ARR = "double[]";
    static INT_ARR = "int[]";
    static FLOAT_32_ARR = "float[]";
    static STR_ARR = "string[]";
}

export class NT4_ValReq {
    topics = new Set();

    toGetValsObj() {
        return {
            "topics": Array.from(this.topics),
        };
    }
}

export class NT4_Subscription {
    topics = new Set();
    options = new NT4_SubscriptionOptions();
    uid = -1;

    toSubscribeObj() {
        return {
            "topics": Array.from(this.topics),
            "options": this.options.toObj(),
            "subuid": this.uid,
        };
    }

    toUnSubscribeObj() {
        return {
            "subuid": this.uid,
        };
    }
}

export class NT4_SubscriptionOptions {
    periodicRate_s = 0.1;
    all = false;
    topicsonly = false;
    prefix = true; //nonstandard default

    toObj() {
        return {
            "periodic": this.periodicRate_s,
            "all": this.all,
            "topicsonly": this.topicsonly,
            "prefix": this.prefix,
        };
    }
}

export class NT4_Topic {
    name = "";
    type = "";
    id = 0;
    pubuid = 0;
    properties = {}; //Properties are free-form, might have anything in them

    toPublishObj() {
        return {
            "name": this.name,
            "type": this.type,
            "pubuid": this.pubuid,
        }
    }

    toUnPublishObj() {
        return {
            "name": this.name,
            "pubuid": this.pubuid,
        }
    }

    toPropertiesObj() {
        return {
            "name": this.name,
            "update": this.properties,
        }
    }

    getTypeIdx() {
        return typestrIdxLookup[this.type];
    }
}

export class NT4_Client {


    constructor(serverAddr,
        onTopicAnnounce_in,    //Gets called when server announces enough topics to form a new signal
        onTopicUnAnnounce_in,  //Gets called when server unannounces any part of a signal
        onNewTopicData_in,     //Gets called when any new data is available
        onConnect_in,          //Gets called once client completes initial handshake with server
        onDisconnect_in) {     //Gets called once client detects server has disconnected

        this.onTopicAnnounce = onTopicAnnounce_in;
        this.onTopicUnAnnounce = onTopicUnAnnounce_in;
        this.onNewTopicData = onNewTopicData_in;
        this.onConnect = onConnect_in;
        this.onDisconnect = onDisconnect_in;

        this.subscriptions = new Map();
        this.subscription_uid_counter = 0;
        this.publish_uid_counter = 0;

        this.clientPublishedTopics = new Map();
        this.announcedTopics = new Map();

        this.timeSyncBgEvent = setInterval(this.ws_sendTimestamp.bind(this), 5000);

        // WS Connection State (with defaults)
        this.serverBaseAddr = serverAddr;
        this.clientIdx = 0;
        this.serverAddr = "";
        this.serverConnectionActive = false;
        this.serverTimeOffset_us = 0;


    }

    //////////////////////////////////////////////////////////////
    // PUBLIC API

    // Add a new subscription which requests announcment of topics
    subscribeTopicNames(topicPatterns) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewSubUID();
        newSub.options.topicsonly = true;
        newSub.options.periodicRate_s = 1.0;
        newSub.topics = new Set(topicPatterns);

        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub;
    }

    // Add a new subscription. Returns a subscription object
    subscribePeriodic(topicPatterns, period) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewSubUID();
        newSub.options.periodicRate_s = period;
        newSub.topics = new Set(topicPatterns);

        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub;
    }

    // Add a new subscription. Returns a subscription object
    subscribeAllSamples(topicPatterns) {
        var newSub = new NT4_Subscription();
        newSub.uid = this.getNewSubUID();
        newSub.topics = new Set(topicPatterns);
        newSub.options.all = true;

        this.subscriptions.set(newSub.uid, newSub);
        if (this.serverConnectionActive) {
            this.ws_subscribe(newSub);
        }
        return newSub;
    }

    // Given an existing subscription, unsubscribe from it.
    unSubscribe(sub) {
        this.subscriptions.delete(sub.uid);
        if (this.serverConnectionActive) {
            this.ws_unsubscribe(sub);
        }
    }

    // Unsubscribe from all current subscriptions
    clearAllSubscriptions() {
        for (const sub of this.subscriptions.values()) {
            this.unSubscribe(sub);
        }
    }

    // Set the properties of a particular topic
    setProperties(topic, isPersistent, isRetained) {
        topic.properties.persistent = isPersistent;
        topic.properties.retained = isRetained;
        if (this.serverConnectionActive) {
            this.ws_setproperties(topic);
        }
    }

    // Publish a new topic from this client with the provided name and type
    publishNewTopic(name, type) {
        var newTopic = new NT4_Topic();
        newTopic.name = name;
        newTopic.type = type;
        this.publishTopic(newTopic);
        return newTopic;
    }

    // Publish an existing topic to the server
    publishTopic(topic) {
        topic.pubuid = this.getNewPubUID();
        this.clientPublishedTopics.set(topic.name, topic);
        if (this.serverConnectionActive) {
            this.ws_publish(topic);
        }
    }

    // UnPublish a previously-published topic from this client.
    unPublishTopic(oldTopic) {
        this.clientPublishedTopics.delete(oldTopic.name);
        if (this.serverConnectionActive) {
            this.ws_unpublish(oldTopic);
        }
    }

    // Send some new value to the server
    // Timestamp is whatever the current time is.
    addSample(topic, value) {
        var timestamp = this.getServerTime_us();
        this.addSample(topic, timestamp, value);
    }

    // Send some new timestamped value to the server
    addSample(topic, timestamp, value) {

        if (typeof topic === 'string') {
            var topicFound = false;
            //Slow-lookup - strings are assumed to be topic names for things the server has already announced.
            for (const topicIter of this.announcedTopics.values()) {
                if (topicIter.name === topic) {
                    topic = topicIter;
                    topicFound = true;
                    break;
                }
            }
            if (!topicFound) {
                throw "Topic " + topic + " not found in announced server topics!";
            }
        }

        var sourceData = [topic.pubuid, timestamp, topic.getTypeIdx(), value];
        var txData = msgpack.serialize(sourceData);

        this.ws_sendBinary(txData);
    }

    //////////////////////////////////////////////////////////////
    // Server/Client Time Sync Handling

    getClientTime_us() {
        return Math.round(performance.now() * 1000.0);
    }

    getServerTime_us() {
        return this.getClientTime_us() + this.serverTimeOffset_us;
    }

    ws_sendTimestamp() {
        var timeTopic = this.announcedTopics.get(-1);
        if (timeTopic) {
            var timeToSend = this.getClientTime_us();
            this.addSample(timeTopic, 0, timeToSend);
        }
    }

    ws_handleReceiveTimestamp(serverTimestamp, clientTimestamp) {
        var rxTime = this.getClientTime_us();

        //Recalculate server/client offset based on round trip time
        var rtt = rxTime - clientTimestamp;
        var serverTimeAtRx = serverTimestamp - rtt / 2.0;
        this.serverTimeOffset_us = serverTimeAtRx - rxTime;

    }

    //////////////////////////////////////////////////////////////
    // Websocket Message Send Handlers

    ws_subscribe(sub) {
        this.ws_sendJSON("subscribe", sub.toSubscribeObj());
    }

    ws_unsubscribe(sub) {
        this.ws_sendJSON("unsubscribe", sub.toUnSubscribeObj());
    }

    ws_publish(topic) {
        this.ws_sendJSON("publish", topic.toPublishObj());
    }

    ws_unpublish(topic) {
        this.ws_sendJSON("unpublish", topic.toUnPublishObj());
    }

    ws_setproperties(topic) {
        this.ws_sendJSON("setproperties", topic.toPropertiesObj());
    }

    ws_sendJSON(method, params) { //Sends a single json message
        if (this.ws.readyState === WebSocket.OPEN) {
            var txObj = [{
                "method": method,
                "params": params
            }];
            var txJSON = JSON.stringify(txObj);

            //console.log("[NT4] Client Says: " + txJSON);

            this.ws.send(txJSON);
        }
    }

    ws_sendBinary(data) {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(data);
        }
    }

    //////////////////////////////////////////////////////////////
    // Websocket connection Maintenance

    ws_onOpen() {

        // Add default time topic
        var timeTopic = new NT4_Topic();
        timeTopic.name = "Time";
        timeTopic.id = -1;
        timeTopic.pubuid = -1;
        timeTopic.type = NT4_TYPESTR.INT;
        this.announcedTopics.set(timeTopic.id, timeTopic);

        // Set the flag allowing general server communication
        this.serverConnectionActive = true;

        //Publish any existing topics
        for (const topic of this.clientPublishedTopics.values()) {
            this.ws_publish(topic);
            this.ws_setproperties(topic);
        }

        //Subscribe to existing subscriptions
        for (const sub of this.subscriptions.values()) {
            this.ws_subscribe(sub);
        }

        // User connection-opened hook
        this.onConnect();
    }

    ws_onClose(e) {
        //Clear flags to stop server communication
        this.ws = null;
        this.serverConnectionActive = false;

        // User connection-closed hook
        this.onDisconnect();

        //Clear out any local cache of server state
        this.announcedTopics.clear();

        console.log('[NT4] Socket is closed. Reconnect will be attempted in 0.5 second.', e.reason);
        setTimeout(this.ws_connect.bind(this), 500);

        if (!e.wasClean) {
            console.error('Socket encountered error!');
        }

    }

    ws_onError(e) {
        console.log("[NT4] Websocket error - " + e.toString());
        this.ws.close();
    }

    ws_onMessage(e) {
        if (typeof e.data === 'string') {
            //console.log("[NT4] Server Says: " + e.data);
            //JSON Message
            var rxArray = JSON.parse(e.data);

            rxArray.forEach(function (msg) {

                //Validate proper format of message
                if (typeof msg !== 'object') {
                    console.log("[NT4] Ignoring text message, JSON parsing did not produce an object.");
                    return;
                }

                if (!("method" in msg) || !("params" in msg)) {
                    console.log("[NT4] Ignoring text message, JSON parsing did not find all required fields.");
                    return;
                }

                var method = msg["method"];
                var params = msg["params"];

                if (typeof method !== 'string') {
                    console.log("[NT4] Ignoring text message, JSON parsing found \"method\", but it wasn't a string.");
                    return;
                }

                if (typeof params !== 'object') {
                    console.log("[NT4] Ignoring text message, JSON parsing found \"params\", but it wasn't an object.");
                    return;
                }

                // Message validates reasonably, switch based on supported methods
                if (method === "announce") {

                    //Check to see if we already knew about this topic. If not, make a new object.

                    var newTopic = null;
                    for (const topic of this.clientPublishedTopics.values()) {
                        if (params.name === topic.name) {
                            newTopic = topic; //Existing topic, use it.
                        }
                    }

                    // Did not know about the topic. Make a new one.
                    if(newTopic === null){
                        newTopic = new NT4_Topic();
                    }

                    newTopic.name = params.name;
                    newTopic.id = params.id;

                    //Strategy - if server sends a pubid use it
                    // otherwise, preserve whatever we had?
                    //TODO - ask peter about this. It smells wrong.
                    if (params.pubid != null) {
                        newTopic.pubuid = params.pubuid;
                    }

                    newTopic.type = params.type;
                    newTopic.properties = params.properties;
                    this.announcedTopics.set(newTopic.id, newTopic);
                    this.onTopicAnnounce(newTopic);
                } else if (method === "unannounce") {
                    var removedTopic = this.announcedTopics.get(params.id);
                    if (!removedTopic) {
                        console.log("[NT4] Ignorining unannounce, topic was not previously announced.");
                        return;
                    }
                    this.announcedTopics.delete(removedTopic.id);
                    this.onTopicUnAnnounce(removedTopic);

                } else if (method === "properties") {
                    //TODO support property changes
                } else {
                    console.log("[NT4] Ignoring text message - unknown method " + method);
                    return;
                }
            }, this);

        } else {
            //MSGPack
            var rxArray = msgpack.deserialize(e.data, { multiple: true });

            rxArray.forEach(function (unpackedData) { //For every value update...
                var topicID = unpackedData[0];
                var timestamp_us = unpackedData[1];
                var typeIdx = unpackedData[2];
                var value = unpackedData[3];

                if (topicID >= 0) {
                    var topic = this.announcedTopics.get(topicID);
                    this.onNewTopicData(topic, timestamp_us, value);
                } else if (topicID === -1) {
                    this.ws_handleReceiveTimestamp(timestamp_us, value);
                } else {
                    console.log("[NT4] Ignoring binary data - invalid topic id " + topicID.toString());
                }
            }, this);

        }
    }

    ws_connect() {

        this.clientIdx = Math.floor(Math.random() * 99999999); //Not great, but using it for now

        var port = 5810; //fallback - unsecured
        var prefix = "ws://";

        this.serverAddr = prefix + this.serverBaseAddr + ":" + port.toString() + "/nt/" + "JSClient_" + this.clientIdx.toString();

        this.ws = new WebSocket(this.serverAddr, "v4.1.networktables.first.wpi.edu");
        this.ws.binaryType = "arraybuffer";
        this.ws.onopen = this.ws_onOpen.bind(this);
        this.ws.onmessage = this.ws_onMessage.bind(this);
        this.ws.onclose = this.ws_onClose.bind(this);
        this.ws.onerror = this.ws_onError.bind(this);

        console.log("[NT4] Connected with idx " + this.clientIdx.toString());
    }



    //////////////////////////////////////////////////////////////
    // General utilties

    getNewSubUID() {
        this.subscription_uid_counter++;
        return this.subscription_uid_counter + this.clientIdx;
    }

    getNewPubUID() {
        this.publish_uid_counter++;
        return this.publish_uid_counter + this.clientIdx;
    }


}
