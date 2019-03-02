let ws = new WebSocket("ws://localhost:8888/websocket");
ws.onopen = function () {
        ws.send("hello");
}
ws.onmessage = function (ev) {
    alert(ev.data);
}