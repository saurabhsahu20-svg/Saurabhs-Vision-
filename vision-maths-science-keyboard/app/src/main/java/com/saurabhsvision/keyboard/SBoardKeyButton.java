package com.saurabhsvision.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.Button;

/**
 * Real, accessible native Android key button with a small neon indicator under
 * the Space label. Indicator scales with the key, even in landscape/compact IME.
 * It is drawn on the button itself (not an overlapping/fake Space image).
 */
public final class SBoardKeyButton extends Button {
    private boolean isSpaceKey=false;
    private boolean lightTheme=false;
    private final Paint neon=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF line=new RectF();

    public SBoardKeyButton(Context context){super(context);setAllCaps(false);}
    public void setSpaceIndicator(boolean show,boolean light){
        isSpaceKey=show;lightTheme=light;invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(!isSpaceKey || getWidth()<24 || getHeight()<18)return;
        float density=getResources().getDisplayMetrics().density;
        float center=getWidth()*.5f;
        float barHalf=Math.max(12*density,Math.min(getWidth()*.24f,44*density));
        float y=getHeight()-Math.max(6*density,getHeight()*.14f);
        line.set(center-barHalf,y,center+barHalf,y+Math.max(2*density,getHeight()*.045f));
        neon.reset();neon.setAntiAlias(true);
        int cyan=lightTheme?0xff2466d2:0xff2ef4ff;
        neon.setShader(new LinearGradient(line.left,line.top,line.right,line.top,
            new int[]{cyan&0x00ffffff,cyan,cyan&0x00ffffff},null,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(line,4*density,4*density,neon);
        neon.setShader(null);
    }
}
