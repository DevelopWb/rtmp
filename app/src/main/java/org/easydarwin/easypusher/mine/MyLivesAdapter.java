package org.easydarwin.easypusher.mine;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.bean.LiveBean;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/8/30 17:15
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/8/30 17:15
 */
public class MyLivesAdapter extends BaseQuickAdapter<LiveBean, BaseViewHolder> {
    public MyLivesAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, LiveBean item) {
        int type = item.getItemType();
        if (0 == type) {
            helper.setText(R.id.live_item_name_tv, item.getLiveName());
            if (item.isSelect()) {
                helper.setVisible(R.id.select_status_iv,true);
            }else {
                helper.setVisible(R.id.select_status_iv,false);

            }
        }else {
            helper.setText(R.id.live_item_name_tv,"+");
            helper.setVisible(R.id.select_status_iv,false);
        }

    }

}
