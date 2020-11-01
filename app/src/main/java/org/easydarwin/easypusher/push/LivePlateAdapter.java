package org.easydarwin.easypusher.push;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.bean.LiveBean;
import org.easydarwin.easypusher.mine.SettingActivity;

/**
 * @Author: tobato
 * @Description: 作用描述  直播平台适配器
 * @CreateDate: 2020/10/31 16:22
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/10/31 16:22
 */
public class LivePlateAdapter extends BaseQuickAdapter<LiveBean, BaseViewHolder> {
    public LivePlateAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, LiveBean item) {
        String plateName = item.getLiveName();
        boolean isPushing = item.isPushing();
        helper.setText(R.id.plate_name_tv, plateName);
        if (plateName.equals(SettingActivity.LIVE_TYPE_BILI) || plateName.equals(SettingActivity.LIVE_TYPE_HUYA)
                || plateName.equals(SettingActivity.LIVE_TYPE_DOUYU)
                || plateName.equals(SettingActivity.LIVE_TYPE_XIGUA)
                || plateName.equals(SettingActivity.LIVE_TYPE_YI)) {
            helper.setGone(R.id.plate_icon_iv, true);
            helper.setGone(R.id.plate_name_tv, false);
            if (isPushing) {
                switch (plateName) {
                    case SettingActivity.LIVE_TYPE_BILI:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.bilibili_on);
                        break;
                    case SettingActivity.LIVE_TYPE_HUYA:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.huya_on);
                        break;
                    case SettingActivity.LIVE_TYPE_DOUYU:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.douyu_live_on);
                        break;
                    case SettingActivity.LIVE_TYPE_XIGUA:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.xigua_live_on);
                        break;
                    case SettingActivity.LIVE_TYPE_YI:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.yi_live_on);
                        break;
                    default:
                        break;
                }
            }else {
                switch (plateName) {
                    case SettingActivity.LIVE_TYPE_BILI:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.bilibili_off);
                        break;
                    case SettingActivity.LIVE_TYPE_HUYA:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.huya_off);
                        break;
                    case SettingActivity.LIVE_TYPE_DOUYU:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.douyu_live_off);
                        break;
                    case SettingActivity.LIVE_TYPE_XIGUA:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.xigua_live_off);
                        break;
                    case SettingActivity.LIVE_TYPE_YI:
                        helper.setImageResource(R.id.plate_icon_iv,R.mipmap.yi_live_off);
                        break;
                    default:
                        break;
                }
            }
        } else {
            helper.setGone(R.id.plate_icon_iv, false);
            helper.setGone(R.id.plate_name_tv, true);
            if (isPushing) {
                helper.setBackgroundRes(R.id.plate_name_tv,R.drawable.bt_green_clicked);
            }else {
                helper.setBackgroundRes(R.id.plate_name_tv,R.drawable.sp_filled_gray_circle);
            }
        }

    }
}
