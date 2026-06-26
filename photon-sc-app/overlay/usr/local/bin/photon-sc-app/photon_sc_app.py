#!/usr/bin/env python3
"""
Photon SC App - Tab Dashboard HTTP Server

This service serves a dynamic tab-based dashboard UI that can display multiple
external web content in iframes. It supports two deployment modes:

1. Systemd socket activation (production): The service is managed by systemd
   and receives a pre-bound socket via FD 3.

2. Local development mode (--local flag): Direct HTTP server on localhost.

Key Features:
- Dynamic tab list via /tabs API endpoint
- Service status endpoint at /status for health checks
- Graceful shutdown via SIGINT/SIGTERM
"""

import argparse
import http.server
import json
import os
import signal
import socket
import socketserver
import sys
from datetime import datetime
from typing import Any, Dict, Optional

# Global flag to track deployment mode (True for systemd, False for local)
SOCKET_ACTIVATED: bool = False


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
        # Serve index.html for root requests
        if self.path in ("/", "/index.html", ""):
            self.path = "/index.html"
            return super().do_GET()

        # Service status endpoint for health checks
        if self.path in ("/status", "/health"):
            return self.send_status()

        # Dynamic tab configuration endpoint
        if self.path == "/tabs":
            return self.send_tabs()

        # Default: serve static files from www directory
        return super().do_GET()

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
        }

        self.wfile.write(json.dumps(response, indent=2).encode())

    def send_tabs(self) -> None:
        """Return list of available tabs as JSON.

        The frontend fetches this on startup and refresh to populate the tab bar.
        Tab format: {"title": "Display Name", "url": "https://..."}

        TODO: Make this configurable from a file or database.
        """
        tabs: list[Dict[str, str]] = [
            {"title": "Example", "url": "https://example.com"},
            {"title": "Docs", "url": "https://docs.photonvision.org"},
        ]

        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"tabs": tabs}, indent=2).encode())


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
        """Initialize the socket-activated service."""
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

    def __init__(self, host: str = "127.0.0.1", port: int = 8080) -> None:
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
            print(
                "ERROR: Must be started by systemd socket activation or use --local "
                "for development mode"
            )
            sys.exit(1)
        service: SocketActivatedService | LocalService = SocketActivatedService()
    else:
        # Development: local server
        service = LocalService(host=args.host, port=args.port)

    # Register signal handlers for clean shutdown
    signal.signal(signal.SIGINT, signal_handler)  # Ctrl-C
    signal.signal(signal.SIGTERM, signal_handler)  # Termination request

    try:
        service.start()
    except (KeyboardInterrupt, SystemExit):
        # Ensure cleanup happens on any exit (Ctrl-C, signal, etc.)
        service.stop()
        print("Server stopped.")


if __name__ == "__main__":
    main()
