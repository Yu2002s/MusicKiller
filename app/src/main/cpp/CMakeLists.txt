cmake_minimum_required(VERSION 4.1.2)

project("musickiller")

add_library(
        musickiller
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

find_library(
        z-lib
        z)

target_link_libraries(
        musickiller
        ${log-lib}
        ${z-lib})

# 默认隐藏符号以进行混淆
target_compile_options(musickiller PRIVATE -fvisibility=hidden)
