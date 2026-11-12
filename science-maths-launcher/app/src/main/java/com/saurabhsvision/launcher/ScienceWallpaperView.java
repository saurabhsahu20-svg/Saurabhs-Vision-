package com.saurabhsvision.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import java.util.Random;

/**
 * Offline Math/Science wallpaper drawn natively by Android.
 * Pure Canvas, no Internet images or heavy animations; renders on demand.
 */
public final class ScienceWallpaperView extends View {
    public static final String[] THEMES={
        "Cosmic Mathematics","Atomic Science","DNA Galaxy","Golden Equations"
    };
    private static final int[][] PALETTE={
        {0xff06142e,0xff0b3477,0xff061528,0xff3edfff,0xffffd36a},
        {0xff041c27,0xff0b4d56,0xff061c39,0xff64f7d5,0xfff5d568},
        {0xff180d3e,0xff1f306a,0xff07152d,0xffd4a7ff,0xff46f7ff},
        {0xff28102c,0xff653542,0xff0b1c39,0xffffd477,0xff58e7ff}
    };
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int theme=0;
    public ScienceWallpaperView(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void setThemeIndex(int number){
        theme=LauncherRules.themeIndex(number,THEMES.length);invalidate();
    }
    public int getThemeIndex(){return theme;}
    private void color(int color,float stroke){
        paint.reset();paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(stroke>0?Paint.Style.STROKE:Paint.Style.FILL);
        if(stroke>0)paint.setStrokeWidth(stroke);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        int[] colors=PALETTE[theme];
        color(colors[0],0);
        paint.setShader(new LinearGradient(0,0,w,h,
            new int[]{colors[0],colors[1],colors[2]},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);
        float scale=Math.min(w/420f,h/850f);
        Random rand=new Random(711+theme*31);
        for(int i=0;i<132;i++){
            float x=rand.nextFloat()*w,y=rand.nextFloat()*h;
            int alpha=55+rand.nextInt(125);
            color(Color.argb(alpha,210,237,255),0);
            canvas.drawCircle(x,y,scale*(0.55f+rand.nextFloat()*1.05f),paint);
        }
        // Decorative low-opacity formulae, leaving the app icons readable.
        float[] coords={.09f,.27f,.65f,.41f,.12f,.58f,.60f,.76f,.16f,.86f};
        String[] equations={"π ≈ 3.14159","E = mc²","∑ n²","H₂O","f(x) = x²"};
        for(int i=0;i<equations.length;i++){
            color((colors[3]&0x00ffffff)|0x34000000,0);
            paint.setTextSize(scale*(i==0?23:25));
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.BOLD));
            canvas.drawText(equations[i],w*coords[i*2],h*coords[i*2+1],paint);
        }
        // Neon central atom.
        float ax=w*.73f,ay=h*.38f;
        float radius=scale*65;
        for(int i=0;i<3;i++){
            color((colors[3]&0x00ffffff)|0xa8000000,scale*1.75f);
            canvas.save();
            canvas.rotate(i*60,ax,ay);
            canvas.drawOval(new RectF(ax-radius,ay-radius*.36f,
                ax+radius,ay+radius*.36f),paint);
            canvas.restore();
        }
        color(colors[4],0);
        canvas.drawCircle(ax,ay,scale*9,paint);
        // Helix motif behind the lower app drawer/dock.
        float start=h*.63f,length=h*.3f,xCenter=w*.72f;
        for(int i=0;i<47;i++){
            float t=i/46f;
            float y=start+t*length;
            float phase=(float)(t*Math.PI*6);
            float x1=xCenter+(float)Math.sin(phase)*scale*29;
            float x2=xCenter-(float)Math.sin(phase)*scale*29;
            color((colors[3]&0x00ffffff)|0x99000000,scale*1.25f);
            canvas.drawLine(x1,y,x2,y,paint);
            color((colors[4]&0x00ffffff)|0xc5000000,0);
            canvas.drawCircle(x1,y,scale*2.5f,paint);
            color((colors[3]&0x00ffffff)|0xc5000000,0);
            canvas.drawCircle(x2,y,scale*2.5f,paint);
        }
        // Low-key grid at lower edge.
        color((colors[3]&0x00ffffff)|0x22000000,scale*.7f);
        for(int i=0;i<=10;i++){
            float y=h*.83f+i*scale*15f;
            if(y<h)canvas.drawLine(0,y,w,y,paint);
        }
        for(int i=0;i<9;i++)canvas.drawLine(i*w/8f,h*.8f,i*w/8f,h,paint);
    }
}
