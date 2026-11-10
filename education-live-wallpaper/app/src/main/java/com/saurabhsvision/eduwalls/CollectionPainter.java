package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Fifty numbered original, procedurally drawn education wallpaper presets.
 * Each preset uses one of the original eight poster scenes with a unique
 * color treatment, framing, headline and theme animation (50 static + 50 live).
 */
public final class CollectionPainter {
    private final PosterPainter poster=new PosterPainter();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final Path path=new Path();
    private static final int WHITE=Color.WHITE;

    private void fill(int col,int alpha){paint.reset();paint.setAntiAlias(true);paint.setColor(col);paint.setAlpha(Math.max(0,Math.min(255,alpha)));}
    private void dot(Canvas c,float x,float y,float r,int col,int a){fill(col,a);c.drawCircle(x,y,r,paint);}
    private void line(Canvas c,float x,float y,float xx,float yy,int col,int a,float wd){
        fill(col,a);paint.setStrokeWidth(wd);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,paint);
    }
    private void txt(Canvas c,String t,float x,float y,float sz,int col,boolean bold){
        fill(col,255);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(sz);
        paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(t,x,y,paint);
    }
    private static float noise(int k){double d=Math.sin(k*93.7+18.2)*43111.453;return (float)(d-Math.floor(d));}
    public void draw(Canvas c,int width,int height,float seconds,int index,boolean live,float tapX,float tapY,long tapAt) {
        if(width<=0||height<=0)return;
        final int id=CollectionCatalog.norm(index),category=id/10,variation=id%10;
        final int hi=CollectionCatalog.accent(id);
        final float t=live?seconds:0f;
        c.save();
        c.scale(width/360f,height/640f);
        poster.draw(c,360,640,t,CollectionCatalog.posterBase(id),live?(float)Math.sin(t*.13f)*.6f:0f);

        // Each collection entry receives its own tint and contrast overlay.
        int shade=Color.rgb((hi>>16)&255,(hi>>8)&255,hi&255);
        fill(shade,12+variation*3);
        paint.setShader(new LinearGradient(0,0,360,640,new int[]{shade & 0x33ffffff,0x00000000,shade & 0x22ffffff},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,paint);paint.setShader(null);
        fill(0xff06132e,173);
        paint.setShader(new LinearGradient(0,0,0,640,
            new int[]{0xe6051533,0x00000000,0x11051533,0xcd07142d},
            new float[]{0f,.24f,.73f,1f},Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,paint);paint.setShader(null);

        // Variation-specific decorative corner geometry and orbit tracks.
        for(int j=0;j<7;j++){
            float x=22+noise(id*11+j)*315,y=68+noise(id*13+j+71)*508;
            float shift=live?(float)Math.sin(t*.65f+j)*4:0;
            dot(c,x+shift,y,1.7f+noise(j+id)*2.5f,j%2==0?hi:WHITE,128);
        }
        if(variation%2==0){
            fill(hi,152);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);
            bounds.set(22+variation*1.1f,90,338-variation*1.1f,530);
            c.drawRoundRect(bounds,26,26,paint);
            paint.setStyle(Paint.Style.FILL);
        }else{
            for(int j=0;j<3;j++){
                line(c,22,65+j*4,73+j*9,65+j*4,hi,156,1.4f);
                line(c,288-j*8,575+j*4,336,575+j*4,hi,156,1.4f);
            }
        }
        if(variation%3==0){
            float y=358+(live?(float)Math.sin(t*.6f)*6:0);
            for(int n=0;n<13;n++){
                float angle=(float)(n*Math.PI*2/13);
                dot(c,180+(float)Math.cos(angle)*124,y+(float)Math.sin(angle)*51,
                    2.1f,n%3==0?hi:WHITE,98);
            }
        }
        // Readable curated collection title.
        fill(0xff081635,206);
        bounds.set(26,25,334,79);c.drawRoundRect(bounds,17,17,paint);
        txt(c,CollectionCatalog.category(id)+"  •  "+String.format(java.util.Locale.US,"%02d",id+1),180,46,13,hi,true);
        txt(c,CollectionCatalog.name(id),180,68,17,WHITE,true);
        fill(0xff081635,180);
        bounds.set(34,555,326,600);c.drawRoundRect(bounds,14,14,paint);
        txt(c,"SAURABH'S VISION",180,576,15,WHITE,true);
        txt(c,"LEARN TODAY • LEAD TOMORROW",180,592,9,hi,true);

        if(live){
            float motion=t*.66f+variation*.7f;
            // Different motion modes across 50 live scenes.
            switch(variation%5){
                case 0: // orbital knowledge sparks
                    for(int j=0;j<9;j++){
                        float a=motion+j*(float)Math.PI*2/9;
                        dot(c,180+(float)Math.cos(a)*95,335+(float)Math.sin(a)*120,
                            2.8f,j%2==0?hi:WHITE,200);
                    }break;
                case 1: // sine-wave maths
                    for(int j=0;j<43;j++){
                        float x=45+j*6.3f,y=484+(float)Math.sin(j*.3+motion)*16;
                        dot(c,x,y,1.5f,hi,190);
                    }break;
                case 2: // falling stardust
                    for(int j=0;j<20;j++){
                        float x=20+noise(id*71+j)*320;
                        float y=(noise(id*31+j+7)*640 + motion*15*(.5f+noise(j+13)))%640;
                        dot(c,x,y,1.3f+noise(j)*2.2f,j%3==0?hi:WHITE,142);
                    }break;
                case 3: // expanding geometry
                    float rr=42+(float)Math.sin(motion)*11;
                    fill(hi,170);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
                    c.drawCircle(180,335,rr,paint);
                    c.drawCircle(180,335,rr*1.72f,paint);
                    paint.setStyle(Paint.Style.FILL);break;
                default: // travelling comet
                    float x=20+(motion*45)%360,y=210+(float)Math.sin(motion)*45;
                    for(int j=0;j<10;j++)dot(c,x-j*7,y+j*2,1.1f+j*.18f,hi,23+j*19);
            }
            // Actual reactive touch ripple, shared by all 50 live designs.
            if(tapAt>0){
                float elapsed=(android.os.SystemClock.uptimeMillis()-tapAt)/1000f;
                if(elapsed>=0&&elapsed<1.5f){
                    float px=tapX*360f/width,py=tapY*640f/height;
                    fill(hi,(int)(190*(1-elapsed/1.5f)));
                    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
                    c.drawCircle(px,py,25+elapsed*100,paint);paint.setStyle(Paint.Style.FILL);
                    for(int j=0;j<12;j++){
                        float a=(float)(j*Math.PI/6);
                        dot(c,px+(float)Math.cos(a)*(25+elapsed*74),py+(float)Math.sin(a)*(25+elapsed*74),
                            2.6f,hi,(int)(195*(1-elapsed/1.5f)));
                    }
                }
            }
        }
        c.restore();
    }
}
