"""Tests for request/OS address helpers used by team auto-detection.

These tests live outside overlay/ so they are not packaged into the IPK.
"""

import os
import sys

sys.path.insert(
    0,
    os.path.join(
        os.path.dirname(__file__),
        "..",
        "overlay",
        "usr",
        "local",
        "bin",
        "photon-sc-app",
    ),
)

from discovery.network_scan_discovery import (  # noqa: E402
    parse_host_ipv4,
    parse_ip_addr_output,
    team_from_local_ips,
)

SAMPLE_IP_ADDR_OUTPUT = r"""\
1: lo    inet 127.0.0.1/8 scope host lo\       valid_lft forever preferred_lft forever
2: eth0    inet 10.17.36.2/24 brd 10.17.36.255 scope global eth0\       valid_lft forever preferred_lft forever
3: wlan0    inet 192.168.1.50/24 brd 192.168.1.255 scope global wlan0\       valid_lft forever preferred_lft forever
4: eth1    inet6 fe80::1/64 scope link \       valid_lft forever preferred_lft forever
"""


def test_parse_ip_addr_output_filters_loopback_and_inet6():
    assert parse_ip_addr_output(SAMPLE_IP_ADDR_OUTPUT) == {
        "10.17.36.2",
        "192.168.1.50",
    }


def test_parse_ip_addr_output_empty():
    assert parse_ip_addr_output("") == set()


def test_parse_host_ipv4_with_port():
    assert parse_host_ipv4("10.17.36.2:9002") == "10.17.36.2"


def test_parse_host_ipv4_without_port():
    assert parse_host_ipv4("10.17.36.2") == "10.17.36.2"


def test_parse_host_ipv4_loopback_rejected():
    assert parse_host_ipv4("127.0.0.1:9002") is None


def test_parse_host_ipv4_hostname_rejected():
    assert parse_host_ipv4("photon.local:9002") is None


def test_parse_host_ipv4_ipv6_literal_rejected():
    assert parse_host_ipv4("[::1]:9002") is None


def test_parse_host_ipv4_empty_rejected():
    assert parse_host_ipv4("") is None


def test_parse_host_ipv4_garbage_rejected():
    assert parse_host_ipv4("999.1.1.1:9002") is None


def test_host_header_drives_team_guess():
    host_address = parse_host_ipv4("10.17.36.2:9002")
    assert team_from_local_ips([host_address]) == "1736"


def test_host_header_loopback_falls_through_to_system_ips():
    assert parse_host_ipv4("127.0.0.1:9002") is None
    assert team_from_local_ips(["10.0.0.3", "10.111.76.58"]) == "11176"
