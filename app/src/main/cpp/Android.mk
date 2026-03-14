LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := LUXPRO
LOCAL_SRC_FILES := main.cpp
LOCAL_LDLIBS    := -llog -landroid

# Include KittyMemory path if needed, here we assume it's in the same dir or subfolder
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/KittyMemory

include $(BUILD_SHARED_LIBRARY)
