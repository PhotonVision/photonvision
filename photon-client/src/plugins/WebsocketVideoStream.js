var image = undefined;
const wsCamerasURL = "ws://" + window.location.host + "/websocket_cameras";


function initVideoStream(port, drawDiv) {

    image = document.getElementById(drawDiv);

    var webSocket = new WebSocket(wsCamerasURL);

    webSocket.onopen = () => {
        webSocket.send(JSON.stringify({"cmd": "subscribe", "port":1181}));
    };

    webSocket.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        var images = msg["frameData"];

        for(var img of images){
            if(img['port'] == port){
                image.setAttribute(
                    'src', `data:image/jpeg;base64,${img['data']}`
                );
            }
        }


    }

}


export default {initVideoStream}
