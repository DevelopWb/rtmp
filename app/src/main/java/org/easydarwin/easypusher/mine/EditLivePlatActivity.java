package org.easydarwin.easypusher.mine;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.juntai.wisdom.basecomponent.base.BaseMvpActivity;
import com.juntai.wisdom.basecomponent.mvp.BasePresenter;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.bean.LiveBean;
import org.easydarwin.easypusher.mine.scan.QRScanActivity;
import org.easydarwin.easypusher.util.PublicUtil;

import java.util.List;

/**
 * @aouther tobato
 * @description 描述 添加平台
 */
public class EditLivePlatActivity extends BaseMvpActivity implements View.OnClickListener {

    private EditText mPlatNameValueEt;
    private ImageView mPushScanIv;
    private EditText mPushUrlValueEt;
    private static final int REQUEST_SCAN_TEXT_URL = 1003;      // 扫描二维码
    /**
     * 确定
     */
    private TextView mAddPlatConfirmTv;

    public static String PLATE = "platname";//平台名称
    public static String PLATE_LIVE_SIZE = "platname_live";//直播平台
    private TextView mPushAddr, mPushAddr2;
    /**
     * 是
     */
    private RadioButton mShowLiveRb;
    /**
     * 否
     */
    private RadioButton mUnShowLiveRb;
    private RadioGroup mShowLiveRg;
    /**
     * 删除
     */
    private TextView mDelLiveTv;

    private boolean isCanShowLive = false;//是否可添加到直播平台
    private LiveBean liveBean;
    private boolean isShow;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    public int getLayoutView() {
        return R.layout.activity_add_live_plat;
    }

    @Override
    public void initView() {
        setTitleName("直播平台信息");
        mPlatNameValueEt = (EditText) findViewById(R.id.plat_name_value_et);
        mPushScanIv = (ImageView) findViewById(R.id.push_scan_iv);
        mPushScanIv.setOnClickListener(this);
        mPushUrlValueEt = (EditText) findViewById(R.id.push_url_value_et);
        mAddPlatConfirmTv = (TextView) findViewById(R.id.add_plat_confirm_tv);
        mPushAddr = (TextView) findViewById(R.id.push_addr_tv);
        mPushAddr2 = (TextView) findViewById(R.id.push_url_key_tv);
        mAddPlatConfirmTv.setOnClickListener(this);
        mShowLiveRb = (RadioButton) findViewById(R.id.show_live_rb);
        mUnShowLiveRb = (RadioButton) findViewById(R.id.un_show_live_rb);
        mShowLiveRg = (RadioGroup) findViewById(R.id.show_live_rg);
        mDelLiveTv = (TextView) findViewById(R.id.del_live_tv);
        mDelLiveTv.setOnClickListener(this);
        mShowLiveRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.show_live_rb:
                        if (!isShow) {
                            if (isCanShowLive) {
                                mShowLiveRg.check(R.id.show_live_rb);
                            } else {
                                mShowLiveRg.check(R.id.un_show_live_rb);
                                if (PublicUtil.isMoreThanTheAndroid10()) {
                                    ToastUtils.toast(mContext, "最多只能显示5个直播平台");
                                } else {
                                    ToastUtils.toast(mContext, "最多只能显示2个直播平台");
                                }

                            }

                        }

                        break;
                    case R.id.un_show_live_rb:
                        mShowLiveRg.check(R.id.un_show_live_rb);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void initData() {
        if (getIntent() != null) {
            int selectedSize = getIntent().getIntExtra(PLATE_LIVE_SIZE, 0);
            if (PublicUtil.isMoreThanTheAndroid10()) {
                //不能大于5
                if (selectedSize > 4) {
                    isCanShowLive = false;
                } else {
                    isCanShowLive = true;
                }
            } else {
                //不能大于2
                if (selectedSize > 1) {
                    isCanShowLive = false;
                } else {
                    isCanShowLive = true;
                }
            }
            liveBean = (LiveBean) getIntent().getSerializableExtra(PLATE);
            String urlContent = liveBean.getPushUrlCustom();
            if (!TextUtils.isEmpty(urlContent)) {
                mPushUrlValueEt.setText(urlContent);
            }
            String plateName = liveBean.getLiveName();
            isShow = liveBean.isSelect();
            if (isShow) {
                mShowLiveRg.check(R.id.show_live_rb);
            } else {
                mShowLiveRg.check(R.id.un_show_live_rb);
            }
            switch (liveBean.getItemType()) {
                case 1:
                    mPlatNameValueEt.setBackgroundResource(R.drawable.setting_url_shape);
                    mPlatNameValueEt.setClickable(true);
                    mPlatNameValueEt.setFocusable(true);
                    mDelLiveTv.setVisibility(View.GONE);
                    break;
                default:
                    mPlatNameValueEt.setBackgroundResource(0);
                    mPlatNameValueEt.setClickable(false);
                    mPlatNameValueEt.setFocusable(false);
                    mPlatNameValueEt.setText(plateName);
                    switch (plateName) {
                        case SettingActivity.LIVE_TYPE_BILI:
                            initDefaultPlate(getString(R.string.biliurl));
                            break;
                        case SettingActivity.LIVE_TYPE_HUYA:
                            initDefaultPlate(getString(R.string.huyaurl));
                            break;
                        case SettingActivity.LIVE_TYPE_DOUYU:
                            initDefaultPlate(getString(R.string.douyuurl));
                            break;
                        case SettingActivity.LIVE_TYPE_XIGUA:
                            initDefaultPlate(getString(R.string.xiguaurl));
                            break;
                        case SettingActivity.LIVE_TYPE_YI:
                            initDefaultPlate(getString(R.string.yiurl));
                            break;
                        default:
                            mPushAddr.setVisibility(View.GONE);
                            mPushAddr2.setText("推流地址");
                            mDelLiveTv.setVisibility(View.VISIBLE);
                            break;
                    }

                    break;
            }
        }

    }

    /**
     * 默认5个平台的状态
     */
    private void initDefaultPlate(String pushAddr) {
        //显示推流固定头链接
        mPushAddr.setVisibility(View.VISIBLE);
        mPushAddr2.setText("推流码");
        mPushAddr.append(pushAddr);
        mDelLiveTv.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.push_scan_iv:
                startActivityForResult(new Intent(this, QRScanActivity.class),
                        REQUEST_SCAN_TEXT_URL);
                break;
            case R.id.add_plat_confirm_tv:
                boolean isShow = false;
                if (mShowLiveRb.isChecked()) {
                    isShow = true;
                }
                String name = mPlatNameValueEt.getText().toString().trim();
                String pushUrl = mPushUrlValueEt.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    ToastUtils.toast(mContext, getString(R.string.notice_plat_name));
                    return;
                }
                if (TextUtils.isEmpty(pushUrl)) {
                    ToastUtils.toast(mContext, getString(R.string.notice_push_url));
                    return;
                }
                List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);

                switch (liveBean.getItemType()) {
                    case 1:
                        if (hasPlateName(name)) {
                            ToastUtils.toast(mContext, getString(R.string.notice_add_platform));
                            return;
                        }
                        //添加平台
                        liveBean.config(name, 0, isShow, 0).setUrlCustom(pushUrl);
                        arrays.add(liveBean);

                        break;
                    default:
                        liveBean.setUrlCustom(pushUrl).setSelect(isShow);
                        int position = 0;
                        //替换之前的平台
                        for (int i = 0; i < arrays.size(); i++) {
                            LiveBean bean = arrays.get(i);
                            if (bean.getLiveName().equals(liveBean.getLiveName())) {
                                position = i;
                                break;
                            }
                        }
                        arrays.remove(position);
                        arrays.add(position, liveBean);
                        break;
                }
                Hawk.put(HawkProperty.PLATFORMS, arrays);
                finish();
                break;
            case R.id.del_live_tv:
                //删除平台
                new AlertDialog.Builder(mContext)
                        .setMessage("是否删除当前平台")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                List<LiveBean> plates = Hawk.get(HawkProperty.PLATFORMS);
                                int position = 0;
                                for (int i = 0; i < plates.size(); i++) {
                                    LiveBean bean = plates.get(i);
                                    if (bean.getLiveName().equals(liveBean.getLiveName())) {
                                        position = i;
                                        break;
                                    }
                                }
                                plates.remove(position);
                                Hawk.put(HawkProperty.PLATFORMS, plates);
                                finish();

                            }
                        }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
                break;
        }
    }

    /**
     * 是否已经存在相同名称的平台了
     *
     * @param name
     * @return
     */
    private boolean hasPlateName(String name) {
        boolean hasPlate = false;
        List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);
        for (LiveBean array : arrays) {
            if (name.equals(array.getLiveName())) {
                hasPlate = true;
                break;
            }
        }
        return hasPlate;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN_TEXT_URL) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("result");
                mPushUrlValueEt.setText(url);
            }

        }
    }

    @Override
    public void onSuccess(String tag, Object o) {

    }

}
