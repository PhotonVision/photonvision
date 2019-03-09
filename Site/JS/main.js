// let ws = new WebSocket("ws://localhost:8888/websocket");
// ws.onopen = function () {
//         ws.send("hello");
// }
// ws.onmessage = function (ev) {
//     alert(ev.data);
// }

// function changeTab(index) {
//     arr = document.getElementsByClassName("tab");

//     for (let element of arr) {
//         element.style.display = "none";
//     }

//     document.getElementById(index).style.display = "block";
// }

let inputTab = new Vue({
    el: "#input-tab",
    data: {
        sliders: [
            { value: 25 },
            { value: 30 }
        ]
      }
})