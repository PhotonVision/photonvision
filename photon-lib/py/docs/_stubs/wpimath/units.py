"""Minimal wpimath.units stub for documentation builds."""

def degreesToRadians(deg: float) -> float:
    from math import pi

    return deg * (pi / 180.0)


# Represent seconds as a float alias for annotations
seconds = float

__all__ = ["degreesToRadians", "seconds"]

# Common unit aliases used in type annotations in WPILib stubs
meters = float
meters_per_second = float
meters_per_second_squared = float
kilograms = float
kilogram_square_meters = float

__all__.extend([
    "meters",
    "meters_per_second",
    "meters_per_second_squared",
    "kilograms",
    "kilogram_square_meters",
    "hertz",
])

# frequency
hertz = float
