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
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.Utils.RegOperateManager;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;
import com.squareup.otto.Subscribe;

import org.easydarwin.bus.StartRecord;
import org.easydarwin.bus.StopRecord;
import org.easydarwin.bus.StreamStat;
import org.easydarwin.easypusher.BaseProjectActivity;
import org.easydarwin.easypusher.BuildConfig;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.record.RecordService;
import org.easydarwin.easypusher.util.Config;
import org.easydarwin.easypusher.util.DoubleClickListener;
import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;
import org.easydarwin.easyrtmp.push.EasyRTMP;
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
        TextureView.SurfaceTextureListener , CameraViewInterface.Callback{
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

    List<String> listResolution = new ArrayList<>();

    public MediaStream mMediaStream;
    public static Intent mResultIntent;
    public static int mResultCode;

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


    private boolean isRequest;
    private boolean isPreview;
    private UVCCameraHelper mCameraHelper;


    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {



            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            handler.sendEmptyMessage(UVC_CONNECT);

            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Thread.sleep(2500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        Looper.prepare();
//                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
////                            mSeekbarBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
////                            mSeekbarContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
//                        }
//                        Looper.loop();
//                    }
//                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            handler.sendEmptyMessage(UVC_DISCONNECT);
            showShortMsg("disconnecting");
        }
    };
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE:
                    String state = msg.getData().getString("state");
                    txtStatus.setText(state);
                    break;
                case UVC_CONNECT:
                    mUvcTextureView.setVisibility(View.VISIBLE);
                    mMediaStream.destroyCamera();
                    mSurfaceView.setVisibility(View.GONE);
//                    mMediaStream.initConsumer(1280,720);
                    break;
                case UVC_DISCONNECT:
                    mCameraHelper.closeCamera();
                    mSurfaceView.setVisibility(View.VISIBLE);
                    mMediaStream.startPreview();
                    mUvcTextureView.setVisibility(View.GONE);
                    Display mDisplay = getWindowManager().getDefaultDisplay();
                    int W = mDisplay.getWidth();
                    int H = mDisplay.getHeight();
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
                    params.height = H;
                    params.width = W;
                    mSurfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
                    if (mMediaStream != null) {
                        if (mMediaStream.isZeroPushStream) {
                            startOrStopPush();
                        }
                        if (mMediaStream.isFirstPushStream) {
                            startOrStopFirstPush();
                        }
                        if (mMediaStream.isSecendPushStream) {
                            startOrStopSecendPush();
                        }
                    }
                    mScreenResTv.setVisibility(View.VISIBLE);
                    mSwitchOritation.setVisibility(View.VISIBLE);
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


    private TextureView mSurfaceView;
    private ImageView mPushBgIv;
    private ImageView mPushStreamIv, mSwitchOritation;
    private ImageView mFirstLiveIv;
    private ImageView mThirdLiveIv;
    private ImageView mFourthLiveIv;
    private ImageView mVedioPushBottomTagIv;
    private ImageView mSecendLiveIv, mBlackBgIv;
    private Intent uvcServiceIntent;
    private CameraViewInterface mUvcCameraView;
    private UVCCameraTextureView mUvcTextureView;

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
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultPreviewSize(640,480);
        mCameraHelper.initUSBMonitor(this, mUvcCameraView, listener);


    }
    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }
    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
//        if (!isPreview && mCameraHelper.isCameraOpened()) {
//            mCameraHelper.startPreview(mUvcCameraView);
//            isPreview = true;
//        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
//        if (isPreview && mCameraHelper.isCameraOpened()) {
//            mCameraHelper.stopPreview();
//            isPreview = false;
//        }
    }
    /**
     * 初始化view
     */
    private void initView() {
        mUvcTextureView = (UVCCameraTextureView) findViewById(R.id.uvc_camera_view);
        mUvcCameraView = (CameraViewInterface)mUvcTextureView;
        //        spnResolution = findViewById(R.id.spn_resolution);
        streamStat = findViewById(R.id.stream_stat);
        txtStatus = findViewById(R.id.txt_stream_status);
        mSelectCameraTv = findViewById(R.id.select_camera_tv);
        mSelectCameraTv.setOnClickListener(this);
        mSelectCameraTv.setText("摄像头:" + getSelectedCamera());
        txtStreamAddress = findViewById(R.id.txt_stream_address);
        textRecordTick = findViewById(R.id.tv_start_record);
        mScreenResTv = findViewById(R.id.txt_res);
        mSurfaceView = findViewById(R.id.sv_surfaceview);
        //        mPushBgIv = (ImageView) findViewById(R.id.push_bg_iv);
        //        mPushBgIv.setOnClickListener(this);
        mPushStreamIv = (ImageView) findViewById(R.id.push_stream_iv);
        mSwitchOritation = (ImageView) findViewById(R.id.switch_oritation_iv);
        mPushStreamIv.setOnClickListener(this);
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
        mSecendLiveIv = (ImageView) findViewById(R.id.secend_live_iv);
        mBlackBgIv = (ImageView) findViewById(R.id.black_bg_iv);
        mSecendLiveIv.setOnClickListener(this);
        mVedioPushBottomTagIv = findViewById(R.id.streaming_activity_push);
//        mBlackBgIv.setOnClickListener(new DoubleClickListener() {
//            @Override
//            public void onDoubleClick(View v) {
//                mBlackBgIv.setVisibility(View.GONE);
//                //推流
//                mPushStreamIv.performClick();
//            }
//
//            @Override
//            public void onOneClick(View v) {
//
//            }
//        });
        String title = resDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 2)].toString();
        mScreenResTv.setText(String.format("分辨率:%s", title));
        initSurfaceViewClick();
        setPushLiveIv();
        if (PublicUtil.isMoreThanTheAndroid10()) {
            setViewsVisible(mSecendLiveIv, mThirdLiveIv, mFourthLiveIv);
        } else {
            setViewsInvisible(true, mSecendLiveIv, mThirdLiveIv, mFourthLiveIv);
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
        goonWithPermissionGranted();
    }

    @Override
    protected void onDestroy() {
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
        BUSUtil.BUS.unregister(this);
        if (conn != null) {
            unbindService(conn);
            conn = null;
        }

        handler.removeCallbacksAndMessages(null);
        if (mMediaStream != null) {
            mMediaStream.stopPreview();
            if (isStreaming() && SPUtil.getEnableBackgroundCamera(this)) {
                mService.activePreview();
            } else {
                for (int i = 0; i < 5; i++) {
                    mMediaStream.stopPusherStream(i);
                }
                mMediaStream.release();
                mMediaStream = null;
                stopService(new Intent(this, BackgroundCameraService.class));
            }
        }
        super.onDestroy();
    }

    /**
     * 是否正在推流
     */
    private boolean isStreaming() {
        return mMediaStream != null && (mMediaStream.isZeroPushStream || mMediaStream.isFirstPushStream ||
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
        Intent backCameraIntent = new Intent(this, BackgroundCameraService.class);
        startService(backCameraIntent);
        if (conn == null) {
            conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    mService = ((BackgroundCameraService.LocalBinder) iBinder).getService();
                    if (mSurfaceView.isAvailable()) {
                        goonWithAvailableTexture(mSurfaceView.getSurfaceTexture());
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
        }
        bindService(new Intent(this, BackgroundCameraService.class), conn, 0);


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
        mSurfaceView.setSurfaceTextureListener(this);
        mSurfaceView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick(View v) {
                if (mBlackBgIv.getVisibility() == View.VISIBLE) {
                    mBlackBgIv.setVisibility(View.GONE);
                } else {
                    mBlackBgIv.setVisibility(View.VISIBLE);
                }
                //                //推流
                //                if (!mMediaStream.isStreaming()) {
                //                    mPushStreamIv.performClick();
                //                }
            }

            @Override
            public void onOneClick(View v) {
                try {
                    mMediaStream.getCamera().autoFocus(null);
                } catch (Exception e) {

                }
            }
        });
    }


    /*
     * 初始化MediaStream
     * */


    private void goonWithAvailableTexture(SurfaceTexture surface) {
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

            if (ms.getDisplayRotationDegree() != getDisplayRotationDegree()) {
                int orientation = getRequestedOrientation();

                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        } else {

            boolean enableVideo = SPUtil.getEnableVideo(this);

            ms = new MediaStream(getApplicationContext(), surface, enableVideo);
            ms.setRecordPath(easyPusher.getPath());
            mMediaStream = ms;
            startCamera();
            mService.setMediaStream(ms);
        }
    }

    private void startCamera() {
        mMediaStream.updateResolution();
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
        streamStat.post(() -> streamStat.setText(getString(R.string.stream_stat, stat.framePerSecond,
                stat.bytesPerSecond * 8 / 1024)));
    }

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
        if (mMediaStream.isZeroPushStream) {
            if (mMediaStream.isFirstPushStream || mMediaStream.isSecendPushStream) {
                return "直播中";
            }
            return "直播中";
        } else {
            if (mMediaStream.isFirstPushStream || mMediaStream.isSecendPushStream) {
                return "直播中";
            }
        }
        return "";
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
                StreamActivity.super.onBackPressed();
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
                                SPUtil.setScreenPushingCameraIndex(StreamActivity.this, which);
                                switch (which) {
                                    case 0:
                                        mSelectCameraTv.setText("摄像头:后置");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
                                        break;
                                    case 1:
                                        mSelectCameraTv.setText("摄像头:前置");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
                                        break;
                                    case 2:
                                        mSelectCameraTv.setText("摄像头:外置");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK_UVC);
                                        break;
                                    default:
                                        break;
                                }
                                dialog.dismiss();
                            }
                        }).show();

                break;
            case R.id.push_stream_iv:
                startOrStopPush();
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

            case R.id.switch_oritation_iv:
                /*
                 * 切换屏幕方向
                 * */

                if (isStreaming()) {
                    Toast.makeText(this, "正在推送中,无法更改屏幕方向", Toast.LENGTH_SHORT).show();
                    return;
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

                ImageView ib = findViewById(R.id.streaming_activity_record);

                if (mMediaStream != null) {
                    if (mMediaStream.isRecording()) {
                        mMediaStream.stopRecord();
                        ib.setImageResource(R.drawable.record_pressed);
                    } else {
                        mMediaStream.startRecord();
                        ib.setImageResource(R.drawable.record);
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
//        if (UVCCameraService.uvcConnected) {
//            return new CharSequence[]{"外置摄像头"};
//        }
        return new CharSequence[]{"后置摄像头", "前置摄像头"};

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
//        if (UVCCameraService.uvcConnected) {
//            SPUtil.setScreenPushingCameraIndex(this, 2);
//            return "外置";
//        }

        return 0 == position ? "后置" : "前置";

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
//        if (UVCCameraService.uvcConnected) {
//            setCameraRes(resUvcDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 1));
//        } else {
//            setCameraRes(resDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 2));
//        }

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
                        } else {
                            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, position);
                            Hawk.put(HawkProperty.KEY_UVC_WIDTH, Integer.parseInt(titles[0]));
                            Hawk.put(HawkProperty.KEY_UVC_HEIGHT, Integer.parseInt(titles[1]));
                        }
                        mScreenResTv.setText("分辨率:" + title);

                        if (mMediaStream != null) {
                            mMediaStream.updateResolution();
                        }
                        dialog.dismiss();
                    }


                }).show();
    }


    /*
     * 推流or停止
     * type   推流
     * */
    public void startOrStopPush() {

        if (mMediaStream != null && !mMediaStream.isZeroPushStream) {
            isPushingStream = true;
            try {
                String ip = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_IP, "yjyk.beidoustar.com");
                String port = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_PORT, "10085");
                String tag = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_TAG, "");
                if (TextUtils.isEmpty(ip)) {
                    ToastUtils.toast(this, "请在设置中输入IP地址");
                    return;
                }
                if (TextUtils.isEmpty(port)) {
                    ToastUtils.toast(this, "请在设置中输入端口号");
                    return;
                }
                if (TextUtils.isEmpty(tag)) {
                    ToastUtils.toast(this, "请在设置中输入标识");
                    return;
                }
                mMediaStream.startPushStream(0, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mPushStreamIv.setImageResource(R.mipmap.push_stream_on);
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                isPushingStream = false;
                mMediaStream.stopPusherStream(0);
                mPushStreamIv.setImageResource(R.mipmap.push_stream_off);
                sendMessage("断开连接");
            }
        } else {
            isPushingStream = false;
            mMediaStream.stopPusherStream(0);
            mPushStreamIv.setImageResource(R.mipmap.push_stream_off);
            sendMessage("断开连接");
        }
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
                mMediaStream.startPushStream(1, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("参数初始化失败");
            }
        } else {
            isPushingFirstStream = false;
            mMediaStream.stopPusherStream(1);
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
                mMediaStream.startPushStream(3, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingThirdStream = false;
            mMediaStream.stopPusherStream(3);
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
                mMediaStream.startPushStream(4, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingFourthStream = false;
            mMediaStream.stopPusherStream(4);
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
                mMediaStream.startPushStream(2, code -> BUSUtil.BUS.post(new PushCallback(code)));
                setPushLiveIv();
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("参数初始化失败");
            }
        } else {
            isPushingSecendStream = false;
            mMediaStream.stopPusherStream(2);
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            setPushLiveIv();
            sendMessage("断开连接");
        }
    }



    /* ========================= TextureView.SurfaceTextureListener ========================= */

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        if (mService != null) {
            goonWithAvailableTexture(surface);

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

//    @Override
//    public void onUvcCameraConnected() {
//        //        Toast.makeText(getApplicationContext(),"connect",Toast.LENGTH_SHORT).show();
//        if (mMediaStream != null) {
//            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK_UVC);
//        }
//        Display mDisplay = getWindowManager().getDefaultDisplay();
//
//        int W = mDisplay.getWidth();
//
//        int H = mDisplay.getHeight();
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
//        params.height = H / 2;
//        params.width = W;
//        mSurfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
//        mSelectCameraTv.setText("摄像头:" + getSelectedCamera());
////        mScreenResTv.setVisibility(View.INVISIBLE);
//        mSwitchOritation.setVisibility(View.INVISIBLE);
//        //        String title = resUvcDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 1)].toString();
//        //        mScreenResTv.setText(String.format("分辨率:%s", title));
//    }
//
//    @Override
//    public void onUvcCameraAttached() {
//        //        Toast.makeText(getApplicationContext(),"Attached",Toast.LENGTH_SHORT).show();
//
//    }
//
//    @Override
//    public void onUvcCameraDisConnected() {
//        //        Toast.makeText(getApplicationContext(),"disconnect",Toast.LENGTH_SHORT).show();
//        handler.sendEmptyMessage(UVC_DISCONNECT);
//
//    }
}
