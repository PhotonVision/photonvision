def write_stubgen():
    import os

    cwd = os.getcwd()

    # From nanobind==2.1.0
    # from nanobind.stubgen import StubGen

    from photonlibpy import _photonlibpy

    script_path = os.path.dirname(os.path.realpath(__file__))
    # sg = StubGen(_photonlibpy)
    # sg.put(_photonlibpy)
    # script_path = os.path.dirname(os.path.realpath(__file__))
    # with open(f"{script_path}/photonlibpy/_photonlibpy.pyi", "w") as f:
    #     print(f)
    #     f.write(sg.get())


    # mess with argv
    import sys
    sys.argv = [
            "<dummy>",
            "--exit-code",
            "--ignore-invalid-expressions=<.*>",
            "--root-suffix=",
            "-o",
            f"{script_path}/photonlibpy",
            "photonlibpy", 
    ]

    import pybind11_stubgen
    pybind11_stubgen.main()


if __name__ == "__main__":
    write_stubgen()
