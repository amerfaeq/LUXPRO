# LUX PRO - Native Injection Master

This repository contains the core "Native Injection" logic and bridge for the LUX PRO application.

## Core Files
- **main.cpp**: Native Master Bridge with `JNI_OnLoad` PID detection and `KittyMemory` patching.
- **Android.mk / Application.mk**: NDK build scripts for dual-ABI support (arm64-v8a, v7a).
- **FloatingMenuService.java**: UI bridge with direct native methods and FOREGROUND_SERVICE stability.
- **.github/workflows/build.yml**: Automated CI/CD pipeline using NDK r25b.

## Features
1. Automatic Entry Bot
2. Secure Bridge (Active Detection)
3. Direct Win Sequence
4. Master Auto Play

## Build Instructions
Use `ndk-build` or let the GitHub Actions workflow compile the APK automatically.
