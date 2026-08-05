project('photonlibpy', ['cpp'],
        default_options: ['warning_level=1', 'cpp_std=c++20',
                          'b_colorout=auto', 'optimization=2', 'b_pie=true'])

# All dependencies are resolved via pkg-config.
# build-native.sh exports PKG_CONFIG_PATH covering:
#   - photontargeting-native, photonlib-native (built locally)
#   - semiwrap, pybind11 (from the venv's site-packages)
# semiwrap.pc has `Requires: pybind11`, so pybind11 flags come through transitively.
semiwrap_dep = dependency('semiwrap')
photontargeting_native_dep = dependency('photontargeting-native')

# photon-lib's own C++ headers (sibling of this directory)
native_inc = include_directories('../native/include')

photonlibpy_sources = files('photonlibpy/src/main.cpp')

photonlibpy_deps = [
  semiwrap_dep,
  photontargeting_native_dep,
  declare_dependency(include_directories: native_inc),
]
