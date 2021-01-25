/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.easypusher.util;

import android.os.Environment;

import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.orhanobut.hawk.Hawk;

import java.io.File;

/**
 * 推流地址的常量类
 */
public class Config {

    private static final String SERVER_URL = "serverUrl";
    public static final String DEFAULR_IP = "58.49.46.179";
    public static final String DEFAULR_PORT = "10935";
//rtmp://58.49.46.179:10935/hls/cze8ZEHR
    public static String getServerURL() {

        String url_head = "rtmp://";
        String ip = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_IP, DEFAULR_IP);
        String port = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_PORT, DEFAULR_PORT);
//        String tag = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_TAG, "");
        String regCode =Hawk.get(HawkProperty.REG_CODE);
        return String.format("%s%s%s%s%s%s", url_head, ip, ":", port, "/hls/", regCode );
    }


    public static String recordPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "手机监控系统";
    }
}
