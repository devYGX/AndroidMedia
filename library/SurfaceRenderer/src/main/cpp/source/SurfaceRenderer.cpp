
#include "SurfaceRenderer.h"
#include "base_frame.h"
#include <stdlib.h>
#include <memory.h>

SurfaceRenderer::SurfaceRenderer(int width, int height, int fmt, int degree) :
        width(width),
        height(height),
        fmt(fmt),
        degree(degree),
        renderWidth(0),
        renderHeight(0),
        window(NULL) {
    pthread_mutex_init(&lock, NULL);
    LOGD("---SurfaceRenderer:%d %d", width, height);
    rgbx = (unsigned char *) malloc(sizeof(unsigned char) * width * height * PREVIEW_PIXEL_BYTES);
}

SurfaceRenderer::~SurfaceRenderer() {

    release();
    pthread_mutex_destroy(&lock);
    LOGD("~SurfaceRenderer");
}

int SurfaceRenderer::setSurface(ANativeWindow *surface_window) {
    LOGD("setSurface %p, %d", window, degree);
    pthread_mutex_lock(&lock);
    if (!IS_NULL(window)) {
        ANativeWindow_release(window);
        window = NULL;
    }
    window = surface_window;
    // WINDOW_FORMAT_RGBA_8888
    // WINDOW_FORMAT_RGB_565
    // 1表示MJPEG, 对应的是RGBX的色彩模式, 一个像素需要4个字节来表示;
    if (degree == ROTATE_90 || degree == ROTATE_270) {
        ANativeWindow_setBuffersGeometry(surface_window, renderWidth = height, renderHeight = width,
                                         WINDOW_FORMAT_RGBA_8888);
    } else {
        ANativeWindow_setBuffersGeometry(surface_window, renderWidth = width, renderHeight = height,
                                         WINDOW_FORMAT_RGBA_8888);
    }
    LOGD("setSurface: %d %d", renderWidth, renderHeight);
    pthread_mutex_unlock(&lock);

    return 0;
}


void SurfaceRenderer::release() {
    pthread_mutex_lock(&lock);
    // todo
    LOGD("release");
    if (!IS_NULL(rgbx)) {
        free(rgbx);
        rgbx = NULL;
    }
    pthread_mutex_unlock(&lock);
}

static void
copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src,
          const int stride_dest) {
    // 一次复制8行
    const int h8 = height % 8;
    // LOGD("h8: %d, width: %d; height: %d, stride_src: %d, stride_dest: %d\n",h8,width,height,stride_src,stride_dest);
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
}


int SurfaceRenderer::refreshFrame(unsigned char *frame, size_t size) {

    if (!buffer_size_right(width, height, fmt, size)) {
        return BAD_FRAME_SIZE;
    }

    int ret = 0;
    pthread_mutex_lock(&lock);

    // ANativeWindow_Buffer
    ANativeWindow_Buffer buffer;

    if (IS_NULL(window)) {
        ret = SURFACE_INVALID;
        goto __FINALLY;
    }

    memset(rgbx, 0, width * height * PREVIEW_PIXEL_BYTES);
    ret = anytorgba(fmt, width, height, degree, frame, rgbx);
    // LOGE("after anytorgba: %d", ret);

    if (ret) {
        goto __FINALLY;
    }

    // ANativeWindow_lock
    if (ANativeWindow_lock(window, &buffer, NULL) == 0) {

        /* unsigned char *src = rgbx;
         const int src_w = renderWidth * PREVIEW_PIXEL_BYTES;
         const int src_step = renderWidth * PREVIEW_PIXEL_BYTES;

         unsigned char *dest = reinterpret_cast<unsigned char *>(buffer.bits);
         const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
         const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;

         const int w = src_w < dest_w ? src_w : dest_w;
         const int h = renderHeight < buffer.height ? renderHeight : buffer.height;
         copyFrame(src, dest, w, h, src_step, dest_step);*/
        uint8_t *dst = (uint8_t *) buffer.bits;
        uint8_t *src = (uint8_t *) rgbx;

        int dst_stride = buffer.stride * 4;
        int src_stride = renderWidth * 4;

        for (int i = 0; i < renderHeight; i++) {
            memcpy(dst + i * dst_stride, src + i * src_stride, src_stride);
        }

        ANativeWindow_unlockAndPost(window);
    } else {
        ret = LOCK_SURFACE_FAIL;
        goto __FINALLY;
    }


    __FINALLY:
    pthread_mutex_unlock(&lock);
    return ret;
}