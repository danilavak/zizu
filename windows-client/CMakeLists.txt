cmake_minimum_required(VERSION 3.21)

project(zizu_windows_client VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(zizu-windows-client WIN32
    src/main.cpp
    resources/tray.rc
)

target_compile_definitions(zizu-windows-client PRIVATE
    UNICODE
    _UNICODE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
)

target_link_libraries(zizu-windows-client PRIVATE
    shell32
    user32
    advapi32
)
