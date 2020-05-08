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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;
import com.squareup.otto.Subscribe;

import org.easydarwin.bus.StartRecord;
import org.easydarwin.bus.StopRecord;
import org.easydarwin.bus.StreamStat;
import org.easydarwin.easypusher.BackgroundCameraService;
import org.easydarwin.easypusher.BaseProjectActivity;
import org.easydarwin.easypusher.PushCallback;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.UVCCameraService;
import org.easydarwin.easypusher.util.Config;
import org.easydarwin.easypusher.util.DoubleClickListener;
import org.easydarwin.easypusher.util.HawkProperty;
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
public class StreamActivity extends BaseProjectActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {
    static final String TAG = "StreamActivity";
    private CharSequence[] resDisplay = new CharSequence[]{"640x480", "1280x720", "1920x1080", "2560x1440", "3840x2160"};
    public static final int REQUEST_MEDIA_PROJECTION = 1002;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;

    // 默认分辨率
    int width = 1280, height = 720;
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
//                    mScreenResTv.setVisibility(View.VISIBLE);
//                    mSelectCameraTv.setText("摄像头:后置");
//                    if (mMediaStream != null) {
//                        mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
//                    }

//                    int position = SPUtil.getScreenPushingCameraIndex(StreamActivity.this);
//                    switch (position) {
//                        case 0:
//                            mSelectCameraTv.setText("摄像头:后置");
//                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_BACK);
//                            break;
//                        case 1:
//                            mSelectCameraTv.setText("摄像头:前置");
//                            mMediaStream.switchCamera(MediaStream.CAMERA_FACING_FRONT);
//                            break;
//                        default:
//                            break;
//                    }
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
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_interval_shape, 0, 0, 0);
            }

            textRecordTick.removeCallbacks(this);
            textRecordTick.postDelayed(this, 1000);
        }
    };


    private TextureView surfaceView;
    private ImageView mPushBgIv;
    private ImageView mPushStreamIv;
    private ImageView mBiliIv;
    private ImageView mVedioPushBottomTagIv;
    private ImageView mHuyaIv, mBlackBgIv;
    private Intent uvcServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        BUSUtil.BUS.register(this);
//        RegOperateUtil.getInstance(this);


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
        mScreenResTv = findViewById(R.id.txt_res);
        surfaceView = findViewById(R.id.sv_surfaceview);
        //        mPushBgIv = (ImageView) findViewById(R.id.push_bg_iv);
        //        mPushBgIv.setOnClickListener(this);
        mPushStreamIv = (ImageView) findViewById(R.id.push_stream_iv);
        mPushStreamIv.setOnClickListener(this);
        mBiliIv = (ImageView) findViewById(R.id.bili_iv);
        mBiliIv.setOnClickListener(this);
        mHuyaIv = (ImageView) findViewById(R.id.huya_iv);
        mBlackBgIv = (ImageView) findViewById(R.id.black_bg_iv);
        mHuyaIv.setOnClickListener(this);
        mVedioPushBottomTagIv = findViewById(R.id.streaming_activity_push);
        mBlackBgIv.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick(View v) {
                mBlackBgIv.setVisibility(View.GONE);
                //推流
                mPushStreamIv.performClick();
            }

            @Override
            public void onOneClick(View v) {

            }
        });
        String title = resDisplay[Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 3)].toString();
        mScreenResTv.setText(String.format("分辨率:%s", title));
        initSurfaceViewClick();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        goonWithPermissionGranted();
    }

    @Override
    protected void onDestroy() {
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
                mMediaStream.stopPusherStream();
                mMediaStream.stopBiliPusherStream();
                mMediaStream.stopHuyaPusherStream();
                mMediaStream.release();
                mMediaStream = null;
                stopService(new Intent(this, BackgroundCameraService.class));
                stopService(new Intent(this, UVCCameraService.class));
            }
        }
        super.onDestroy();
    }

    /**
     * 是否正在推流
     */
    private boolean isStreaming() {
        return mMediaStream != null && (mMediaStream.isPushStreaming() || mMediaStream.isBiliPushStreaming() || mMediaStream.isHuyaPushStreaming());
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
        }
    }

    //    /*
    //     * 推送屏幕
    //     * */
    //    private void startScreenPushIntent() {
    //        if (StreamActivity.mResultIntent != null && StreamActivity.mResultCode != 0) {
    //            Intent intent = new Intent(getApplicationContext(), RecordService.class);
    //            startService(intent);
    //
    //            ImageView im = findViewById(R.id.streaming_activity_push_screen);
    //            im.setImageResource(R.drawable.push_screen_click);
    //
    //            TextView viewById = findViewById(R.id.push_screen_url);
    //            viewById.setText(Config.getServerURL(this));
    //        } else {
    //            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    //                // 2.创建屏幕捕捉的Intent
    //                MediaProjectionManager mMpMngr = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
    //                startActivityForResult(mMpMngr.createScreenCaptureIntent(), StreamActivity.REQUEST_MEDIA_PROJECTION);
    //            }
    //        }
    //    }

    private void goonWithPermissionGranted() {

//        RegOperateUtil regOprateUtil = RegOperateUtil.getInstance(this);
//        regOprateUtil.setCancelCallBack(new RegLatestContact.CancelCallBack() {
//            @Override
//            public void toFinishActivity() {
//
//            }
//
//            @Override
//            public void toDoNext() {
//
//            }
//        });
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
        surfaceView.setSurfaceTextureListener(this);
        surfaceView.setOnClickListener(new DoubleClickListener() {
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

    //    /*
    //     * 显示key有效期
    //     * */
    //    private void notifyAboutColorChange() {
    //        ImageView iv = findViewById(R.id.toolbar_about);
    //
    //        if (EasyApplication.activeDays >= 9999) {
    //            iv.setImageResource(R.drawable.green);
    //        } else if (EasyApplication.activeDays > 0) {
    //            iv.setImageResource(R.drawable.yellow);
    //        } else {
    //            iv.setImageResource(R.drawable.red);
    //        }
    //    }

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

            if (ms.isPushStreaming() || ms.isBiliPushStreaming() || ms.isHuyaPushStreaming()) {
                String url = Config.getServerURL();
//                txtStreamAddress.setText(url);

                sendMessage(getPushStatusMsg());

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
        mMediaStream.updateResolution(width, height);
        mMediaStream.setDisplayRotationDegree(getDisplayRotationDegree());
        mMediaStream.createCamera(getSelectedCameraIndex());
        mMediaStream.startPreview();

        sendMessage(getPushStatusMsg());
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

    //    /*
    //     * 初始化下拉控件的列表（显示分辨率）
    //     * */
    //    private void initSpinner() {
    //        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spn_item, listResolution);
    //        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    //        spnResolution.setAdapter(adapter);
    //
    //        int position = listResolution.indexOf(String.format("%dx%d", width, height));
    //        spnResolution.setSelection(position, false);
    //
    //        spnResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    //            @Override
    //            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    //
    //            }
    //
    //            @Override
    //            public void onNothingSelected(AdapterView<?> parent) {
    //
    //            }
    //        });
    //    }

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
        streamStat.post(() -> streamStat.setText(getString(R.string.stream_stat, stat.framePerSecond, stat.bytesPerSecond * 8 / 1024)));
    }

    //    /*
    //     * 获取可以支持的分辨率
    //     * */
    //    @Subscribe
    //    public void onSupportResolution(SupportResolution res) {
    //        runOnUiThread(() -> {
    //            listResolution = Util.getSupportResolution(getApplicationContext());
    //            boolean supportdefault = listResolution.contains(String.format("%dx%d", width, height));
    //
    //            if (!supportdefault) {
    //                String r = listResolution.get(0);
    //                String[] splitR = r.split("x");
    //
    //                width = Integer.parseInt(splitR[0]);
    //                height = Integer.parseInt(splitR[1]);
    //            }
    //
    //            initSpinner();
    //        });
    //    }

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
                sendMessage(getPushStatusMsg());
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
     * @return
     */
    private String getPushStatusMsg() {
        if (mMediaStream.isPushStreaming()) {
            if (mMediaStream.isBiliPushStreaming()||mMediaStream.isHuyaPushStreaming()) {
                return "取证+直播中";
            }
            return "取证中";
        }else{
            if (mMediaStream.isBiliPushStreaming()||mMediaStream.isHuyaPushStreaming()) {
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
        //        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        //
        //        if (isStreaming && SPUtil.getEnableBackgroundCamera(this)) {
        //            new AlertDialog.Builder(this).setTitle("是否允许后台上传？")
        //                    .setMessage("您设置了使能摄像头后台采集,是否继续在后台采集并上传视频？如果是，记得直播结束后,再回来这里关闭直播。")
        //                    .setNeutralButton("后台采集", (dialogInterface, i) -> {
        //                        StreamActivity.super.onBackPressed();
        //                    })
        //                    .setPositiveButton("退出程序", (dialogInterface, i) -> {
        //                        mMediaStream.stopStream();
        //                        StreamActivity.super.onBackPressed();
        //                        Toast.makeText(StreamActivity.this, "程序已退出。", Toast.LENGTH_SHORT).show();
        //                    })
        //                    .setNegativeButton(android.R.string.cancel, null)
        //                    .show();
        //            return;
        //        } else {
        //            super.onBackPressed();
        //        }


        if (isStreaming() && SPUtil.getEnableBackgroundCamera(this)) {
            new AlertDialog.Builder(this).setTitle("是否允许后台上传？").setMessage("您设置了使能摄像头后台采集,是否继续在后台采集并上传视频？如果是，记得直播结束后,再回来这里关闭直播。").setNeutralButton("后台采集", (dialogInterface, i) -> {
                StreamActivity.super.onBackPressed();
            }).setPositiveButton("退出程序", (dialogInterface, i) -> {
                mMediaStream.stopPusherStream();
                mMediaStream.stopBiliPusherStream();
                mMediaStream.stopHuyaPusherStream();
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
                new AlertDialog.Builder(this).setTitle("选择摄像头").setSingleChoiceItems(getCameras(), getSelectedCameraIndex(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isStreaming()) {
                            Toast.makeText(StreamActivity.this, getPushStatusMsg()+",无法切换摄像头", Toast.LENGTH_SHORT).show();
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
            case R.id.bili_iv:
                String url_bili = Hawk.get(HawkProperty.KEY_BILIBILI_URL);
                if (TextUtils.isEmpty(url_bili)) {
                    Toast.makeText(getApplicationContext(), "还没有配置哔哩哔哩直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopBiliPush();
                break;
            case R.id.huya_iv:
                String url_huya = Hawk.get(HawkProperty.KEY_HU_YA_URL);
                if (TextUtils.isEmpty(url_huya)) {
                    Toast.makeText(getApplicationContext(), "还没有配置虎牙直播地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                startOrStopHuyaPush();
                break;
        }
    }

    /**
     * 获取摄像头数据
     *
     * @return
     */
    private CharSequence[] getCameras() {
        if (UVCCameraService.uvcConnected) {
            return new CharSequence[]{"外置摄像头"};
        }
        return new CharSequence[]{"后置摄像头", "前置摄像头"};

    }

    /**
     * 获取选择的摄像头的index
     *
     * @return
     */
    private int getSelectedCameraIndex() {
        int position = SPUtil.getScreenPushingCameraIndex(this);
        if (UVCCameraService.uvcConnected) {
            SPUtil.setScreenPushingCameraIndex(this, 2);
            return 2;
        }
        return position;

    }

    /**
     * 获取选择的摄像头的index
     *
     * @return
     */
    private String getSelectedCamera() {
        int position = SPUtil.getScreenPushingCameraIndex(this);
        if (UVCCameraService.uvcConnected) {
            SPUtil.setScreenPushingCameraIndex(this, 2);
            return "外置";
        }

        return 0 == position ? "后置" : "前置";

    }

    /*
     * 录像
     * */
    public void onRecord(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
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
    }

    //    /*
    //     * 推送屏幕
    //     * */
    //    public void onPushScreen(final View view) {
    //        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
    //            new AlertDialog.Builder(this).setMessage("推送屏幕需要安卓5.0以上,您当前系统版本过低,不支持该功能。").setTitle("抱歉").show();
    //            return;
    //        }
    //
    //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    //            if (!Settings.canDrawOverlays(this)) {
    //                new AlertDialog.Builder(this)
    //                        .setMessage("推送屏幕需要APP出现在顶部.是否确定?")
    //                        .setPositiveButton(android.R.string.ok,
    //                                (dialogInterface, i) -> {
    //                                    // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
    //                                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
    //                                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    //                                })
    //                        .setNegativeButton(android.R.string.cancel, null)
    //                        .setCancelable(false)
    //                        .show();
    //                return;
    //            }
    //        }
    //
    //        if (!SPUtil.getScreenPushing(this)) {
    //            new AlertDialog.Builder(this).setTitle("提醒").setMessage("屏幕直播将要开始,直播过程中您可以切换到其它屏幕。不过记得直播结束后,再进来停止直播哦!").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    //                @Override
    //                public void onClick(DialogInterface dialogInterface, int i) {
    //                    SPUtil.setScreenPushing(StreamActivity.this, true);
    //                    onPushScreen(view);
    //                }
    //            }).show();
    //            return;
    //        }
    //
    //        if (RecordService.mEasyPusher != null) {
    //            Intent intent = new Intent(getApplicationContext(), RecordService.class);
    //            stopService(intent);
    //
    //            TextView viewById = findViewById(R.id.push_screen_url);
    //            viewById.setText(Config.getServerURL(this));
    //
    //            ImageView im = findViewById(R.id.streaming_activity_push_screen);
    //            im.setImageResource(R.drawable.push_screen);
    //        } else {
    //            startScreenPushIntent();
    //        }
    //    }

    /*
     * 切换分辨率
     * */
    public void onClickResolution(View view) {
        new AlertDialog.Builder(this).setTitle("设置分辨率").setSingleChoiceItems(resDisplay, Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, 3), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                String title = resDisplay[position].toString();
                if (!Util.getSupportResolution(StreamActivity.this).contains(title)) {
                    Toast.makeText(StreamActivity.this, "您的相机不支持此分辨率", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }
                if (isStreaming()) {
                    Toast.makeText(StreamActivity.this, getPushStatusMsg()+",无法切换分辨率", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }
                Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_RES_INDEX, position);
                mScreenResTv.setText("分辨率:" + title);
                String[] splitR = title.split("x");

                int wh = Integer.parseInt(splitR[0]);
                int ht = Integer.parseInt(splitR[1]);

                if (width != wh || height != ht) {
                    width = wh;
                    height = ht;

                    if (mMediaStream != null) {
                        mMediaStream.updateResolution(width, height);
                    }
                }
                dialog.dismiss();
            }


        }).show();
    }

    //    /*
    //     * 切换屏幕方向
    //     * */
    //    public void onSwitchOrientation(View view) {
    //        if (mMediaStream != null) {
    //            if (mMediaStream.isStreaming()){
    //                Toast.makeText(this,"正在推送中,无法更改屏幕方向", Toast.LENGTH_SHORT).show();
    //                return;
    //            }
    //        }
    //
    //        int orientation = getRequestedOrientation();
    //
    //        if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
    //            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    //        } else {
    //            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    //        }
    //
    ////        if (mMediaStream != null)
    ////            mMediaStream.setDisplayRotationDegree(getDisplayRotationDegree());
    //    }

    /*
     * 推流or停止
     * type   推流
     * */
    public void startOrStopPush() {

        String url = Config.getServerURL();
        if (mMediaStream != null && !mMediaStream.isPushStreaming()) {
            isPushingStream = true;
            try {
                mMediaStream.startUrlStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mPushStreamIv.setImageResource(R.mipmap.push_stream_on);
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
//                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingStream = false;
            mMediaStream.stopPusherStream();
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            mPushStreamIv.setImageResource(R.mipmap.push_stream_off);
            sendMessage("断开连接");
        }
    }

    /*
     * 推流or停止
     * type   bili直播
     * */
    public void startOrStopBiliPush() {

        String url_bi = Hawk.get(HawkProperty.KEY_BILIBILI_URL);
        if (mMediaStream != null && !mMediaStream.isBiliPushStreaming()) {
            isPushingBiliStream = true;
            try {
//                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startBiliUrlStream(url_bi, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mBiliIv.setImageResource(R.mipmap.bilibili_on);

                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
//                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingBiliStream = false;
            mMediaStream.stopBiliPusherStream();
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            mBiliIv.setImageResource(R.mipmap.bilibili_off);
            sendMessage("断开连接");
        }
    }

    /*
     * 推流or停止
     * type   虎牙直播
     * */
    public void startOrStopHuyaPush() {

        String url_hu = Hawk.get(HawkProperty.KEY_HU_YA_URL);
        if (mMediaStream != null && !mMediaStream.isHuyaPushStreaming()) {
            isPushingHuyaStream = true;
            try {
//                mMediaStream.startStream(url, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mMediaStream.startHuyaUrlStream(url_hu, code -> BUSUtil.BUS.post(new PushCallback(code)));
                mHuyaIv.setImageResource(R.mipmap.huya_on);
                mVedioPushBottomTagIv.setImageResource(R.drawable.start_push_pressed);
//                txtStreamAddress.setText(url);
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage("激活失败，无效Key");
            }
        } else {
            isPushingHuyaStream = false;
            mMediaStream.stopHuyaPusherStream();
            mVedioPushBottomTagIv.setImageResource(R.drawable.start_push);
            mHuyaIv.setImageResource(R.mipmap.huya_off);
            sendMessage("断开连接");
        }
    }


    //    /*
    //     * 关于我们
    //     * */
    //    public void onAbout(View view) {
    //        Intent intent = new Intent(this, AboutActivity.class);
    //        startActivityForResult(intent, 0);
    //        overridePendingTransition(R.anim.slide_right_in,R.anim.slide_left_out);
    //    }

    /*
     * 设置
     * */
    public void onSetting(View view) {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivityForResult(intent, 0);
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
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
        mSelectCameraTv.setText("摄像头:" + getSelectedCamera());
        mScreenResTv.setVisibility(View.INVISIBLE);
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
}
