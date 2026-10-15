# photon-sc-app

IPK-packaged PhotonVision dashboard for SystemCore / Orange Pi 5. Python serves the static app and performs bounded ICMP scans; browser JavaScript owns NT4 discovery, PhotonVision UI verification, and iframe tabs. Runtime requires no pip/npm installation, cache, or writable application filesystem.

## How to build

```sh
bash build.sh
```

Produces `../build/sc-app/photon-sc-app_<version>.ipk`. The version comes from `git describe --tags --always`; when `photon-sc-app/` has Git changes, the build appends a `YYMMDDHHMMSS` timestamp.

## How to develop

A static single-page app is served by Python's `ThreadingHTTPServer`. The FRC team number is detected automatically — no team entry field exists — and scanning starts on page load. It provides live ping/probe status, a browser-side NT4 connection indicator, and verified PhotonVision iframe tabs. JavaScript owns all candidate and tab state.

The browser verifies each responding address using `GET http://<host>:5800/api/status`. A tab is created only for an HTTP 200 response whose body is `not dead yet`. The iframe URL is fixed to HTTP port 5800 and is never accepted from a discovery payload.

Same-origin endpoints served by the SC app:

| Path | Method | Purpose |
|------|--------|---------|
| `/network_info?team=<number>` | GET | `{configured_team, team_number, subnet, nt_server, local_ips}` — reports the `10.TE.AM.0/24` subnet, default NT server, and local SystemCore addresses. `team` is optional; when omitted the server guesses the team from the request's `Host` header and the device's local `10.TE.AM.X` addresses. |
| `/scan` | POST | `text/event-stream` — body `{"team_number":"1736"}`. Streams `scan_started`, individual `ping`, and `scan_complete` events. |
| `/status` or `/health` | GET | `{service, status, socket_activated, pid, timestamp}` — health check for monitoring. |

Static files under `overlay/usr/local/bin/photon-sc-app/www/` are served for all other paths. The `/` and `/index.html` paths both serve `index.html`. The vendored NT4 client and license live under `www/vendor/nt4/`.

The Python process holds no discovery cache and writes no files. It streams bounded host ping results; browser JavaScript checks `/api/status`, discovers NT topics, and owns the resulting tabs.

## Requirements

### Web UI
No team number is ever entered by the user. The server guesses it from two sources, in order: the HTTP `Host` header of the request (the address the browser used to reach the app) and the device's own interface addresses (via `ip -4 addr`, falling back to hostname resolution). The first address matching the `10.TE.AM.X` pattern determines the team, so browsing via `http://10.17.36.2:9002/` yields team 1736. An explicit team may still be supplied with `?team=<number>` for clients that know it. The team string is padded to at least four decimal digits; all digits before the final two form TE and the final two form AM. Leading zeros are removed when formatting IPv4 octets, so `254` becomes `10.2.54.0/24` and `12345` becomes `10.123.45.0/24`.

The server pings 1-254 with ten concurrent probes and a 0.5-second per-probe deadline. Each result is streamed immediately. JavaScript separately checks the PhotonVision status endpoint and keeps only successful endpoints in its iframe tab list.

The vendored browser client is pinned to `gerth2/NetworkTablesClients` commit `2f8d378ac08d5ca703d590cfb019fc4af062db89`. It speaks NT4.0 read-only subscriptions; WPILib's server is expected to negotiate its 4.0 fallback. The vendor directory includes the required MessagePack file, BSD license, and source provenance. No runtime npm or pip installation is used.

All other paths serve static files from `www/`. The `/` and `/index.html` paths are both mapped to serve `index.html`.

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

The browser-side NT4 client connects to `10.TE.AM.2:5810` and subscribes to `/photonvision/` topics for camera discovery. Hostname verification is performed by the browser via `GET http://<ip>:5800/api/status` (expecting `not dead yet`).

## Roadmap / TODO

- [x] **Migrate discovery ownership to browser.** Python no longer generates the authoritative tab list; JavaScript owns NT connection status, `/api/status` verification, and iframe tab state.
- [x] **Vendor pinned NT4 client.** `gerth2/NetworkTablesClients` commit `2f8d378ac08d5ca703d590cfb019fc4af062db89` under `www/vendor/nt4/`, including MessagePack and upstream license.
- [ ] **Test on a real SystemCore.** Validate the IPK installs, socket activates, discovery finds coprocessors, and the web UI renders correctly.
- [ ] **Test NT 4.0 negotiation against deployed 2027 server.** Verify the pinned 2022 NT4 client can connect and subscribe on SystemCore hardware.
- [ ] **Expose rich coprocessor info in the UI.** Show metadata alongside each tab — hostname, camera count, pipeline name, version, connection quality — without cluttering the tab bar.
