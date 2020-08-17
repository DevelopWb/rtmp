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
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.bean.AppInfoBean;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.push.UVCCameraService;
import org.easydarwin.easypusher.util.SPUtil;

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
