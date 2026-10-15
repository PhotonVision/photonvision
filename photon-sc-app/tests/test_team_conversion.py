"""Tests for FRC team number to subnet conversion.

These tests live outside overlay/ so they are not packaged into the IPK.
"""

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "overlay", "usr", "local", "bin", "photon-sc-app"))

from discovery.network_scan_discovery import team_subnet_from_number  # noqa: E402


def test_team_1736():
    assert str(team_subnet_from_number("1736")) == "10.17.36.0/24"


def test_team_254():
    assert str(team_subnet_from_number("254")) == "10.2.54.0/24"


def test_team_12345():
    assert str(team_subnet_from_number("12345")) == "10.123.45.0/24"


def test_team_5123():
    assert str(team_subnet_from_number("5123")) == "10.51.23.0/24"


def test_team_1():
    assert str(team_subnet_from_number("1")) == "10.0.1.0/24"


def test_team_zero_rejected():
    try:
        team_subnet_from_number("0")
        assert False, "Expected ValueError for team 0"
    except ValueError:
        pass


def test_team_negative_rejected():
    try:
        team_subnet_from_number("-5")
        assert False, "Expected ValueError for negative team"
    except ValueError:
        pass


def test_team_non_numeric_rejected():
    try:
        team_subnet_from_number("abc")
        assert False, "Expected ValueError for non-numeric team"
    except ValueError:
        pass


def test_team_float_rejected():
    try:
        team_subnet_from_number("12.5")
        assert False, "Expected ValueError for float team"
    except ValueError:
        pass


def test_team_empty_rejected():
    try:
        team_subnet_from_number("")
        assert False, "Expected ValueError for empty team"
    except ValueError:
        pass


def test_team_too_large_rejected():
    try:
        team_subnet_from_number("99999")
        assert False, "Expected ValueError for TE octet > 255"
    except ValueError:
        pass


def test_team_whitespace_rejected():
    try:
        team_subnet_from_number("   ")
        assert False, "Expected ValueError for whitespace-only team"
    except ValueError:
        pass
