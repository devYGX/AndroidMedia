
#define FMT_NV21 17
#define FMT_I420 1

#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX

#define ROTATE_0 0
#define ROTATE_90 90
#define ROTATE_180 180
#define ROTATE_270 270

bool support_fmt(int fmt);

bool buffer_size_right(int width, int height, int fmt, int size);

int anytorgba(int fmt, int width, int height, int degree, uint8_t *src, uint8_t *rgbx);