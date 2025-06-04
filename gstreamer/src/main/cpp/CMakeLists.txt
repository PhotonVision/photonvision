cmake_minimum_required(VERSION 3.10)
project(OpenCV_GStreamer_Capture)

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find OpenCV
find_package(OpenCV REQUIRED)

# Print OpenCV version
message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "OpenCV include path: ${OpenCV_INCLUDE_DIRS}")

add_library(gstreamer SHARED "")
target_link_libraries(gstreamer ${OpenCV_LIBS})
target_sources(gstreamer PRIVATE gstreamer.cpp)
