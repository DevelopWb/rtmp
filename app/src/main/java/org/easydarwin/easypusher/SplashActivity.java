/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.easypusher;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.basenetlib.RequestStatus;
import com.basenetlib.util.NetWorkUtil;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.bean.AppInfoBean;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.easydarwin.easypusher.mine.LiveBean;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

/**
 * 启动页
 */
public class SplashActivity extends BaseProjectActivity implements RequestStatus {
    private RegLatestPresent present;

    @Override
    public void onUvcCameraConnected() {
//        Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUvcCameraAttached() {
//        Toast.makeText(getApplicationContext(),"Attached888",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUvcCameraDisConnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hawk.put(HawkProperty.HIDE_FLOAT_VIEWS,false);
        initPlatform();
        present = new RegLatestPresent();
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
              };
        SPUtil.setBitrateKbps(this,SPUtil.BITRATEKBPS);
        setContentView(R.layout.splash_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏

        new RxPermissions(this)
                .request(permissions)
                .delay(1, TimeUnit.SECONDS)
                .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            if (!NetWorkUtil.isNetworkAvailable()) {
                                ToastUtils.toast(mContext,"网络连接异常，请检查手机网络！");
                               return;
                            }
                            //获取软件的key
                            present.getAppVersionInfoAndKeyFromService(RegLatestContact.GET_KEY, SplashActivity.this);

                        } else {
                            //有一个权限没通过
                            finish();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });



    }

    /**
     * 初始化平台数据
     */
    private void initPlatform() {
        List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);
        if (arrays == null) {
            arrays = new ArrayList<>();
            arrays.add(new LiveBean(SettingActivity.LIVE_TYPE_BILI, R.mipmap.bilibili_off, true, 0));
            arrays.add(new LiveBean(SettingActivity.LIVE_TYPE_HUYA, R.mipmap.huya_off, true, 0));
            if (PublicUtil.isMoreThanTheAndroid10()) {
                arrays.add(new LiveBean(SettingActivity.LIVE_TYPE_DOUYU, R.mipmap.douyu_live_off, true, 0));
                arrays.add(new LiveBean(SettingActivity.LIVE_TYPE_XIGUA, R.mipmap.xigua_live_off, true, 0));
                arrays.add(new LiveBean(SettingActivity.LIVE_TYPE_YI, R.mipmap.yi_live_off, true, 0));
            }
            Hawk.put(HawkProperty.PLATFORMS,arrays);
        }
    }

    @Override
    public void onStart(String tag) {

    }

    @Override
    public void onSuccess(Object o, String tag) {
        //获取key
        AppInfoBean appInfoBean = (AppInfoBean) o;
        if (appInfoBean != null) {
            if (appInfoBean.getModel() != null && appInfoBean.getModel().size() > 0) {
                AppInfoBean.ModelBean dataBean = appInfoBean.getModel().get(0);
                String key = dataBean.getSoftDescription();
                if (key != null) {
                    Hawk.put(HawkProperty.APP_KEY, key);
//                    startService(new Intent(SplashActivity.this, UVCCameraService.class));
//                    //所有权限通过
//                    try {
//                        Thread.sleep(600);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    startActivity(new Intent(SplashActivity.this, StreamActivity.class));
                    finish();
                }else {
                    ToastUtils.toast(this,"参数初始化失败");
                }
            }
        }
    }

    @Override
    public void onError(String tag) {

    }
}
