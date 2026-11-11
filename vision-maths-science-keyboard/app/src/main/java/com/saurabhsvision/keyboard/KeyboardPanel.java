package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Height-constrained Android IME panel.
 *
 * v1 used a FrameLayout with an independent MATCH_PARENT background View:
 * this forced the input view to almost screen height on some Android devices.
 * Here the art is painted inside the WRAP_CONTENT keys container itself.
 */
public final class KeyboardPanel extends LinearLayout {
    private KeyboardBackdrop painter;
    private final int maxPortraitDp=390;
    private final int maxLandscapeDp=280;

    public KeyboardPanel(Context context,int theme){
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setWillNotDraw(false);
        setClipToPadding(false);
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setTheme(theme);
    }
    public void setTheme(int theme){
        painter=new KeyboardBackdrop(getContext(),theme);
        invalidate();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onDraw(Canvas c){
        if(painter!=null)painter.drawArtwork(c,getWidth(),getHeight());
        super.onDraw(c);
    }
    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        int cap=dp(landscape?maxLandscapeDp:maxPortraitDp);
        int mode=View.MeasureSpec.getMode(heightMeasureSpec);
        if(mode!=View.MeasureSpec.UNSPECIFIED){
            cap=Math.min(cap,View.MeasureSpec.getSize(heightMeasureSpec));
        }
        // Never accept the parent IME's full-screen EXACTLY height.
        // Keys and toolbar determine the height; the app remains visible above.
        super.onMeasure(widthMeasureSpec,
            View.MeasureSpec.makeMeasureSpec(cap,View.MeasureSpec.AT_MOST));
    }
}
