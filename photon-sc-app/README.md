# photon-sc-app

IPK-packaged dashboard aggregator for PhotonVision. Runs on a coprocessor (SystemCore / Orange Pi 5) and provides a tabbed web UI that auto-discovers all PhotonVision camera coprocessors on the FRC team network, loading each into its own iframe.

## How to build

```sh
bash build.sh
```

Produces `photon-sc-app_1.0.0.ipk` in the current directory.

## How to develop

```sh
bash run_local.sh
```

Opens a dev server at `http://127.0.0.1:8080`. The web UI is served from the `www/` directory — refresh the browser to see changes. Pass `--team 5123` to enable FRC network scanning, or `--help` to see all options.

## File layout

| Path | Purpose |
|------|---------|
| `build.sh` | Assembles the IPK: copies overlay/ into a directory, merges control/ files, tars both, wraps with `ar` |
| `run_local.sh` / `run_local.bat` | Launches `photon_sc_app.py --local` for development without systemd |
| `control/` | OPKG metadata (`control`) and lifecycle scripts (`postinst`, `prerm`, `postrm`) |
| `overlay/` | Filesystem tree installed verbatim to the target coprocessor root |
| `overlay/usr/local/bin/photon-sc-app/photon_sc_app.py` | Python HTTP server entrypoint |
| `overlay/usr/local/bin/photon-sc-app/discovery/` | Network discovery strategies (one module per strategy) |
| `overlay/usr/local/bin/photon-sc-app/www/` | Web frontend (HTML, CSS, JS) |
| `overlay/etc/systemd/system/` | Systemd socket + service units for production |
| `overlay/usr/share/photon-sc-app.png` | App icon for SystemCore launcher |

## Requirements

### Web UI

A dark-themed single-page app served from a built-in Python HTTP `ThreadingHTTPServer`. No build step, no framework — just static HTML/CSS/JS served from `www/`. The UI must:

- Display a horizontal tab bar. Each tab is a `<button>` labeled with the discovered coprocessor's IP address.
- Show a full-height `<iframe>` that loads the selected coprocessor's PhotonVision dashboard (port 5800). The iframe sandbox attribute is `allow-scripts allow-same-origin allow-forms` — the iframe lives on a separate origin (the coprocessor IP), so same-origin restrictions do not block cross-origin content.
- Include a "Update Clients" button that re-fetches the tab list from the backend. The page load also triggers the initial fetch.
- Display an empty state message ("No clients found") when no coprocessors are discovered, so the user knows the discovery is running but hasn't found anything yet.

A dark palette (`#101820` background, `#1a67d0` accent blue, tabs with active bottom-border highlight) keeps visual consistency with the PhotonVision brand. The tab bar sits above the iframe, separated from the content area by a thin border, so the user always knows which coprocessor they're viewing.

### Front-end / back-end communication

The web UI communicates with *this* Python server (not with the remote PhotonVision coprocessor directly) via two HTTP endpoints served on the same host:port:

| Endpoint | Method | Response | Purpose |
|----------|--------|----------|---------|
| `/tabs` | GET | `{"tabs": [{"title": "10.51.23.42", "url": "http://10.51.23.42:5800"}, ...]}` | Returns the cached list of discovered coprocessors. The frontend calls this on page load and when the user clicks "Update Clients". |
| `/status` or `/health` | GET | `{"service": "...", "status": "running", "socket_activated": true/false, "pid": ..., "timestamp": "..."}` | Health check for system monitoring / orchestration. |

All other paths serve static files from `www/`. The `/` and `/index.html` paths are both mapped to serve `index.html`.

This design means the frontend never needs to know about network discovery, caching intervals, or the remote coprocessor's availability. The backend handles all of that and serves a single, simple JSON contract.

### Backend discovery strategies

The backend finds PhotonVision coprocessors by running four parallel strategies in a background thread (driven by `DiscoveryCache`). Results are cached and served instantly via `/tabs` without blocking.

**Why four strategies:** The FRC pit environment is unpredictable. A coprocessor may be reachable via mDNS, or only by its static IP on the team subnet, or only via NetworkTables if it's publishing robot data. Each strategy covers a different failure mode:

1. **mDNS** — resolves `photonvision.local` via `socket.gethostbyname()`. Requires avahi-daemon or similar running on the coprocessor. Fast and zero-config, but not all networks have working mDNS.

2. **Network scan** — pings every host on the FRC team subnet (`10.TE.AM.0/24`). Derives the subnet from the `--team` argument (e.g., team 5123 → `10.51.23.1-254`). Slow (254 pings) but finds any coprocessor on the wired team network regardless of hostname resolution.

3. **Port 5800 check** — TCP-connects to port 5800 on candidate IPs (from mDNS and network scan results). Filters out non-PhotonVision hosts. The 2-second timeout per connection avoids hanging on dead IPs.

4. **NetworkTables** — queries the `/photonvision` table via ntcore for IP addresses published by PhotonVision instances. Requires ntcore installed. Useful as a supplement when other strategies miss a coprocessor that is publishing NT data.

**Fast vs. slow intervals:** mDNS and port checks run every 10 seconds (fast). Network scan and NetworkTables run every 60 seconds (slow). This avoids flooding the team network with pings while still providing near-real-time tab updates for strategies that are cheap.

**Caching:** `DiscoveryCache` stores results and serves them synchronously to `/tabs` requests. The background thread never blocks the HTTP server. If discovery fails, the last known good tab list remains visible rather than disappearing — the empty state only shows when no coprocessors have ever been found since the service started.

### Production deployment

Production uses systemd socket activation (port 9002). Only the `.socket` unit is enabled — the service starts on-demand when a connection arrives, then stays running. The `postinst` script in the IPK handles enabling and starting the socket unit automatically.

## NetworkTables structure (reference)

Confirmed by inspecting the actual PV source code (`NetworkTablesManager.java`, `NTTopicSet.java`, `NTDataPublisher.java`, `PhotonCamera.java`).

Each PV coprocessor publishes camera data under `/photonvision/{CAMERA_NAME}/`. A subtable is a valid PV camera **if and only if** it has a `rawBytes` entry. This is the same detection logic `PhotonCamera.java` uses on the robot side.

**Key topics per camera:**

| Topic | Type | Purpose |
|-------|------|---------|
| `rawBytes` | raw | Primary pipeline result packet (type `"photonstruct:PhotonPipelineResult:{UUID}"`) |
| `heartbeat` | int64 | Increments each frame — used to detect connectivity |
| `pipelineIndexState` / `pipelineIndexRequest` | int64 | Active pipeline index (bidirectional) |
| `driverMode` / `driverModeRequest` | bool | Driver mode toggle (bidirectional) |
| `latencyMillis` | double | Pipeline latency |
| `fps` | double | Frames per second |
| `hasTarget` | bool | Whether the current frame has a target |
| `targetPitch`, `targetYaw`, `targetArea`, `targetSkew` | double | Best target scalar values |
| `cameraIntrinsics` | double[] (9) | Camera calibration matrix |
| `cameraDistortion` | double[] (up to 8) | Camera distortion coefficients |

**Coprocessor metadata** (published once at startup):

| Topic | Type |
|-------|------|
| `/photonvision/coprocessors/{MAC}/hostname` | string |
| `/photonvision/coprocessors/{MAC}/cameraNames` | string[] |
| `/photonvision/version` | string |
| `/photonvision/buildDate` | string |

**System metrics** (published each cycle):

| Topic | Type |
|-------|------|
| `/photonvision//metrics/{HOSTNAME}` | protobuf `DeviceMetrics` (includes `ipAddress`) |

NT discovery connects to the roboRIO's NT server (port 1735, identified by team number), enumerates subtables under `/photonvision/`, confirms each by checking for `rawBytes`, reads coprocessor hostnames from the metadata, and resolves them to IPs via mDNS.

## Roadmap / TODO

- [x] **Implement proper NetworkTables table layout.** Rewrote `networktables_discovery.py` to connect to the roboRIO NT server, enumerate subtables under `/photonvision/`, validate cameras by checking for `rawBytes` (same logic as `PhotonCamera.java`), read coprocessor hostnames from `/photonvision/coprocessors/{MAC}/hostname`, and resolve via mDNS.
- [ ] **Test on a real SystemCore.** Validate the IPK installs, socket activates, discovery finds coprocessors, and the web UI renders correctly.
- [ ] **Prefer mDNS hostnames over raw IPs.** When mDNS resolves a coprocessor, use its hostname (e.g., `http://photonvision.local:5800`) in the tab URL instead of its IP. Fall back to IP when mDNS is unavailable. This keeps tabs working if the coprocessor's DHCP lease changes.
- [ ] **Expose rich coprocessor info in the UI.** Show metadata alongside each tab — hostname, camera count, pipeline name, version, connection quality — without cluttering the tab bar. Options: tooltip on tab hover, a subtitle row below the tab bar, or a collapsible sidebar.
- [ ] **Test error conditions.** Validate behavior when: ping sweep succeeds but port 5800 is unreachable; NT is unavailable; a previously discovered coprocessor goes offline; mDNS resolves intermittently; the team network has non-PV hosts on port 5800. Ensure the UI degrades gracefully (shows empty state, re-attempts discovery, doesn't cache stale entries that fail to load).
- [ ] **Check "likely a PV coprocessor" NT client IPs.** Cross-reference IPs from NT `/photonvision` entries with mDNS and port-scan results. Build a confidence score so the UI can prefer verified PV instances over speculative ones.
