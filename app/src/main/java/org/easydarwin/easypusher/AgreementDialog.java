package org.easydarwin.easypusher;

import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juntai.wisdom.basecomponent.utils.StringTools;


/**
 * 提示弹窗
 */
public class AgreementDialog {
    TextView titleTv;
    TextView contentTv;
    TextView cancelBtn;
    TextView okBtn;

    private Context context;
    private Dialog dialog;
    private RelativeLayout lLayout_bg;
    private Display display;

    public AgreementDialog(Context context) {
        this.context = context;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public AgreementDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_agreement_dialog, null);

        // 获取自定义Dialog布局中的控件
//        lLayout_bg = view.findViewById(R.id.lLayout_bg);
        contentTv = view.findViewById(R.id.content_tv);
        cancelBtn = view.findViewById(R.id.cancel_btn);
        okBtn = view.findViewById(R.id.ok_btn);
        titleTv = view.findViewById(R.id.title_tv);

        // 定义Dialog布局和参数
//        dialog = new Dialog(context, R.style.AlertDialogStyle);
        dialog = new Dialog(context,R.style.ActionSheetDialogStyle);
        dialog.setContentView(view);
//        ScreenAdapterTools.getInstance().loadView(view);
        // 调整dialog背景大小
//        lLayout_bg.setLayoutParams(new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT));

        return this;
    }

    /**
     * 标题
     * @param title
     * @return
     */
    public AgreementDialog setTitle(String title){
        if (!StringTools.isStringValueOk(title)){
            titleTv.setVisibility(View.GONE);
        }else {
            titleTv.setVisibility(View.VISIBLE);
            titleTv.setText(title);
        }
        return this;
    }

    /**
     * shezh 内容
     * @param content
     * @return
     */
    public AgreementDialog setContent(CharSequence content){
        contentTv.setText(content);
        return this;
    }

    public TextView getContentTextView(){
        return contentTv;
    }

    /**
     * 设置确认监听
     * @param text
     * @param listener
     * @return
     */
    public AgreementDialog setOkButton(String text, final View.OnClickListener listener) {
        if (text != null && !text.equals("")){
            okBtn.setText(text);
        }
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                listener.onClick(v);
            }
        });
        return this;
    }

    /**
     * 设置取消监听
     * @param text
     * @param listener
     * @return
     */
    public AgreementDialog setCancelButton(String text, final View.OnClickListener listener) {
        if (text != null && !text.equals("")){
            cancelBtn.setText(text);
        }
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                listener.onClick(v);
            }
        });
        return this;
    }

    /**
     * 是否点击外部消失
     * @param isCan
     * @return
     */
    public AgreementDialog setCanceledOnTouchOutside(boolean isCan) {
        dialog.setCanceledOnTouchOutside(isCan);
        dialog.setCancelable(isCan);
        return this;
    }

    public void show() {
        dialog.show();
    }
}
