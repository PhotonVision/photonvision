def write_stubgen():
    import os

    os.getcwd()

    # From nanobind==2.1.0
    from nanobind.stubgen import StubGen

    # this is legal since we import everything from this shared library in __init__.py 
    # but still isn't /great/, lol
    from photonlibpy.lib import _photonlibpy as _pp

    sg = StubGen(_pp)
    sg.put(_pp)
    script_path = os.path.dirname(os.path.realpath(__file__))
    with open(f"{script_path}/photonlibpy/lib/_photonlibpy.pyi", "w") as f:
        f.write(sg.get())


if __name__ == "__main__":
    write_stubgen()
