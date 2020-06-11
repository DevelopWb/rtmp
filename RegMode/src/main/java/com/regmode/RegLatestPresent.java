package com.regmode;

import com.basenetlib.RequestStatus;
import com.basenetlib.networkProxy.HttpProxy;
import com.basenetlib.okgo.NetResponseCallBack;

/**
 * Author:wang_sir
 * Time:2019/12/24 20:10
 * Description:This is RegLatestPresent
 */
public class RegLatestPresent implements RegLatestContact.IRegLatestPresent {


    @Override
    public void setRegisCodeNumber(String regisCode, int size, final RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("softType", "mb")
                .params("regisCode", regisCode)
                .params("number", size)
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/RegisCode.asmx/setRegisCodeNumber",
                        new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.SET_CODE);
                    }

                    @Override
                    public void onError(String str) {

                    }
                });
    }

    @Override
    public void checkRegStatus(String regisCode,  final RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("softwareType", "mb")
                .params("regisCode", regisCode)
                .params("softwareId", AppHttpUrl.APP_MARK)
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/GetRegisCodeInfo_NoPhoneMessage", new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.CHECK_REG);
                    }

                    @Override
                    public void onError(String str) {

                    }
                });
    }

    @Override
    public void uploadVersionInfo(String regisCode, String versionMsg,final  RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("regisCode", regisCode)
                .params("versionMsg", versionMsg)
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/SetVersionInfo", new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.UPLOAD_V_INFO);
                    }

                    @Override
                    public void onError(String str) {

                    }
                });
    }


    @Override
    public void getNearestVersionFromService(final RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("softwareType", "mb")
                .params("softwareId", AppHttpUrl.APP_MARK)
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/GetAllSoftWareInfo", new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.GET_VERSION);
                    }

                    @Override
                    public void onError(String str) {

                    }
                });
    }
    @Override
    public void regist(String regisCode, String phoneMessage, final RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("softType", "mb")
                .params("regisCode", regisCode)
                .params("softIdentification", AppHttpUrl.APP_MARK)
                .params("model","")
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/RegisCode.asmx/GetRegisCodeInfo", new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.REGIST);
                    }

                    @Override
                    public void onError(String str) {
                        requestStatus.onError(str);

                    }
                });
    }

    @Override
    public void registImei(String regisCode, String imei, final RequestStatus requestStatus) {
        HttpProxy.getInstance()
                .params("softwareType", "mb")
                .params("regisCode", regisCode)
                .params("imei",imei)
                .postToNetwork(AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/SoftWareRegister", new NetResponseCallBack() {
                    @Override
                    public void onSuccess(String content) {
                        requestStatus.onSuccess(content, RegLatestContact.REGIST_IMEI);
                    }

                    @Override
                    public void onError(String str) {
                        requestStatus.onError(str);

                    }
                });
    }

}
