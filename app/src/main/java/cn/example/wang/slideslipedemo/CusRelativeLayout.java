package cn.example.wang.slideslipedemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Created to :
 *
 * @author WANG
 * @date 2020/8/8
 */
public class CusRelativeLayout extends RelativeLayout {
    public CusRelativeLayout(Context context) {
        super(context);
    }

    public CusRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CusRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if(ev.getAction() != MotionEvent.ACTION_MOVE) {
//            Log.e("WANG", "CusRelativeLayout.dispatchTouchEvent" + ev.getAction());
        }

        boolean b = super.dispatchTouchEvent(ev);

//        Log.e("WANG", "CusRelativeLayout.dispatchTouchEvent" + b);
        return b;
    }
}
