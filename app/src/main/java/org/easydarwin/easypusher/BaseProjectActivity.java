package org.easydarwin.easypusher;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/5/5 20:47
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/5 20:47
 */
public abstract class BaseProjectActivity extends RxAppCompatActivity {

    protected boolean isPushingStream = false;//是否正在推流
    protected boolean isPushingBiliStream = false;//是否正在推流
    protected boolean isPushingHuyaStream = false;//是否正在推流
    protected boolean isPushingYiStream = false;//是否正在推流
    protected boolean isPushingNowStream = false;//是否正在推流

    public abstract void onUvcCameraConnected();

    public abstract void onUvcCameraAttached();

    public abstract void onUvcCameraDisConnected();

    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContext = null;
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivedStringMsg(String msg) {
        switch (msg) {
            case "onAttach":
//                Toast.makeText(getApplicationContext(),"Attached",Toast.LENGTH_SHORT).show();
                onUvcCameraAttached();
                break;
            case "onConnect":
//                Toast.makeText(getApplicationContext(),"connect",Toast.LENGTH_SHORT).show();
                onUvcCameraConnected();
                break;
            case "onDisconnect":
//                Toast.makeText(getApplicationContext(),"disconnect",Toast.LENGTH_SHORT).show();

                onUvcCameraDisConnected();
                break;
            default:
                break;
        }
    }


}
