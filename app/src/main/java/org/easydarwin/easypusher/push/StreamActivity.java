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
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juntai.wisdom.basecomponent.utils.DisplayUtil;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
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
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.record.RecordService;
import org.easydarwin.easypusher.util.Config;
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
        TextureView.SurfaceTextureListener{
    static final String TAG = "StreamActivity";
    private CharSequence[] resDisplay = new CharSequence[]{"640x480", "1280x720", "1920x1080", "2560x1440",
            "3840x2160"};
    private CharSequence[] resUvcDisplay = new CharSequence[]{"1280x720", "1920x1080"};
    public static final int REQUEST_MEDIA_PROJECTION = 1002;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;

    TextView txtStreamAddress;
    TextView mSelectCameraTv;
    //    Spinner spnResolution;
    TextView txtStatus, streamStat;
    TextView textRecordTick;
    TextView mScreenResTv;//屏幕Resolution
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
    private ImageView mTurnRightIv, mTurnLeftIv;
    public static boolean IS_VERTICAL_SCREEN = true;//是否是竖屏
    private boolean isBackPush = false;//后台推流
    private boolean isRollHor = false;//水平翻转
    private boolean isRollVer = false;//垂直翻转
    // 屏幕的角度
    int degrees = -1;


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
                    initSurfaceViewLayout(0);
                    int position = SPUtil.getScreenPushingCameraIndex(StreamActivity.this);
                    if (2 == position) {
                        position = 0;
                        SPUtil.setScreenPushingCameraIndex(StreamActivity.this, position);
                    }
                    switch (position) {
                        case 0:
                            mSelectCameraTv.setText("Camera:Second");
                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
                            break;
                        case 1:
                            mSelectCameraTv.setText("Camera:First");
                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
                            break;
                        default:
                            break;
                    }

                    String title = resDisplay[getIndex(resDisplay, Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT,
                            MediaStream.nativeHeight))].toString();
                    mScreenResTv.setText(String.format("Resolution:%s", title));
                    break;
                default:
                    break;
            }
        }
    };
    private ImageView startRecordIv;
    private LinearLayout mRightPushIconsLl;
    private LinearLayout mFullScreenLl;
    private ImageView mRollHorIv;
    private ImageView mRollVerIv;
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
    private LinearLayout mOperateCameraDisplayLl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initSurfaceViewLayout(0);
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
        mSelectCameraTv.setText("Camera:" + getSelectedCamera());
        txtStreamAddress = findViewById(R.id.txt_stream_address);
        textRecordTick = findViewById(R.id.tv_start_record);
        startRecordIv = findViewById(R.id.streaming_activity_record);
        mScreenResTv = findViewById(R.id.txt_res);
        surfaceView = findViewById(R.id.sv_surfaceview);

        //        mPushBgIv = (ImageView) findViewById(R.id.push_bg_iv);
        //        mPushBgIv.setOnClickListener(this);
        mSwitchOritation = (ImageView) findViewById(R.id.switch_oritation_iv);
        mRightPushIconsLl = (LinearLayout) findViewById(R.id.right_icon_ll);
        LinearLayout mRecordLl = (LinearLayout) findViewById(R.id.record_ll);
        mRecordLl.setOnClickListener(this);
        mTurnRightIv = (ImageView) findViewById(R.id.turn_right_iv);
        mTurnRightIv.setOnClickListener(this);
        mTurnLeftIv = (ImageView) findViewById(R.id.turn_left_iv);
        mTurnLeftIv.setOnClickListener(this);
        LinearLayout bottomPushLl = (LinearLayout) findViewById(R.id.push_stream_ll);
        bottomPushLl.setOnClickListener(this);
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
        mFullScreenLl = (LinearLayout) findViewById(R.id.video_record_full_screen_Ll);
        mFullScreenLl.setOnClickListener(this);
        mSecendLiveIv = (ImageView) findViewById(R.id.secend_live_iv);
        mFloatViewGp = findViewById(R.id.float_views_group);
        mSecendLiveIv.setOnClickListener(this);
        mVedioPushBottomTagIv = findViewById(R.id.streaming_activity_push);
        String title = resDisplay[getIndex(resDisplay, Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT,
                MediaStream.nativeHeight))].toString();
        mScreenResTv.setText(String.format("Resolution:%s", title));
        initSurfaceViewClick();
        setPushLiveIv();
        if (!MediaStream.isOnlyOnePush) {
            if (PublicUtil.isMoreThanTheAndroid10()) {
                setViewsVisible(mThirdLiveIv, mFourthLiveIv);
            } else {
                setViewsInvisible(true, mThirdLiveIv, mFourthLiveIv);
            }
        } else {
            setViewsInvisible(true, mRightPushIconsLl);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(new Intent(this, BackgroundService.class));
        } else {
            // Pre-O behavior.
            startService(new Intent(this, BackgroundService.class));
        }

        mRollHorIv = (ImageView) findViewById(R.id.roll_hor_iv);
        mRollHorIv.setOnClickListener(this);
        mRollVerIv = (ImageView) findViewById(R.id.roll_ver_iv);
        mRollVerIv.setOnClickListener(this);
        mOperateCameraDisplayLl = (LinearLayout) findViewById(R.id.operate_camera_display_ll);
    }

    /**
     * 获取索引
     *
     * @param arrays
     * @param height
     */
    public int getIndex(CharSequence[] arrays, int height) {
        int index = 0;
        for (int i = 0; i < arrays.length; i++) {
            CharSequence str = arrays[i];
            if (str.toString().contains(String.valueOf(height))) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    protected void onPause() {
        if (mMediaStream != null) {
            if (isStreaming() && SPUtil.getEnableBackgroundCamera(this)) {
                isBackPush = true;
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBackPush = false;
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
        //        if (connUVC != null) {
        //            unbindService(connUVC);
        //            connUVC = null;
        //        }

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
            //Set界面返回
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
        //        startService(new Intent(this, UVCCameraService.class));
        //        if (connUVC == null) {
        //            connUVC = new ServiceConnection() {
        //
        //
        //                @Override
        //                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        //                    mUvcService = ((UVCCameraService.LocalBinder) iBinder).getService();
        //                }
        //
        //                @Override
        //                public void onServiceDisconnected(ComponentName componentName) {
        //
        //                }
        //            };
        //        }
        //        bindService(new Intent(this, UVCCameraService.class), connUVC, 0);
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
        Configuration mConfiguration = getResources().getConfiguration(); //获取Set的配置信息
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
            //                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo
            //                .SCREEN_ORIENTATION_PORTRAIT) {
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
//            if (ms.getDisplayRotationDegree() != getDisplayRotationDegree()) {
//                int orientation = getRequestedOrientation();
//
//                if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                } else {
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                }
//            }
        }
//        mMediaStream.setResetCallBack(this);
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

        if (degrees == -1) {
            degrees = 0;
        } else if (degrees == 0) {
            degrees = 270;
        } else if (degrees == 270) {
            degrees = 180;
        } else if (degrees == 180) {
            degrees = 90;
        } else {
            degrees = 0;
        }

        return degrees;
    }
    public void change() {
        if (mMediaStream != null) {
            if (isStreaming()){
                Toast.makeText(this,"Video is Pushing,nothing can be done!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        mMediaStream.stopPreview();
        mMediaStream.destroyCamera();
        int rotation = getDisplayRotationDegree();
        if (0==rotation||180==rotation) {
            //宽高对调
            initSurfaceViewLayout(true);
        }else {
            initSurfaceViewLayout(false);
        }
        mMediaStream.setDisplayRotationDegree(rotation);
        mMediaStream.createCamera(getSelectedCameraIndex());
        mMediaStream.startPreview();
//        degrees+=90;
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
    //         * 获取可以支持的Resolution
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
                sendMessage("Invalid key");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
                sendMessage("Authentication is successful");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTING:
                sendMessage("Connecting");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTED:
                sendMessage("Connect Successfully");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_FAILED:
                sendMessage("Connection failed");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_ABORT:
                sendMessage("Abnormal connection interruption");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_PUSHING:
                sendMessage("Pushing");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_DISCONNECTED:
                sendMessage("Disconnect");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
                sendMessage("Platform mismatch");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
                sendMessage("Authorized user mismatch");
                break;
            case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
                sendMessage("Process name length does not match");
                break;
            case EASY_ACTIVATE_VALIDITY_PERIOD_ERR:
                sendMessage("Process name length does not match");
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
                return "Pushing";
            }
            return "Pushing";
        } else {
            if (mMediaStream.isFirstPushStream || mMediaStream.isSecendPushStream) {
                return "Pushing";
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
            new AlertDialog.Builder(this).setTitle("Whether to allow background upload？").setMessage("Do you agree to" +
                    " run app in the background ?").setPositiveButton("exit", (dialogInterface, i) -> {
                for (int i1 = 0; i1 < 5; i1++) {
                    mMediaStream.stopPusherStream(i1);
                }
                StreamActivity.super.onBackPressed();
                Toast.makeText(StreamActivity.this, "Program has exited。", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Background collection", (dialogInterface, i) -> {
                //实现home键效果
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }).show();
            return;
        }
        //与上次点击返回键时刻作差
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            if (mMediaStream.isRecording()) {
                mMediaStream.stopRecord();
                startRecordIv.setImageResource(R.drawable.record);
            }
            //大于2000ms则认为是误操作，使用Toast进行提示
            Toast.makeText(this, "Press again to exit the App", Toast.LENGTH_SHORT).show();
            //并记录下本次点击“返回键”的时刻，以便下次进行判断
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.turn_right_iv:
                change();
                //                mMediaStream.turnRight();
                break;
            case R.id.turn_left_iv:
                //                mMediaStream.turnLeft();
                break;

            case R.id.select_camera_tv:
                new AlertDialog.Builder(this).setTitle("ChoseCamera").setSingleChoiceItems(getCameras(),
                        getSelectedCameraIndex(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isStreaming()) {
                                    Toast.makeText(StreamActivity.this, getPushStatusMsg() + ",Can Not Switch Camera",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    return;
                                }
                                //                                if (2 == which) {
                                //                                    mUvcService.reRequestOtg();
                                //                                    try {
                                //                                        Thread.sleep(200);
                                //                                    } catch (InterruptedException e) {
                                //                                        e.printStackTrace();
                                //                                    }
                                //                                }

                                if (2 != which) {
                                    SPUtil.setScreenPushingCameraIndex(StreamActivity.this, which);
                                }
                                surfaceView.setScaleX(1f);
                                surfaceView.setScaleY(1f);
                                switch (which) {
                                    case 0:
                                        //后置摄像头
                                        initSurfaceViewLayout(0);
                                        mSelectCameraTv.setText("Camera:Second");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
                                        break;
                                    case 1:
                                        initSurfaceViewLayout(0);
                                        mSelectCameraTv.setText("Camera:First");
                                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
                                        break;
                                    case 2:
                                        if (UVCCameraService.uvcConnected) {
                                            mSelectCameraTv.setText("Camera:Other");
                                            SPUtil.setScreenPushingCameraIndex(StreamActivity.this, which);
                                        } else {
                                            ToastUtils.toast(mContext, "have not OtherCamera");
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
                //                String url_bili = Hawk.get(HawkProperty.KEY_FIRST_URL);
                //                if (TextUtils.isEmpty(url_bili)) {
                //                    Toast.makeText(getApplicationContext(), R.string.no_config_push, Toast
                //                    .LENGTH_SHORT).show();
                //                    return;
                //                }
                //                startOrStopFirstPush();
                break;
            case R.id.push_stream_ll:
                startOrStopPush();
                break;

            case R.id.secend_live_iv:
                String url_huya = Hawk.get(HawkProperty.KEY_SECEND_URL);
                if (TextUtils.isEmpty(url_huya)) {
                    Toast.makeText(getApplicationContext(), R.string.no_config_push, Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopSecendPush();
                break;

            case R.id.third_live_iv:
                String url_yi = Hawk.get(HawkProperty.KEY_THIRD_URL);
                if (TextUtils.isEmpty(url_yi)) {
                    Toast.makeText(getApplicationContext(), R.string.no_config_push, Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopThirdPush();
                break;
            case R.id.fourth_live_iv:
                String url_now = Hawk.get(HawkProperty.KEY_FOURTH_URL);
                if (TextUtils.isEmpty(url_now)) {
                    Toast.makeText(getApplicationContext(), R.string.no_config_push, Toast.LENGTH_SHORT).show();
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
            case R.id.video_record_full_screen_Ll:
                mFullScreenIv.performClick();
                break;
            case R.id.switch_oritation_iv:
                /*
                 * 切换屏幕方向
                 * */
                if (mMediaStream.isRecording()) {
                    ToastUtils.toast(mContext, "recording in service");
                    return;
                }
                if (isStreaming()) {
                    ToastUtils.toast(mContext, "Push  in service");
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


                if (mMediaStream != null) {
                    if (mMediaStream.isRecording()) {
                        ToastUtils.toast(mContext, "Stopped recording");

                        mMediaStream.stopRecord();
                        startRecordIv.setImageResource(R.drawable.record);
                    } else {
                        ToastUtils.toast(mContext, "Starting recording");
                        mMediaStream.startRecord();
                        startRecordIv.setImageResource(R.drawable.record_pressed);
                    }
                }
                break;
            case R.id.set_ll:
                if (mMediaStream.isRecording()) {
                    ToastUtils.toast(mContext, "recording in service");
                    return;
                }
                if (isStreaming()) {
                    ToastUtils.toast(mContext, "Push  in service");
                    return;
                }
                //Set
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, 100);
                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
                break;
            case R.id.roll_hor_iv:
                //水平翻转
                isRollHor = !isRollHor;
                mMediaStream.setRollHor(isRollHor);
                surfaceView.setScaleX(isRollHor ? -1f : 1f);
                break;
            case R.id.roll_ver_iv:
                isRollVer = !isRollVer;
                mMediaStream.setRollVer(isRollVer);
                surfaceView.setScaleY(isRollVer ? -1f : 1f);
                break;
        }
    }

    /*
     * 推流or停止
     * type   推流
     * */
    public void startOrStopPush() {

        if (mMediaStream != null && !mMediaStream.isZeroPushStream) {
            isPushingStream = true;
            try {
                String ip = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_IP, Config.DEFAULR_IP);
                String port = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_PORT, Config.DEFAULR_PORT);
                //                String tag = Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_TAG, "");
                if (TextUtils.isEmpty(ip)) {
                    ToastUtils.toast(this, "Please enter the IP address in the settings");
                    return;
                }
                if (TextUtils.isEmpty(port)) {
                    ToastUtils.toast(this, "Please enter the port number in the settings");
                    return;
                }
                //                if (TextUtils.isEmpty(tag)) {
                //                    ToastUtils.toast(this, "请在Set中输入标识");
                //                    return;
                //                }
                mMediaStream.startPushStream(0, code -> BUSUtil.BUS.post(new PushCallback(code)));
                //                mPushStreamIv.setImageResource(R.mipmap.push_stream_on);
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                isPushingStream = false;
                mMediaStream.stopPusherStream(0);
                //                mPushStreamIv.setImageResource(R.mipmap.push_stream_off);
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
                sendMessage("Push stream authentication failed");
            }
        } else {
            isPushingStream = false;
            mMediaStream.stopPusherStream(0);
            //            mPushStreamIv.setImageResource(R.mipmap.push_stream_off);
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            sendMessage("Disconnect");
        }
    }

    /**
     * 获取Camera数据
     *
     * @return
     */
    private CharSequence[] getCameras() {
        return new CharSequence[]{"SecondCamera", "FirstCamera"};

    }

    /**
     * 获取Chose的Camera的index
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
     * 获取Chose的Camera的index
     *
     * @return
     */
    private String getSelectedCamera() {
        int position = SPUtil.getScreenPushingCameraIndex(this);
        if (0 == position) {
            return "Second";
        }
        if (1 == position) {
            return "First";
        }
        if (2 == position) {
            if (UVCCameraService.uvcConnected) {
                return "Other";
            } else {
                SPUtil.setScreenPushingCameraIndex(this, 0);
                return "Second";
            }

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
     * 切换Resolution
     * */
    public void onClickResolution(View view) {
        if (UVCCameraService.uvcConnected) {
            setCameraRes(resUvcDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 2));
        } else {
            setCameraRes(resDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 2));
        }

    }

    /**
     * 配置相机的Resolution
     */
    private void setCameraRes(CharSequence[] res_display, int index) {
        new AlertDialog.Builder(this).setTitle("SetResolution").setSingleChoiceItems(res_display, index,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        String title = res_display[position].toString();
                        if (isStreaming()) {
                            Toast.makeText(StreamActivity.this, getPushStatusMsg() + ",Can't switch Resolution",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            return;
                        }
                        String[] titles = title.split("x");
                        if (res_display.length > 3) {
                            //原生相机配置Resolution
                            if (!Util.getSupportResolution(StreamActivity.this).contains(title)) {
                                Toast.makeText(StreamActivity.this, "Your camera does not support this Resolution",
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                return;
                            }
                            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, position);
                            Hawk.put(HawkProperty.KEY_NATIVE_WIDTH, Integer.parseInt(titles[0]));
                            Hawk.put(HawkProperty.KEY_NATIVE_HEIGHT, Integer.parseInt(titles[1]));
                            if (mMediaStream != null) {
                                mMediaStream.updateResolution();
                            }
                            initSurfaceViewLayout(0);
                        } else {
                            //                            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX,
                            //                            position);
                            //                            Hawk.put(HawkProperty.KEY_UVC_WIDTH, Integer.parseInt
                            //                            (titles[0]));
                            //                            Hawk.put(HawkProperty.KEY_UVC_HEIGHT, Integer.parseInt
                            //                            (titles[1]));
                            //                            if (mMediaStream != null) {
                            //                                mMediaStream.updateResolution();
                            //                            }
                            //                            mUvcService.reRequestOtg();
                        }
                        mScreenResTv.setText("Resolution:" + title);


                        dialog.dismiss();
                    }


                }).show();
    }


    /*
     * 推流or停止
     * type   第0个直播
     * */
    public void startOrStopZeroPush() {


        if (mMediaStream != null && !mMediaStream.isFirstPushStream) {
            isPushingFirstStream = true;
            try {
                //                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startPushStream(0, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
                //                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("参数初始化失败");
            }
        } else {
            isPushingFirstStream = false;
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            mMediaStream.stopPusherStream(0);
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
        stopAllPushStream();
        if (mMediaStream != null) {
            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK_UVC);
            int uvcWidth = Hawk.get(HawkProperty.KEY_UVC_WIDTH, MediaStream.uvcWidth);
            int uvcHeight = Hawk.get(HawkProperty.KEY_UVC_HEIGHT, MediaStream.uvcHeight);
            mScreenResTv.setText(String.format("%s%s%s%s", "Resolution:", uvcWidth, "x", uvcHeight));
        }
        try {
            Thread.sleep(500);
            initUvcLayout();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //        mScreenResTv.setVisibility(View.INVISIBLE);
        //        mSwitchOritation.setVisibility(View.INVISIBLE);
        //        String title = resUvcDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_UVC_RES_INDEX, 1)].toString();
        //        mScreenResTv.setText(String.format("Resolution:%s", title));
    }

    /**
     * 初始化otgCamera的布局
     */
    private void initUvcLayout() {
        initSurfaceViewLayout(1);
        SPUtil.setScreenPushingCameraIndex(this, 2);
        mSelectCameraTv.setText("Camera:" + getSelectedCamera());
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
        degrees = -1;
        if (!isBackPush) {
            if (newConfig.orientation == newConfig.ORIENTATION_LANDSCAPE) {
                //横屏
                IS_VERTICAL_SCREEN = false;
                mOperateCameraDisplayLl.setVisibility(View.VISIBLE);
                mMediaStream.setDisplayRotationDegree(90);
            } else {
                //竖屏
                mOperateCameraDisplayLl.setVisibility(View.GONE);
                surfaceView.setScaleX(1f);
                surfaceView.setScaleY(1f);
                IS_VERTICAL_SCREEN = true;
                mMediaStream.setDisplayRotationDegree(0);
            }
            if (Hawk.get(HawkProperty.HIDE_FLOAT_VIEWS, false)) {
                mFloatViewGp.setVisibility(View.GONE);
            } else {
                mFloatViewGp.setVisibility(View.VISIBLE);
            }
            //横屏
            if (surfaceView.isAvailable()) {
                if (!UVCCameraService.uvcConnected) {
                    initSurfaceViewLayout(0);

                    goonWithAvailableTexture(surfaceView.getSurfaceTexture());
                } else {
                    initUvcLayout();
                }
            }
        }
        super.onConfigurationChanged(newConfig);
    }

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
        mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
    }

    private Group mFloatViewGp;

    /**
     * 初始化预览控件的布局
     * type 0 代表原生Camera 1代表otgCamera
     */
    private void initSurfaceViewLayout(int type) {
        int width = 0;
        int height = 0;
        Display mDisplay = getWindowManager().getDefaultDisplay();
        int screenWidth = mDisplay.getWidth();
        int screenHeight = mDisplay.getHeight();
        if (0 == type) {
            Log.e(TAG, "layout   原生Camera");
            int nativeWidth = Hawk.get(HawkProperty.KEY_NATIVE_WIDTH, MediaStream.nativeWidth);
            int nativeHeight = Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT, MediaStream.nativeHeight);
            width = IS_VERTICAL_SCREEN ? nativeHeight : nativeWidth;
            height = IS_VERTICAL_SCREEN ? nativeWidth : nativeHeight;
        } else {
            Log.e(TAG, "layout   OTGCamera");

            int uvcWidth = Hawk.get(HawkProperty.KEY_UVC_WIDTH, MediaStream.uvcWidth);
            int uvcHeight = Hawk.get(HawkProperty.KEY_UVC_HEIGHT, MediaStream.uvcHeight);
            width = IS_VERTICAL_SCREEN ? uvcHeight : uvcWidth;
            height = IS_VERTICAL_SCREEN ? uvcWidth : uvcHeight;
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
        if (IS_VERTICAL_SCREEN) {
            //竖屏模式 宽度固定
            params.width = screenWidth;
            if (0 == type) {
                if (width < screenWidth) {
                    params.height = height * screenWidth / width;
                } else {
                    params.height = height * width / screenWidth;
                }
            } else {
                if (width < screenWidth) {
                    params.height = height * screenWidth / width * 2 / 5;
                } else {
                    params.height = height * width / screenWidth / 3;
                }
            }


        } else {
            //横屏模式 高度固定
            params.height = screenHeight;
            if (height < screenHeight) {
                params.width = width * screenHeight / height;
            } else {
                params.width = width * height / screenHeight;
            }
        }
        surfaceView.setLayoutParams(params); //使Set好的布局参数应用到控件
    }

    /**
     * 初始化预览控件的布局
     * type 0 代表原生摄像头 1代表otg摄像头
     */
    private void initSurfaceViewLayout(boolean isHorScreen) {
        int width = 0;
        int height = 0;
        Display mDisplay = getWindowManager().getDefaultDisplay();
        int screenWidth = mDisplay.getWidth();
        int screenHeight = mDisplay.getHeight();
        int nativeWidth = Hawk.get(HawkProperty.KEY_NATIVE_WIDTH, MediaStream.nativeWidth);
        int nativeHeight = Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT, MediaStream.nativeHeight);
        width = IS_VERTICAL_SCREEN ? nativeHeight : nativeWidth;
        height = IS_VERTICAL_SCREEN ? nativeWidth : nativeHeight;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();
        if (IS_VERTICAL_SCREEN) {
            if (isHorScreen) {
                //横屏模式 宽度固定
                params.width = screenWidth;
                params.height = width * screenWidth / height;
            } else {
                params.width = screenWidth;
                params.height = screenWidth * height / width;
            }
        } else {
            if (isHorScreen) {
                //手机横屏  画面坐旋转 或者右旋转
                //这时候 画面的宽是屏幕的高度  画面的高是  屏幕的高度/原来的宽度（这是宽度转换比）*原来的高度
                params.height = screenHeight;
                params.width = height * screenHeight / width;
            } else {
                params.height = height;
                params.width = width;
            }

        }

        surfaceView.setLayoutParams(params); //使设置好的布局参数应用到控件
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
}
