
#include "base.h"
#include <android/native_window_jni.h>
#include <pthread.h>

#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX


class SurfaceRenderer {
private:
    int width;
    int height;
    int fmt;
    unsigned char *rgbx;
    pthread_mutex_t lock;
    ANativeWindow *window;

public:
    SurfaceRenderer(int width, int height, int fmt);

    ~SurfaceRenderer();

    int setSurface(ANativeWindow *surface_window);

    void release();

    int refreshFrame(unsigned char *frame, size_t size);

};