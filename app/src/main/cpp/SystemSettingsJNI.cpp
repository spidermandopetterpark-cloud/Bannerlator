#include <jni.h>

#include "SystemSettings.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_titanpc_system_SystemSettings_nativeGetAvailableMemoryMB(
        JNIEnv* env,
        jobject obj) {

    return static_cast<jlong>(
            TitanPC::SystemSettings::getAvailableMemoryMB()
    );
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_titanpc_system_SystemSettings_nativeSetMemorySizeMB(
        JNIEnv* env,
        jobject obj,
        jint memoryMB) {

    return TitanPC::SystemSettings::setMemorySizeMB(
            static_cast<uint64_t>(memoryMB)
    );
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_titanpc_system_SystemSettings_nativeSetSwapSizeMB(
        JNIEnv* env,
        jobject obj,
        jint swapMB) {

    return TitanPC::SystemSettings::setSwapSizeMB(
            static_cast<uint64_t>(swapMB)
    );
}
