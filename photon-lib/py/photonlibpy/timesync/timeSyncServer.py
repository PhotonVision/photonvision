import logging
import socket
import struct
import threading
from typing import Callable, Optional

from wpilib import Timer


class TspPing:
    def __init__(self, version: int, message_id: int, client_time: int):
        self.version = version
        self.message_id = message_id
        self.client_time = client_time

    @staticmethod
    def unpack(data: bytes) -> "TspPing":
        # Unpack using struct.unpack
        version, message_id, client_time = struct.unpack("<BBQ", data)
        return TspPing(version, message_id, client_time)

    def pack(self) -> bytes:
        # Pack using struct.pack
        return struct.pack("<BBQ", self.version, self.message_id, self.client_time)


class TspPong:
    def __init__(self, ping: "TspPing", server_time: int):
        self.version = ping.version
        self.message_id = 2  # Pong message ID
        self.client_time = ping.client_time
        self.server_time = server_time

    def pack(self) -> bytes:
        # Pack using struct.pack
        return struct.pack(
            "<BBQQ", self.version, self.message_id, self.client_time, self.server_time
        )

    @staticmethod
    def unpack(data: bytes) -> "TspPong":
        # Unpack using struct.unpack
        version, message_id, client_time, server_time = struct.unpack("<BBQQ", data)
        ping = TspPing(version, message_id, client_time)
        return TspPong(ping, server_time)


class TimeSyncServer:
    """This class is a python re-write of the UDP time sync server protocol
    which runs on a roboRIO to establish a timebase for all PhotonVision coprocessors.
    """

    PORT = 5810

    def __init__(self, time_provider: Optional[Callable[[], int]] = None):
        self.time_provider = time_provider or Timer.getFPGATimestamp
        self._process: Optional[threading.Thread] = None
        self.logger = logging.getLogger("PhotonVision-TimeSyncServer")

    def _udp_server(self):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp_socket:
            udp_socket.bind(("0.0.0.0", self.PORT))
            while True:
                data, addr = udp_socket.recvfrom(1024)  # Buffer size of 1024 bytes

                if len(data) < 10:
                    self.logger.error("Too few bytes")
                    continue  # Ignore incomplete packets

                ping = TspPing.unpack(data)
                if ping.version != 1 or ping.message_id != 1:
                    self.logger.error("Invalid Version/ID")
                    continue  # Ignore invalid pings

                server_time = int(self.time_provider() * 1e6)  # Convert to microseconds
                pong = TspPong(ping, server_time)
                udp_socket.sendto(pong.pack(), addr)

    def start(self):
        if self._process is not None and self._process.is_alive():
            return  # Nothing to do

        self._process = threading.Thread(target=self._udp_server, daemon=True)
        self._process.start()
        self.logger.info("Server Started")

    def stop(self):
        if self._process is not None:
            self._process.join()
            self._process = None
            self.logger.info("Server Stopped")


inst = TimeSyncServer()
