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
    private boolean previewEnabled=false;
    private String previewLabel="";
    private boolean lightTheme=false;
    private int indicatorColor=0xff2ef4ff;
    private final Paint neon=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF line=new RectF();

    public SBoardKeyButton(Context context){super(context);setAllCaps(false);}
    public void setSpaceIndicator(boolean show,boolean light,int accent){
        isSpaceKey=show;lightTheme=light;
        indicatorColor=accent;
        invalidate();
    }
    public void setKeyPopup(boolean enabled,String label){
        previewEnabled=enabled;
        previewLabel=label==null?"":label;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        // Cheap in-key pressed preview: never creates floating windows or
        // animations, so fast typing and Android's IME touch target stay stable.
        if(previewEnabled&&isPressed()&&!previewLabel.isEmpty()&&
            getWidth()>16&&getHeight()>16){
            float d=getResources().getDisplayMetrics().density;
            float w=getWidth(),h=getHeight();
            neon.reset();neon.setAntiAlias(true);
            neon.setColor(lightTheme?0xff356cd6:0xffafeaff);
            line.set(w*.13f,h*.03f,w*.87f,h*.60f);
            canvas.drawRoundRect(line,8*d,8*d,neon);
            neon.setTextAlign(Paint.Align.CENTER);
            neon.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            neon.setColor(lightTheme?0xffffffff:0xff12314b);
            neon.setTextSize(Math.min(h*.46f,23*d));
            canvas.drawText(previewLabel,w*.5f,h*.44f,neon);
        }
        if(!isSpaceKey || getWidth()<24 || getHeight()<18)return;
        float density=getResources().getDisplayMetrics().density;
        float center=getWidth()*.5f;
        float barHalf=Math.max(12*density,Math.min(getWidth()*.24f,44*density));
        float y=getHeight()-Math.max(6*density,getHeight()*.14f);
        line.set(center-barHalf,y,center+barHalf,y+Math.max(2*density,getHeight()*.045f));
        neon.reset();neon.setAntiAlias(true);
        int glow=indicatorColor;
        neon.setShader(new LinearGradient(line.left,line.top,line.right,line.top,
            new int[]{glow&0x00ffffff,glow,glow&0x00ffffff},null,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(line,4*density,4*density,neon);
        neon.setShader(null);
    }
}
