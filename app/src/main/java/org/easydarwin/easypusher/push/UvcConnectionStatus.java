package org.easydarwin.easypusher.push;

/**
 * @Author: tobato
 * @Description: 作用描述  uvc摄像机连接状态回调
 * @CreateDate: 2020/5/23 21:46
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/23 21:46
 */
public interface UvcConnectionStatus {

    void onUvcCameraConnected();
    void onUvcCameraAttached();
    void onUvcCameraDisConnected();
}
