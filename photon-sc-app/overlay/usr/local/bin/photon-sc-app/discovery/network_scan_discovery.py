"""Network scanning discovery strategy.

Scans the FRC team network (10.TE.AM.0/24) by pinging each host.

FRC team networks follow the pattern 10.TE.AM.XX where:
- 10 = FRC standard first octet
- TE = team number first two digits (zero-padded)
- AM = team number last two digits (zero-padded)
- XX = host ID (1-254, 255 is broadcast)

Example: Team 5123 -> 10.51.23.1-254
Example: Team 254 -> 10.02.54.1-254

Returns:
    Set of IP addresses that respond to ping.
"""

import logging
import subprocess
from typing import Optional, Set

logger = logging.getLogger(__name__)


def discover_network_scan(team_number: Optional[int] = None) -> Set[str]:
    """Scan the FRC team network for responding hosts.

    Constructs the team network address from the team number and pings
    each host (XX from 1 to 254). Returns IPs that respond to ICMP ping.

    Args:
        team_number: FRC team number (e.g., 5123, 254, 1).
                     If None, uses environment variable PHOTONVISION_TEAM
                     or skips network scanning.

    Returns:
        Set of IP addresses that respond to ping on the team network.
    """
    if team_number is None:
        import os

        team_str = os.environ.get("PHOTONVISION_TEAM")
        if not team_str:
            logger.debug("No team number provided for network scanning")
            return set()
        try:
            team_number = int(team_str)
        except ValueError:
            logger.error(f"Invalid team number: {team_str}")
            return set()

    # Format team number to IP: 10.TE.AM.XX
    # Team 5123 -> 10.51.23.XX
    # Team 254 -> 10.02.54.XX
    tens = team_number // 100
    ones = team_number % 100
    base_ip = f"10.{tens:02d}.{ones:02d}"

    responding: Set[str] = set()

    # Scan hosts 1-254 (0 is network, 255 is broadcast)
    for host_id in range(1, 255):
        ip = f"{base_ip}.{host_id}"
        if _ping_host(ip):
            logger.info(f"Network scan found: {ip}")
            responding.add(ip)

    return responding


def _ping_host(ip: str, timeout: int = 1) -> bool:
    """Check if a host responds to ping.

    Args:
        ip: IP address to ping.
        timeout: Timeout in seconds for the ping.

    Returns:
        True if host responds to ping, False otherwise.
    """
    try:
        # Use ping with count=1 and timeout
        # Platform-specific timeout flag
        result = subprocess.run(
            ["ping", "-c", "1", "-W", str(timeout * 1000), ip],
            capture_output=True,
            timeout=timeout + 1,
        )
        return result.returncode == 0
    except (subprocess.TimeoutExpired, FileNotFoundError, Exception) as e:
        logger.debug(f"Ping failed for {ip}: {e}")
        return False
