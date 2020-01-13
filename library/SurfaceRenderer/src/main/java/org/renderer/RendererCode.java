package org.renderer;

public interface RendererCode {

    /** 不支持的渲染格式 */
    int UNSUPPORT_FMT       = -1;

    /** 渲染器无效, 有可能还未创建, 也有可能是销毁后仍然去调用渲染方法 */
    int RENDERER_INVALID    = -2;

    /** Surface无效, 有可能是未设置Surface, 也有可能是Surface被销毁了 */
    int SURFACE_INVALID     = -3;

    /** Surface锁定渲染窗口时无效 */
    int WINDOWS_INVALID     = -4;

    /** 创建的SurfaceRenderer的格式, 分辨率, 与刷新的帧数据长度对应不上 */
    int BAD_FRAME_SIZE      = -5;

    /** window在锁定Surface的时候锁定失败 */
    int LOCK_SURFACE_FAIL   = -6;
}
