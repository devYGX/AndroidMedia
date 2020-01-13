#include "../base_frame.h"
#include "../base.h"

static unsigned char sat(int value){
    return (unsigned int) value < 0 ? 0 : (value > 0xFF ? 0xFF : value);
}

bool support_fmt(int fmt){
    switch (fmt){
        case FMT_NV21:
            return true;
    }
    return false;
}

float get_unit_pixel(int fmt){
    switch (fmt){
        case FMT_NV21:
            return 1.5F;
    }
    return 0;
}

bool buffer_size_right(int width, int height, int fmt, int size){
    switch (fmt){
        case FMT_NV21:
            return width * height * 3 / 2 == size;
    }
    return false;
}

static int nv21_2_rgbx(int w, int h, unsigned char *nv21, unsigned char *rgbx){
    /*
     NV21: w: 4, h:4; bytes_data: 4 * 4 * 3 / 2 = 24
     Y00 Y01 Y10  Y11 公用U00 V00分量

     Y00 Y01 Y02 Y03
     Y10 Y11 Y12 Y13
     Y20 Y21 Y22 Y23
     Y30 Y31 Y32 Y33
     U00 V00 U01 V01
     U10 V11 U11 V11
     */
    int i = 0;
    int j;
    // 这里的y00, y01 y10 y11, u, v和转换出来的r, g,b 建议都使用int类型, 如果使用char类型, 可能会造成数据越界,
    // 导致计算结果离实际期望效果出现比较大的偏差, 最终导致图片显示异常
    int y00;
    int y01;
    int y10;
    int y11;
    int u;
    int v;
    for(; i < h; i += 2){
        j = 0;
        for(; j < w; j += 2){
            y00 = nv21[i * w + j];
            y01 = nv21[i * w + j + 1];

            y10 = nv21[(i + 1) * w + j];
            y11 = nv21[(i + 1) * w + j + 1];

            v = nv21[(h + i / 2) * w + j];
            u = nv21[(h + i / 2) * w + j + 1];

            int r = 0;
            int g = 0;
            int b = 0;
            if(v != 0 || u != 0){

                /*
                // 支持
                r = 1.4075 * (v - 128);    // v to r;
                g = - 0.3455 * (u - 128) - 0.7169 * (v - 128);    // v and u to g;
                b = 1.779 * (u - 128);    // u to b; */



                r = (22987 * (v - 128)) >> 14;    // v to r;
                g = (-5636 * (u - 128) - 11698 * (v - 128)) >> 14;    // v and u to g;
                b = (29049 * (u - 128)) >> 14;    // u to b;

                /*
                r = (360 * (v - 128)) >> 8;    // v to r;
                g = (-88 * (u - 128) - 174 * (v - 128)) >> 8;    // v and u to g;
                b = (455 * (u - 128)) >> 8;    // u to b;*/
            }


            rgbx[i * w * 4 + j * 4 + 0] = sat(y00 + r);
            rgbx[i * w * 4 + j * 4 + 1] = sat(y00 + g);
            rgbx[i * w * 4 + j * 4 + 2] = sat(y00 + b);
            rgbx[i * w * 4 + j * 4 + 3] = 0xff;

            rgbx[i * w * 4 + (j + 1) * 4 + 0] = sat(y01 + r);
            rgbx[i * w * 4 + (j + 1) * 4 + 1] = sat(y01 + g);
            rgbx[i * w * 4 + (j + 1) * 4 + 2] = sat(y01 + b);
            rgbx[i * w * 4 + (j + 1) * 4 + 3] = 0xff;

            rgbx[(i + 1) * w * 4 + j * 4 + 0] = sat(y10 + r);
            rgbx[(i + 1) * w * 4 + j * 4 + 1] = sat(y10 + g);
            rgbx[(i + 1) * w * 4 + j * 4 + 2] = sat(y10 + b);
            rgbx[(i + 1) * w * 4 + j * 4 + 3] = 0xff;

            rgbx[(i + 1) * w * 4 + (j + 1) * 4 + 0] = sat(y11 + r);
            rgbx[(i + 1) * w * 4 + (j + 1) * 4 + 1] = sat(y11 + g);
            rgbx[(i + 1) * w * 4 + (j + 1) * 4 + 2] = sat(y11 + b);
            rgbx[(i + 1) * w * 4 + (j + 1) * 4 + 3] = 0xff;
        }
    }
    return 0;
}

int anytorgbx(int fmt, int w, int h, unsigned char *src, unsigned char *rgbx){
    switch (fmt){
        case FMT_NV21:
            return nv21_2_rgbx(w,h,src,rgbx);
        default:
            return UNSUPPORT_FMT;
    }
}