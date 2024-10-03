cmake_minimum_required(VERSION 3.15...3.29)
project(example LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 20)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Checked out commit https://github.com/pybind/pybind11/tree/a5b0cdcb937b2853e012489633d692099dab7078
add_subdirectory(pybind11)

pybind11_add_module(wrap_a meme/bind_a.cpp)

target_compile_definitions(wrap_a PRIVATE
    PYBIND11_USE_SMART_HOLDER_AS_DEFAULT=1
)
message(asdfasdf ${CMAKE_SOURCE_DIR})
target_include_directories(
    wrap_a
    PRIVATE
    ${CMAKE_SOURCE_DIR}/../.venv/Lib/site-packages/wpimath/_impl/include/
    ${CMAKE_SOURCE_DIR}/../.venv/Lib/site-packages/wpiutil/include/
)
target_link_libraries(
    wrap_a
    PRIVATE
    # /home/mmorley/photonvision/.venv/lib/python3.10/site-packages/wpimath/_impl/lib/libwpimath.so
    "C:\\Users\\matth\\Documents\\GitHub\\photonvision\\.venv\\Lib\\site-packages\\wpimath\\_impl\\lib\\wpimath.lib"
    # /home/mmorley/photonvision/.venv/lib/python3.10/site-packages/wpiutil/lib/libwpiutil.so
    "C:\\Users\\matth\\Documents\\GitHub\\photonvision\\.venv\\Lib\\site-packages\\wpiutil\\lib\\wpiutil.lib"
)
