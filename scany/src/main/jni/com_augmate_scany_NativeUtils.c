#include <jni.h>
#include <stdio.h>

#if defined(__arm__)
  #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #define ABI "armeabi-v7a with NEON"
    #else
      #define ABI "armeabi-v7a"
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
   #define ABI "x86"
#elif defined(__mips__)
   #define ABI "mips"
#else
   #define ABI "unknown"
#endif

#include "com_augmate_scany_NativeUtils.h"

JNIEXPORT void JNICALL Java_com_augmate_scany_NativeUtils_binarize(
    JNIEnv *env, jclass unused, jbyteArray srcArray, jbyteArray dstArray, jint width, jint height
) {
    jbyte* src = (*env)->GetByteArrayElements(env, srcArray, NULL);
    jbyte* dst = (*env)->GetByteArrayElements(env, dstArray, NULL);

    int i;
    for(i = 0; i < width * height; i ++) {
        int value = (src[i] & 0xFF) < 80 ? 0 : 255;
        //dst[i] = 0xff000000 | ((value << 8) & 0x0000ff00);
        dst[i] = value & 0xFF;
    }

    (*env)->ReleaseByteArrayElements(env, srcArray, src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, dstArray, dst, 0);

    return;
}
