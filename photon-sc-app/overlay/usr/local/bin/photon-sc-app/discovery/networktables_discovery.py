"""NetworkTables discovery strategy.

Connects to a NetworkTables server (typically on the roboRIO) and enumerates
PhotonVision coprocessors by looking for cameras publishing under /photonvision/.

Uses the same camera-detection logic as PhotonCamera.java / photonCamera.py:
a subtable under /photonvision/ is a valid PV camera if it has a "rawBytes" entry.

Discovered hostnames are returned for mDNS resolution. The aggregator
cross-references them with port-scan and network-scan results to build
the final IP list.
"""

import logging
import socket
import time
from typing import Any, Optional, Set

logger = logging.getLogger(__name__)

try:
    from ntcore import NetworkTableInstance
    NTCORE_AVAILABLE = True
except ImportError:
    NTCORE_AVAILABLE = False
    logger.debug("ntcore not available, NetworkTables discovery disabled")

NT_SERVER_PORT = 1735
NT_CLIENT_IDENTITY = "photon-sc-app"
NT_CONNECT_TIMEOUT = 2.0
NT_SCAN_TIMEOUT = 3.0


def discover_networktables(team_number: Optional[int] = None, timeout: float = NT_CONNECT_TIMEOUT + NT_SCAN_TIMEOUT) -> Set[str]:
    """Discover PhotonVision coprocessors via NetworkTables.

    Connects to the NT server (roboRIO, identified by team number) and
    enumerates cameras under /photonvision/. A camera is valid if its
    subtable contains a "rawBytes" entry. Returns hostnames for each
    unique coprocessor found.

    Args:
        team_number: FRC team number. Used to auto-discover the NT server
                     address (10.TE.AM.2). If None, discovery is skipped
                     since there is no server to connect to.
        timeout: Maximum total time in seconds for the NT discovery attempt.

    Returns:
        Set of hostnames (e.g. "photonvision", "photonvision-2") that
        were confirmed to be publishing PV camera data.
        Empty set if ntcore is unavailable, no team is given, or no
        coprocessors are found.
    """
    if not NTCORE_AVAILABLE:
        return set()

    if team_number is None:
        return set()

    instance = NetworkTableInstance.create()
    instance.setServerTeam(team_number, NT_SERVER_PORT)
    instance.startClient(NT_CLIENT_IDENTITY)

    deadline = time.monotonic() + timeout

    try:
        connect_timeout = min(NT_CONNECT_TIMEOUT, timeout)
        if not _wait_for_connection(instance, connect_timeout):
            logger.debug("NT server not reachable for team %d", team_number)
            return set()

        remaining = deadline - time.monotonic()
        if remaining <= 0:
            return set()

        return _discover_from_nt(instance, timeout=remaining)

    finally:
        try:
            instance.stopClient()
        except Exception:
            pass


def _wait_for_connection(instance: "NetworkTableInstance", timeout: float = NT_CONNECT_TIMEOUT) -> bool:
    """Wait up to `timeout` seconds for the NT client to connect to a server.

    Returns True if connected, False if timeout elapsed.
    """
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if instance.isConnected():
            return True
        time.sleep(0.1)
    return False


def _discover_from_nt(instance: "NetworkTableInstance", timeout: float = NT_SCAN_TIMEOUT) -> Set[str]:
    """Enumerate PV coprocessors from a connected NT instance.

    Strategy:
    1. Wait briefly for topic data to arrive
    2. List subtables under /photonvision/
    3. Each subtable with a "rawBytes" entry is a valid PV camera
    4. Group cameras by coprocessor
    5. Resolve coprocessor hostnames from /photonvision/coprocessors/ subtables
    6. Try mDNS to convert hostnames to IPs

    Args:
        instance: Connected NT instance.
        timeout: Maximum time to wait for topic data.

    Returns a set of IP addresses (as strings).
    """
    wait = min(NT_SCAN_TIMEOUT, timeout)
    if wait > 0:
        time.sleep(wait)

    root_table = instance.getTable("/photonvision")
    if root_table is None:
        return set()

    subtable_names = root_table.getSubTables()
    if not subtable_names:
        logger.debug("No subtables under /photonvision/")
        return set()

    # Filter subtables: a PV camera subtable has a "rawBytes" entry
    camera_names: Set[str] = set()
    for name in subtable_names:
        if _is_camera_table(root_table, name):
            camera_names.add(name)

    if not camera_names:
        logger.debug("No PV camera subtables found under /photonvision/")
        return set()

    logger.info("Found %d PV camera(s): %s", len(camera_names), ", ".join(sorted(camera_names)))

    # Try to read coprocessor metadata to get hostnames
    coprocessor_hostnames = _get_coprocessor_hostnames(root_table)

    found: Set[str] = set()

    # For each hostname from coprocessor metadata, try mDNS resolution
    for hostname in coprocessor_hostnames:
        ip = _resolve_mdns(hostname)
        if ip:
            found.add(ip)
            logger.info("NT+mDNS resolved %s -> %s", hostname, ip)
        else:
            logger.debug("Could not resolve coprocessor hostname: %s", hostname)

    return found


def _is_camera_table(root_table: Any, name: str) -> bool:
    """Check if a subtable looks like a PV camera (has a rawBytes entry)."""
    try:
        sub = root_table.getSubTable(name)
        if sub is None:
            return False
        return sub.getEntry("rawBytes").exists()
    except Exception:
        return False


def _get_coprocessor_hostnames(root_table: Any) -> Set[str]:
    """Read hostnames from /photonvision/coprocessors/{MAC}/hostname.

    Returns a set of hostname strings (e.g. {"photonvision", "photonvision-2"}).
    """
    hostnames: Set[str] = set()

    try:
        coprocessors_table = root_table.getSubTable("coprocessors")
        if coprocessors_table is None:
            return hostnames

        macs = coprocessors_table.getSubTables()
        for mac in macs:
            try:
                coprocessor_table = coprocessors_table.getSubTable(mac)
                if coprocessor_table is None:
                    continue
                hostname = coprocessor_table.getString("hostname", "")
                if hostname:
                    hostnames.add(hostname)
                    logger.debug("Coprocessor %s hostname: %s", mac, hostname)
            except Exception as e:
                logger.debug("Error reading coprocessor %s: %s", mac, e)

        if hostnames:
            logger.info("Found %d coprocessor(s) via NT metadata: %s", len(hostnames), ", ".join(hostnames))

    except Exception as e:
        logger.debug("Error reading coprocessors table: %s", e)

    return hostnames


def _resolve_mdns(hostname: str) -> Optional[str]:
    """Try to resolve a hostname to an IPv4 address via mDNS.

    Tries both the bare hostname and with .local suffix.
    Returns the IP string or None.
    """
    candidates = [hostname]
    if not hostname.endswith(".local"):
        candidates.append(f"{hostname}.local")

    for name in candidates:
        try:
            result = socket.gethostbyname(name)
            logger.debug("mDNS resolved %s -> %s", name, result)
            return result
        except socket.gaierror:
            continue

    return None
