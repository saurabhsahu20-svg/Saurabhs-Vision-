package com.saurabhsvision.keyboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Original, offline geometric/nature artwork; no photos or third-party images. */
final class ThemeArtwork {
    private ThemeArtwork(){}
    static void drawOverlay(Canvas c,int theme,float w,float h){
        if(theme<45||theme>50||w<=1||h<=1)return;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        if(theme<=46){
            int tint=ThemePalette.ACCENTS[theme];
            for(int i=0;i<7;i++){
                float x=w*(.09f+.16f*(i%5));
                float y=h*(.13f+.24f*(i%4));
                float r=Math.min(w,h)*(.19f+.06f*(i%3));
                p.setShader(new RadialGradient(x,y,r,
                    new int[]{(tint&0xffffff)|0x33000000,0x00000000},
                    null,Shader.TileMode.CLAMP));
                c.drawCircle(x,y,r,p);p.setShader(null);
            }
            p.setColor(0x4dffffff);
            p.setStrokeWidth(Math.max(1,w*.003f));
            p.setStyle(Paint.Style.STROKE);
            for(int i=0;i<5;i++){
                c.drawLine(w*(.14f+i*.18f),0,w*(.03f+i*.18f),h,p);
            }
            p.setStyle(Paint.Style.FILL);
            return;
        }
        // Four locally painted scenic silhouettes. Theme color determines mood.
        int glow=ThemePalette.ACCENTS[theme];
        float sunX=w*(theme==49?.78f:.22f);
        float sunY=h*(theme==47?.22f:.28f);
        p.setColor((glow&0xffffff)|0x99_000000);
        c.drawCircle(sunX,sunY,Math.min(w,h)*.13f,p);
        p.setColor(0x1ff0ffff);
        for(int i=0;i<5;i++){
            float x=w*(.05f+i*.23f);
            c.drawOval(x,h*.13f,x+w*.23f,h*.25f,p);
        }
        Path hills=new Path();
        hills.moveTo(0,h*.85f);
        hills.lineTo(0,h*(theme==50?.56f:.64f));
        for(int i=0;i<7;i++){
            float x=w*i/6f;
            float y=h*(.42f+.22f*(float)(Math.sin(i*1.77+theme)*.5+.5));
            hills.lineTo(x,y);
        }
        hills.lineTo(w,h);hills.lineTo(0,h);
        hills.close();
        p.setColor(theme==49?0xff184b42:
            theme==47?0xff51406a:theme==48?0xff1e547d:0xff87533a);
        p.setAlpha(170);
        c.drawPath(hills,p);
        Path foreground=new Path();
        foreground.moveTo(0,h);
        foreground.lineTo(0,h*.81f);
        for(int i=0;i<9;i++){
            float x=w*i/8f;
            foreground.lineTo(x,h*(.71f+.13f*((i*7+theme)%4)/4f));
        }
        foreground.lineTo(w,h);foreground.close();
        p.setColor(theme==49?0xff143d37:
            theme==47?0xff2d234f:theme==48?0xff103c5c:0xff62412f);
        p.setAlpha(190);c.drawPath(foreground,p);
    }
}
