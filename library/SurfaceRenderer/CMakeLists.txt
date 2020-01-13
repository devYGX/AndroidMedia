# cmake版本
cmake_minimum_required(VERSION 3.4.1)

file(GLOB srcs "src/main/cpp/*.cpp")
file(GLOB frame_srcs "src/main/cpp/frame/*.cpp")
add_library(surface_renderer SHARED ${srcs} ${frame_srcs})

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(surface_renderer ${log-lib})
target_link_libraries(surface_renderer ${android-lib})