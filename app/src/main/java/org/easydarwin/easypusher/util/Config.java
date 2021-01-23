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
    public static final String DEFAULR_IP = "47.104.110.95";
    //    private static final String DEFAULT_SERVER_URL = "rtmp://demo.easydss.com:10085/live/stream_"+String.valueOf((int) (Math.random() * 1000000 + 100000));

    public static String getServerURL() {

        String url_head = "rtmp://";
        String ip = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_IP, DEFAULR_IP);
        String port = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_PORT, "10085");
        String tag = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_TAG, "");
        String regCode =Hawk.get(HawkProperty.REG_CODE);
        return String.format("%s%s%s%s%s%s%s%s", url_head, ip, ":", port, "/hls/", regCode, "?sign=", tag);
    }


    public static String recordPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "手机监控系统";
    }
}
