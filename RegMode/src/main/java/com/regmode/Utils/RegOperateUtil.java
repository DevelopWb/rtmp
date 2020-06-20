package com.regmode.Utils;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.basenetlib.RequestStatus;
import com.basenetlib.util.GsonManager;
import com.regmode.R;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.adapter.CommonProgressDialog;
import com.regmode.adapter.DialogAdapter;
import com.regmode.bean.RegBean;
import com.regmode.bean.RegCodeBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * 注册码操作工具
 * Created by Administrator on 2017/3/30.
 */

public class RegOperateUtil extends BaseReg implements RequestStatus {
    public static boolean RegStatus = false;//注册码状态是否正常
    private SharedPreferences sp;
    public static boolean istoolTip = false;//注册码状态改变是否提醒，例如：注册码到期，禁用等
    public static boolean isNumberLimit = false;//是否限制次数
    public static boolean isForbidden = false;//是否禁用
    public static int REGSIZE = 0;
    public static String URL_Reg_Center = "http://zc.xun365.net";//注册码中心系统
    public static String APP_MARK = "YJZB";//软件标识
    private CommonProgressDialog mProgressDialog;
    private String nearestVersion;
    private Context context;
    public static String strreg;
    public static String username;//登录名
    private AMapLocationClient locationClient = null;
    private String Lat;
    private String Lng;
    private String Addr;
    private Dialog dialog_Reg;
    private ProgressDialog progressDialog;
    private RegLatestContact.CancelCallBack cancelCallBack;
    private boolean firstCite = true;//第一次被引用
    public static RegOperateUtil regOperateUtil;
    private RegLatestPresent present;
    private String input;


    public RegOperateUtil(Context context) {
        this.context = context;
        present = new RegLatestPresent();
        if (firstCite) {
            firstCite = false;
            initReg(context);
        }

    }

    public static RegOperateUtil getInstance(Context context) {

        if (regOperateUtil == null) {
            synchronized (RegOperateUtil.class) {
                return new RegOperateUtil(context);
            }
        } else {
            return regOperateUtil;
        }
    }

    /**
     * 初始化
     *
     * @param context
     */
    private void initReg(Context context) {
        strreg = getRegCode();
        initLocation();
        if (strreg == null || TextUtils.isEmpty(strreg)) {
            showRegDialog();
        } else {
            checkRegStatus();
        }
    }

    /**
     * 获取保存的注册码
     *
     * @return
     */
    public String getRegCode() {
        sp = context.getSharedPreferences("REG", MODE_PRIVATE);
        return sp.getString("OBJREG", "");
    }

    /**
     * 保存注册码状态
     *
     * @param str 状态描述
     */
    private void SaveRegStatus(String str) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("REGSTATUS", str);
        editor.commit();
    }

    /**
     * 检查有没有未减掉的注册码次数
     */
    public void CheckUnMinusedRegSizeToMinus() {
        if (isTheRegStatusOkNoToast(context)) {
            if (sp.getInt("UNMINUSEDSIZE", 0) > 0) {
                if (isNumberLimit) {
                    present.setRegisCodeNumber(strreg, sp.getInt("UNMINUSEDSIZE", 0), this);
                    sp.edit().putInt("UNMINUSEDSIZE", 0).commit();
                }

            }
        }

    }

    //查看注册码未减的次数
    public void UnMinusedRegSizeToCommit() {
        if (REGSIZE > 1) {
            SharedPreferences.Editor et = sp.edit();
            et.putInt("UNMINUSEDSIZE", REGSIZE - 1);
            et.commit();
        }
    }

    /**
     * 判定注册码状态是否正常
     *
     * @return
     */
    public boolean isTheRegStatusOk(Context context) {
        SharedPreferences sp = context.getSharedPreferences("REG", MODE_PRIVATE);
        String reg_status = sp.getString("REGSTATUS", "注册码正常");
        //  "注册码已经禁用"
        if (reg_status.equals("注册码已禁用")) {
            Toast.makeText(context, "注册码无效，请联系管理员", Toast.LENGTH_SHORT).show();
            return false;
        } else if (reg_status.equals("注册码次数已用完")) {
            Toast.makeText(context, "注册码次数已用完，请联系管理员", Toast.LENGTH_SHORT).show();

            return false;
        } else if (reg_status.equals("注册码已过期")) {
            Toast.makeText(context, "注册码使用时间过期，请联系管理员", Toast.LENGTH_SHORT).show();
            return false;
        } else if (reg_status.equals("注册码不正确")) {
            Toast.makeText(context, "注册码不存在，请联系管理员", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }


    /**
     * 判定注册码状态是否正常
     *
     * @return
     */
    public boolean isTheRegStatusOkNoToast(Context context) {
        SharedPreferences sp = context.getSharedPreferences("REG", MODE_PRIVATE);
        String reg_status = sp.getString("REGSTATUS", "注册码正常");
        //  "注册码已经禁用"
        if (reg_status.equals("注册码已禁用")) {
            return false;
        } else if (reg_status.equals("注册码次数已用完")) {
            return false;
        } else if (reg_status.equals("注册码已过期")) {
            return false;
        } else if (reg_status.equals("注册码不正确")) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * 检查注册码的状态
     */
    @Override
    public void checkRegStatus() {
        if (!RegPubUtils.isConnected(context.getApplicationContext())) {
            Toast.makeText(context, "网络异常，请检查手机网络", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        present.getRegInfo(strreg, this);
    }

    /**
     * 重置下次提醒的日期
     *
     * @param status
     */
    private void resetNextWarnTime(String status) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
        SharedPreferences.Editor et = sharedPreferences.edit();
        et.putString("nextRegStatusTime" + status, "");
        et.commit();
    }


    /**
     * 判定是否提醒注册吗状态
     */
    private boolean IsTheRegStatusTime(String status) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
        final String time = sharedPreferences.getString("nextRegStatusTime" + status, "");
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        String time2 = new SimpleDateFormat("yyyy-MM-dd").format(date);
        if (TextUtils.isEmpty(time)) {
            return true;
        } else {
            if (RegPubUtils.compareTime(time2, time)) {
                return true;
            } else {
                return false;
            }
        }

    }


    /**
     * 提醒注册码的状态
     *
     * @param text
     * @param status
     */
    private void WarnRegStatus(final String text, final String status) {

        View v = LayoutInflater.from(context).inflate(R.layout.warn_reg_layout
                , null);
        final Dialog dialog_toWarn = new Dialog(context, R.style.DialogStyle);
        dialog_toWarn.setCanceledOnTouchOutside(false);
        dialog_toWarn.setCancelable(false);
        dialog_toWarn.show();
        Window window = dialog_toWarn.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        window.setAttributes(lp);
        window.setContentView(v);
        final TextView nfs_set_no_tv = (TextView) v.findViewById(R.id.warn_reg_tv);
        final TextView warn_reg_textViewreg = (TextView) v.findViewById(R.id.warn_reg_textViewreg);
        nfs_set_no_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text != null && !TextUtils.isEmpty(text)) {
                    if (text.equals("注册码无效，请联系管理员")) {
                        if (cancelCallBack != null) {
                            cancelCallBack.toFinishActivity();
                        }
                    } else if (text.equals("注册码绑定MAC不匹配，请联系管理员")) {
                        if (cancelCallBack != null) {
                            cancelCallBack.toFinishActivity();
                        }
                    } else if (text.equals("注册码绑定IMEI不匹配，请联系管理员")) {
                        if (cancelCallBack != null) {
                            cancelCallBack.toFinishActivity();
                        }
                    } else {
                        String nextTime = GetNextWarnTime(1);
                        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
                        SharedPreferences.Editor et = sharedPreferences.edit();
                        et.putString("nextRegStatusTime" + status, nextTime);
                        et.commit();
                        dialog_toWarn.dismiss();
                    }

                }
            }

        });
        warn_reg_textViewreg.setText(text);
    }

    /**
     * 获取下次提醒的时间,day天后
     */
    private String GetNextWarnTime(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, day);
        Date date = calendar.getTime();
        String time = new SimpleDateFormat("yyyy-MM-dd").format(date);
        return time;
    }


    /**
     * 通过软件的版本名称判定是否升级
     *
     * @param localVersionName  本地软件的版本名称
     * @param serverVersionName 服务端软件的版本名称
     * @return
     */
    private boolean updateableSoftVersion(String localVersionName, String serverVersionName) {
        if (TextUtils.isEmpty(localVersionName) || TextUtils.isEmpty(serverVersionName)) {
            return false;
        }
        String local3 = "0";
        String server3 = "0";
        String[] localVersion = localVersionName.split("\\.");
        String[] serverVersion = serverVersionName.split("\\.");
        String local1 = localVersion[0];
        String local2 = localVersion[1];
        if (localVersion.length == 3) {
            local3 = localVersion[2];
        }
        String server1 = serverVersion[0];
        String server2 = serverVersion[1];
        if (serverVersion.length == 3) {
            server3 = serverVersion[2];
        }
        if (Integer.parseInt(server1) > Integer.parseInt(local1)) {
            return true;
        }
        if (Integer.parseInt(server2) > Integer.parseInt(local2)) {
            return true;
        }
        if (Integer.parseInt(server3) > Integer.parseInt(local3)) {
            return true;
        }
        return false;
    }

    /**
     * 判定是否提醒升级
     */
    private boolean IsTheTime() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
        final String time = sharedPreferences.getString("nextTime", "");
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        String time2 = new SimpleDateFormat("yyyy-MM-dd").format(date);
        if (TextUtils.isEmpty(time)) {
            return true;
        } else {
            if (RegPubUtils.compareTime(time2, time)) {
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * 提醒升级的对话框
     *
     * @param url
     * @param description
     */
    private void WarnUpgradeDialog(final String url, String description) {

        View v = LayoutInflater.from(context).inflate(R.layout.to_nfc_set, null);
        final Dialog dialog_toSet = new Dialog(context, R.style.DialogStyle);
        dialog_toSet.setCanceledOnTouchOutside(false);
        dialog_toSet.setCancelable(false);
        dialog_toSet.show();
        Window window = dialog_toSet.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        lp.width = RegPubUtils.dip2px(context, 300); // 宽度
        lp.height = RegPubUtils.dip2px(context, 260); // 高度
        // lp.alpha = 0.7f; // 透明度
        window.setAttributes(lp);
        window.setContentView(v);
        ListView feature_lv = (ListView) v.findViewById(R.id.feature_lv);
        feature_lv.setDivider(null);
        feature_lv.setAdapter(new DialogAdapter(context, description));
        dialog_toSet.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == event.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        dialog_toSet.dismiss();
                    }
                }
                return false;
            }
        });
        final TextView nfs_set_sure_tv = (TextView) v.findViewById(R.id.nfs_set_sure_tv);
        final TextView nfs_set_no_tv = (TextView) v.findViewById(R.id.nfs_set_no_tv);
        nfs_set_sure_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_toSet.dismiss();
                DownAPKfromService(url);
            }
        });
        nfs_set_no_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_toSet.dismiss();
                String nextTime = GetNextWarnTime(7);
                SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
                SharedPreferences.Editor et = sharedPreferences.edit();
                et.putString("nextTime", nextTime);
                et.commit();
            }
        });
    }

    /**
     * 从服务器下载apk包
     *
     * @param url
     */
    private void DownAPKfromService(String url) {

        if (RegPubUtils.isConnected(context)) {
            mProgressDialog = new CommonProgressDialog(context);
            mProgressDialog.setMessage("正在下载");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            final DownloadTask downloadTask = new DownloadTask(context);
            downloadTask.execute(url);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    downloadTask.cancel(true);
                }
            });
            //downFile(url);
        }

    }

    @Override
    public void onStart(String tag) {

    }

    @Override
    public void onSuccess(Object o, String tag) {
        String str = (String) o;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        switch (tag) {
            case RegLatestContact.SET_CODE:
                if (str == null) {
                    Toast.makeText(context, "服务器返回异常，请联系管理员", Toast.LENGTH_SHORT).show();
                    return;
                }
                sp.edit().putInt("MINUSTIMES", 0).commit();
                try {
                    JSONObject obj = new JSONObject(str);
                    String model = obj.getString("Model");
                    //  "注册码已经禁用"
                    if (model.equals("注册码已经禁用")) {
                        RegStatus = false;
                        isForbidden = true;
                        SaveRegStatus("注册码已禁用");
                        if (istoolTip) {
                            Toast.makeText(context, "注册码已禁用，请联系管理员", Toast.LENGTH_SHORT).show();
                        }

                        return;
                    } else if (model.equals("注册码次数已用完")) {
                        RegStatus = false;
                        SaveRegStatus("注册码次数已用完");
                        REGSIZE++;
                        if (istoolTip) {
                            Toast.makeText(context, "注册码次数已用完，请联系管理员", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    } else if (model.equals("注册码使用时间过期")) {
                        RegStatus = false;
                        SaveRegStatus("注册码已过期");
                        if (istoolTip) {
                            Toast.makeText(context, "注册码使用时间过期，请联系管理员", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    } else if (model.equals("注册码不正确")) {
                        RegStatus = false;
                        SaveRegStatus("注册码不正确");
                        if (istoolTip) {
                            Toast.makeText(context, "注册码不存在，请联系管理员", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    } else {
                        RegStatus = true;
                        SaveRegStatus("注册码正常");
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case RegLatestContact.GET_REG_INFO:
                //获取注册码信息  验证接口  每次进入软件的时候需要调用这个接口  检测注册码的状态
                RegCodeBean regInfo = (RegCodeBean) o;
                if (regInfo != null) {
                    String resultTag = regInfo.getResult();
                    if ("OK".equals(resultTag)) {
                        if (regInfo.getModel() != null&&regInfo.getModel().size()>0) {
                            RegCodeBean.ModelBean regInfoBean = regInfo.getModel().get(0);
//                            regInfoBean.getIs
                        }
                    }
                }

                if (!TextUtils.isEmpty(str)) {
                    try {
                        JSONObject obj = new JSONObject(str);
                        String result = obj.getString("Result");
                        JSONArray mArray = obj.getJSONArray("Model");
                        if (!TextUtils.isEmpty(result) && result.equals("ok")) {
                            if (mArray.length() == 0) {
                                boolean warn = sp.getBoolean("ISTOOLTIP", false);//0代表不提示
                                if (warn) {//提示
                                    SaveRegStatus("注册码不正确");
                                    WarnRegStatus("注册码无效，请联系管理员", "");
                                }
                            } else {
                                JSONObject obj_ = (JSONObject) mArray.get(0);
                                String Imei = obj_.getString("Imei").trim();
                                String MAC = obj_.getString("MAC").trim();
                                String isValid = obj_.getString("isValid");
                                String isNumber = obj_.getString("isNumber");
                                String isDisabled = obj_.getString("isDisabled");
                                String isAutoUpdate = obj_.getString("isAutoUpdate");
                                String isToolTip = obj_.getString("isToolTip").trim();
//保存注册码状态信息
                                String RegisCodeState = obj_.getString("RegisCodeState");
                                RegSuccess(null, null, isToolTip, isNumber);
                                if (RegisCodeState.equals("正常")) {
                                    RegStatus = true;
                                    SaveRegStatus("注册码正常");
                                } else if (RegisCodeState.equals("已过期")) {
                                    RegStatus = false;
                                    SaveRegStatus("注册码已过期");
                                } else if (RegisCodeState.equals("次数用尽")) {
                                    RegStatus = false;
                                    SaveRegStatus("注册码次数已用完");
                                } else if (RegisCodeState.equals("已禁用")) {
                                    RegStatus = false;
                                    SaveRegStatus("注册码已禁用");
                                }
                                if (istoolTip) {
                                    if (isValid != null && !TextUtils.isEmpty(isValid)) {
                                        if (isValid.equals("0")) {//注册码限制时间
                                            String ValidEnd = obj_.getString("ValidEnd");
                                            String time = ValidEnd.split(" ")[0];
                                            if (RegPubUtils.TheDayToNextDay(time) > 0 && RegPubUtils.TheDayToNextDay(time) < 8) {

                                                if (IsTheRegStatusTime("isValid")) {
                                                    WarnRegStatus("注册码有效期还剩" + RegPubUtils.TheDayToNextDay(time) + "天，请联系管理员", "isValid");
                                                }

                                            } else {//重置下次提醒的时间
                                                resetNextWarnTime("isValid");
                                            }
                                        }
                                    }
                                    if (!TextUtils.isEmpty(MAC)) {
                                        if (!macAddress().equals(MAC)) {
                                            //TOdo 关闭程序
                                            WarnRegStatus("注册码绑定MAC不匹配，请联系管理员", "disable");
                                        }

                                    }
                                    if (!TextUtils.isEmpty(Imei)) {//说明该注册码没有绑定IMEI
                                        if (!GetImei().equals(Imei)) {
                                            //todo 关闭软件
                                            WarnRegStatus("注册码绑定IMEI不匹配，请联系管理员", "disable");
                                        }

                                    }
                                    if (isNumber != null && !TextUtils.isEmpty(isNumber)) {
                                        if (isNumber.equals("0")) {//注册码有次数限制
                                            String NumberTotal = obj_.getString("Number");
                                            String NumberUsed = obj_.getString("NumberNow");
                                            int NumberNow = Integer.parseInt(NumberTotal) - Integer.parseInt(NumberUsed);
                                            if (NumberNow < 100) {
                                                if (IsTheRegStatusTime("isNumber")) {
                                                    WarnRegStatus("注册码次数还剩" + NumberNow + "次，请联系管理员", "isNumber");
                                                }

                                            } else {//重置下次提醒的日期
                                                resetNextWarnTime("isNumber");
                                            }
                                        }
                                    }

                                }
                                if (isDisabled != null && !TextUtils.isEmpty(isDisabled)) {
                                    if (isDisabled.equals("0")) {//注册码已禁用
                                        isForbidden = true;
                                        WarnRegStatus("注册码无效，请联系管理员", "disable");
                                        return;
                                    } else {
                                        isForbidden = false;
                                    }
                                }

                                if (isAutoUpdate != null && !TextUtils.isEmpty(isAutoUpdate)) {
                                    if (isAutoUpdate.equals("1")) {//允许自动升级
                                        present.getNearestVersionFromService(this);
                                    }
                                }
                            }
                        } else {
                            if (IsTheRegStatusTime("isWrong")) {
                                WarnRegStatus("服务器连接异常", "isWrong");
                            }

                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                }
                break;
            case RegLatestContact.GET_VERSION:
                if (str != null && !TextUtils.isEmpty(str)) {

                    try {
                        JSONObject obj = new JSONObject(str);
                        JSONArray infos = obj.getJSONArray("Model");
                        if (infos.length() > 0) {
                            JSONObject obj_ = (JSONObject) infos.get(0);
                            nearestVersion = obj_.getString("SoftwareVersion").trim();
                            String down_url = obj_.getString("softDownloadUrl");
                            String appDescription = obj_.getString("softDescription");
                            if (updateableSoftVersion(getAPPVersion(), nearestVersion)) {
                                if (IsTheTime()) {
                                    WarnUpgradeDialog(down_url, appDescription);
                                }

                            } else {//将
                                SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
                                SharedPreferences.Editor et = sharedPreferences.edit();
                                et.putString("nextTime", "");
                                et.commit();
                            }
                        } else {
                            Toast.makeText(context, "服务器上查不到该软件", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RegLatestContact.REGIST:
                //如果注册码已经注册
                if (!TextUtils.isEmpty(str)) {
                    RegBean regBean = GsonManager.getInstance().parseJsonToBean(str, RegBean.class);
                    List<RegBean.ModelBean> arrays = regBean.getModel();
                    if (arrays == null || arrays.size() == 0) {
                        Toast.makeText(context, "注册码不存在",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    RegBean.ModelBean modelBean = arrays.get(0);
//                    if (!getAPPVersion().equals(modelBean.getVersion())) {
//                        Toast.makeText(context, "此注册码已绑定软件版本，请联系管理员",
//                                Toast.LENGTH_LONG).show();
//                        return;
//                    }
                    String regStatus = modelBean.getRegisCodeState();
                    String mac = modelBean.getMAC();
                    String guestName = modelBean.getCustomerName();
                    String isToolTip = modelBean.getIsToolTip();
                    String isNumber = modelBean.getIsNumber();
                    if (regStatus.equals("正常")) {
                        if (TextUtils.isEmpty(mac)) {//没有绑定MAC
                            RegSuccess(input, guestName, isToolTip, isNumber);
                        } else {
                            if (macAddress().equals(mac)) {
                                RegSuccess(input, guestName, isToolTip, isNumber);
                            } else {
                                Toast.makeText(context, "请确定注册码绑定的手机(MAC)是否正确",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    } else if (regStatus.equals("已过期")) {
                        RegStatus = false;
                        Toast.makeText(context, "注册码已过期，请联系管理员", Toast.LENGTH_LONG).show();
                    } else if (regStatus.equals("次数用尽")) {
                        RegStatus = false;
                        Toast.makeText(context, "注册码调用次数已用尽，请联系管理员", Toast.LENGTH_LONG).show();
                    } else if (regStatus.equals("已禁用")) {
                        RegStatus = false;
                        Toast.makeText(context, "注册码已禁用，请联系管理员", Toast.LENGTH_LONG).show();
                    } else {
                        RegStatus = false;
                        Toast.makeText(context, "注册码暂不可用，请联系管理员", Toast.LENGTH_LONG).show();
                    }
                    progressDialog.dismiss();

                }

                break;
            case RegLatestContact.UPLOAD_V_INFO:
                if (str != null && !TextUtils.isEmpty(str)) {
                    SharedPreferences.Editor et = sp.edit();
                    et.putString("SavedVersion", getAPPVersion());
                    et.commit();
                }
                break;
            case RegLatestContact.CHECK_REG:
                //验证注册码
                RegCodeBean regCodeBean = (RegCodeBean) o;
                if (regCodeBean != null) {
                    String resultTag = regCodeBean.getResult();
                    if ("OK".equals(resultTag)) {
                        SharedPreferences.Editor editor = sp.edit();
                        if (input != null) {
                            Toast.makeText(context, "注册码验证成功",
                                    Toast.LENGTH_LONG).show();
                            strreg = input;
                            editor.putString("OBJREG", input);
                            editor.commit();
                        }
                        if (dialog_Reg != null && dialog_Reg.isShowing()) {
                            dialog_Reg.dismiss();
                        }
                    }else {
                        Toast.makeText(context,resultTag,Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onError(String tag) {
        Toast.makeText(context,tag,Toast.LENGTH_SHORT).show();
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public void setRegistCodeNumber(int size) {
        present.setRegisCodeNumber(strreg, size, this);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;

        DownloadTask(Context context) {
            this.context = context;
        }

        //执行异步任务（doInBackground）之前执行，并且在ui线程中执行
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //开始下载 对话框进度条显示
            mProgressDialog.show();
            mProgressDialog.setProgress(0);
        }

        @Override
        protected String doInBackground(String... params) {
            int i = 0;
            String uri = URL_Reg_Center + params[0];
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file


                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                mProgressDialog.setMax(fileLength);
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(GetAPKPath());
                byte data[] = new byte[4096];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
//					if (fileLength > 0) // only if total length is known
//					{
//						i = (int) (total * 100 / fileLength);
//					}
                    mProgressDialog.setProgress(total);
//					publishProgress(i);
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return "right";
        }

        //在ui线程中执行 可以操作ui
        @Override
        protected void onPostExecute(String string) {
            // TODO Auto-generated method stub
            super.onPostExecute(string);
            //下载完成 对话框进度条隐藏
            if (string.equals("right")) {
                mProgressDialog.cancel();
                Toast.makeText(context, "下载完成", Toast.LENGTH_SHORT).show();
                installApk();
            } else {
                mProgressDialog.cancel();
                Toast.makeText(context, "下载失败", Toast.LENGTH_LONG).show();
            }


        }

        /*
         * 在doInBackground方法中已经调用publishProgress方法 更新任务的执行进度后
         * 调用这个方法 实现进度条的更新
         * */
        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);

        }
    }

    /**
     * 安装APK
     */
    private void installApk() {
        File file = new File(GetAPKPath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * 定义下载包存储路径
     */
    private String GetAPKPath() {
        File file = new File("/mnt/sdcard/.toInstallPG");
        if (!file.exists()) {
            file.mkdir();
        }
        String path = "/mnt/sdcard/.toInstallPG" + "/" + getAPPName() + nearestVersion + ".apk";
        return path;
    }


    /**
     * 获取软件名称
     */
    public String getAPPName() {
        String appName = "";
        PackageManager pm = context.getPackageManager();//得到PackageManager对象
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            appName = (String) pm.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }


    /**
     * 弹出注册窗口
     */
    public void showRegDialog() {

        View v = LayoutInflater.from(context).inflate(R.layout.reg_dialog, null);
        dialog_Reg = new Dialog(context, R.style.DialogStyle);
        dialog_Reg.setCanceledOnTouchOutside(false);
        dialog_Reg.show();
        dialog_Reg.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog_Reg.dismiss();
                //退出程序
                if (cancelCallBack != null) {
                    cancelCallBack.toFinishActivity();
                }
            }
        });
        Window window = dialog_Reg.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        lp.width = RegPubUtils.dip2px(context, 290); // 宽度
        lp.height = RegPubUtils.dip2px(context, 200); // 高度
        lp.alpha = 0.7f; // 透明度
        window.setAttributes(lp);
        window.setContentView(v);
        final TextView reg = (TextView) v.findViewById(R.id.editTextReg);
        ImageButton ib = (ImageButton) v.findViewById(R.id.imageButtonReg);
        ImageButton.OnClickListener listener = new ImageButton.OnClickListener() {

            public void onClick(View v) {
                if (!RegPubUtils.isConnected(context.getApplicationContext())) {
                    Toast.makeText(context, "网络异常，请检查手机网络", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                input = reg.getText().toString().trim();
                if (input == null || TextUtils.isEmpty(input)) {
                    Toast.makeText(context, "请输入注册码",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // 网络验证中
                progressDialog = ProgressDialog.show(context, "请稍候",
                        "注册码验证中请不要进行其他操作", true);
                progressDialog.setCancelable(true);
                present.checkReg(input, getRegImeiIccid(), APP_MARK,RegLatestContact.CHECK_REG,RegOperateUtil.this);

            }
        };

        ib.setOnClickListener(listener);

    }


    /**
     * 初始化定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void initLocation() {
        //初始化client
        locationClient = new AMapLocationClient(context.getApplicationContext());
        // 设置定位监听
        locationClient.setLocationListener(locationListener);
        startLocation();
    }

    /**
     * 默认的定位参数
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
//		mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
//		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
//		mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        return mOption;
    }

    /**
     * 开始定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startLocation() {
//		//根据控件的选择，重新设置定位参数
//		resetOption();
        // 设置定位参数
        locationClient.setLocationOption(getDefaultOption());
        // 启动定位
        locationClient.startLocation();
    }

    /**
     * 停止定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void stopLocation() {
        // 停止定位
        locationClient.stopLocation();
    }

    /**
     * 销毁定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void destroyLocation() {
        if (null != locationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            stopLocation();
            locationClient.onDestroy();
            locationClient = null;
        }
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation loc) {
            if (null != loc) {
                //解析定位结果
                Lat = loc.getLatitude() + "";
                Lng = loc.getLongitude() + "";
                Addr = loc.getAddress();
                CheckSavedVersion();
            }
        }
    };

    /**
     * 获取运营商sim卡的ICCID号
     *
     * @return ICCID号
     */
    public String getRegImeiIccid() {
        String msg = "";
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (TextUtils.isEmpty(tm.getDeviceId())) {
            // 获取sim卡的ICCID号
            return tm.getSimSerialNumber();
        }else{
            return tm.getDeviceId();
        }

    }

    private String getPhoneMessage() {
        String lac = "";
        String cid = "";
        String nid = "";
        String imei = "";
        String phoneNo = "";
        String imsi = "";
        String mac = "";
        StringBuffer phoneInfo_sb = new StringBuffer();
        TelephonyManager mTManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return "";
            }
            imei = mTManager.getDeviceId();
            phoneNo = mTManager.getLine1Number();
            imsi = mTManager.getSubscriberId();
        }
        mac = macAddress();
        phoneInfo_sb.append("PhoneNo:" + phoneNo + "," + "Imei:" + imei + "," + "Imsi:" + imsi + "," + "Mac:" + mac + ",");

        if (mTManager != null) {
            int phonetype = mTManager.getPhoneType();
            if (phonetype == TelephonyManager.PHONE_TYPE_GSM) {
                GsmCellLocation gcl = (GsmCellLocation) mTManager.getCellLocation();
                cid = gcl.getCid() + "";
                lac = gcl.getLac() + "";
            } else if (phonetype == TelephonyManager.PHONE_TYPE_CDMA) {
                CdmaCellLocation gcl = (CdmaCellLocation) mTManager
                        .getCellLocation();
                if (gcl != null) {
                    nid = gcl.getNetworkId() + "";// nid
                    cid = gcl.getBaseStationId() + "";// cellid
                    lac = gcl.getSystemId() + ""; // sid
                }

            }
            phoneInfo_sb.append("Lat:" + Lat + "," + "Log:" + Lng + "," + "Lac:" + lac + "," + "Cid:" + cid + "," + "Nid:" + nid + "," + "Addr:" + Addr);
        }
        return phoneInfo_sb.toString();
    }

    /**
     * 获取软件版本号
     */
    private String getAPPVersion() {
        PackageManager pm = context.getPackageManager();//得到PackageManager对象
        String version_app = "";
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);//得到PackageInfo对象，封装了一些软件包的信息在里面
            version_app = pi.versionName;//获取清单文件中versionCode节点的值
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version_app;
    }


    public String macAddress() {
        String address = null;
        try {
            // 把当前机器上的访问网络接口的存入 Enumeration集合中
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netWork = interfaces.nextElement();
                // 如果存在硬件地址并可以使用给定的当前权限访问，则返回该硬件地址（通常是 MAC）。
                byte[] by = netWork.getHardwareAddress();
                if (by == null || by.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (byte b : by) {
                    builder.append(String.format("%02X:", b));
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                String mac = builder.toString();
                // 从路由器上在线设备的MAC地址列表，可以印证设备Wifi的 name 是 wlan0
                if (netWork.getName().equals("wlan0")) {
                    address = mac;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return address;
    }


    /**
     * 注册码验证成功后
     */
    private void RegSuccess(String input, String guestName, String isToolTip, String isNumber) {
        SharedPreferences.Editor editor = sp.edit();
        if (input != null && guestName != null) {
            Toast.makeText(context, "注册码验证成功",
                    Toast.LENGTH_LONG).show();
            strreg = input;
            editor.putString("OBJREG", input);
            editor.putString("GUESTNAME", guestName);
        }

        if (isToolTip.equals("0")) {//0代表不提示
            istoolTip = false;
            editor.putBoolean("ISTOOLTIP", false);
        } else {
            istoolTip = true;
            editor.putBoolean("ISTOOLTIP", true);
        }
        if (isNumber.equals("0")) {//0代表有次数限制
            isNumberLimit = true;
            editor.putBoolean("ISNUMBER", true);
        } else {
            isNumberLimit = false;
            editor.putBoolean("ISNUMBER", false);
        }
        editor.commit();
        if (dialog_Reg != null && dialog_Reg.isShowing()) {
            dialog_Reg.dismiss();
        }

        if (cancelCallBack != null) {
            cancelCallBack.toDoNext();
        }

    }

    /**
     * 获取imei
     *
     * @return
     */
    private String GetImei() {

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        return imei;
    }

    /**
     * 检测保存本地的版本号
     */
    private void CheckSavedVersion() {
        String savedVersion = sp.getString("SavedVersion", "");
//        String savedVersion = "1.0";
        String nowVersion = getAPPVersion();
        if (savedVersion.equals("")) {
            SharedPreferences.Editor et = sp.edit();
            et.putString("SavedVersion", nowVersion);
            et.commit();
        } else {
            if (!savedVersion.equals(nowVersion) && Double.parseDouble(nowVersion) > Double.parseDouble(savedVersion)) {
                //上传版本信息
                String info = GetInfoWhenVersionChanged(savedVersion, nowVersion);
                present.uploadVersionInfo(strreg, info, this);
            }

        }

    }

    private String GetInfoWhenVersionChanged(String originalVersion, String newestVersion) {
        String PhoneNo = "";
        String Imei = "";
        String time = "";
        time = RegPubUtils.getDateToString(System.currentTimeMillis());
        StringBuffer Info_sb = new StringBuffer();
        TelephonyManager mTManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTManager != null) {
            Imei = mTManager.getDeviceId();
            PhoneNo = mTManager.getLine1Number();
        }
        Info_sb.append("SoftName:" + getAPPName() + "," + "GuestName:" + sp.getString("GUESTNAME", "") + "," + "RegCode:" + strreg + "," + "PhoneNo:" + PhoneNo + "," + "Imei:" + Imei + "," + "Mac:" + macAddress() + "," + "Lat:" + Lat + "," + "Lng:" + Lng + "," + "Addr:" + Addr + "," + "OriginalVersion:" + originalVersion + "," + "NewestVersion:" + newestVersion + "," + "Time:" + time);

        return Info_sb.toString();
    }


    public void setCancelCallBack(RegLatestContact.CancelCallBack callBack) {
        this.cancelCallBack = callBack;
    }


}
