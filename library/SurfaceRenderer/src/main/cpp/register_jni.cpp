
#include "base.h"
#include <android/native_window_jni.h>
#include <stdio.h>
#include "SurfaceRenderer.h"
#include "base_frame.h"

#define CLASS_NAME  "org/renderer/SurfaceRenderer"

static jlong
nativeCreate(JNIEnv *env, jobject obj, jint width, jint height, jint fmt) {
    if (!support_fmt(fmt)) {
        THROW_RUNTIME_EXCEPTION(env, "unsupport format");
    }

    SurfaceRenderer *renderer = new SurfaceRenderer(width, height, fmt);
    return reinterpret_cast<jlong>(renderer);
}

static jint nativeRelease(JNIEnv *env, jobject obj, jlong ptr) {
    if (IS_NULL(ptr)) {
        return RENDERER_INVALID;
    }
    SurfaceRenderer *renderer = reinterpret_cast<SurfaceRenderer *>(ptr);
    renderer->release();
    delete (renderer);
    return 0;
}

static jint nativeRefreshFrame(JNIEnv *env, jobject obj, jlong ptr, jbyteArray frame) {

    jbyte *data = env->GetByteArrayElements(frame, 0);
    jsize size = env->GetArrayLength(frame);

    unsigned char *frame_data = reinterpret_cast<unsigned char *>(data);
    size_t frame_size = (size_t) size;
    SurfaceRenderer *renderer = reinterpret_cast<SurfaceRenderer *>(ptr);
    int ret = renderer->refreshFrame(frame_data, frame_size);
    env->ReleaseByteArrayElements(frame, data, 0);
    return ret;
}

static jint nativeSetSurface(JNIEnv *env, jobject obj, jlong ptr, jobject surface) {
    if (IS_NULL(surface)) {
        return SURFACE_INVALID;
    }
    if (IS_NULL(ptr)) {
        return RENDERER_INVALID;
    }
    SurfaceRenderer *renderer = reinterpret_cast<SurfaceRenderer *>(ptr);
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        return WINDOWS_INVALID;
    }
    // LOGD("fromSurface: %p, renderer:%p ", window, renderer);
    renderer->setSurface(window);
    return 0;
}

static JNINativeMethod methods[] = {
        {"nativeCreate",       "(III)J",                     (void *) nativeCreate},
        {"nativeRelease",      "(J)V",                       (void *) nativeRelease},
        {"nativeSetSurface",   "(JLandroid/view/Surface;)I", (void *) nativeSetSurface},
        {"nativeRefreshFrame", "(J[B)I",                     (void *) nativeRefreshFrame}
};

jint register_native_methods(JNIEnv *env, const char *class_name, JNINativeMethod *methods,
                             int num_methods) {
    int result = 0;
    jclass clazz = env->FindClass(class_name);

    // Find Class Success
    if (clazz) {
        result = env->RegisterNatives(clazz, methods, num_methods);
        if (result < 0) {
            LOGE("Register Natives Fail: %d", result);
        }
    } else {
        LOGE("Find Class Fail");
    }
    return result;
}

int register_jni(JNIEnv *env) {

    if (register_native_methods(env, CLASS_NAME, methods, ARRAY_LENGTH(methods)) < 0) {
        return -1;
    }
    return 0;
}