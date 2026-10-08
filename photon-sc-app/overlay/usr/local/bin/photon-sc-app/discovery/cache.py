"""Background discovery cache and refresh manager.

Manages periodic discovery of PhotonVision dashboards in a background thread
with low CPU overhead. Supports different refresh intervals for expensive
operations (network scanning) vs. quick checks (mDNS, port verification).
"""

import logging
import threading
import time
from typing import Dict, List, Optional

from .aggregator import discover_all

logger = logging.getLogger(__name__)


class DiscoveryCache:
    """Background discovery cache with configurable refresh intervals.

    Runs discovery strategies periodically in a background thread and caches
    results for fast serving to the /tabs endpoint.

    Supports tuning different discovery strategies independently:
    - Fast strategies (mDNS, port checks): ~10 seconds default
    - Slow strategies (network scan, NetworkTables): ~60 seconds default
    """

    def __init__(
        self,
        team_number: Optional[int] = None,
        fast_interval: float = 10.0,
        slow_interval: float = 60.0,
        enable_fast: bool = True,
        enable_slow: bool = True,
    ):
        """Initialize the discovery cache.

        Args:
            team_number: FRC team number for network scanning.
            fast_interval: Seconds between fast discovery cycles (mDNS, port checks).
            slow_interval: Seconds between slow discovery cycles (network scan).
            enable_fast: Whether to run fast discovery strategies.
            enable_slow: Whether to run expensive discovery strategies.
        """
        self.team_number = team_number
        self.fast_interval = fast_interval
        self.slow_interval = slow_interval
        self.enable_fast = enable_fast
        self.enable_slow = enable_slow

        # Cache storage and synchronization
        self._tabs_cache: List[Dict[str, str]] = []
        self._cache_lock = threading.RLock()

        # Background thread management
        self._discovery_thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()

    def start(self) -> None:
        """Start the background discovery thread.

        The thread will periodically call discover_all() and update the cache.
        Uses daemon threads so the service can shut down cleanly.
        """
        if self._discovery_thread is not None:
            logger.warning("Discovery cache already started")
            return

        self._stop_event.clear()
        self._discovery_thread = threading.Thread(
            target=self._discovery_loop,
            daemon=True,
            name="DiscoveryCache",
        )
        self._discovery_thread.start()
        logger.info("Background discovery cache started")

    def stop(self) -> None:
        """Stop the background discovery thread.

        Signals the thread to stop and waits for it to finish.
        """
        if self._discovery_thread is None:
            return

        logger.info("Stopping background discovery cache")
        self._stop_event.set()
        if self._discovery_thread.is_alive():
            self._discovery_thread.join(timeout=5.0)

    def get_tabs(self) -> List[Dict[str, str]]:
        """Get the currently cached list of tabs.

        Returns immediately without blocking for new discovery.

        Returns:
            List of discovered tabs, or empty list if none found.
        """
        with self._cache_lock:
            return self._tabs_cache.copy()

    def _discovery_loop(self) -> None:
        """Background discovery loop.

        Periodically runs discovery strategies with different intervals:
        - Fast strategies every fast_interval seconds
        - Slow strategies every slow_interval seconds

        The loop is designed to be low-CPU: it sleeps most of the time and
        wakes periodically to check if discovery is needed.
        """
        next_fast_discovery = time.time()
        next_slow_discovery = time.time()

        while not self._stop_event.is_set():
            now = time.time()
            run_fast = now >= next_fast_discovery
            run_slow = now >= next_slow_discovery

            if run_fast or run_slow:
                try:
                    # Run discovery with appropriate strategies enabled
                    tabs = discover_all(
                        team_number=self.team_number,
                        enable_mdns=self.enable_fast and run_fast,
                        enable_network_scan=self.enable_slow and run_slow,
                        enable_port_check=self.enable_fast and run_fast,
                        enable_networktables=self.enable_slow and run_slow,
                    )

                    # Update cache atomically
                    with self._cache_lock:
                        self._tabs_cache = tabs

                    # Log results periodically (not on every cycle)
                    if run_fast or run_slow:
                        logger.debug(f"Discovery updated: {len(tabs)} dashboards found")

                except Exception as e:
                    logger.error(f"Discovery error: {e}")

            # Schedule next runs
            if run_fast:
                next_fast_discovery = now + self.fast_interval
            if run_slow:
                next_slow_discovery = now + self.slow_interval

            # Sleep briefly to be responsive to stop signal
            # Use a small sleep so we check stop_event frequently
            sleep_time = min(0.5, self.fast_interval / 2)  # At most 0.5s, smart default
            self._stop_event.wait(sleep_time)
