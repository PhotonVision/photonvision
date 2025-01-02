import threading
import socket
from typing import Callable, Optional
from wpilib import Timer
import logging


class TimeSyncServer:
    """This class is a python re-write of the UDP time sync server protocol 
    which runs on a roboRIO to establish a timebase for all PhotonVision coprocessors.
    """
    PORT = 5810

    class TspPing:
        def __init__(self, version: int, message_id: int, client_time: int):
            self.version = version
            self.message_id = message_id
            self.client_time = client_time

        @staticmethod
        def unpack(data: bytes) -> 'TimeSyncServer.TspPing':
            version = data[0]
            message_id = data[1]
            client_time = int.from_bytes(data[2:10], byteorder='little')
            return TimeSyncServer.TspPing(version, message_id, client_time)

    class TspPong:
        def __init__(self, ping: 'TimeSyncServer.TspPing', server_time: int):
            self.version = ping.version
            self.message_id = 2  # Pong message ID
            self.client_time = ping.client_time
            self.server_time = server_time

        def pack(self) -> bytes:
            return (
                self.version.to_bytes(1, byteorder='little') +
                self.message_id.to_bytes(1, byteorder='little') +
                self.client_time.to_bytes(8, byteorder='little') +
                self.server_time.to_bytes(8, byteorder='little')
            )

    def __init__(self, time_provider: Optional[Callable[[], int]] = None):
        self.time_provider = time_provider or Timer.getFPGATimestamp
        self._process: Optional[threading.Thread] = None
        self.logger = logging.getLogger("PhotonVision-TimeSyncServer")

    def _udp_server(self):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp_socket:
            udp_socket.bind(("0.0.0.0", self.PORT))
            while True:
                data, addr = udp_socket.recvfrom(1024)  # Buffer size of 1024 bytes
                self.logger.debug( f"Data: { ''.join([format(x, '02x') for x in data]) }" )

                if len(data) < 10:
                    self.logger.error("Too few bytes")
                    continue  # Ignore incomplete packets

                ping = self.TspPing.unpack(data)
                if ping.version != 1 or ping.message_id != 1:
                    self.logger.error("Invalid Version/ID")
                    continue  # Ignore invalid pings

                server_time = int(self.time_provider() * 1e6)  # Convert to microseconds
                pong = self.TspPong(ping, server_time)
                udp_socket.sendto(pong.pack(), addr)
                self.logger.debug(f"Ponged at {server_time} with { ''.join([format(x, '02x') for x in pong.pack()]) }")


    def start(self):
        if self._process is not None and self._process.is_alive():
            return # Nothing to do

        self._process = threading.Thread(target=self._udp_server, daemon=True)
        self._process.start()
        self.logger.info("Server Started")

    def stop(self):
        if self._process is not None:
            self._process.join()
            self._process = None
            self.logger.info("Server Stopped")

inst = TimeSyncServer()