package com.regmode;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/6/11 15:16
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/6/11 15:16
 */
public class AppHttpUrl {
    public static String BASE_URL = "https://zc.cha365.cn";//注册码中心系统
    public  static  String  SoftWare =BASE_URL+ "/WebService/SoftWare.asmx";
    public  static  String  RegisCode = BASE_URL+"/WebService/RegisCode.asmx";
    public  static  String  InterfaceRegisCode = BASE_URL+"/WebService/InterfaceRegisCode.asmx";

    public  static  String   CHECK_REGIST = AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/SoftWareRegister";//验证注册码
    public  static  String   GET_REG_INFO = AppHttpUrl.BASE_URL + "/WebService/SoftWare.asmx/GetRegisCodeInfo_NoPhoneMessage";//获取验证码信息


}
