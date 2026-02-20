/////////////////////////////////////////////////////////////////////////
// SignalDAQ - wrapper around NT4 to specifically extract signal information
// and allow clients to request one or more signals
//
// Mirroring (I assume) NT4 architecture, it's heavily callback driven
/////////////////////////////////////////////////////////////////////////

import { NT4_Client } from "./nt4.js";

export class SignalDAQNT4 {


    constructor(onSignalAnnounce_in,   //Gets called when server announces enough topics to form a new signal
                onSignalUnAnnounce_in, //Gets called when server unannounces any part of a signal
                onNewSampleData_in,    //Gets called when any new data is available
                onConnect_in,          //Gets called once client completes initial handshake with server
                onDisconnect_in,        //Gets called once client detects server has disconnected
                statusTextCallback_in) {
        this.onSignalAnnounce = onSignalAnnounce_in;
        this.onSignalUnAnnounce = onSignalUnAnnounce_in;
        this.onNewSampleData = onNewSampleData_in;
        this.onConnect = onConnect_in;
        this.onDisconnect = onDisconnect_in;
        this.statusTextCallback = statusTextCallback_in;

        this.daqSignalList = new Set(); //start assuming no signals.

        this.daqRunning = false;

        this.rxCount = 0;

        this.timeOffset = 0;

        this.nt4Client = new NT4_Client(window.location.hostname, 
                                        this.topicAnnounceHandler.bind(this), 
                                        this.topicUnannounceHandler.bind(this),
                                        this.valueUpdateHandler.bind(this),
                                        this.localOnConnect.bind(this),
                                        this.onDisconnect.bind(this)
                                        );

        this.statusTextCallback("Starting connection...");
        this.nt4Client.ws_connect();
        this.statusTextCallback("NT4 Connected.");
    }

    localOnConnect() {
        this.nt4Client.subscribeTopicNames(["/SmartDashboard"]);
        this.onConnect();
    }

    topicAnnounceHandler( newTopic ) {
        //If a signal units topic is announced, request what those units value actually is.
        var sigName = newTopic.name;
        var sigUnits = "";    
        if(newTopic.properties.units){
            sigUnits = newTopic.properties.units;
        }
        this.onSignalAnnounce(sigName, sigUnits); //Announce signal when we know the value of its units
    }

    topicUnannounceHandler( removedTopic ) {
        this.onSignalUnAnnounce(removedTopic.name);
    }

    valueUpdateHandler(topic, timestamp, value){
        // Got a new sample
        var sigName = topic.name;
        this.onNewSampleData(sigName, timestamp - this.timeOffset, value);
        if(this.daqRunning){
            this.rxCount++;
        }
        this.updateStatusText();
    }

    //Request a signal get added to the DAQ
    addSignal(signalNameIn){
        this.daqSignalList.add(signalNameIn);
    }

    //Call to remove a signal from the DAQ
    removeSignal(signalNameIn){
        this.daqSignalList.delete(signalNameIn);
    }

    clearSignalList(){
        this.daqSignalList.clear();
    }

    //Request RIO start sending periodic updates with data values
    startDAQ(){
        this.daqRunning = true;
        this.daqSignalList.forEach(sigName => {
            this.nt4Client.subscribeAllSamples([sigName]);
        });
        this.rxCount = 0;
        this.timeOffset = this.nt4Client.getServerTime_us();
        this.updateStatusText();
    }

    //Request RIO stop sending periodic updates
    stopDAQ(){
        this.nt4Client.clearAllSubscriptions();
        this.daqRunning = false;
        this.updateStatusText();
    }

    updateStatusText(){
        var text = "";
        if(this.daqRunning){
            text += "DAQ Running";
        } else {
            text += "DAQ Stopped";
        }
        text += " RX Count: " + this.rxCount.toString();
        this.statusTextCallback(text);
    }


}