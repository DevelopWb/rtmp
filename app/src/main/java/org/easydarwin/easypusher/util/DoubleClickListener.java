package org.easydarwin.easypusher.util;

import android.view.View;

/**
 * Author:wang_sir
 * Time:2020/3/20 21:58
 * Description:This is DoubleClickListener
 */
public abstract class DoubleClickListener implements View.OnClickListener{
    private static final long DOUBLE_TIME = 1000;
    private static long lastClickTime = 0;

    @Override
    public void onClick(View v) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastClickTime < DOUBLE_TIME) {
            onDoubleClick(v);
        }else{
            onOneClick(v);
        }
        lastClickTime = currentTimeMillis;
    }
    public abstract void onDoubleClick(View v);
    public abstract void onOneClick(View v);
}
