# cmake版本
cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src/main/cpp/source CPP_SOURCE)
aux_source_directory(src/main/cpp/libyuv/source LIBYUV_SOURCE)
add_library(surface_renderer SHARED ${CPP_SOURCE} ${LIBYUV_SOURCE})

find_library(log-lib log)
find_library(android-lib android)

include_directories(src/main/cpp/libyuv/include src/main/cpp/include)

target_link_libraries(surface_renderer ${log-lib} ${android-lib})