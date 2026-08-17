cmake_minimum_required(VERSION 3.22.1)

project(Winlator)

# ============================================================
# Global compiler configuration
# ============================================================

# Disable clang-tidy (AGP enables it by default but its include
# paths don't work with NDK cross-compilation for the lsfg-vk
# dependency).

set(CMAKE_C_FLAGS
        "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration"
)

set(CMAKE_CXX_FLAGS
        "${CMAKE_CXX_FLAGS} -O2"
)

# ============================================================
# External dependencies
# ============================================================

add_subdirectory(OpenXR-SDK)
add_subdirectory(patchelf)
add_subdirectory(adrenotools)

# lsfg-vk is built separately by build-lsfg.yml
# and its .so is copied to jniLibs.
#
# add_subdirectory(lsfg-vk)


# ============================================================
# Main Winlator native library
# ============================================================

add_library(winlator SHARED

        # OpenXR
        xr/engine.c
        xr/framebuffer.c
        xr/input.c
        xr/main.c
        xr/math.c
        xr/renderer.c

        # Winlator native components
        winlator/drawable.c
        winlator/gpu_image.c
        winlator/sysvshared_memory.c
        winlator/xconnector_epoll.c
        winlator/alsa_client.c
        winlator/vulkan.c
        winlator/perf_turbo.c

        # Memory information
        memory.cpp
)

target_compile_options(winlator PRIVATE
        -Wall
        -Wextra
)

# Allow memory.cpp to include memory.h
target_include_directories(winlator PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(winlator
        log
        android
        jnigraphics
        openxr_loader
        aaudio
        EGL
        GLESv2
        GLESv3
        adrenotools
)


# ============================================================
# DrawableBufferPool native heap
# ============================================================

add_library(drawablepool SHARED
        winlator/drawablepool.c
)

target_compile_options(drawablepool PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(drawablepool
        log
)


# ============================================================
# Fake Input
# ============================================================

add_library(fakeinput SHARED
        winlator/fakeinput.cpp
)

target_compile_options(fakeinput PRIVATE
        -Wall
        -Wextra
        -fvisibility=hidden
)

target_link_libraries(fakeinput
        log
        dl
)


# ============================================================
# Vulkan renderer
# ============================================================

add_library(vulkan_renderer SHARED

        winlator/vulkan_jni.cpp
        winlator/VulkanRendererContext.cpp
        winlator/VulkanRendererScanout.cpp

        scanout/ScanoutContext.cpp
)

target_compile_options(vulkan_renderer PRIVATE
        -Wall
        -Wextra
        -fvisibility=hidden
        -std=c++17
)

target_link_libraries(vulkan_renderer
        log
        android
        vulkan
        adrenotools
        dl
        atomic
)


# ============================================================
# SurfaceFlinger / ASurfaceRenderer
# ============================================================

add_library(asurface_renderer SHARED

        asurfacerenderer/asurface_jni.cpp
        asurfacerenderer/ASurfaceRendererContext.cpp
        asurfacerenderer/blit_converter.cpp
)

target_compile_options(asurface_renderer PRIVATE
        -Wall
        -Wextra
        -fvisibility=hidden
        -std=c++17
)

target_link_libraries(asurface_renderer
        log
        android
        dl
        atomic
        EGL
        GLESv3
)


# ============================================================
# AHBImage
# ============================================================

add_library(ahbimage SHARED

        asurfacerenderer/ahbimage.c
)

target_compile_definitions(ahbimage PRIVATE
        EGL_EGLEXT_PROTOTYPES
        GL_GLEXT_PROTOTYPES
)

target_compile_options(ahbimage PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(ahbimage
        log
        android
        EGL
        GLESv2
)


# ============================================================
# Direct Scanout
# ============================================================

add_library(direct_scanout SHARED

        scanout/directscanout_jni.cpp
        scanout/ScanoutContext.cpp
)

target_compile_options(direct_scanout PRIVATE
        -Wall
        -Wextra
        -fvisibility=hidden
        -std=c++17
)

target_link_libraries(direct_scanout
        log
        android
        dl
        atomic
)


# ============================================================
# PulseAudio sink controller
# ============================================================

add_library(pasink SHARED

        pasink/pasink.c
)

target_compile_options(pasink PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(pasink
        log
        dl
)
