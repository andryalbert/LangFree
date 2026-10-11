cmake_minimum_required(VERSION 3.22.1)

project("langfree")

# Configuration C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Inclure les dossiers nécessaires
include_directories(${CMAKE_SOURCE_DIR}/include)

# Sources C++
set(LLAMA_SOURCES
    llama_wrapper.cpp
)

# Créer la bibliothèque partagée
add_library(llama_wrapper SHARED ${LLAMA_SOURCES})

# Lier les bibliothèques système
find_library(log-lib log)
target_link_libraries(llama_wrapper ${log-lib})

# Configuration pour les différentes architectures
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_definitions(llama_wrapper PRIVATE ARM32)
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(llama_wrapper PRIVATE ARM64)
endif()
