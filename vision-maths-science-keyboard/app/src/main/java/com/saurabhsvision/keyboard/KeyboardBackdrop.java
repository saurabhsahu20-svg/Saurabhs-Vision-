package com.saurabhsvision.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Original offline, lightweight Maths and Science neon visual backgrounds. */
public final class KeyboardBackdrop extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval=new RectF();
    private int theme;
    private static final int[] BASE={0xff061531,0xff1a0b31,0xff05283b,0xff1c103a};
    private static final int[] MID={0xff0f356c,0xff37225c,0xff135568,0xff46306f};
    private static final int[] HIGHLIGHT={0xff70e7ff,0xffffcf65,0xff9df7cb,0xffd5b8ff};
    public KeyboardBackdrop(Context context,int theme){super(context);this.theme=((theme%4)+4)%4;}
    private void spot(Canvas c,float x,float y,float r,int color,int alpha){
        paint.reset();paint.setAntiAlias(true);paint.setColor(color);paint.setAlpha(alpha);
        c.drawCircle(x,y,r,paint);
    }
    private void label(Canvas c,String s,float x,float y,float size,int color,int alpha){
        paint.reset();paint.setAntiAlias(true);paint.setTextSize(size);
        paint.setColor(color);paint.setAlpha(alpha);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        c.drawText(s,x,y,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        paint.reset();paint.setAntiAlias(true);
        paint.setShader(new LinearGradient(0,0,w,h,
            new int[]{BASE[theme],MID[theme],BASE[theme]},
            new float[]{0f,.56f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
        int hi=HIGHLIGHT[theme];
        for(int i=0;i<3;i++){
            float x=w*(i==0?.15f:i==1?.8f:.58f),y=h*(i==0?.16f:i==1?.65f:.99f);
            float r=w*(i==0?.38f:.28f);
            paint.setShader(new RadialGradient(x,y,r,
                new int[]{(hi&0x00ffffff)|0x37000000,(hi&0x00ffffff)},
                new float[]{0f,1f},Shader.TileMode.CLAMP));
            canvas.drawCircle(x,y,r,paint);paint.setShader(null);
        }
        // Stars, letters and scientific line art are deliberately faint behind keys.
        for(int i=0;i<42;i++){
            double v=Math.sin(i*19.712+theme*11)*2791.218;
            double a=v-Math.floor(v);
            double k=Math.sin(i*41.399+theme*5)*1321.593;
            double b=k-Math.floor(k);
            spot(canvas,(float)(a*w),(float)(b*h),.65f+(i%3)*.45f,
                i%5==0?hi:Color.WHITE,45+(i%4)*18);
        }
        float sz=Math.min(w,h);
        label(canvas,theme==0?"π² + E = mc²":theme==1?"∞   ∑   √":theme==2?"H₂O   DNA   ⚛":"α   β   Ω   λ",
            w*.06f,h*.37f,sz*.065f,hi,68);
        canvas.save();
        canvas.rotate(-12,w*.76f,h*.52f);
        paint.reset();paint.setAntiAlias(true);paint.setColor(hi);
        paint.setAlpha(55);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f,sz*.004f));
        for(int i=0;i<3;i++){
            canvas.save();canvas.rotate(i*60,w*.76f,h*.52f);
            oval.set(w*.76f-sz*.25f,h*.52f-sz*.06f,w*.76f+sz*.25f,h*.52f+sz*.06f);
            canvas.drawOval(oval,paint);canvas.restore();
        }
        canvas.restore();
        spot(canvas,w*.76f,h*.52f,sz*.018f,hi,88);
        // Footer decorative neon rule.
        paint.setStyle(Paint.Style.STROKE);paint.setColor(hi);paint.setAlpha(75);
        paint.setStrokeWidth(Math.max(1f,sz*.003f));
        canvas.drawLine(w*.05f,h-2,w*.95f,h-2,paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
