"""NetworkTables discovery strategy.

Uses PyNetworkTables (WPILib NetworkTables client library) to discover
PhotonVision instances that are publishing to the network.

Looks for clients publishing to /photonvision or related topics that
indicate IP addresses of running PhotonVision instances.

Returns:
    Set of IP addresses discovered via NetworkTables.
"""

import logging
from typing import Optional, Set

logger = logging.getLogger(__name__)

try:
    from ntcore import NetworkTableInstance
    NTCORE_AVAILABLE = True
except ImportError:
    NTCORE_AVAILABLE = False
    logger.debug("ntcore not available, NetworkTables discovery disabled")


def discover_networktables(
    ntables_server: Optional[str] = None, timeout: float = 2.0
) -> Set[str]:
    """Discover PhotonVision instances via NetworkTables.

    Connects to a NetworkTables server and looks for entries in the
    /photonvision topic that contain IP address information.

    Args:
        ntables_server: NetworkTables server address (hostname or IP).
                        Defaults to localhost if not provided.
        timeout: Connection timeout in seconds.

    Returns:
        Set of IP addresses found in NetworkTables, or empty set
        if NetworkTables is unavailable or connection fails.
    """
    if not NTCORE_AVAILABLE:
        logger.debug("NetworkTables discovery skipped (ntcore not installed)")
        return set()

    if ntables_server is None:
        # Default to localhost for development
        ntables_server = "localhost"

    try:
        instance = NetworkTableInstance.getDefault()
        
        # Attempt to start client using available API
        # Try modern ntcore API first, fall back to legacy
        try:
            # Modern ntcore 4.x+ API
            instance.startClient4(ntables_server)
        except (AttributeError, TypeError):
            try:
                # Fallback: ntcore 3.x API
                instance.startClient(ntables_server)
            except (AttributeError, TypeError):
                # Last resort: default client with no arguments
                logger.debug("Using default NetworkTables client connection")
                instance.startClient()

        # Try to read photonvision topic entries
        discovered: Set[str] = set()

        # Wait briefly for connection
        import time

        time.sleep(min(timeout, 0.5))

        # Query for /photonvision entries (structure TBD based on actual setup)
        # This is a placeholder - exact topic structure depends on deployment
        try:
            table = instance.getTable("/photonvision")
            if table:
                # Look for entries that contain IP addresses
                # Common patterns: ips, addresses, instances, etc.
                for key in ["ips", "addresses", "instances", "servers"]:
                    try:
                        value = table.getStringArray(key, [])
                        if value:
                            discovered.update(value)
                            logger.info(f"NetworkTables found IPs under /{key}: {value}")
                    except Exception:
                        # Key not found or wrong type, continue
                        pass
        except Exception as e:
            logger.debug(f"Error querying /photonvision table: {e}")

        instance.stopClient()
        return discovered

    except Exception as e:
        logger.debug(f"NetworkTables discovery error: {e}")
        return set()
