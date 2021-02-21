/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.easypusher;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.basenetlib.RequestStatus;
import com.basenetlib.util.NetWorkUtil;
import com.gyf.barlibrary.ImmersionBar;
import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.juntai.wisdom.basecomponent.utils.GsonTools;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.LogUtil;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.bean.AppInfoBean;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.easydarwin.easypusher.auth.OneKeyLoginActivity;
import org.easydarwin.easypusher.bean.LiveBean;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

/**
 * 启动页
 */
public class SplashActivity extends BaseProjectActivity implements RequestStatus, View.OnClickListener {
    private RegLatestPresent present;
    /**
     * 登录
     */
    private TextView mLogin;
    private LinearLayout mLoginByMobileLl;

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
        ImmersionBar mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarColor(R.color.gray_light).statusBarDarkFont(true).init();
        Hawk.put(HawkProperty.HIDE_FLOAT_VIEWS, false);
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
        SPUtil.setBitrateKbps(this, SPUtil.BITRATEKBPS);
        setContentView(R.layout.splash_activity);
        initView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏
        if (!NetWorkUtil.isNetworkAvailable()) {
            new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setMessage("网络连接异常，请检查手机网络或系统时间！")
                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityManagerTool.getInstance().finishApp();
                        }
                    }).show();
            return;
        }
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

    /**
     * 初始化平台数据
     */
    private void initPlatform() {
        List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);
        if (arrays == null) {
            arrays = new ArrayList<>();
            arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_BILI, R.mipmap.bilibili_off, true, 0)
                    .setUrlHead(getString(R.string.biliurl)));
            arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_HUYA, R.mipmap.huya_off, true, 0)
                    .setUrlHead(getString(R.string.huyaurl)));
            if (PublicUtil.isMoreThanTheAndroid10()) {
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_DOUYU, R.mipmap.douyu_live_off, true, 0)
                        .setUrlHead(getString(R.string.douyuurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_XIGUA, R.mipmap.xigua_live_off, true, 0)
                        .setUrlHead(getString(R.string.xiguaurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_YI, R.mipmap.yi_live_off, true, 0)
                        .setUrlHead(getString(R.string.yiurl)));
            } else {
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_DOUYU, R.mipmap.douyu_live_off, false, 0)
                        .setUrlHead(getString(R.string.douyuurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_XIGUA, R.mipmap.xigua_live_off, false, 0)
                        .setUrlHead(getString(R.string.xiguaurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_YI, R.mipmap.yi_live_off, false, 0)
                        .setUrlHead(getString(R.string.yiurl)));
            }
            Hawk.put(HawkProperty.PLATFORMS, arrays);
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
                    //                    boolean isAgree = Hawk.get(HawkProperty.AGREE_PROTOCAL, false);
                    //                    if (!isAgree) {
                    //                        showAgreementAlter();
                    //                    } else {
                    //                        startActivity(new Intent(SplashActivity.this, StreamActivity.class));
                    //                        finish();
                    //                    }
                } else {
                    ToastUtils.toast(this, "参数初始化失败");
                }
            }
        }
    }

    @Override
    public void onError(String tag) {

    }

    private void showAgreementAlter() {
        Intent intentAgreement = new Intent(this, UserAgreementActivity.class);
        SpannableStringBuilder spannable = new SpannableStringBuilder(getString(R.string.agreement_xieyi_tag));
        // 在设置点击事件、同时设置字体颜色
        ClickableSpan clickableSpanOne = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                LogUtil.e("isGo", "点击了用户协议");
                intentAgreement.putExtra("url", getString(R.string.user_xieyi_url));
                startActivity(intentAgreement);
            }

            @Override
            public void updateDrawState(TextPaint paint) {
                paint.setColor(getResources().getColor(R.color.colorTheme));
                // 设置下划线 true显示、false不显示
                paint.setUnderlineText(false);
            }
        };
        ClickableSpan clickableSpanTwo = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                intentAgreement.putExtra("url", getString(R.string.secret_xieyi_url));
                startActivity(intentAgreement);
            }

            @Override
            public void updateDrawState(TextPaint paint) {
                paint.setColor(getResources().getColor(R.color.colorTheme));
                // 设置下划线 true显示、false不显示
                paint.setUnderlineText(false);
            }
        };
        spannable.setSpan(clickableSpanOne, 50, 56, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(clickableSpanTwo, 57, 63, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


        AgreementDialog agreementDialog = new AgreementDialog(this).builder();
        agreementDialog.getContentTextView().setMovementMethod(LinkMovementMethod.getInstance());
        agreementDialog.setCanceledOnTouchOutside(false)
                .setTitle("服务协议和隐私政策")
                .setContent(spannable)
                .setCancelButton("暂不使用", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                })
                .setOkButton("同意并进入", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Hawk.put(HawkProperty.AGREE_PROTOCAL, true);
                        startActivity(new Intent(mContext, StreamActivity.class));
                        finish();
                    }
                }).show();
    }

    private void initView() {
        mLogin = (TextView) findViewById(R.id.login);
        mLogin.setOnClickListener(this);
        mLoginByMobileLl = (LinearLayout) findViewById(R.id.login_by_mobile_ll);
        mLoginByMobileLl.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.login:
                break;
            case R.id.login_by_mobile_ll:
                startActivity(new Intent(this, OneKeyLoginActivity.class));
                break;
        }
    }


}
