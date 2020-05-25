/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.easypusher.mine;

import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegOperateUtil;

import org.easydarwin.easypusher.BaseProjectActivity;
import org.easydarwin.easypusher.BuildConfig;
import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.record.MediaFilesActivity;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.databinding.ActivitySettingBinding;
import org.easydarwin.easypusher.mine.scan.QRScanActivity;
import org.easydarwin.easypusher.util.HawkProperty;
import org.easydarwin.easypusher.util.SPUtil;
import org.easydarwin.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置页
 */
public class SettingActivity extends BaseProjectActivity implements Toolbar.OnMenuItemClickListener, View.OnClickListener {

    public static final int REQUEST_OVERLAY_PERMISSION = 1004;  // 悬浮框
    private static final int REQUEST_SCAN_TEXT_URL_BILI = 1003;      // 扫描二维码bili
    private static final int REQUEST_SCAN_TEXT_URL_HUYA = 1005;      // 扫描二维码huya
    private static final int REQUEST_SCAN_TEXT_URL_YI = 1006;      // 扫描二维码yi
    private static final int REQUEST_SCAN_TEXT_URL_NOW = 1007;      // 扫描二维码now
    private CharSequence[] lives = new CharSequence[]{"哔哩哔哩", "虎牙直播", "斗鱼直播", "一直播", "NOW直播", "战旗TV", "西瓜视频", "映客直播", "cc直播"};
    private boolean[] selectStatus = new boolean[]{true, true, false, true, true, false, false, false, false};
    private ActivitySettingBinding binding;
    private List<Boolean> selectArray = new ArrayList<>();
    ;

    @Override
    public void onUvcCameraConnected() {

    }

    @Override
    public void onUvcCameraAttached() {

    }

    @Override
    public void onUvcCameraDisConnected() {

    }

    //    EditText url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        setSupportActionBar(binding.mainToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.mainToolbar.setOnMenuItemClickListener(this);
        // 左边的小箭头（注意需要在setSupportActionBar(toolbar)之后才有效果）
        binding.mainToolbar.setNavigationIcon(R.drawable.com_back);
        binding.registCodeValue.setText(RegOperateUtil.strreg);
        binding.pushServerIpEt.setText(Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_IP, "rtmp://ttcolour.com"));
        binding.pushServerPortEt.setText(Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_PORT, "10085"));
        binding.firstLiveValueEt.setText(Hawk.get(HawkProperty.KEY_BILIBILI_URL, ""));
        binding.secendLiveValueEt.setText(Hawk.get(HawkProperty.KEY_YI_URL, ""));
        binding.thirdLiveValueEt.setText(Hawk.get(HawkProperty.KEY_NOW_URL, ""));
        binding.firstLiveValueEt.setText(Hawk.get(HawkProperty.KEY_HU_YA_URL, ""));
        binding.liveTagEt.setText(Hawk.get(HawkProperty.KEY_SCREEN_PUSHING_TAG, "hls"));
        binding.firstLiveKey.setText(Hawk.get(HawkProperty.FIRST_LIVE, "哔哩哔哩"));
        binding.secendLiveKey.setText(Hawk.get(HawkProperty.SECENDLIVE, "虎牙直播"));
        binding.thirdLiveKey.setText(Hawk.get(HawkProperty.THIRD_LIVE, "一直播"));
        binding.fourthLiveKey.setText(Hawk.get(HawkProperty.FOURTH_LIVE, "NOW直播"));
        binding.firstLiveScanIv.setOnClickListener(this);
        binding.secendLiveScanIv.setOnClickListener(this);
        binding.thirdLiveScanIv.setOnClickListener(this);
        binding.fourthLiveScanIv.setOnClickListener(this);
        binding.firstLiveKey.setOnClickListener(this);
        binding.secendLiveKey.setOnClickListener(this);
        binding.thirdLiveKey.setOnClickListener(this);
        binding.fourthLiveKey.setOnClickListener(this);
        binding.openRecordLocalBt.setOnClickListener(this);
        // 使能摄像头后台采集
        onPushBackground();
        onEncodeType();
        // 推送内容
        onRadioGroupCheckedStatus();
        onAutoRun();
    }

    /**
     * 自启动
     */
    private void onAutoRun() {
        //        initBitrateData();
        binding.autoPushWhenRunCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Hawk.put(HawkProperty.AUTO_RUN, true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                    builder.setMessage("开启后需要手动开启软件自启动权限").setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.create().show();
                } else {
                    Hawk.put(HawkProperty.AUTO_RUN, false);
                }
            }
        });
    }

    /**
     * radiogroup的选中状态
     */
    private void onRadioGroupCheckedStatus() {
        boolean videoEnable = SPUtil.getEnableVideo(this);
        if (videoEnable) {
            boolean audioEnable = SPUtil.getEnableAudio(this);

            if (audioEnable) {
                RadioButton push_av = findViewById(R.id.push_av);
                push_av.setChecked(true);
            } else {
                RadioButton push_v = findViewById(R.id.push_v);
                push_v.setChecked(true);
            }
        } else {
            RadioButton push_a = findViewById(R.id.push_a);
            push_a.setChecked(true);
        }
        binding.pushContentRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.push_av) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_a) {
                    SPUtil.setEnableVideo(SettingActivity.this, false);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_v) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, false);
                }
            }
        });
    }

    /**
     *
     */
    private void onEncodeType() {
        // 是否使用软编码
        CheckBox x264enc = findViewById(R.id.use_x264_encode);
        x264enc.setChecked(Hawk.get(HawkProperty.KEY_SW_CODEC, true));
        x264enc.setOnCheckedChangeListener((buttonView, isChecked) -> Hawk.put(HawkProperty.KEY_SW_CODEC, isChecked)
        );

        //        // 使能H.265编码
        //        CheckBox enable_hevc_cb = findViewById(R.id.enable_hevc);
        //        enable_hevc_cb.setChecked(SPUtil.getHevcCodec(this));
        //        enable_hevc_cb.setOnCheckedChangeListener(
        //                (buttonView, isChecked) -> SPUtil.setHevcCodec(this, isChecked)
        //        );

        //        // 叠加水印
        //        CheckBox enable_video_overlay = findViewById(R.id.enable_video_overlay);
        //        enable_video_overlay.setChecked(SPUtil.getEnableVideoOverlay(this));
        //        enable_video_overlay.setOnCheckedChangeListener(
        //                (buttonView, isChecked) -> SPUtil.setEnableVideoOverlay(this, isChecked)
        //        );
    }

    /**
     * 后台采集
     */
    private void onPushBackground() {
        CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
        backgroundPushing.setChecked(SPUtil.getEnableBackgroundCamera(this));
        backgroundPushing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(SettingActivity.this)) {
                            SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                        } else {
                            new AlertDialog.Builder(SettingActivity.this).setTitle("后台上传视频").setMessage("后台上传视频需要APP出现在顶部.是否确定?").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
                                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                                    buttonView.toggle();
                                }
                            }).setCancelable(false).show();
                        }
                    } else {
                        SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                    }
                } else {
                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                }
            }
        });
    }

    //    /**
    //     * 码率
    //     */
    //    private void initBitrateData() {
    //        SeekBar sb = findViewById(R.id.bitrate_seekbar);
    //        final TextView bitrateValue = findViewById(R.id.bitrate_value);
    //
    //        int bitrate_added_kbps = SPUtil.getBitrateKbps(this);
    //        int kbps = 72000 + bitrate_added_kbps;
    //        bitrateValue.setText(kbps/1000 + "kbps");
    //
    //        sb.setMax(5000000);
    //        sb.setProgress(bitrate_added_kbps);
    //        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    //            @Override
    //            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    //                int kbps = 72000 + progress;
    //                bitrateValue.setText(kbps/1000 + "kbps");
    //            }
    //
    //            @Override
    //            public void onStartTrackingTouch(SeekBar seekBar) {
    //
    //            }
    //
    //            @Override
    //            public void onStopTrackingTouch(SeekBar seekBar) {
    //                SPUtil.setBitrateKbps(SettingActivity.this, seekBar.getProgress());
    //            }
    //        });
    //    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onBackPressed() {
        if (!isPushingStream) {
            String text = binding.pushServerIpEt.getText().toString().trim();
            if (text.contains("//")) {
                text = text.substring(text.indexOf("//") + 2, text.length());
            }
            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_IP, text);
            String textPort = binding.pushServerPortEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_PORT, textPort);
            String tag = binding.liveTagEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_TAG, tag);
        } else {
            ToastUtils.toast(mContext, "正在推流，无法更改推流地址");
        }
        if (!isPushingBiliStream) {
            String bilibili = binding.firstLiveValueEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_BILIBILI_URL, bilibili);

        } else {
            ToastUtils.toast(mContext, "正在推送哔哩直播，无法更改地址");
        }
        if (!isPushingYiStream) {
            String url = binding.thirdLiveValueEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_YI_URL, url);

        } else {
            ToastUtils.toast(mContext, "正在推送一直播，无法更改地址");
        }
        if (!isPushingNowStream) {
            String url = binding.fourthLiveValueEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_NOW_URL, url);

        } else {
            ToastUtils.toast(mContext, "正在推送Now直播，无法更改地址");
        }
        if (!isPushingHuyaStream) {
            String huya = binding.secendLiveValueEt.getText().toString().trim();
            Hawk.put(HawkProperty.KEY_HU_YA_URL, huya);
        } else {
            ToastUtils.toast(mContext, "正在推送虎牙直播，无法更改地址");
        }
        String registCode = binding.registCodeValue.getText().toString().trim();
        Hawk.put(HawkProperty.KEY_REGIST_CODE, registCode);
        super.onBackPressed();
    }


    //    /*
    //    * 二维码扫码
    //    * */
    //    public void onScanQRCode(View view) {
    //        Intent intent = new Intent(this, ScanQRActivity.class);
    //        startActivityForResult(intent, REQUEST_SCAN_TEXT_URL);
    //        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
    //    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDraw = Settings.canDrawOverlays(this);
                SPUtil.setEnableBackgroundCamera(SettingActivity.this, canDraw);

                if (!canDraw) {
                    CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
                    backgroundPushing.setChecked(false);
                }
            }
        } else if (requestCode == REQUEST_SCAN_TEXT_URL_BILI) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("result");
                this.binding.firstLiveValueEt.setText(url);
            }

        } else if (requestCode == REQUEST_SCAN_TEXT_URL_HUYA) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("result");
                this.binding.secendLiveValueEt.setText(url);
            }
        } else if (requestCode == REQUEST_SCAN_TEXT_URL_YI) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("result");
                this.binding.thirdLiveValueEt.setText(url);
            }
        } else if (requestCode == REQUEST_SCAN_TEXT_URL_NOW) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("result");
                this.binding.fourthLiveValueEt.setText(url);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    // 返回的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_record_local_bt:
                Intent intent = new Intent(this, MediaFilesActivity.class);
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
                break;
            case R.id.first_live_scan_iv:
                startActivityForResult(new Intent(this, QRScanActivity.class), REQUEST_SCAN_TEXT_URL_BILI);
                break;
            case R.id.secend_live_scan_iv:
                startActivityForResult(new Intent(this, QRScanActivity.class), REQUEST_SCAN_TEXT_URL_HUYA);
                break;
            case R.id.third_live_scan_iv:
                startActivityForResult(new Intent(this, QRScanActivity.class), REQUEST_SCAN_TEXT_URL_YI);
                break;
            case R.id.fourth_live_scan_iv:
                startActivityForResult(new Intent(this, QRScanActivity.class), REQUEST_SCAN_TEXT_URL_NOW);
                break;
            case R.id.first_live_key:
                selectLiveType(1);
                break;
            case R.id.secend_live_key:
                selectLiveType(2);
                break;
            case R.id.third_live_key:
                selectLiveType(3);
                break;
            case R.id.fourth_live_key:
                selectLiveType(4);
                break;
            default:
                break;
        }
    }

    /**
     * 选择平台
     *
     * @param type
     */
    private void selectLiveType(int type) {
        new AlertDialog.Builder(mContext).setSingleChoiceItems(getCharSequence(), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CharSequence name = getCharSequence()[which];
                switch (type) {
                    case 1:
                        binding.firstLiveKey.setText(name);
                        Hawk.put(HawkProperty.FIRST_LIVE, name);
                        break;
                    case 2:
                        binding.secendLiveKey.setText(name);
                        Hawk.put(HawkProperty.SECENDLIVE, name);
                        break;
                    case 3:
                        binding.thirdLiveKey.setText(name);
                        Hawk.put(HawkProperty.THIRD_LIVE, name);
                        break;
                    case 4:
                        binding.fourthLiveKey.setText(name);
                        Hawk.put(HawkProperty.FOURTH_LIVE, name);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        }).show();
    }

    //"哔哩哔哩", "虎牙直播", "斗鱼直播", "一直播  ", "NOW直播",
    private List<String> getLives() {
        List<String> arrays = new ArrayList<>();
        arrays.add(Hawk.get(HawkProperty.FIRST_LIVE, "哔哩哔哩"));
        arrays.add(Hawk.get(HawkProperty.SECENDLIVE, "虎牙直播"));
        arrays.add(Hawk.get(HawkProperty.THIRD_LIVE, "一直播"));
        arrays.add(Hawk.get(HawkProperty.FOURTH_LIVE, "NOW直播"));
        return arrays;
    }

    private CharSequence[] getCharSequence() {
        List<CharSequence> charSequences = new ArrayList<>();
        for (int i = 0; i < lives.length; i++) {
            CharSequence life = lives[i];
            if (!getLives().contains(life)) {
                charSequences.add(life);
            }
        }
        return charSequences.toArray(new CharSequence[charSequences.size()]);
    }
}
