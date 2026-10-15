#!/usr/bin/env python3
"""
Photon SC App - Tab Dashboard HTTP Server

This service serves a dynamic tab-based dashboard UI that can display multiple
external web content in iframes. It supports two deployment modes:

1. Systemd socket activation (production): The service is managed by systemd
   and receives a pre-bound socket via FD 3.

2. Local development mode (--local flag): Direct HTTP server on localhost.

Key Features:
- Network info endpoint at /network_info with team, subnet, NT server
- Network scan endpoint at /scan with SSE streaming ping results
- Service status endpoint at /status for health checks
- Graceful shutdown via SIGINT/SIGTERM
"""

import argparse
import http.server
import json
import logging
import os
import queue
import signal
import socket
import socketserver
import sys
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from typing import Any, Dict, List, Optional
from urllib.parse import parse_qs, urlparse

from discovery.network_scan_discovery import (
    discover_network_scan,
    local_ipv4_addresses,
    parse_host_ipv4,
    team_from_local_ips,
    team_subnet_from_number,
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Global flag to track deployment mode (True for systemd, False for local)
SOCKET_ACTIVATED: bool = False

# Service version, populated from VERSION file (written by build.sh at package time)
VERSION: str = "unknown"
_version_path = os.path.join(os.path.dirname(__file__), "VERSION")
if os.path.exists(_version_path):
    try:
        with open(_version_path, "r", encoding="utf-8-sig") as _vf:
            VERSION = _vf.read().strip()
    except OSError as error:
        logger.debug("Could not read VERSION file: %s", error)


class ServiceHandler(http.server.SimpleHTTPRequestHandler):
    """HTTP request handler for the Photon SC App.

    Extends SimpleHTTPRequestHandler to add custom endpoints while maintaining
    the ability to serve static files from the 'www' directory.
    """

    def __init__(
        self, *args: Any, directory: Optional[str] = None, **kwargs: Any
    ) -> None:
        """Initialize handler with www directory for static file serving.

        Args:
            directory: Optional directory path. Defaults to 'www' subdirectory.
        """
        www_dir = directory or os.path.join(os.path.dirname(__file__), "www")
        super().__init__(*args, directory=www_dir, **kwargs)

    def do_GET(self) -> None:
        """Route incoming GET requests to appropriate handlers."""
        logger.debug("HTTP GET %s", self.path)
        request_path = urlparse(self.path)
        # Serve index.html for root requests
        if request_path.path in ("/", "/index.html", ""):
            self.path = "/index.html"
            return super().do_GET()

        # Service status endpoint for health checks
        if request_path.path in ("/status", "/health"):
            return self.send_status()

        if request_path.path == "/network_info":
            return self.send_network_info(parse_qs(request_path.query))

        # Default: serve static files from www directory
        return super().do_GET()

    def do_POST(self) -> None:
        """Handle discovery scans as a streamed Server-Sent Events response."""
        logger.debug("HTTP POST %s", self.path)
        if urlparse(self.path).path != "/scan":
            self.send_error(404)
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        if content_length > 4096:
            self.send_json(413, {"error": "Request body too large"})
            return

        request_body = self.rfile.read(content_length) if content_length else b"{}"
        try:
            payload = json.loads(request_body)
        except json.JSONDecodeError:
            self.send_json(400, {"error": "Expected a JSON request body"})
            return
        if not isinstance(payload, dict):
            self.send_json(400, {"error": "Expected a JSON object"})
            return

        team_number = payload.get("team_number")
        if team_number is None:
            self.send_json(400, {"error": "Enter an FRC team number to scan"})
            return
        try:
            subnet = team_subnet_from_number(team_number)
        except ValueError as error:
            self.send_json(400, {"error": str(error)})
            return

        scan_id = uuid.uuid4().hex
        nt_server = str(subnet.network_address + 2)
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache, no-transform")
        self.send_header("Connection", "close")
        self.send_header("X-Accel-Buffering", "no")
        self.end_headers()

        def write_event(event_name: str, event_data: Dict[str, object]) -> None:
            encoded = json.dumps(event_data, separators=(",", ":"))
            self.wfile.write(f"event: {event_name}\ndata: {encoded}\n\n".encode())
            self.wfile.flush()

        write_event(
            "scan_started",
            {
                "scan_id": scan_id,
                "team_number": str(team_number),
                "subnet": str(subnet),
                "nt_server": nt_server,
            },
        )

        events: queue.Queue[tuple[str, Dict[str, object]]] = queue.Queue()
        completed_probes = 0
        progress_lock = threading.Lock()

        def on_ping_result(ip: str, responded: bool) -> None:
            nonlocal completed_probes
            with progress_lock:
                completed_probes += 1
                current_count = completed_probes
            events.put(
                (
                    "ping",
                    {
                        "scan_id": scan_id,
                        "ip": ip,
                        "responded": responded,
                        "completed": current_count,
                        "total": 254,
                    },
                )
            )

        client_connected = True
        with ThreadPoolExecutor(
            max_workers=1, thread_name_prefix="NetworkScan"
        ) as executor:
            scan_future = executor.submit(
                discover_network_scan, str(team_number), on_ping_result
            )
            while not scan_future.done() or not events.empty():
                try:
                    event_name, event_data = events.get(timeout=0.25)
                except queue.Empty:
                    continue
                if client_connected:
                    try:
                        write_event(event_name, event_data)
                    except (BrokenPipeError, ConnectionResetError):
                        client_connected = False

            responsive_ips = scan_future.result()

        if client_connected:
            write_event(
                "scan_complete",
                {
                    "scan_id": scan_id,
                    "responsive_ips": sorted(responsive_ips),
                    "count": len(responsive_ips),
                },
            )

    def send_status(self) -> None:
        """Return service status as JSON for health monitoring."""
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()

        response: Dict[str, Any] = {
            "service": os.path.basename(os.path.dirname(__file__)),
            "status": "running",
            "socket_activated": SOCKET_ACTIVATED,
            "pid": os.getpid(),
            "timestamp": datetime.now().isoformat(),
            "version": VERSION,
        }

        self.wfile.write(json.dumps(response, indent=2).encode())

    def send_json(self, status_code: int, payload: Dict[str, object]) -> None:
        self.send_response(status_code)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(payload, indent=2).encode())

    def send_network_info(self, query: Dict[str, List[str]]) -> None:
        requested_team = query.get("team", [None])[0]
        if requested_team == "":
            requested_team = None

        # The Host header is the address the browser used to reach this server,
        # which is a reliable team signal even when hostname resolution on the
        # device only yields loopback addresses.
        candidates: List[str] = []
        host_address = parse_host_ipv4(self.headers.get("Host", ""))
        if host_address is not None:
            candidates.append(host_address)
        for address in local_ipv4_addresses():
            if address not in candidates:
                candidates.append(address)

        # If no explicit team, try to guess from local IP addresses (10.TE.AM.X)
        if requested_team is None:
            guessed_team = team_from_local_ips(candidates)
            if guessed_team is not None:
                requested_team = guessed_team

        network_info: Dict[str, object] = {
            "configured_team": None,
            "team_number": str(requested_team) if requested_team is not None else None,
            "subnet": None,
            "nt_server": None,
            "local_ips": candidates,
        }
        if requested_team is not None:
            try:
                subnet = team_subnet_from_number(requested_team)
            except ValueError as error:
                self.send_json(400, {"error": str(error)})
                return
            nt_server = str(subnet.network_address + 2)
            for address in local_ipv4_addresses(nt_server):
                if address not in candidates:
                    candidates.append(address)
            network_info.update(
                {
                    "subnet": str(subnet),
                    "nt_server": nt_server,
                    "local_ips": candidates,
                }
            )

        self.send_json(200, network_info)


class SocketActivatedService:
    """Service wrapper for systemd socket activation mode (production).

    In this mode, systemd creates a listening socket and passes it to the
    service via file descriptor 3. This separation of concerns allows:

    - Privileged socket binding (e.g., port 80/443) without service privileges
    - Socket reuse across service restarts without TIME_WAIT delays
    - Systemd to manage service lifecycle and auto-restart on failure
    - Better integration with systemd security features and resource limits
    """

    def __init__(self) -> None:
        """Initialize the socket-activated service.

        Args:
        """
        self.httpd: Optional[socketserver.ThreadingTCPServer] = None

    def get_systemd_socket(self) -> socket.socket:
        """Retrieve and validate the socket passed by systemd.

        Systemd sets LISTEN_PID and LISTEN_FDS environment variables:
        - LISTEN_PID: Our process ID (to ensure the socket is for us)
        - LISTEN_FDS: Number of file descriptors (should be >= 1)

        The first socket is always FD 3 (by convention):
        - FD 0 = stdin
        - FD 1 = stdout
        - FD 2 = stderr
        - FD 3+ = sockets/files from systemd

        We validate all three conditions before returning the socket.

        Returns:
            A socket object for the systemd-provided listening socket.

        Raises:
            RuntimeError: If not started by systemd or validation fails.
        """
        listen_pid = os.environ.get("LISTEN_PID")
        listen_fds = os.environ.get("LISTEN_FDS")

        if not listen_pid or not listen_fds:
            raise RuntimeError("Not started by systemd socket activation")

        if int(listen_pid) != os.getpid():
            raise RuntimeError(
                f"PID mismatch: expected {listen_pid}, got {os.getpid()}"
            )

        if int(listen_fds) < 1:
            raise RuntimeError(f"No sockets provided: LISTEN_FDS={listen_fds}")

        # Convert file descriptor to a Python socket object
        sock = socket.fromfd(3, socket.AF_INET, socket.SOCK_STREAM)
        return sock

    def start(self) -> None:
        """Start the HTTP server using the systemd-provided socket.

        Key details:
        - bind_and_activate=False: Socket from systemd is already bound/listening
        - daemon_threads=True: Allows quick shutdown (threads don't block exit)
        - poll_interval=0.5: Checks signals frequently for responsive Ctrl-C
        """
        server_socket = self.get_systemd_socket()

        # Create threaded server WITHOUT calling bind() or listen()
        # The socket from systemd is already bound and listening
        self.httpd = socketserver.ThreadingTCPServer(
            ("", 0), ServiceHandler, bind_and_activate=False
        )
        self.httpd.daemon_threads = True  # Allow quick shutdown
        self.httpd.socket = server_socket  # Replace with systemd socket

        print(f"Service started (PID: {os.getpid()})")
        # Poll interval allows signal handler to interrupt serve_forever()
        self.httpd.serve_forever(poll_interval=0.5)

    def stop(self) -> None:
        """Cleanly shut down the server and release resources."""
        if self.httpd:
            self.httpd.shutdown()
            self.httpd.server_close()


def signal_handler(signum: int, frame: Any) -> None:
    """Handle SIGINT (Ctrl-C) and SIGTERM signals for graceful shutdown.

    Prints a message and exits, allowing the main exception handler to run
    cleanup. We don't directly call service.stop() here because:
    - If called from a signal handler during blocking I/O, it may not work cleanly
    - Using sys.exit() allows the exception handler to call stop() properly

    Args:
        signum: Signal number received.
        frame: Current stack frame.
    """
    print("\nShutdown signal received, exiting.")
    sys.exit(0)


class LocalService:
    """Service wrapper for local development mode (--local flag).

    Runs a standard threaded HTTP server on localhost without systemd.
    This is convenient for development without requiring:
    - Systemd service file setup
    - Privileged socket binding
    - Special environment variables

    Default: localhost:8080 (customizable via --host and --port arguments)
    """

    def __init__(
        self,
        host: str = "127.0.0.1",
        port: int = 8080,
    ) -> None:
        """Initialize the local service.

        Args:
            host: Hostname to bind to. Defaults to '127.0.0.1'.
            port: Port to bind to. Defaults to 8080.
        """
        self.httpd: Optional[http.server.ThreadingHTTPServer] = None
        self.host = host
        self.port = port

    def start(self) -> None:
        """Start the HTTP server on the specified host:port.

        Key details:
        - ThreadingHTTPServer: Handles each request in a separate thread
        - daemon_threads=True: Threads don't block shutdown
        - allow_reuse_address=True: Allows quick restart without TIME_WAIT
        - poll_interval=0.5: Responsive to Ctrl-C and other signals
        """
        self.httpd = http.server.ThreadingHTTPServer(
            (self.host, self.port), ServiceHandler
        )
        self.httpd.daemon_threads = True  # Allow quick shutdown
        self.httpd.allow_reuse_address = True  # Reuse port after restart
        print(f"Local service started at http://{self.host}:{self.port}")
        # Poll interval allows signal handler to interrupt serve_forever()
        self.httpd.serve_forever(poll_interval=0.5)

    def stop(self) -> None:
        """Cleanly shut down the server and release resources."""
        if self.httpd:
            self.httpd.shutdown()
            self.httpd.server_close()


def main() -> None:
    """Entry point: parse arguments, select deployment mode, and start service.

    Deployment modes:

    1. Systemd socket activation (default):
       - Requires LISTEN_PID and LISTEN_FDS environment variables
       - Best for production with systemd

    2. Local development (--local flag):
       - Direct TCP server on localhost
       - Customizable host/port via --host and --port

    """

    parser = argparse.ArgumentParser(description="Photon SC App service")
    parser.add_argument(
        "--local",
        action="store_true",
        help="Run in local development mode without systemd socket activation",
    )
    parser.add_argument(
        "--host",
        default="127.0.0.1",
        help="Local host address when running in local mode",
    )
    parser.add_argument(
        "--port", type=int, default=8080, help="Local port when running in local mode"
    )
    args = parser.parse_args()

    # Select deployment mode based on --local flag
    if not args.local:
        # Production: systemd socket activation
        if not os.environ.get("LISTEN_PID") or not os.environ.get("LISTEN_FDS"):
            logger.error(
                "Must be started by systemd socket activation or use --local "
                "for development mode"
            )
            sys.exit(1)
        service: SocketActivatedService | LocalService = SocketActivatedService()
    else:
        # Development: local server
        service = LocalService(
            host=args.host,
            port=args.port,
        )

    # Register signal handlers for clean shutdown
    signal.signal(signal.SIGINT, signal_handler)  # Ctrl-C
    signal.signal(signal.SIGTERM, signal_handler)  # Termination request

    try:
        service.start()
    except (KeyboardInterrupt, SystemExit):
        # Ensure cleanup happens on any exit (Ctrl-C, signal, etc.)
        service.stop()
        logger.info("Server stopped.")


if __name__ == "__main__":
    main()
