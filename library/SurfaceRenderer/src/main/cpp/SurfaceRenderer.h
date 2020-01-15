
#include "base.h"
#include <android/native_window_jni.h>
#include <pthread.h>




class SurfaceRenderer {
private:
    int width;
    int height;
    int fmt;
    int degree;
    unsigned char *rgbx;
    pthread_mutex_t lock;
    ANativeWindow *window;

public:
    SurfaceRenderer(int width, int height, int fmt, int degree);

    ~SurfaceRenderer();

    int setSurface(ANativeWindow *surface_window);

    void release();

    int refreshFrame(unsigned char *frame, size_t size);

};