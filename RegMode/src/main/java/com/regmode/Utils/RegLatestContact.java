package com.regmode.Utils;

import com.basenetlib.RequestStatus;

/**
 * Author:wang_sir
 * Time:2019/12/24 20:05
 * Description:This is RegLatestContact
 */
public interface RegLatestContact {

    String  SET_CODE = "set_code";
    String  CHECK_REG = "check_reg";
    String  GET_VERSION = "get_version";
    String  REGIST = "regist";
    String  REGIST_IMEI = "regist_imei";
    String  UPLOAD_V_INFO = "upload_v_info";

    interface  IRegLatestPresent{
        void  setRegisCodeNumber(String regisCode, int size, RequestStatus requestStatus);
        void checkRegStatus(String regisCode,RequestStatus requestStatus);
        void uploadVersionInfo(String regisCode,String versionMsg,RequestStatus requestStatus);
        void getNearestVersionFromService(RequestStatus requestStatus);
        void  regist(String regisCode,String phoneMessage,RequestStatus requestStatus);
        void  registImei(String regisCode,String imei,RequestStatus requestStatus);
    }
    interface CancelCallBack {
        void toFinishActivity();

        void toDoNext();
    }
}
