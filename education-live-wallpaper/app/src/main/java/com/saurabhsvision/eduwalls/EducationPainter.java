package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Rendered on both the activity preview and the system live wallpaper. */
public final class EducationPainter {
    public static final int MATHS = 0;
    public static final int SCIENCE = 1;
    public static final int COSMOS = 2;
    public static final int JUNIOR = 3;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();

    private static final String[][] SYMBOLS = {
        {"π", "x²", "∑", "√", "45°", "a+b", "∫", "∞", "Δ", "÷", "3.14", "sin"},
        {"H₂O", "CO₂", "DNA", "O₂", "NaCl", "Fe", "F=ma", "⚛", "λ", "E=mc²", "pH", "ATP"},
        {"★", "☾", "3.14", "✦", "π", "∞", "☄", "R²", "⋆", "Σ", "☀", "✧"},
        {"ABC", "123", "A+", "✦", "2+2", "🌟", "√", "5×5", "☺", "✿", "GO!", "π"}
    };

    private static final int[] BACK_A = {0xff05102d,0xff061f33,0xff080f33,0xff173068};
    private static final int[] BACK_B = {0xff153d9e,0xff07566a,0xff292269,0xff6d3b98};
    private static final int[] HIGHLIGHT = {0xffffd342,0xff64eeff,0xffffcb60,0xffffe476};
    private static final int[] SECONDARY = {0xff70d5ff,0xff8cf4ca,0xffad9fff,0xff91f5ff};
    private static final String[] TITLES = {"MATHS GALAXY","SCIENCE LAB","COSMIC CLASSROOM","JUNIOR LEARNING"};

    public static String title(int theme) {
        return TITLES[Math.max(0, Math.min(3, theme))];
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
    private static float noise(int seed) { return frac((float)Math.sin(seed * 12.9898f + 78.233f) * 43758.5453f); }
    private void color(int c, int alpha) {
        p.reset();
        p.setAntiAlias(true);
        p.setColor(c);
        p.setAlpha(alpha);
    }
    private void dot(Canvas c,float x,float y,float radius,int color,int alpha) {
        color(color,alpha);
        c.drawCircle(x,y,radius,p);
    }
    private void line(Canvas c,float x,float y,float x2,float y2,int color,int alpha,float width) {
        color(color,alpha);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x,y,x2,y2,p);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color,int alpha,boolean bold) {
        color(color,alpha);
        p.setTextSize(size);
        p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(value,x,y,p);
    }
    public void draw(Canvas c, int w, int h, float seconds, int requestedTheme, int speed, float tapX, float tapY, long tapAtMillis) {
        if (w <= 0 || h <= 0) return;
        final int theme = Math.max(0,Math.min(requestedTheme,3));
        final float motion = speed == 0 ? 0.18f : (speed == 2 ? 1.2f : 0.52f);
        final float time = seconds * motion;
        final float size = Math.min(w,h);
        final int hi = HIGHLIGHT[theme];
        final int sec = SECONDARY[theme];
        color(Color.WHITE,255);
        p.setShader(new LinearGradient(0,0,w,h,BACK_A[theme],BACK_B[theme],Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p);
        p.setShader(null);

        // Dim glowing colored orbs that drift slowly in the background.
        for(int k=0;k<4;k++) {
            float x=w*(0.12f+0.72f*noise(k+17))+(float)Math.sin(time*.22f+k)*w*.035f;
            float y=h*(.10f+.82f*noise(k+57))+(float)Math.cos(time*.20f+k)*h*.025f;
            dot(c,x,y,size*(.13f+noise(k+23)*.15f),k%2==0?hi:sec,13);
        }
        // Starfield and small depth lights.
        for(int k=0;k<45;k++) {
            float x=w*noise(k+190);
            float y=h*noise(k+480);
            float pulse=.65f+.35f*(float)Math.sin(time*.8f+k*.8f);
            dot(c,x,y,(.55f+noise(k+440)*1.55f)*size/420f*(w/320f),Color.WHITE,Math.max(25,(int)((40+noise(k+88)*125)*pulse)));
        }

        // Animated floating academic notation.
        String[] symbols=SYMBOLS[theme];
        for(int k=0;k<14;k++){
            float x=w*(.09f+.82f*noise(k+37));
            float y=h*frac(noise(k+96) + time*(.008f+.006f*noise(k+20)));
            float drift=(float)Math.sin(time*.7f+k*.9f)*w*.025f;
            text(c,symbols[k%symbols.length],x+drift,y, Math.max(12f,size*(.034f+.013f*noise(k+71))), k%3==0?hi:Color.WHITE,70+(int)(noise(k+91)*85), k%3==0);
        }

        float cx=w*.50f,cy=h*.49f;
        if(theme==MATHS || theme==JUNIOR) drawBook(c,cx,cy,size,hi,sec,time,theme==JUNIOR);
        else if(theme==SCIENCE) drawAtom(c,cx,cy,size,hi,sec,time);
        else drawPlanet(c,cx,cy,size,hi,sec,time);

        // Elegant border arc and title/brand.
        color(hi,150);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.5f,size*.004f));
        oval.set(w*.09f,h*.12f,w*.91f,h*.88f);c.drawRoundRect(oval,size*.07f,size*.07f,p);p.setStyle(Paint.Style.FILL);
        float top=h*.165f;
        text(c,"SAURABH’S VISION",cx,top,Math.min(size*.058f,28f),Color.WHITE,240,true);
        text(c,"LEARN TODAY, LEAD TOMORROW",cx,top+Math.min(size*.037f,18f)*1.7f,Math.min(size*.026f,13f),hi,215,true);
        text(c,TITLES[theme],cx,h*.81f,Math.min(size*.052f,26f),Color.WHITE,230,true);
        text(c,"STRONG CONCEPTS • BRIGHT RESULTS",cx,h*.85f,Math.min(size*.021f,11f),hi,210,true);

        if (tapAtMillis > 0 && speed > 0) {
            float elapsed=(android.os.SystemClock.uptimeMillis()-tapAtMillis)/1000f;
            if(elapsed>=0 && elapsed < 1.2f){
                float radius=elapsed*size*.35f;
                color(hi,(int)(180*(1f-elapsed/1.2f)));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.8f,size*.007f));
                c.drawCircle(tapX,tapY,radius,p);p.setStyle(Paint.Style.FILL);
                for(int j=0;j<8;j++) {
                    float angle=(float)(j*Math.PI/4);
                    dot(c,tapX+(float)Math.cos(angle)*radius*.7f,tapY+(float)Math.sin(angle)*radius*.7f,2.7f,sec,(int)(175*(1-elapsed/1.2f)));
                }
            }
        }
    }
    private void drawBook(Canvas c,float cx,float cy,float z,int gold,int blue,float t,boolean junior){
        float s=z*.45f;
        float bob=(float)Math.sin(t*.8f)*z*.020f;
        float y=cy+bob;
        // A bright open book with pages and a floating knowledge spark.
        path.reset();
        path.moveTo(cx,y+s*.10f);
        path.cubicTo(cx-s*.33f,y-s*.15f,cx-s*.66f,y-s*.15f,cx-s*.82f,y-s*.04f);
        path.lineTo(cx-s*.82f,y+s*.49f);
        path.cubicTo(cx-s*.47f,y+s*.37f,cx-s*.15f,y+s*.44f,cx,y+s*.60f);
        path.close();
        color(Color.WHITE,229);c.drawPath(path,p);
        path.reset();
        path.moveTo(cx,y+s*.10f);
        path.cubicTo(cx+s*.33f,y-s*.15f,cx+s*.66f,y-s*.15f,cx+s*.82f,y-s*.04f);
        path.lineTo(cx+s*.82f,y+s*.49f);
        path.cubicTo(cx+s*.47f,y+s*.37f,cx+s*.15f,y+s*.44f,cx,y+s*.60f);
        path.close();
        color(0xffc5eaff,233);c.drawPath(path,p);
        for(int j=0;j<3;j++){
            float yy=y+s*(.10f+j*.095f);
            line(c,cx-s*.68f,yy,cx-s*.17f,yy+s*.10f,0xff2255b6,126,1.8f);
            line(c,cx+s*.18f,yy+s*.10f,cx+s*.68f,yy,0xff2255b6,126,1.8f);
        }
        line(c,cx,y+s*.10f,cx,y+s*.59f,gold,224,3.5f);
        for(int j=0;j<8;j++) {
            float ang=(float)(j*Math.PI/4+t*.07f);
            float r=s*.42f;
            float xx=cx+(float)Math.cos(ang)*r;
            float yy=y-s*.36f+(float)Math.sin(ang)*r*.52f;
            dot(c,xx,yy,2.6f,gold,190);
        }
        text(c,junior?"A + B":"π  +  ∑",cx,y-s*.50f,s*.26f,gold,244,true);
        dot(c,cx,y-s*.74f,Math.max(3f,s*.035f),blue,225);
    }
    private void drawAtom(Canvas c,float cx,float cy,float z,int gold,int blue,float t) {
        float r=z*.30f;
        for(int k=0;k<3;k++){
            c.save();c.rotate(k*60f+(float)Math.sin(t*.35f)*9f,cx,cy);
            color(k==0?gold:blue,198);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2.0f,z*.007f));
            oval.set(cx-r,cy-r*.45f,cx+r,cy+r*.45f);c.drawOval(oval,p);
            p.setStyle(Paint.Style.FILL);
            float ang=t*(.6f+.14f*k)+k*2.1f;
            dot(c,cx+(float)Math.cos(ang)*r,cy+(float)Math.sin(ang)*r*.45f,z*.022f,k%2==0?blue:gold,245);
            c.restore();
        }
        dot(c,cx,cy,z*.079f,gold,210);
        dot(c,cx-z*.022f,cy-z*.012f,z*.034f,Color.WHITE,170);
        dot(c,cx+z*.023f,cy+z*.014f,z*.031f,blue,200);
        text(c,"SCIENCE",cx,cy+r*.98f,z*.07f,Color.WHITE,200,true);
    }
    private void drawPlanet(Canvas c,float cx,float cy,float z,int gold,int blue,float t) {
        float r=z*.23f, bob=(float)Math.sin(t*.7f)*z*.022f;cy+=bob;
        dot(c,cx,cy,r,blue,225);
        dot(c,cx-r*.29f,cy-r*.30f,r*.39f,gold,70);
        c.save();c.rotate(-24,cx,cy);
        color(gold,210);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(z*.024f);
        oval.set(cx-r*1.63f,cy-r*.36f,cx+r*1.63f,cy+r*.36f);c.drawOval(oval,p);
        p.setStrokeWidth(z*.006f);p.setColor(Color.WHITE);p.setAlpha(150);c.drawOval(oval,p);
        p.setStyle(Paint.Style.FILL);c.restore();
        float mX=cx+(float)Math.cos(t*.48f)*r*1.7f,mY=cy+(float)Math.sin(t*.48f)*r*.8f;
        dot(c,mX,mY,r*.13f,gold,230);
        text(c,"EXPLORE THE UNIVERSE",cx,cy+r*1.65f,z*.043f,Color.WHITE,220,true);
    }
}
