cmake_minimum_required(VERSION 3.10.2)

project("LUXPRO")

add_library( LUXPRO SHARED

             # Core Engine
             core/core_engine.cpp
             core/jni_bridge.cpp
             core/auto_play.cpp
             core/sentinel.cpp
             core/bot_automation.cpp
             core/hook_manager.cpp

             # Security Layer
             security/ghost_protocol.cpp
             security/crypto_layer.cpp
             security/polymorphic_engine.cpp
             security/anti_telemetry.cpp

             # VFX / Visual Mastery
             vfx/vfx_shader.cpp

             # UI & Hooks
             native-lib.cpp
             lux_os.cpp
             Menu.cpp

             # ImGui
             imgui/imgui.cpp
             imgui/imgui_demo.cpp
             imgui/imgui_draw.cpp
             imgui/imgui_tables.cpp
             imgui/imgui_widgets.cpp
             imgui/backends/imgui_impl_android.cpp
             imgui/backends/imgui_impl_opengl3.cpp
           )

find_package(shadowhook REQUIRED CONFIG)

find_library(log-lib  log)
find_library(gles-lib GLESv3)
find_library(egl-lib  EGL)

target_link_libraries( LUXPRO
                       android
                       ${log-lib}
                       ${gles-lib}
                       ${egl-lib}
                       shadowhook::shadowhook )

# ASan Linker Flags
set_target_properties(LUXPRO PROPERTIES LINK_FLAGS "-fsanitize=address")

# Include Directories
target_include_directories( LUXPRO PUBLIC 
                            ${CMAKE_CURRENT_SOURCE_DIR}
                            ${CMAKE_CURRENT_SOURCE_DIR}/imgui
                            ${CMAKE_CURRENT_SOURCE_DIR}/imgui/backends )