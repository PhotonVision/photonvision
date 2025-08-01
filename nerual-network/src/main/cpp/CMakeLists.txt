cmake_minimum_required(VERSION 3.10)
project(nerual-network)

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)


include_directories(/usr/local/cuda/include)
link_directories(/usr/local/cuda/lib64)

include_directories(/usr/include/x86_64-linux-gnu/)
link_directories(/usr/lib/x86_64-linux-gnu/)

# Find OpenCV
find_package(OpenCV REQUIRED)

# Print OpenCV version
message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "OpenCV include path: ${OpenCV_INCLUDE_DIRS}")

add_library(nerual-network SHARED "")
target_link_libraries(nerual-network ${OpenCV_LIBS})
target_sources(nerual-network PRIVATE nerual_network.cpp)
target_link_libraries(nerual-network nvinfer)
target_link_libraries(nerual-network nvinfer_plugin)
target_link_libraries(nerual-network cudart)

