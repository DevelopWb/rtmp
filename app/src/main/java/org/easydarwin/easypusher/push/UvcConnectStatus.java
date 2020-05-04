package org.easydarwin.easypusher.push;

/**
 * @Author: tobato
 * @Description: 作用描述  uvc摄像头连接状态
 * @CreateDate: 2020/4/28 20:13
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/4/28 20:13
 */
public interface UvcConnectStatus {


    /**
     * 摄像头连接
     */
    void  onUvcCameraConnected();

    /**
     * attach
     */
    void  onUvcCameraAttached();

    /**
     * 摄像头断开
     */
    void  onUvcCameraDisConnected();
}
