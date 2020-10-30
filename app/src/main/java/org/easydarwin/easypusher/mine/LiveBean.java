package org.easydarwin.easypusher.mine;

import java.io.Serializable;

/**
 * @Author: tobato
 * @Description: 作用描述  直播平台对象
 * @CreateDate: 2020/8/30 17:17
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/8/30 17:17
 */
public class LiveBean implements Serializable {

    private String liveName;//平台名称
    private String pushUrlHeard;//推流地址固定部分  只有默认的五个平台有固定部分
    private String pushUrlCustom;//推流地址 自定义部分
    private String liveTag;//直播标识
    private int liveImage;//
    private int itemType;//0是已添加平台 1是未添加平台
    private boolean isSelect;//是否在首页显示

    public LiveBean config(String liveName, int liveImage, boolean isSelect, int itemType) {
        this.liveName = liveName;
        this.liveImage = liveImage;
        this.isSelect = isSelect;
        this.itemType = itemType;
        return this;
    }
    public LiveBean setUrlHead(String urlHead) {
        this.pushUrlHeard = urlHead;
        return this;
    }
    public LiveBean setUrlCustom(String pushUrlCustom) {
        this.pushUrlCustom = pushUrlCustom;
        return this;
    }

    public String getPushUrlCustom() {
        return pushUrlCustom == null ? "" : pushUrlCustom;
    }


    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public String getLiveName() {
        return liveName == null ? "" : liveName;
    }

    public void setLiveName(String liveName) {
        this.liveName = liveName == null ? "" : liveName;
    }

    public String getPushUrlHeard() {
        return pushUrlHeard == null ? "" : pushUrlHeard;
    }


    public String getLiveTag() {
        return liveTag == null ? "" : liveTag;
    }

    public void setLiveTag(String liveTag) {
        this.liveTag = liveTag == null ? "" : liveTag;
    }

    public int getLiveImage() {
        return liveImage;
    }

    public void setLiveImage(int liveImage) {
        this.liveImage = liveImage;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }
}
