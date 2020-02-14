#include <jni.h>
#include <android/log.h>
#include "errorcode.h"


#define TAG "surface_renderer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


#define ARRAY_LENGTH(array)    ((int) sizeof(array) / sizeof(array[0]))
// int __android_log_print(int prio, const char *tag,  const char *fmt, ...)
#define THROW_RUNTIME_EXCEPTION(env, msg) {\
    (env)->ThrowNew((env)->FindClass("java/lang/RuntimeException"), (msg));\
}

#define IS_NULL(x) (x == NULL)

#define FREE(x) {free(x)}
#define DELETE(x) {delete(x)}