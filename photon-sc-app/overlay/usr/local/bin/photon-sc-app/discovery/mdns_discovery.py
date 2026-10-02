"""mDNS discovery strategy.

Attempts to resolve 'photonvision.local' via multicast DNS (mDNS).
This is typically set up on a PhotonVision host running avahi-daemon
or similar mDNS responder.

Returns:
    Set of IP addresses (as strings) that resolve photonvision.local.
    Empty set if resolution fails or mDNS is unavailable.
"""

import logging
import socket
from typing import Set

logger = logging.getLogger(__name__)


def discover_mdns() -> Set[str]:
    """Discover PhotonVision via mDNS hostname resolution.

    Attempts to resolve 'photonvision.local' to an IP address.
    On success, returns the resolved IPv4 address.

    Returns:
        Set containing the resolved IP, or empty set if resolution fails.
    """
    try:
        # Try to resolve photonvision.local
        result = socket.gethostbyname("photonvision.local")
        logger.info(f"mDNS discovery found: {result}")
        return {result}
    except socket.gaierror as e:
        logger.debug(f"mDNS resolution failed: {e}")
        return set()
    except Exception as e:
        logger.error(f"Unexpected error during mDNS discovery: {e}")
        return set()
