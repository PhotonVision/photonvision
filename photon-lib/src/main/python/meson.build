project('photonlibpy', ['cpp'],
        default_options: ['warning_level=1', 'cpp_std=c++20',
                          'b_colorout=auto', 'optimization=2', 'b_pie=true'])

python = import('python').find_installation()

subdir('semiwrap')

# All of this is so stupid and absoutly needs to be changed
native_inc = include_directories('../native/include')
semiwrap_dep = dependency('semiwrap')

semiwrap_inc_path = run_command(  
  python,
  '-c',
  'import pathlib, semiwrap; print(pathlib.Path(semiwrap.__file__).resolve().parent / "include")',
  check: true,
).stdout().strip()
semiwrap_inc = include_directories(semiwrap_inc_path)

pybind11_inc_path = run_command(
  python,
  '-c',
  'import pybind11; print(pybind11.get_include())',
  check: true,
).stdout().strip()

pybind11_inc = include_directories(pybind11_inc_path)

photonlibpy_sources = files(
  'photonlibpy/src/main.cpp',
)

photonlibpy_deps += [
  semiwrap_dep,
  declare_dependency(
    include_directories: [native_inc, semiwrap_inc, pybind11_inc],
  )
]

subdir('semiwrap/modules')