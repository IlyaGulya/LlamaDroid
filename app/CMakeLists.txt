cmake_minimum_required(VERSION 3.14)

project(llama-android)  # Указывает имя проекта

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS ON)

# checkout llama.cpp
include(FetchContent)
FetchContent_Declare(
        llama.cpp
        GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
        GIT_TAG b2175
)
FetchContent_MakeAvailable(llama.cpp)

set(LLAMA_CLBLAST ON CACHE BOOL "Use clblast for OpenCL")

add_library(
        llama-android
        SHARED
        src/main/cpp/llama-android.cpp  # Проверьте, что этот путь верный
)

target_link_libraries(llama-android PRIVATE llama common android log)