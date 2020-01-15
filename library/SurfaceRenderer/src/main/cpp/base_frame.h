

#define FMT_NV21 17
#define FMT_RGBA 0x2A

#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX

#define ROTATE_90 90
#define ROTATE_270 270

bool support_fmt(int fmt);

float get_unit_pixel(int fmt);

bool buffer_size_right(int width, int height, int fmt, int size);

int anytorgbx(int fmt, int w, int h, unsigned char *src, unsigned char *rgbx);