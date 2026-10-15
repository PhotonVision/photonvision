import {NT4_Client} from "./vendor/nt4/nt4.js";

const tabList = document.getElementById("tabList");
const tabFrame = document.getElementById("tabFrame");
const discoveryPanel = document.getElementById("discoveryPanel");
const candidates = new Map();
const verifiedTabs = new Map();
const ntCameraTopics = new Set();
const ntHostnameSubscriptions = new Set();
const verificationQueue = [];
const internalTab = {id: "discovery", title: "Discovery", url: null};
const MAX_CONCURRENT_VERIFICATIONS = 8;

let activeTabId = internalTab.id;
let currentScan = null;
let activeVerifications = 0;
let scanCounter = 0;
let ntClient = null;
let ntServer = null;
let networkInfo = null;

function setConnectionStatus(scConnected, ntStatus) {
  document.getElementById("scConnection").textContent = scConnected ? "Connected" : "Disconnected";
  document.getElementById("ntConnection").textContent = ntStatus;
}

function renderTabs() {
  const availableTabs = [internalTab, ...verifiedTabs.values()];
  if (!availableTabs.some(tab => tab.id === activeTabId)) {
    activeTabId = internalTab.id;
  }

  tabList.replaceChildren();
  availableTabs.forEach(tab => {
    const button = document.createElement("button");
    button.className = "tab-button";
    button.textContent = tab.title;
    button.classList.toggle("active", tab.id === activeTabId);
    button.addEventListener("click", () => activateTab(tab.id));
    tabList.appendChild(button);
  });
  activateTab(activeTabId, false);
}

function activateTab(id, updateButtons = true) {
  const availableTabs = [internalTab, ...verifiedTabs.values()];
  const tab = availableTabs.find(item => item.id === id) || internalTab;
  activeTabId = tab.id;

  if (tab.id === internalTab.id) {
    tabFrame.style.display = "none";
    discoveryPanel.style.display = "flex";
  } else {
    discoveryPanel.style.display = "none";
    tabFrame.style.display = "block";
    if (tabFrame.src !== tab.url) {
      tabFrame.src = tab.url;
    }
  }

  if (updateButtons) {
    tabList.querySelectorAll(".tab-button").forEach(button => {
      button.classList.toggle("active", button.textContent === tab.title);
    });
  }
}

function renderCandidates(message = "No responding devices yet") {
  const body = document.getElementById("discoveryTableBody");
  body.replaceChildren();

  const rows = [...candidates.values()].sort((left, right) => left.title.localeCompare(right.title));
  if (rows.length === 0) {
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 4;
    cell.className = "empty-state";
    cell.textContent = message;
    row.appendChild(cell);
    body.appendChild(row);
    return;
  }

  rows.forEach(candidate => {
    const row = document.createElement("tr");
    [candidate.title, candidate.ping, candidate.ui, candidate.ntSource].forEach(value => {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.appendChild(cell);
    });
    body.appendChild(row);
  });
}

function renderNetworkInfo(info) {
  const details = [];
  if (info.subnet) details.push(info.subnet);
  if (info.nt_server) details.push(`NT ${info.nt_server}:5810`);
  if (Array.isArray(info.local_ips) && info.local_ips.length) {
    details.push(`SystemCore ${info.local_ips.join(", ")}`);
  }
  document.getElementById("networkLine").textContent = details.join("  |  ");
}

async function getNetworkInfo(teamNumber) {
  const query = teamNumber ? `?team=${encodeURIComponent(teamNumber)}` : "";
  const response = await fetch(`/network_info${query}`, {cache: "no-store"});
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || `Network info failed (${response.status})`);
  }
  networkInfo = data;
  renderNetworkInfo(data);
  return data;
}

function stopNtClient() {
  if (ntClient) {
    ntClient.stop();
    ntClient = null;
  }
  ntServer = null;
  ntCameraTopics.clear();
  ntHostnameSubscriptions.clear();
  document.getElementById("ntCameraCount").textContent = "0";
  setConnectionStatus(true, "Disconnected");
}

function connectNtClient(server) {
  if (ntClient && ntServer === server) return;
  stopNtClient();
  ntServer = server;
  setConnectionStatus(true, "Connecting");

  ntClient = new NT4_Client(
    server,
    topic => {
      if (/^\/photonvision\/[^/]+\/rawBytes$/.test(topic.name)) {
        ntCameraTopics.add(topic.name);
        document.getElementById("ntCameraCount").textContent = String(ntCameraTopics.size);
      }

      if (/^\/photonvision\/coprocessors\/[^/]+\/hostname$/.test(topic.name)
        && !ntHostnameSubscriptions.has(topic.name)) {
        ntHostnameSubscriptions.add(topic.name);
        ntClient.subscribePeriodic([topic.name], 1.0);
      }
    },
    topic => {
      ntCameraTopics.delete(topic.name);
      document.getElementById("ntCameraCount").textContent = String(ntCameraTopics.size);
    },
    (topic, _timestamp, value) => {
      if (/^\/photonvision\/coprocessors\/[^/]+\/hostname$/.test(topic?.name)
        && typeof value === "string") {
        const hostname = value.trim();
        if (hostname) queueVerification(hostname, hostname, "NT", null);
      }
    },
    () => setConnectionStatus(true, "Connected"),
    () => setConnectionStatus(true, "Disconnected")
  );
  ntClient.subscribeTopicNames(["/photonvision/"]);
}

function normalizeHost(host) {
  const normalized = host.trim().replace(/\.$/, "");
  if (!/^[a-zA-Z0-9.-]+$/.test(normalized) || normalized.includes("..")) {
    return null;
  }
  return normalized;
}

function queueVerification(host, title, source, scan) {
  return new Promise(resolve => {
    verificationQueue.push({host, title, source, scan, resolve});
    processVerificationQueue();
  });
}

function processVerificationQueue() {
  while (activeVerifications < MAX_CONCURRENT_VERIFICATIONS && verificationQueue.length) {
    const task = verificationQueue.shift();
    activeVerifications += 1;
    verifyPhotonUi(task.host).then(verifiedHost => {
      if (!task.scan || currentScan === task.scan) {
        const candidateId = task.host.toLowerCase();
        const candidate = candidates.get(candidateId) || {
          id: candidateId,
          title: task.title,
          ping: task.source === "Ping" ? "Yes" : "-",
          ui: "Checking",
          ntSource: task.source === "NT" ? "Yes" : "No",
          sources: new Set()
        };
        candidate.sources.add(task.source);
        candidate.ping = candidate.sources.has("Ping") ? "Yes" : "-";
        candidate.ntSource = candidate.sources.has("NT") ? "Yes" : "No";
        candidate.ui = verifiedHost ? "Yes" : "No";
        candidates.set(candidateId, candidate);

        if (verifiedHost) {
          verifiedTabs.set(candidateId, {
            id: candidateId,
            title: task.title,
            url: `http://${verifiedHost}:5800`,
            sources: candidate.sources
          });
        } else {
          verifiedTabs.delete(candidateId);
        }
        renderCandidates();
        renderTabs();
      }
    }).catch(error => {
      console.error(`PV UI verification failed for ${task.host}`, error);
    }).finally(() => {
      activeVerifications -= 1;
      task.resolve();
      processVerificationQueue();
    });
  }
}

async function verifyPhotonUi(host) {
  const normalizedHost = normalizeHost(host);
  if (!normalizedHost) return null;

  const hosts = [normalizedHost];
  if (!/^\d{1,3}(\.\d{1,3}){3}$/.test(normalizedHost) && !normalizedHost.endsWith(".local")) {
    hosts.push(`${normalizedHost}.local`);
  }

  for (const targetHost of hosts) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 1800);
    try {
      const response = await fetch(`http://${targetHost}:5800/api/status`, {
        cache: "no-store",
        mode: "cors",
        signal: controller.signal
      });
      if (response.ok && (await response.text()).trim() === "not dead yet") {
        return targetHost;
      }
    } catch (error) {
      if (error.name !== "AbortError" && !(error instanceof TypeError)) throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
  return null;
}

async function handleScanEvent(scan, eventName, payload) {
  if (eventName === "scan_started") {
    scan.id = payload.scan_id;
    document.getElementById("discoveryLine").textContent = `Scanning ${payload.subnet}...`;
    return;
  }

  if (eventName === "ping") {
    scan.completed = payload.completed;
    document.getElementById("discoveryLine").textContent =
      `Scanning ${payload.subnet || scan.subnet} · ${payload.completed}/${payload.total}`;
    if (payload.responded) {
      const ip = payload.ip;
      scan.responding.add(ip);
      const id = ip.toLowerCase();
      candidates.set(id, {id, title: ip, ping: "Yes", ui: "Checking", ntSource: "No", sources: new Set(["Ping"])});
      renderCandidates();
      const verification = queueVerification(ip, ip, "Ping", scan);
      scan.pending.push(verification);
    }
    return;
  }

  if (eventName === "scan_complete") {
    await Promise.all(scan.pending);
    for (const [id, tab] of verifiedTabs) {
      if (tab.sources.has("Ping") && !scan.responding.has(id)) {
        const candidate = candidates.get(id);
        if (!candidate?.sources.has("NT")) verifiedTabs.delete(id);
      }
    }
    document.getElementById("discoveryLine").textContent =
      `Scan complete · ${payload.count} responding · ${verifiedTabs.size} PhotonVision UIs`;
    renderCandidates();
    renderTabs();
    scan.complete = true;
    return;
  }

  if (eventName === "scan_error") {
    throw new Error(payload.error || "Network scan failed");
  }
}

async function consumeScanStream(response, scan) {
  if (!response.body) throw new Error("Streaming responses are not supported by this browser");
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  async function processBlock(block) {
    let eventName = "message";
    const dataLines = [];
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith("event:")) eventName = line.slice(6).trim();
      else if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
    }
    if (dataLines.length) {
      await handleScanEvent(scan, eventName, JSON.parse(dataLines.join("\n")));
    }
  }

  while (true) {
    const {value, done} = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), {stream: !done});
    let delimiter;
    while ((delimiter = buffer.indexOf("\n\n")) !== -1) {
      const block = buffer.slice(0, delimiter).replace(/\r$/, "");
      buffer = buffer.slice(delimiter + 2);
      await processBlock(block);
    }
    if (done) break;
  }
  if (buffer.trim()) await processBlock(buffer);
  if (!scan.complete) throw new Error("Scan stream ended before completion");
}

async function runScan(teamNumber) {
  const scan = {id: null, teamNumber, subnet: "", completed: 0, responding: new Set(), pending: [], complete: false};
  currentScan = scan;
  candidates.clear();
  renderCandidates("Waiting for the first ping responses...");
  document.getElementById("discoveryLine").textContent = "Preparing scan...";

  try {
    const info = await getNetworkInfo(teamNumber);
    scan.subnet = info.subnet;
    connectNtClient(info.nt_server);

    const response = await fetch("/scan", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({team_number: teamNumber})
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || `Scan request failed (${response.status})`);
    }
    await consumeScanStream(response, scan);
  } catch (error) {
    console.error("Discovery scan failed", error);
    document.getElementById("discoveryLine").textContent = `Scan failed: ${error.message}`;
    if (scan.id === null) renderCandidates("Scan could not start.");
  }
}

async function initialize() {
  renderTabs();
  setConnectionStatus(false, "Disconnected");
  document.getElementById("ntCameraCount").textContent = "0";

  try {
    const response = await fetch("/status", {cache: "no-store"});
    setConnectionStatus(response.ok, "Disconnected");
    if (response.ok) {
      const status = await response.json();
      document.getElementById("scVersion").textContent = status.version || "unknown";
    }
  } catch {
    setConnectionStatus(false, "Disconnected");
  }

  try {
    const info = await getNetworkInfo();
    if (info.team_number) {
      await runScan(info.team_number);
    }
  } catch (error) {
    document.getElementById("discoveryLine").textContent = `Network info failed: ${error.message}`;
  }
}

window.addEventListener("load", initialize);
