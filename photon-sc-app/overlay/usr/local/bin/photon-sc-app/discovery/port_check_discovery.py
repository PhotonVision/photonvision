"""Port 5800 verification strategy.

Given a set of candidate IP addresses, checks which ones are serving
a web server on port 5800 (PhotonVision default).

Returns:
    Set of IP addresses with accessible web servers on port 5800.
"""

import logging
import socket
from typing import Set

logger = logging.getLogger(__name__)

# Timeout for TCP connection attempts (seconds)
PORT_CHECK_TIMEOUT = 2


def verify_port_5800(candidates: Set[str]) -> Set[str]:
    """Check which candidates have web servers on port 5800.

    Attempts a TCP connection to port 5800 on each candidate IP.
    Includes IPs where the port is accessible.

    Args:
        candidates: Set of IP addresses to check.

    Returns:
        Set of IP addresses with accessible port 5800.
    """
    verified: Set[str] = set()

    for ip in candidates:
        if _has_port_5800(ip):
            logger.info(f"Port verification found: {ip}:5800")
            verified.add(ip)

    return verified


def _has_port_5800(ip: str) -> bool:
    """Check if port 5800 is open on the given IP.

    Attempts a TCP connection to port 5800. Success indicates
    a web server is likely running there.

    Args:
        ip: IP address to check.

    Returns:
        True if port 5800 is open, False otherwise.
    """
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(PORT_CHECK_TIMEOUT)
        result = sock.connect_ex((ip, 5800))
        sock.close()

        if result == 0:
            return True
        return False
    except Exception as e:
        logger.debug(f"Port check failed for {ip}:5800: {e}")
        return False
