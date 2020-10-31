package org.easydarwin.easypusher.push;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.LiveBean;
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
        helper.setText(R.id.plate_name_tv,plateName);
        if (plateName.equals(SettingActivity.LIVE_TYPE_BILI)||plateName.equals(SettingActivity.LIVE_TYPE_HUYA)
                ||plateName.equals(SettingActivity.LIVE_TYPE_DOUYU)
                ||plateName.equals(SettingActivity.LIVE_TYPE_XIGUA)
                ||plateName.equals(SettingActivity.LIVE_TYPE_YI)) {
            helper.setGone(R.id.plate_icon_iv,true);
            helper.setGone(R.id.plate_name_tv,false);

        }else {
            helper.setGone(R.id.plate_icon_iv,false);
            helper.setGone(R.id.plate_name_tv,true);
        }

    }
}
