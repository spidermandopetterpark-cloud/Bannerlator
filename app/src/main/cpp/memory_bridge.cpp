#include "memory.h"

#include <jni.h>
#include <stdint.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_star_core_Memory_nativeGetTotalMemory(
        JNIEnv* env,
        jclass clazz) {

    (void)env;
    (void)clazz;

    return static_cast<jlong>(
        Memory::GetTotalCombinedMemory()
    );
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_star_core_Memory_nativeGetAvailableMemory(
        JNIEnv* env,
        jclass clazz) {

    (void)env;
    (void)clazz;

    return static_cast<jlong>(
        Memory::GetAvailableCombinedMemory()
    );
}
