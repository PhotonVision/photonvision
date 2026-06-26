"""Discovery aggregator - combines multiple strategies into a single list.

This module orchestrates all discovery strategies:
1. mDNS (photonvision.local)
2. Network scanning (10.TE.AM.XX)
3. Port 5800 verification
4. NetworkTables discovery

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
    ntables_server: Optional[str] = None,
) -> List[Dict[str, str]]:
    """Discover all PhotonVision dashboards using multiple strategies.

    Runs all enabled discovery strategies in parallel and combines results:
    - mDNS resolution of photonvision.local
    - Network scanning of 10.TE.AM.0/24
    - Verification of port 5800 accessibility
    - NetworkTables discovery

    Duplicates are removed and results are sorted for consistent output.

    Args:
        team_number: FRC team number for network scanning (e.g., 5123).
        enable_mdns: Whether to attempt mDNS discovery.
        enable_network_scan: Whether to scan the team network.
        enable_port_check: Whether to verify port 5800 accessibility.
        enable_networktables: Whether to use NetworkTables discovery.
        ntables_server: NetworkTables server address (optional).

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
                logger.info(f"mDNS discovery found {len(mdns_results)} candidates")
            else:
                logger.debug("mDNS discovery found no candidates")
            all_candidates.update(mdns_results)
        except Exception as e:
            logger.error(f"mDNS discovery error: {e}")

    # Strategy 1: Network scanning (10.TE.AM.XX)
    if enable_network_scan:
        try:
            network_results = discover_network_scan(team_number)
            if network_results:
                logger.info(f"Network scan found {len(network_results)} candidates")
            else:
                logger.debug("Network scan found no candidates")
            all_candidates.update(network_results)
        except Exception as e:
            logger.error(f"Network scan error: {e}")

    # Strategy 2 & 3: Port verification (more efficient than scanning for port)
    # Only check port 5800 on candidates found above
    if enable_port_check and all_candidates:
        try:
            verified = verify_port_5800(all_candidates)
            if verified:
                logger.info(f"Port verification found {len(verified)} active dashboards")
            else:
                logger.debug("Port verification found no accessible dashboards")
            # After port check, only keep verified IPs
            all_candidates = verified
        except Exception as e:
            logger.error(f"Port verification error: {e}")

    # Strategy 3: NetworkTables discovery (alternative/supplement)
    if enable_networktables:
        try:
            nt_results = discover_networktables(ntables_server)
            if nt_results:
                logger.info(f"NetworkTables discovery found {len(nt_results)} candidates")
            else:
                logger.debug("NetworkTables discovery found no candidates")
            all_candidates.update(nt_results)
        except Exception as e:
            logger.error(f"NetworkTables discovery error: {e}")

    # Convert IPs to tab entries and sort for consistency
    if not all_candidates:
        logger.debug("No PhotonVision dashboards discovered")
        return []

    # Sort IPs for consistent ordering
    sorted_ips = sorted(all_candidates, key=lambda x: tuple(map(int, x.split("."))))

    tabs: List[Dict[str, str]] = [
        {"title": f"{ip}", "url": f"http://{ip}:5800"}
        for ip in sorted_ips
    ]

    logger.info(f"Discovery complete: {len(tabs)} dashboards found")
    return tabs
