package org.easydarwin.easypusher.mine;

/**
 * @Author: tobato
 * @Description: 作用描述  直播平台对象
 * @CreateDate: 2020/8/30 17:17
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/8/30 17:17
 */
public class LiveBean {

    private String liveName;//
    private String pushUrl;//推流地址
    private String liveTag;//直播标识
    private int liveImage;//
    private boolean isSelect;//

    public LiveBean(String liveName, int liveImage, boolean isSelect) {
        this.liveName = liveName;
        this.liveImage = liveImage;
        this.isSelect = isSelect;
    }

    public String getLiveName() {
        return liveName == null ? "" : liveName;
    }

    public void setLiveName(String liveName) {
        this.liveName = liveName == null ? "" : liveName;
    }

    public String getPushUrl() {
        return pushUrl == null ? "" : pushUrl;
    }

    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl == null ? "" : pushUrl;
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
