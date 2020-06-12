package org.easydarwin.easypusher.mine.scan;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.king.zxing.CaptureHelper;
import com.king.zxing.OnCaptureCallback;
import com.king.zxing.ViewfinderView;

import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.mine.SettingActivity;


public class QRScanActivity extends Activity implements View.OnClickListener, OnCaptureCallback {
    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;
    private ImageView mZxingPic;
    private ImageView mIvTorch;
    private ImageView mZxingBackBtn;
    private CaptureHelper mCaptureHelper;

    private int SELECT_PIC_RESULT = 1001;

//    private int pageType;//0扫码，1扫描巡检内容


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
        setContentView(R.layout.activity_qrscan);
        initView();
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
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == SELECT_PIC_RESULT && resultCode == RESULT_OK) {
////            String content = CodeUtils.parseCode(Matisse.obtainPathResult(data).get(0));
////            showResult(content);
////            finish();
//        }
//    }

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
        mCaptureHelper.setOnCaptureCallback(null);
        mCaptureHelper=null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCaptureHelper.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onResultCallback(String result) {
        setResult(RESULT_OK,new Intent(this, SettingActivity.class).putExtra("result",result));
        finish();

        return true;
    }


}
