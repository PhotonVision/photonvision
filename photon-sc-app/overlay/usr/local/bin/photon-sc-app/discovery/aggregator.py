"""Discovery aggregator - combines multiple strategies into a single list.

This module orchestrates all discovery strategies:
1. mDNS (photonvision.local)
2. Network scanning (10.TE.AM.XX)
3. Port 5800 verification
4. NetworkTables discovery (validates cameras on the roboRIO's NT server)

Results are merged, deduplicated, and returned as a list of tabs
for the dashboard UI.
"""

import logging
from typing import Dict, List, Optional, Set

from .mdns_discovery import discover_mdns
from .network_scan_discovery import discover_network_scan
from .networktables_discovery import discover_networktables
from .port_check_discovery import verify_port_5800

logger = logging.getLogger(__name__)


def discover_all(
    team_number: Optional[int] = None,
    enable_mdns: bool = True,
    enable_network_scan: bool = True,
    enable_port_check: bool = True,
    enable_networktables: bool = True,
) -> List[Dict[str, str]]:
    """Discover all PhotonVision dashboards using multiple strategies.

    Runs all enabled discovery strategies and combines results:
    - mDNS resolution of photonvision.local
    - Network scanning of 10.TE.AM.0/24
    - Verification of port 5800 accessibility
    - NetworkTables discovery (connects to roboRIO NT server to find
      coprocessors publishing camera data)

    NT-discovered hostnames are cross-referenced with IPs from other
    strategies. Duplicates are removed and results are sorted.

    Args:
        team_number: FRC team number for network scanning and NT server
                     discovery (e.g., 5123).
        enable_mdns: Whether to attempt mDNS discovery.
        enable_network_scan: Whether to scan the team network.
        enable_port_check: Whether to verify port 5800 accessibility.
        enable_networktables: Whether to use NetworkTables discovery.

    Returns:
        List of dicts with "title" and "url" keys, one per discovered dashboard.
        Empty list if no dashboards found.
    """
    all_candidates: Set[str] = set()

    # Strategy 0: mDNS discovery
    if enable_mdns:
        try:
            mdns_results = discover_mdns()
            if mdns_results:
                logger.info("mDNS discovery found %d candidates", len(mdns_results))
            all_candidates.update(mdns_results)
        except Exception as e:
            logger.error("mDNS discovery error: %s", e)

    # Strategy 1: Network scanning (10.TE.AM.XX)
    if enable_network_scan:
        try:
            network_results = discover_network_scan(team_number)
            if network_results:
                logger.info("Network scan found %d candidates", len(network_results))
            all_candidates.update(network_results)
        except Exception as e:
            logger.error("Network scan error: %s", e)

    # Strategy 2: Port verification
    # Only check port 5800 on candidates found by mDNS and network scan
    if enable_port_check and all_candidates:
        try:
            verified = verify_port_5800(all_candidates)
            if verified:
                logger.info("Port verification found %d active dashboards", len(verified))
            all_candidates = verified
        except Exception as e:
            logger.error("Port verification error: %s", e)

    # Strategy 3: NetworkTables discovery
    # Connects to the roboRIO NT server, enumerates PV cameras, and
    # resolves coprocessor hostnames to IPs via mDNS
    if enable_networktables:
        try:
            nt_results = discover_networktables(team_number=team_number)
            if nt_results:
                logger.info("NT discovery found %d dashboards", len(nt_results))
            all_candidates.update(nt_results)
        except Exception as e:
            logger.error("NT discovery error: %s", e)

    # Convert IPs to tab entries and sort for consistency
    if not all_candidates:
        logger.debug("No PhotonVision dashboards discovered")
        return []

    sorted_ips = sorted(all_candidates, key=lambda x: tuple(map(int, x.split("."))))

    tabs: List[Dict[str, str]] = [
        {"title": f"{ip}", "url": f"http://{ip}:5800"}
        for ip in sorted_ips
    ]

    logger.info("Discovery complete: %d dashboards found", len(tabs))
    return tabs
