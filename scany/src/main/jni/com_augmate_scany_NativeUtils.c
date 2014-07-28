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

// front-facing cameras are mirrored, so we must manually flip horizontally here
JNIEXPORT void JNICALL Java_com_augmate_scany_NativeUtils_binarizeToIntBuffer(
    JNIEnv *env, jclass unused, jbyteArray srcArray, jintArray dstArray, jint width, jint height
) {
    jbyte* src = (*env)->GetByteArrayElements(env, srcArray, NULL);
    jint* dst = (*env)->GetIntArrayElements(env, dstArray, NULL);

    int x, y;
    for(y = 0; y < height; y ++) {
        for(x = 0; x < width; x ++) {
            int value = (src[y * width + x] & 0xFF) < 80 ? 0 : 255;
            //dst[y * width + (width - x - 1)] = 0xff000000 | ((value << 8) & 0x0000ff00); // mirrored
            dst[y * width + x] = 0xff000000 | ((value << 8) & 0x0000ff00);
        }
    }

    (*env)->ReleaseByteArrayElements(env, srcArray, src, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);

    return;
}