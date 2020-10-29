package org.easydarwin.easypusher.mine;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.juntai.wisdom.basecomponent.base.BaseMvpActivity;
import com.juntai.wisdom.basecomponent.mvp.BasePresenter;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.scan.QRScanActivity;

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
    private TextView mPushAddr, mPushAddr2;

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
    }

    @Override
    public void initData() {
        if (getIntent() != null) {
            LiveBean liveBean = (LiveBean) getIntent().getSerializableExtra(PLATE);
            String plateName = liveBean.getLiveName();
            switch (liveBean.getItemType()) {
                case 1:
                    mPlatNameValueEt.setBackgroundResource(R.drawable.setting_url_shape);
                    mPlatNameValueEt.setClickable(true);
                    mPlatNameValueEt.setFocusable(true);
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
                            break;
                    }

                break;
            }
        }

    }

    /**
     * 默认5个平台的状态
     */
    private void initDefaultPlate(String  pushAddr) {
        //显示推流固定头链接
        mPushAddr.setVisibility(View.VISIBLE);
        mPushAddr2.setText("推流码");
        mPushAddr.append(pushAddr);
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
                if (arrays.contains(name)) {
                    ToastUtils.toast(mContext, getString(R.string.notice_add_platform));
                    return;
                }
                if (arrays != null) {
                    arrays.add(new LiveBean(name, R.mipmap.cc_live_off, false, 0));
                    Hawk.put(HawkProperty.PLATFORMS, arrays);
                }
                finish();
                break;
        }
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
