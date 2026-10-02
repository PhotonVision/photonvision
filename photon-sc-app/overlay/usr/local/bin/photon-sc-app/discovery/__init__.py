"""Discovery suite for finding PhotonVision dashboards on the network.

Provides multiple strategies to locate PhotonVision instances:
- mDNS discovery via photonvision.local
- Network scanning on FRC team networks (10.TE.AM.XX)
- Port 5800 verification
- NetworkTables client discovery
"""
