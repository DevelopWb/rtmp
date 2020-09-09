/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.easypusher.push;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.Utils.RegOperateManager;
import com.squareup.otto.Subscribe;

import org.easydarwin.bus.StartRecord;
import org.easydarwin.bus.StopRecord;
import org.easydarwin.bus.StreamStat;
import org.easydarwin.easypusher.BaseProjectActivity;
import org.easydarwin.easypusher.BuildConfig;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.SplashActivity;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.record.RecordService;
import org.easydarwin.easypusher.util.Config;
import org.easydarwin.easypusher.util.DoubleClickListener;

import com.juntai.wisdom.basecomponent.utils.HawkProperty;

import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;
import org.easydarwin.easyrtmp.push.EasyRTMP;
import org.easydarwin.update.UpdateMgr;
import org.easydarwin.util.BUSUtil;
import org.easydarwin.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.easydarwin.easyrtmp.push.EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_VALIDITY_PERIOD_ERR;

/**
 * 预览+推流等主页
 */
public class StreamActivity extends BaseProjectActivity implements View.OnClickListener,
        TextureView.SurfaceTextureListener {
    static final String TAG = "StreamActivity";
    private CharSequence[] resDisplay = new CharSequence[]{"640x480", "1280x720", "1920x1080", "2560x1440",
            "3840x2160"};
    private CharSequence[] resUvcDisplay = new CharSequence[]{"640x480", "1280x720", "1920x1080"};
    public static final int REQUEST_MEDIA_PROJECTION = 1002;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;

    TextView txtStreamAddress;
    TextView mSelectCameraTv;
    //    Spinner spnResolution;
    TextView txtStatus, streamStat;
    TextView textRecordTick;
    TextView mScreenResTv;//屏幕分辨率
    private UVCCameraService mUvcService;
    List<String> listResolution = new ArrayList<>();

    public MediaStream mMediaStream;
    public static Intent mResultIntent;
    public static int mResultCode;
    private UpdateMgr update;

    private BackgroundCameraService mService;
    private ServiceConnection conn = null;

    //    private boolean mNeedGrantedPermission;

    private static final String STATE = "state";
    private static final int MSG_STATE = 1;

    public static long mRecordingBegin;
    public static boolean mRecording;

    private long mExitTime;//声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private final static int UVC_CONNECT = 111;
    private final static int UVC_DISCONNECT = 112;

    public static boolean IS_VERTICAL_SCREEN = true;//是否是竖屏


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE:
                    String state = msg.getData().getString("state");
                    txtStatus.setText(state);
                    break;
                case UVC_CONNECT:

                    break;
                case UVC_DISCONNECT:
                    stopAllPushStream();
                    surfaceViewToNativeCamera();
                    int position = SPUtil.getScreenPushingCameraIndex(StreamActivity.this);
                    if (2 == position) {
                        position = 0;
                        SPUtil.setScreenPushingCameraIndex(StreamActivity.this, position);
                    }
                    switch (position) {
                        case 0:
                            mSelectCameraTv.setText("摄像头:后置");
                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
                            break;
                        case 1:
                            mSelectCameraTv.setText("摄像头:前置");
                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private ImageView startRecordIv;

    /**
     * 停止所有的推流
     */
    private void stopAllPushStream() {
        if (mMediaStream != null) {
            if (mMediaStream.isFirstPushStream) {
                startOrStopFirstPush();
            }
            if (mMediaStream.isSecendPushStream) {
                startOrStopSecendPush();
            }
            if (mMediaStream.isThirdPushStream) {
                startOrStopThirdPush();
            }
            if (mMediaStream.isFourthPushStream) {
                startOrStopFourthPush();
            }
        }
    }

    private Group mFloatViewGp;

    /**
     * 切换到原生摄像头
     */
    private void surfaceViewToNativeCamera() {
        Display mDisplay = getWindowManager().getDefaultDisplay();
        int W = mDisplay.getWidth();
        int H = mDisplay.getHeight();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
        params.height = H;
        params.width = W;
        surfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
        mSwitchOritation.setVisibility(View.VISIBLE);
    }

    // 录像时的线程
    private Runnable mRecordTickRunnable = new Runnable() {
        @Override
        public void run() {
            long duration = System.currentTimeMillis() - mRecordingBegin;
            duration /= 1000;

            textRecordTick.setText(String.format("%02d:%02d", duration / 60, (duration) % 60));

            if (duration % 2 == 0) {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_shape, 0, 0, 0);
            } else {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_interval_shape, 0,
                        0, 0);
            }

            textRecordTick.removeCallbacks(this);
            textRecordTick.postDelayed(this, 1000);
        }
    };


    private TextureView surfaceView;
    private ImageView mPushBgIv;
    private ImageView mSwitchOritation;
    private ImageView mFirstLiveIv;
    private ImageView mThirdLiveIv;
    private ImageView mFourthLiveIv, mFullScreenIv;
    private ImageView mVedioPushBottomTagIv;
    private ImageView mSecendLiveIv;
    private Intent uvcServiceIntent;
    private ServiceConnection connUVC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        BUSUtil.BUS.register(this);
        RegOperateManager.getInstance(this).setCancelCallBack(new RegLatestContact.CancelCallBack() {
            @Override
            public void toFinishActivity() {
                finish();
            }

            @Override
            public void toDoNext() {

            }
        });


    }


    /**
     * 初始化view
     */
    private void initView() {

        //        spnResolution = findViewById(R.id.spn_resolution);
        streamStat = findViewById(R.id.stream_stat);
        txtStatus = findViewById(R.id.txt_stream_status);
        mSelectCameraTv = findViewById(R.id.select_camera_tv);
        mSelectCameraTv.setOnClickListener(this);
        mSelectCameraTv.setText("摄像头:" + getSelectedCamera());
        txtStreamAddress = findViewById(R.id.txt_stream_address);
        textRecordTick = findViewById(R.id.tv_start_record);
        startRecordIv = findViewById(R.id.streaming_activity_record);
        mScreenResTv = findViewById(R.id.txt_res);
        surfaceView = findViewById(R.id.sv_surfaceview);
        //        mPushBgIv = (ImageView) findViewById(R.id.push_bg_iv);
        //        mPushBgIv.setOnClickListener(this);
        mSwitchOritation = (ImageView) findViewById(R.id.switch_oritation_iv);
        LinearLayout mRecordLl = (LinearLayout) findViewById(R.id.record_ll);
        mRecordLl.setOnClickListener(this);
        LinearLayout mSetLl = (LinearLayout) findViewById(R.id.set_ll);
        mSetLl.setOnClickListener(this);
        mSwitchOritation.setOnClickListener(this);
        mFirstLiveIv = (ImageView) findViewById(R.id.first_live_iv);
        mFirstLiveIv.setOnClickListener(this);
        mThirdLiveIv = (ImageView) findViewById(R.id.third_live_iv);
        mThirdLiveIv.setOnClickListener(this);
        mFourthLiveIv = (ImageView) findViewById(R.id.fourth_live_iv);
        mFourthLiveIv.setOnClickListener(this);
        mFullScreenIv = (ImageView) findViewById(R.id.video_record_full_screen_iv);
        mFullScreenIv.setOnClickListener(this);
        mSecendLiveIv = (ImageView) findViewById(R.id.secend_live_iv);
        mFloatViewGp = findViewById(R.id.float_views_group);
        mSecendLiveIv.setOnClickListener(this);
        mVedioPushBottomTagIv = findViewById(R.id.streaming_activity_push);
        String title = resDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 2)].toString();
        mScreenResTv.setText(String.format("分辨率:%s", title));
        initSurfaceViewClick();

        setPushLiveIv();
        if (PublicUtil.isMoreThanTheAndroid10()) {
            setViewsVisible(mThirdLiveIv, mFourthLiveIv);
        } else {
            setViewsInvisible(true, mThirdLiveIv, mFourthLiveIv);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(new Intent(this, BackgroundService.class));
        } else {
            // Pre-O behavior.
            startService(new Intent(this, BackgroundService.class));
        }

    }

    @Override
    protected void onPause() {
//        if (mMediaStream != null) {
//            if (isStreaming() && SPUtil.getEnableBackgroundCamera(this)) {
//                mService.activePreview();
//            }
//        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Hawk.get(HawkProperty.HIDE_FLOAT_VIEWS, false)) {
            mFloatViewGp.setVisibility(View.GONE);
        } else {
            mFloatViewGp.setVisibility(View.VISIBLE);
            mFullScreenIv.setImageResource(R.mipmap.video_record_normal);
        }
        goonWithPermissionGranted();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, BackgroundService.class));
        BUSUtil.BUS.unregister(this);
        if (conn != null) {
            unbindService(conn);
            conn = null;
        }
        if (connUVC != null) {
            unbindService(connUVC);
            connUVC = null;
        }

        handler.removeCallbacksAndMessages(null);
        if (mMediaStream != null) {
            mMediaStream.stopPreview();
            mMediaStream.release();
            mMediaStream = null;
            stopService(new Intent(this, BackgroundCameraService.class));
            stopService(new Intent(this, UVCCameraService.class));
            if (isStreaming()) {
                for (int i = 0; i < 5; i++) {
                    mMediaStream.stopPusherStream(i);
                }

            }
        }
        super.onDestroy();
    }

    /**
     * 是否正在推流
     */
    private boolean isStreaming() {
        return mMediaStream != null && ( mMediaStream.isFirstPushStream ||
                mMediaStream.isSecendPushStream || mMediaStream.isThirdPushStream || mMediaStream.isFourthPushStream);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "get capture permission success!");

                mResultCode = resultCode;
                mResultIntent = data;

                //                startScreenPushIntent();
            }
        } else if (requestCode == 100) {
            //设置界面返回
            setPushLiveIv();


        }
    }

    /**
     * 配置推送直播的图标
     */
    private void setPushLiveIv() {
        String firstLiveName = Hawk.get(HawkProperty.FIRST_LIVE, SettingActivity.LIVE_TYPE_BILI);
        String secendLiveName = Hawk.get(HawkProperty.SECENDLIVE, SettingActivity.LIVE_TYPE_HUYA);
        String thirdLiveName = Hawk.get(HawkProperty.THIRD_LIVE, SettingActivity.LIVE_TYPE_DOUYU);
        String fourthLiveName = Hawk.get(HawkProperty.FOURTH_LIVE, SettingActivity.LIVE_TYPE_XIGUA);
        initLiveImage(firstLiveName, 1);
        initLiveImage(secendLiveName, 2);
        initLiveImage(thirdLiveName, 3);
        initLiveImage(fourthLiveName, 4);
    }

    /**
     * @param liveName
     * @param index    1代表第一个live平台
     */
    private void initLiveImage(String liveName, int index) {
        ImageView imageView = null;
        boolean isOn = false;
        switch (index) {
            case 1:
                imageView = mFirstLiveIv;
                isOn = isPushingFirstStream;
                break;
            case 2:
                imageView = mSecendLiveIv;
                isOn = isPushingSecendStream;
                break;
            case 3:
                imageView = mThirdLiveIv;
                isOn = isPushingThirdStream;
                break;
            case 4:
                imageView = mFourthLiveIv;
                isOn = isPushingFourthStream;
                break;
            default:
                break;
        }

        switch (liveName) {
            case SettingActivity.LIVE_TYPE_BILI:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.bilibili_on);
                } else {
                    imageView.setImageResource(R.mipmap.bilibili_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_HUYA:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.huya_on);
                } else {
                    imageView.setImageResource(R.mipmap.huya_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_YI:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.yi_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.yi_live_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_NOW:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.now_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.now_live_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_DOUYU:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.douyu_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.douyu_live_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_ZHANQI:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.zhanqi_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.zhanqi_live_off);
                }

                break;
            case SettingActivity.LIVE_TYPE_XIGUA:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.xigua_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.xigua_live_off);
                }

                break;
            //            case SettingActivity.LIVE_TYPE_YINGKE:
            //                if (isOn) {
            //                    imageView.setImageResource(R.mipmap.yingke_live_on);
            //                }else {
            //                    imageView.setImageResource(R.mipmap.yingke_live_off);
            //                }
            //                break;
            case SettingActivity.LIVE_TYPE_CUSTOM:
                if (isOn) {
                    imageView.setImageResource(R.mipmap.cc_live_on);
                } else {
                    imageView.setImageResource(R.mipmap.cc_live_off);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 如果所有权限都同意之后
     */
    private void goonWithPermissionGranted() {
        streamStat.setText(null);
        mSelectCameraTv.setOnClickListener(this);
        //        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        //            pushScreen.setVisibility(View.GONE);
        //        }
        //
        //        if (RecordService.mEasyPusher != null) {
        //            push_screen.setImageResource(R.drawable.push_screen_click);
        //            TextView viewById = findViewById(R.id.push_screen_url);
        //            viewById.setText(Config.getServerURL(this));
        //        }

        //        update = new UpdateMgr(this);
        //        update.checkUpdate(url);
        // create background service for background use.
        Intent backCameraIntent = new Intent(this, BackgroundCameraService.class);
        startService(backCameraIntent);

        if (conn == null) {
            conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    mService = ((BackgroundCameraService.LocalBinder) iBinder).getService();
                    if (surfaceView.isAvailable()) {
                        if (!UVCCameraService.uvcConnected) {
                            goonWithAvailableTexture(surfaceView.getSurfaceTexture());
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
        } else {
//            if (!UVCCameraService.uvcConnected) {
//                goonWithAvailableTexture(surfaceView.getSurfaceTexture());
//            }
        }
        bindService(new Intent(this, BackgroundCameraService.class), conn, 0);
        startService(new Intent(this, UVCCameraService.class));
        if (connUVC == null) {
            connUVC = new ServiceConnection() {


                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    mUvcService = ((UVCCameraService.LocalBinder) iBinder).getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
        }
        bindService(new Intent(this, UVCCameraService.class), connUVC, 0);
        if (mRecording) {
            textRecordTick.setVisibility(View.VISIBLE);
            textRecordTick.removeCallbacks(mRecordTickRunnable);
            textRecordTick.post(mRecordTickRunnable);
        } else {
            textRecordTick.setVisibility(View.INVISIBLE);
            textRecordTick.removeCallbacks(mRecordTickRunnable);
        }
    }

    /**
     * 初始化surfaceView 的点击事件
     */
    private void initSurfaceViewClick() {
        surfaceView.setSurfaceTextureListener(this);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Hawk.get(HawkProperty.HIDE_FLOAT_VIEWS, false)) {
                    mFloatViewGp.setVisibility(View.VISIBLE);
//                    //屏幕竖屏
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    mFullScreenIv.setImageResource(R.mipmap.video_record_normal);
                    Hawk.put(HawkProperty.HIDE_FLOAT_VIEWS, false);
                }
            }
        });
    }


    /*
     * 初始化MediaStream
     * */


    private void goonWithAvailableTexture(SurfaceTexture surface) {
        Configuration mConfiguration = getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            //横屏
            IS_VERTICAL_SCREEN = false;
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            //竖屏
            IS_VERTICAL_SCREEN = true;
        }


        final File easyPusher = new File(Config.recordPath());
        easyPusher.mkdir();

        MediaStream ms = mService.getMediaStream();

        if (ms != null) { // switch from background to front
            ms.stopPreview();
            mService.inActivePreview();
            ms.setSurfaceTexture(surface);
            ms.startPreview();
            mMediaStream = ms;

            if (isStreaming()) {
                String url = Config.getServerURL();
                //                txtStreamAddress.setText(url);

                //                sendMessage(getPushStatusMsg());

                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
            }

//            if (ms.getDisplayRotationDegree() != getDisplayRotationDegree()) {
//                int orientation = getRequestedOrientation();
//
//                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                } else {
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                }
//            }
        } else {

            boolean enableVideo = SPUtil.getEnableVideo(this);

            ms = new MediaStream(getApplicationContext(), surface, enableVideo);
            ms.setRecordPath(easyPusher.getPath());
            mMediaStream = ms;
            startCamera();
            mService.setMediaStream(ms);
            if (ms.getDisplayRotationDegree() != getDisplayRotationDegree()) {
                int orientation = getRequestedOrientation();

                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }
    }

    private void startCamera() {
//        mMediaStream.updateResolution();
        mMediaStream.setDisplayRotationDegree(getDisplayRotationDegree());
        mMediaStream.createCamera(getSelectedCameraIndex());
        mMediaStream.startPreview();

        //        sendMessage(getPushStatusMsg());
        //        txtStreamAddress.setText(Config.getServerURL());
    }

    // 屏幕的角度
    private int getDisplayRotationDegree() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }

        return degrees;
    }


    /*
     * 开始录像的通知
     * */
    @Subscribe
    public void onStartRecord(StartRecord sr) {
        // 开始录像的通知，记下当前时间
        mRecording = true;
        mRecordingBegin = System.currentTimeMillis();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textRecordTick.setVisibility(View.VISIBLE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);
                textRecordTick.post(mRecordTickRunnable);

                ImageView ib = findViewById(R.id.streaming_activity_record);
                ib.setImageResource(R.drawable.record_pressed);
            }
        });
    }

    /*
     * 得知停止录像
     * */
    @Subscribe
    public void onStopRecord(StopRecord sr) {
        // 停止录像的通知，更新状态
        mRecording = false;
        mRecordingBegin = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textRecordTick.setVisibility(View.INVISIBLE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);

                ImageView ib = findViewById(R.id.streaming_activity_record);
                ib.setImageResource(R.drawable.record);
            }
        });
    }

    /*
     * 开始推流，获取fps、bps
     * */
    @Subscribe
    public void onStreamStat(final StreamStat stat) {
//        streamStat.post(() -> streamStat.setText(getString(R.string.stream_stat, stat.framePerSecond,
//                stat.bytesPerSecond * 8 / 1024)));
    }

//        /*
//         * 获取可以支持的分辨率
//         * */
//        @Subscribe
//        public void onSupportResolution(SupportResolution res) {
//            runOnUiThread(() -> {
//                listResolution = Util.getSupportResolution(getApplicationContext());
//                boolean supportdefault = listResolution.contains(String.format("%dx%d", width, height));
//
//                if (!supportdefault) {
//                    String r = listResolution.get(0);
//                    String[] splitR = r.split("x");
//
//                    width = Integer.parseInt(splitR[0]);
//                    height = Integer.parseInt(splitR[1]);
//                }
//
//                initSpinner();
//            });
//        }

    /*
     * 得知推流的状态
     * */
    @Subscribe
    public void onPushCallback(final PushCallback cb) {
        switch (cb.code) {
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_INVALID_KEY:
                sendMessage("无效Key");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
                sendMessage("激活成功");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTING:
                sendMessage("连接中");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTED:
                sendMessage("连接成功");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_FAILED:
                sendMessage("连接失败");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_ABORT:
                sendMessage("连接异常中断");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_PUSHING:
                sendMessage("直播中");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_DISCONNECTED:
                sendMessage("断开连接");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
                sendMessage("平台不匹配");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
                sendMessage("断授权使用商不匹配");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
                sendMessage("进程名称长度不匹配");
                break;
            case EASY_ACTIVATE_VALIDITY_PERIOD_ERR:
                sendMessage("进程名称长度不匹配");
                break;
        }
    }

    /**
     * 获取推流状态信息
     *
     * @return
     */
    private String getPushStatusMsg() {
        if (mMediaStream.isFirstPushStream || mMediaStream.isSecendPushStream||mMediaStream.isThirdPushStream || mMediaStream.isFourthPushStream) {
            return "直播中";
        } else {
            return "";
        }
    }

    /*
     * 显示推流的状态
     * */
    private void sendMessage(String message) {
        Message msg = Message.obtain();
        msg.what = MSG_STATE;
        Bundle bundle = new Bundle();
        bundle.putString(STATE, message);
        msg.setData(bundle);

        handler.sendMessage(msg);
    }

    /* ========================= 点击事件 ========================= */

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        if (isStreaming() && SPUtil.getEnableBackgroundCamera(this)) {
            new AlertDialog.Builder(this).setTitle("是否允许后台上传？").setMessage("您设置了使能摄像头后台采集,是否继续在后台采集并上传视频？如果是，记得直播结束后," +
                    "再回来这里关闭直播。").setNeutralButton("后台采集", (dialogInterface, i) -> {
                //实现home键效果
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }).setPositiveButton("退出程序", (dialogInterface, i) -> {
                for (int i1 = 0; i1 < 5; i1++) {
                    mMediaStream.stopPusherStream(i1);
                }
                StreamActivity.super.onBackPressed();
                Toast.makeText(StreamActivity.this, "程序已退出。", Toast.LENGTH_SHORT).show();
            }).setNegativeButton(android.R.string.cancel, null).show();
            return;
        }

        //与上次点击返回键时刻作差
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            //大于2000ms则认为是误操作，使用Toast进行提示
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            //并记录下本次点击“返回键”的时刻，以便下次进行判断
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_camera_tv:
                new AlertDialog.Builder(this).setTitle("选择摄像头").setSingleChoiceItems(getCameras(),
                        getSelectedCameraIndex(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isStreaming()) {
                                    Toast.makeText(StreamActivity.this, getPushStatusMsg() + ",无法切换摄像头",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    return;
                                }
                                if (2 == which && !UVCCameraService.uvcConnected) {
                                    mUvcService.reRequestOtg();
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (2 != which) {
                                    SPUtil.setScreenPushingCameraIndex(StreamActivity.this, which);
                                }
                                switch (which) {
                                    case 0:
                                        surfaceViewToNativeCamera();
                                        mSelectCameraTv.setText("摄像头:后置");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
                                        break;
                                    case 1:
                                        surfaceViewToNativeCamera();
                                        mSelectCameraTv.setText("摄像头:前置");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
                                        break;
                                    case 2:
                                        if (UVCCameraService.uvcConnected) {
                                            mSelectCameraTv.setText("摄像头:外置");
                                            SPUtil.setScreenPushingCameraIndex(StreamActivity.this, which);
                                        } else {
                                            ToastUtils.toast(mContext, "暂无外置摄像头");
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                dialog.dismiss();
                            }
                        }).show();

                break;
            case R.id.first_live_iv:
                String url_bili = Hawk.get(HawkProperty.KEY_FIRST_URL);
                if (TextUtils.isEmpty(url_bili)) {
                    Toast.makeText(getApplicationContext(), "还没有配置对应直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopFirstPush();
                break;
            case R.id.secend_live_iv:
                String url_huya = Hawk.get(HawkProperty.KEY_SECEND_URL);
                if (TextUtils.isEmpty(url_huya)) {
                    Toast.makeText(getApplicationContext(), "还没有配置对应直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopSecendPush();
                break;

            case R.id.third_live_iv:
                String url_yi = Hawk.get(HawkProperty.KEY_THIRD_URL);
                if (TextUtils.isEmpty(url_yi)) {
                    Toast.makeText(getApplicationContext(), "还没有配置对应直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopThirdPush();
                break;
            case R.id.fourth_live_iv:
                String url_now = Hawk.get(HawkProperty.KEY_FOURTH_URL);
                if (TextUtils.isEmpty(url_now)) {
                    Toast.makeText(getApplicationContext(), "还没有配置对应直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopFourthPush();
                break;
            case R.id.video_record_full_screen_iv:
                mFullScreenIv.setImageResource(R.mipmap.video_record_press);

                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("录屏直播前需先打开抖音快手的录屏直播按钮，由于抖音快手录屏要求的限制，请保持手机屏幕处于亮屏和非锁屏状态！")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mFullScreenIv.setImageResource(R.mipmap.video_record_normal);
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //停止本地推流和录像
                                stopAllPushStream();
                                if (mMediaStream.isRecording()) {
                                    mMediaStream.stopRecord();
                                    startRecordIv.setImageResource(R.drawable.record);
                                }
                                dialog.dismiss();
                                Hawk.put(HawkProperty.HIDE_FLOAT_VIEWS, true);
                                if (IS_VERTICAL_SCREEN) {
                                    //屏幕设为横屏
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//横屏
                                } else {
                                    mFloatViewGp.setVisibility(View.GONE);
                                }


                            }
                        }).show();

                break;

            case R.id.switch_oritation_iv:
                /*
                 * 切换屏幕方向
                 * */

                //停止本地推流和录像
                stopAllPushStream();
                if (mMediaStream.isRecording()) {
                    mMediaStream.stopRecord();
                    startRecordIv.setImageResource(R.drawable.record);
                }

                int orientation = getRequestedOrientation();

                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }

                break;

            case R.id.record_ll:
                //录像
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_STORAGE_PERMISSION);
                    return;
                }


                if (mMediaStream != null) {
                    if (mMediaStream.isRecording()) {
                        mMediaStream.stopRecord();
                        startRecordIv.setImageResource(R.drawable.record);
                    } else {
                        mMediaStream.startRecord();
                        startRecordIv.setImageResource(R.drawable.record_pressed);
                    }
                }
                break;
            case R.id.set_ll:
                //设置
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, 100);
                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
                break;
        }
    }

    /**
     * 获取摄像头数据
     *
     * @return
     */
    private CharSequence[] getCameras() {
        return new CharSequence[]{"后置摄像头", "前置摄像头", "外置摄像头"};

    }

    /**
     * 获取选择的摄像头的index
     *
     * @return
     */
    private int getSelectedCameraIndex() {
        int position = SPUtil.getScreenPushingCameraIndex(this);
//        if (UVCCameraService.uvcConnected) {
//            SPUtil.setScreenPushingCameraIndex(this, 2);
//            return 2;
//        }
        return position;

    }

    /**
     * 获取选择的摄像头的index
     *
     * @return
     */
    private String getSelectedCamera() {
        int position = SPUtil.getScreenPushingCameraIndex(this);
        if (0 == position) {
            return "后置";
        }
        if (1 == position) {
            return "前置";
        }
        if (2 == position) {
            return "外置";
        }
        return "";
    }


    /*
     * 推送屏幕
     * */
    public void onPushScreen(final View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            new AlertDialog.Builder(this).setMessage("推送屏幕需要安卓5.0以上,您当前系统版本过低,不支持该功能。").setTitle("抱歉").show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setMessage("推送屏幕需要APP出现在顶部.是否确定?")
                        .setPositiveButton(android.R.string.ok,
                                (dialogInterface, i) -> {
                                    // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
                                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                    startActivityForResult(intent, SettingActivity.REQUEST_OVERLAY_PERMISSION);
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setCancelable(false)
                        .show();
                return;
            }
        }

        if (!SPUtil.getScreenPushing(this)) {
            new AlertDialog.Builder(this).setTitle("提醒").setMessage("屏幕直播将要开始,直播过程中您可以切换到其它屏幕。不过记得直播结束后,再进来停止直播哦!").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SPUtil.setScreenPushing(StreamActivity.this, true);
                    onPushScreen(view);
                }
            }).show();
            return;
        }

        if (RecordService.mEasyPusher != null) {
            Intent intent = new Intent(getApplicationContext(), RecordService.class);
            stopService(intent);
            //                ImageView im = findViewById(R.id.streaming_activity_push_screen);
            //                im.setImageResource(R.drawable.push_screen);
        } else {
            startScreenPushIntent();
        }
    }

    /*
     * 推送屏幕
     * */
    private void startScreenPushIntent() {
        if (StreamActivity.mResultIntent != null && StreamActivity.mResultCode != 0) {
            Intent intent = new Intent(getApplicationContext(), RecordService.class);
            startService(intent);

            //            ImageView im = findViewById(R.id.streaming_activity_push_screen);
            //            im.setImageResource(R.drawable.push_screen_click);

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 2.创建屏幕捕捉的Intent
                MediaProjectionManager mMpMngr =
                        (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mMpMngr.createScreenCaptureIntent(), StreamActivity.REQUEST_MEDIA_PROJECTION);
            }
        }
    }

    /*
     * 切换分辨率
     * */
    public void onClickResolution(View view) {
        if (UVCCameraService.uvcConnected) {
            setCameraRes(resUvcDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 2));
        } else {
            setCameraRes(resDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 2));
        }

    }

    /**
     * 配置相机的分辨率
     */
    private void setCameraRes(CharSequence[] res_display, int index) {
        new AlertDialog.Builder(this).setTitle("设置分辨率").setSingleChoiceItems(res_display, index,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        String title = res_display[position].toString();
                        if (isStreaming()) {
                            Toast.makeText(StreamActivity.this, getPushStatusMsg() + ",无法切换分辨率", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            return;
                        }
                        String[] titles = title.split("x");
                        if (res_display.length > 3) {
                            //原生相机配置分辨率
                            if (!Util.getSupportResolution(StreamActivity.this).contains(title)) {
                                Toast.makeText(StreamActivity.this, "您的相机不支持此分辨率", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                return;
                            }
                            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, position);
                            Hawk.put(HawkProperty.KEY_NATIVE_WIDTH, Integer.parseInt(titles[0]));
                            Hawk.put(HawkProperty.KEY_NATIVE_HEIGHT, Integer.parseInt(titles[1]));
                            if (mMediaStream != null) {
                                mMediaStream.updateResolution();
                            }
                        } else {
                            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, position);
                            Hawk.put(HawkProperty.KEY_UVC_WIDTH, Integer.parseInt(titles[0]));
                            Hawk.put(HawkProperty.KEY_UVC_HEIGHT, Integer.parseInt(titles[1]));
                            if (mMediaStream != null) {
                                mMediaStream.updateResolution();
                            }
                            mUvcService.reRequestOtg();
                        }
                        mScreenResTv.setText("分辨率:" + title);


                        dialog.dismiss();
                    }


                }).show();
    }


    /*
     * 推流or停止
     * type   第一个直播
     * */
    public void startOrStopFirstPush() {


        if (mMediaStream != null && !mMediaStream.isFirstPushStream) {
            isPushingFirstStream = true;
            try {
                //                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startPushStream(0, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("参数初始化失败");
            }
        } else {
            isPushingFirstStream = false;
            mMediaStream.stopPusherStream(0);
            setPushLiveIv();
            sendMessage("断开连接");
        }
    }

    /*
     * 推流or停止
     * type   第三个直播
     * */
    public void startOrStopThirdPush() {


        if (mMediaStream != null && !mMediaStream.isThirdPushStream) {
            isPushingThirdStream = true;
            try {
                //                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startPushStream(2, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingThirdStream = false;
            mMediaStream.stopPusherStream(2);
            setPushLiveIv();
            sendMessage("断开连接");
        }
    }

    /*
     * 推流or停止
     * type   第四个直播
     * */
    public void startOrStopFourthPush() {


        if (mMediaStream != null && !mMediaStream.isFourthPushStream) {
            isPushingFourthStream = true;
            try {
                mMediaStream.startPushStream(3, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingFourthStream = false;
            mMediaStream.stopPusherStream(3);
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            setPushLiveIv();
            sendMessage("断开连接");
        }
    }

    /*
     * 推流or停止
     * type   第二个直播
     * */
    public void startOrStopSecendPush() {

        if (mMediaStream != null && !mMediaStream.isSecendPushStream) {
            isPushingSecendStream = true;
            try {
                //                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startPushStream(1, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("参数初始化失败");
            }
        } else {
            isPushingSecendStream = false;
            mMediaStream.stopPusherStream(1);
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            setPushLiveIv();
            sendMessage("断开连接");
        }
    }



    /* ========================= TextureView.SurfaceTextureListener ========================= */

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        if (mService != null) {
            if (!UVCCameraService.uvcConnected) {
                goonWithAvailableTexture(surface);
            }

        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onUvcCameraConnected() {
        //        Toast.makeText(getApplicationContext(),"connect",Toast.LENGTH_SHORT).show();
        if (mMediaStream != null) {
            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK_UVC);
        }
        initUvcLayout();
//        mScreenResTv.setVisibility(View.INVISIBLE);
//        mSwitchOritation.setVisibility(View.INVISIBLE);
        //        String title = resUvcDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 1)].toString();
        //        mScreenResTv.setText(String.format("分辨率:%s", title));
    }

    /**
     * 初始化otg摄像头的布局
     */
    private void initUvcLayout() {
        Display mDisplay = getWindowManager().getDefaultDisplay();

        int W = mDisplay.getWidth();

        int H = mDisplay.getHeight();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
        if (IS_VERTICAL_SCREEN) {
            params.height = H / 2;
            params.width = W;
        } else {
            params.height = H;
            params.width = W;
        }
        surfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
        SPUtil.setScreenPushingCameraIndex(this, 2);
        mSelectCameraTv.setText("摄像头:" + getSelectedCamera());
    }

    @Override
    public void onUvcCameraAttached() {
        //        Toast.makeText(getApplicationContext(),"Attached",Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onUvcCameraDisConnected() {
        //        Toast.makeText(getApplicationContext(),"disconnect",Toast.LENGTH_SHORT).show();
        handler.sendEmptyMessage(UVC_DISCONNECT);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == newConfig.ORIENTATION_LANDSCAPE) {
            //横屏
            IS_VERTICAL_SCREEN = false;
        } else {
            //竖屏
            IS_VERTICAL_SCREEN = true;
        }
        if (Hawk.get(HawkProperty.HIDE_FLOAT_VIEWS, false)) {
            mFloatViewGp.setVisibility(View.GONE);
        } else {
            mFloatViewGp.setVisibility(View.VISIBLE);
        }
        //横屏
        if (surfaceView.isAvailable()) {
            if (!UVCCameraService.uvcConnected) {
                Display mDisplay = getWindowManager().getDefaultDisplay();
                int W = mDisplay.getWidth();
                int H = mDisplay.getHeight();
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
                params.height = H;
                params.width = W;
                surfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
                mSwitchOritation.setVisibility(View.VISIBLE);
                goonWithAvailableTexture(surfaceView.getSurfaceTexture());
            } else {
                initUvcLayout();
            }
        }
        super.onConfigurationChanged(newConfig);
    }
}
