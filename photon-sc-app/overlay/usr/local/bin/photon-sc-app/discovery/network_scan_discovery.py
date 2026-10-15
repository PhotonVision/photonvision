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
    Set of IP addresses that respond to ping or reachability probes.
"""

import ipaddress
import logging
import socket
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Callable, Set

logger = logging.getLogger(__name__)
PING_WORKERS = 10
PING_TIMEOUT = 0.5


def discover_network_scan(
    team_number: str | int,
    progress_callback: Callable[[str, bool], None],
) -> Set[str]:
    """Ping all usable hosts in an explicitly selected FRC team /24."""
    network = team_subnet_from_number(team_number)
    base_ip = str(network.network_address).rsplit(".", 1)[0]

    responding: Set[str] = set()

    def _probe_host(host_id: int) -> tuple[str, bool]:
        ip = f"{base_ip}.{host_id}"
        responded = _probe_ping(ip, timeout=PING_TIMEOUT)
        progress_callback(ip, responded)

        if responded:
            logger.debug("Network scan ping responded: %s", ip)
        return ip, responded

    with ThreadPoolExecutor(
        max_workers=PING_WORKERS, thread_name_prefix="PingScan"
    ) as executor:
        futures = [executor.submit(_probe_host, host_id) for host_id in range(1, 255)]
        for future in as_completed(futures):
            ip, responded = future.result()
            if responded:
                responding.add(ip)

    return responding


def team_subnet_from_number(team_number: str | int) -> ipaddress.IPv4Network:
    """Convert an FRC team number to its `10.TE.AM.0/24` subnet."""
    value = str(team_number).strip()
    if not value.isdecimal() or int(value) <= 0:
        raise ValueError("Team number must be a positive decimal integer")

    padded = str(int(value)).zfill(4)
    te = int(padded[:-2])
    am = int(padded[-2:])
    if te > 255:
        raise ValueError("Team number TE octet exceeds 255")

    return ipaddress.IPv4Network(f"10.{te}.{am}.0/24")


def local_ipv4_addresses(nt_server: str | None = None) -> list[str]:
    """Return local IPv4 addresses known to the hostname and selected route."""
    addresses: Set[str] = set()
    try:
        addresses.update(
            result[4][0]
            for result in socket.getaddrinfo(
                socket.gethostname(), None, family=socket.AF_INET
            )
            if not result[4][0].startswith("127.")
        )
    except OSError as error:
        logger.debug("Could not resolve local interface addresses: %s", error)

    if nt_server:
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
                probe.connect((nt_server, 5810))
                addresses.add(probe.getsockname()[0])
        except OSError as error:
            logger.debug(
                "Could not determine route to NT server %s: %s", nt_server, error
            )

    return sorted(addresses)


def _probe_ping(ip: str, timeout: float = 1.0) -> bool:
    """Probe the given IP address with a single ping.

    Uses the system ping command to support cross-platform reachability
    checks. This is used to populate the ping-only discovery column.

    Args:
        ip: IP address to probe.
        timeout: Timeout in seconds for the ping command.

    Returns:
        True if the host responded to ping, False otherwise.
    """
    import platform

    if platform.system().lower() == "windows":
        command = ["ping", "-n", "1", "-w", str(int(timeout * 1000)), ip]
    else:
        command = ["ping", "-c", "1", "-W", str(timeout), ip]

    try:
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            check=False,
        )
        return result.returncode == 0
    except (subprocess.TimeoutExpired, OSError) as e:
        logger.debug("Ping probe failed for %s: %s", ip, e)
        return False
