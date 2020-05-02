package org.easydarwin.easypusher.mine.scan;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.NoCopySpan;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.king.zxing.CaptureHelper;
import com.king.zxing.OnCaptureCallback;
import com.king.zxing.ViewfinderView;
import com.king.zxing.util.CodeUtils;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.SettingActivity;

import java.util.function.Consumer;


/**
 * 扫码
 * @aouther ZhangZhenlong
 * @date 2020-3-18
 */
public class QRScanActivity extends AppCompatActivity implements View.OnClickListener, OnCaptureCallback {
    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;
    private ImageView mZxingPic;
    private ImageView mIvTorch;
    private ImageView mZxingBackBtn;
    private CaptureHelper mCaptureHelper;

    private int SELECT_PIC_RESULT = 1001;

//    private int pageType;//0扫码，1扫描巡检内容


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_qrscan);
        initView();
    }


    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinderView);
        mZxingPic = (ImageView) findViewById(R.id.zxing_pic);
        mZxingPic.setOnClickListener(this);
        mIvTorch = (ImageView) findViewById(R.id.ivTorch);
        mZxingBackBtn = (ImageView) findViewById(R.id.zxing_back_btn);
        mZxingBackBtn.setOnClickListener(this);
        mIvTorch.setOnClickListener(this);

        mCaptureHelper = new CaptureHelper(this,mSurfaceView,mViewfinderView,null);
        mCaptureHelper.setOnCaptureCallback(this);
        mCaptureHelper.onCreate();
        mCaptureHelper.playBeep(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.zxing_pic:
//                choseImageFromFragment(0,this,1,SELECT_PIC_RESULT);
                break;
            case R.id.zxing_back_btn:
                finish();
                break;
            case R.id.ivTorch:
                mIvTorch.setSelected(!mIvTorch.isSelected());
                mCaptureHelper.getCameraManager().setTorch(mIvTorch.isSelected());
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PIC_RESULT && resultCode == RESULT_OK) {
//            String content = CodeUtils.parseCode(Matisse.obtainPathResult(data).get(0));
//            showResult(content);
//            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
        mCaptureHelper=null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCaptureHelper.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onResultCallback(String result) {
        showResult(result);
        finish();
        return true;
    }

    /**
     * 结果处理
     * 最好是用ActivityResult将结果返回上个界面
     * @param result
     */
    private void showResult(String result) {
        if (!TextUtils.isEmpty(result)) {
            return;
        }
        setResult(RESULT_OK,new Intent(this, SettingActivity.class).putExtra("result",result));

//        if(pageType == 1){
//            Intent intent = new Intent(this, EditInspectionActivity.class);
//            intent.putExtra("result", result);
//            startActivity(intent);
//        }else {
//            ToastUtils.success(this,result);
//        }
    }

}
