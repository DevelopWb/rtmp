package org.easydarwin.easypusher.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.juntai.wisdom.basecomponent.utils.NotificationTool;
import com.orhanobut.hawk.Hawk;


public class BackgroundService extends Service {
    private static final int NOTIFICATION_ID = 1;

    private WindowManager mWindowManager;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private static final String TAG = BackgroundService.class.getSimpleName()+"push_background";


    private void backGroundNotificate() {
        startForeground(NOTIFICATION_ID, NotificationTool.getNotification(this));
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create new SurfaceView, set its size to 1x1, move it to the top left
        // corner and set this service as a callback
        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        backGroundNotificate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY ;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);

        super.onDestroy();
    }

}
