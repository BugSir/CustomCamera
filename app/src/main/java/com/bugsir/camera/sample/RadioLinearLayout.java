package com.bugsir.camera.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 *@author: Lin Xiongqing
 *@date: 2017/12/6 19:58
 *@description:
 */

public  class RadioLinearLayout extends LinearLayout {
    private float radio=9/16f;
    public RadioLinearLayout(Context context) {
        super(context);
    }

    public RadioLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RadioLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //        super.onMeasure(widthMeasureSpec, widthMeasureSpec*9/16);
        int whidth=MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(whidth,(int)(whidth*radio));
    }
    public void setRadio(float radio)
    {
        this.radio=radio;
    }
}
