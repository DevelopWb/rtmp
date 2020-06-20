package com.regmode;

import com.basenetlib.RequestStatus;

/**
 * Author:wang_sir
 * Time:2019/12/24 20:05
 * Description:This is RegLatestContact
 */
public interface RegLatestContact {

    String  SET_CODE = "set_code";
    String GET_REG_INFO = "get_reg_info";
    String  GET_VERSION = "get_version";
    String  REGIST = "regist";
    String CHECK_REG = "check_reg";//第一次注册码验证
    String  UPLOAD_V_INFO = "upload_v_info";

    interface  IRegLatestPresent{
        void  setRegisCodeNumber(String regisCode, int size, RequestStatus requestStatus);
        void getRegInfo(String regisCode, RequestStatus requestStatus);
        void uploadVersionInfo(String regisCode,String versionMsg,RequestStatus requestStatus);
        void getNearestVersionFromService(RequestStatus requestStatus);
//        void  regist(String regisCode,String phoneMessage,RequestStatus requestStatus);
        //注册码验证
        void checkReg(String regisCode, String imei, String softwareId, String tag, RequestStatus requestStatus);
    }
    interface CancelCallBack {
        void toFinishActivity();

        void toDoNext();
    }
}
