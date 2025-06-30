#!/usr/bin/env python3
import http.server
import socketserver
import threading
import signal
import sys
import os
import json
from datetime import datetime

import mimetypes
# Ensure mimetypes are set for common file types
mimetypes.add_type('application/javascript', '.js')


class PVSystemViewerHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        script_dir = os.path.dirname(os.path.abspath(__file__))
        www_dir = os.path.join(script_dir, 'www')  # Path to the www folder

        if self.path == '/':
            file_path = os.path.join(www_dir, 'systemViewer.html')
        else:
            # Serve the requested file
            file_path = os.path.join(www_dir, self.path.lstrip('/'))

        # Check if the file exists and is within the www directory
        if os.path.commonpath([www_dir, os.path.abspath(file_path)]) != www_dir or not os.path.isfile(file_path):
            self.send_response(404)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(b'<!DOCTYPE html><html lang="en"><p>404 Not Found</p></body></html>')
            return

        # Determine the MIME type of the file
        mime_type, _ = mimetypes.guess_type(file_path)
        print(mime_type)
        self.send_response(200)
        self.send_header('Content-type', mime_type or 'application/octet-stream')
        self.end_headers()

        # Serve the file content
        with open(file_path, 'rb') as f:
            self.wfile.write(f.read())

    def log_message(self, format, *args):
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        print(f"[{timestamp}] {format % args}")

class PVSystemViewerServer:
    def __init__(self, port=5804):
        self.port = port
        self.httpd = None
        self.server_thread = None
        self.shutdown_event = threading.Event()
        
    def start(self):
        try:
            self.httpd = socketserver.TCPServer(("", self.port), PVSystemViewerHandler)
            # Allow socket reuse to prevent "Address already in use" errors
            self.httpd.allow_reuse_address = True
            
            self.server_thread = threading.Thread(target=self.httpd.serve_forever)
            self.server_thread.daemon = True
            self.server_thread.start()
            print(f"PhotonVision System Viewer server started on port {self.port}")
            return True
        except Exception as e:
            print(f"Failed to start server: {e}")
            return False

    def stop(self):
        print("Stopping PhotonVision System Viewer server...")
        self.shutdown_event.set()
        
        if self.httpd:
            self.httpd.shutdown()
            self.httpd.server_close()
            
        if self.server_thread and self.server_thread.is_alive():
            self.server_thread.join(timeout=2)
            
        print("PhotonVision System Viewer server stopped")

# Global server instance for signal handler
server_instance = None

def signal_handler(signum, frame):
    print(f"\nReceived signal {signum}, stopping server...")
    if server_instance:
        server_instance.stop()
    sys.exit(0)

def main():
    global server_instance
    
    # Register signal handlers
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    server_instance = PVSystemViewerServer(5804)
    
    if server_instance.start():
        print("PhotonVision System Viewer service is running. Managed by systemd.")
        try:
            server_instance.shutdown_event.wait()
        except KeyboardInterrupt:
            signal_handler(signal.SIGINT, None)
    else:
        print("Failed to start PhotonVision System Viewer service")
        sys.exit(1)

if __name__ == "__main__":
    main()