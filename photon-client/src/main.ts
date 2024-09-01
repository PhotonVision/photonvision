import "unfonts.css";
import { registerPlugins } from "@/plugins";
import App from "./App.vue";
import { createApp } from "vue";
import axios from "axios";

type PhotonClientRuntimeMode = "production" | "development" | "local-network-development";
const runtimeMode: PhotonClientRuntimeMode = process.env.NODE_ENV as PhotonClientRuntimeMode;

let backendHost: string;
let backendHostname: string;
switch (runtimeMode as PhotonClientRuntimeMode) {
  case "development":
    backendHost = `${location.hostname}:5800`;
    backendHostname = location.hostname;
    break;
  case "local-network-development":
    backendHost = "photonvision.local:5800";
    backendHostname = "photonvision.local";
    break;
  case "production":
    backendHost = location.host;
    backendHostname = location.hostname;
    break;
}

axios.defaults.baseURL = `http://${backendHost}/api`;

const app = createApp(App);

registerPlugins(app);

app.provide("backendHost", backendHostname);
app.provide("backendHostname", backendHostname);
app.mount("#app");
