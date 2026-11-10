package com.saurabhsvision.eduwalls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import java.io.File;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
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

    private final Context ctx;
    private Bitmap customBitmap;
    private long lastArtModified=Long.MIN_VALUE, lastArtSize=-1;
    public EducationPainter(Context context) {
        ctx=context.getApplicationContext();
    }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PosterPainter posterPainter = new PosterPainter();
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
    public void draw(Canvas c, int w, int h, float seconds, int requestedTheme, int speed, float tapX, float tapY, long tapAtMillis, boolean interactive, float pageOffset, boolean customArt, int poster) {
        if (w <= 0 || h <= 0) return;
        final int theme = Math.max(0,Math.min(requestedTheme,3));
        final float motion = speed == 0 ? 0.18f : (speed == 2 ? 1.2f : 0.52f);
        final float time = seconds * motion;
        final float size = Math.min(w,h);
        final int hi = HIGHLIGHT[theme];
        final int sec = SECONDARY[theme];
        if(poster>=0 && poster<PosterPainter.NAMES.length && !customArt) {
            posterPainter.draw(c,w,h,time,poster,pageOffset);
            drawPosterShimmer(c,w,h,time,hi,sec);
            if(interactive && tapAtMillis>0)drawTouchMagic(c,w,h,tapX,tapY,tapAtMillis,theme,hi,sec);
            return;
        }
        boolean photo=customArt && drawCustom(c,w,h,pageOffset,time);
        if(!photo) {
            color(Color.WHITE,255);
            p.setShader(new LinearGradient(0,0,w,h,BACK_A[theme],BACK_B[theme],Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);
            p.setShader(null);
            drawAurora(c,w,h,time,hi,sec);
        } else {
            color(Color.WHITE,255);
            p.setShader(new LinearGradient(0,0,0,h,
                new int[]{0xc5051029,0x10051b38,0x08051b38,0xa8081230},
                new float[]{0f,.25f,.72f,1f},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);
            p.setShader(null);
        }

        // Dim glowing colored orbs that drift slowly in the background.
        for(int k=0;k<(photo?2:4);k++) {
            float x=w*(0.12f+0.72f*noise(k+17))+(float)Math.sin(time*.22f+k)*w*.035f;
            float y=h*(.10f+.82f*noise(k+57))+(float)Math.cos(time*.20f+k)*h*.025f;
            dot(c,x,y,size*(.13f+noise(k+23)*.15f),k%2==0?hi:sec,13);
        }
        // Starfield and small depth lights.
        for(int k=0;k<(photo?38:45);k++) {
            float x=w*noise(k+190)+pageOffset*w*(.04f+.04f*noise(k+12));
            float y=h*noise(k+480);
            float pulse=.65f+.35f*(float)Math.sin(time*.8f+k*.8f);
            dot(c,x,y,(.55f+noise(k+440)*1.55f)*size/420f*(w/320f),Color.WHITE,Math.max(25,(int)((40+noise(k+88)*125)*pulse)));
        }

        // Animated floating academic notation.
        String[] symbols=SYMBOLS[theme];
        for(int k=0;k<(photo?8:14);k++){
            float x=w*(.09f+.82f*noise(k+37));
            float y=h*frac(noise(k+96) + time*(.008f+.006f*noise(k+20)));
            float drift=(float)Math.sin(time*.7f+k*.9f)*w*.025f;
            text(c,symbols[k%symbols.length],x+drift,y, Math.max(12f,size*(.034f+.013f*noise(k+71))), k%3==0?hi:Color.WHITE,(photo?42:70)+(int)(noise(k+91)*(photo?54:85)), k%3==0);
        }

        float cx=w*.50f+pageOffset*w*.024f,cy=h*.49f;
        if(!photo) {
            if(theme==MATHS || theme==JUNIOR) drawBook(c,cx,cy,size,hi,sec,time,theme==JUNIOR);
            else if(theme==SCIENCE) drawAtom(c,cx,cy,size,hi,sec,time);
            else drawPlanet(c,cx,cy,size,hi,sec,time);
            drawKnowledgePulse(c,w,h,theme,time,hi,sec);
        } else {
            drawPosterShimmer(c,w,h,time,hi,sec);
        }

        // Elegant border arc and title/brand.
        color(hi,photo?85:150);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.5f,size*.004f));
        oval.set(w*.09f,h*.12f,w*.91f,h*.88f);c.drawRoundRect(oval,size*.07f,size*.07f,p);p.setStyle(Paint.Style.FILL);
        float top=h*.165f;
        text(c,"SAURABH’S VISION",cx,top,Math.min(size*.058f,28f),Color.WHITE,240,true);
        text(c,"LEARN TODAY, LEAD TOMORROW",cx,top+Math.min(size*.037f,18f)*1.7f,Math.min(size*.026f,13f),hi,215,true);
        if(!photo)text(c,TITLES[theme],cx,h*.81f,Math.min(size*.052f,26f),Color.WHITE,230,true);
        if(!photo)text(c,"STRONG CONCEPTS • BRIGHT RESULTS",cx,h*.85f,Math.min(size*.021f,11f),hi,210,true);

        if (interactive && tapAtMillis > 0) {
            drawTouchMagic(c,w,h,tapX,tapY,tapAtMillis,theme,hi,sec);
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

    // Soft aurora lighting is rendered as canvas gradients, never as large images.
    private boolean drawCustom(Canvas c,int w,int h,float offset,float time) {
        File art = new File(ctx.getFilesDir(),"vision_artwork.jpg");
        long m=art.exists()?art.lastModified():-1;
        long sz=art.exists()?art.length():-1;
        if(m!=lastArtModified||sz!=lastArtSize){
            lastArtModified=m;lastArtSize=sz;
            if(customBitmap!=null){customBitmap.recycle();customBitmap=null;}
            if(sz>0) {
                try {
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inPreferredConfig=Bitmap.Config.RGB_565;
                    customBitmap=BitmapFactory.decodeFile(art.getAbsolutePath(),opts);
                }catch(Exception ignored){customBitmap=null;}
            }
        }
        if(customBitmap==null||customBitmap.isRecycled())return false;
        float bw=customBitmap.getWidth(),bh=customBitmap.getHeight();
        if(bw<=0||bh<=0)return false;
        float scale=Math.max(w/bw,h/bh)*1.06f;
        float wd=bw*scale,ht=bh*scale;
        float x=(w-wd)*.5f+offset*w*.018f+(float)Math.sin(time*.11f)*w*.007f;
        float y=(h-ht)*.5f+(float)Math.cos(time*.12f)*h*.007f;
        color(Color.WHITE,255);p.setFilterBitmap(true);
        c.drawBitmap(customBitmap,null,new RectF(x,y,x+wd,y+ht),p);
        return true;
    }
    private void drawPosterShimmer(Canvas c,int w,int h,float t,int hi,int sec){
        float z=Math.min(w,h);
        for(int k=0;k<30;k++) {
            float x=w*noise(k+120);
            float y=h*frac(noise(k+520)+t*(.003f+.002f*noise(k+9)));
            float f=(float)(.45+.55*Math.abs(Math.sin(t*.85f+k)));
            dot(c,x,y,z*(.0015f+.004f*noise(k+54)),k%3==0?hi:sec,(int)(150*f));
            if(k%6==0) {
                float cross=z*.012f*f;
                line(c,x-cross,y,x+cross,y,Color.WHITE,(int)(170*f),Math.max(1f,z*.002f));
                line(c,x,y-cross,x,y+cross,Color.WHITE,(int)(170*f),Math.max(1f,z*.002f));
            }
        }
        float x=w*frac(t*.065f)-w*.14f,y=h*.64f;
        for(int j=0;j<12;j++){
            dot(c,x-j*z*.024f,y+j*z*.007f,z*(.001f+.0007f*(12-j)),j%2==0?hi:sec,(j+3)*10);
        }
    }
    private void drawAurora(Canvas c,int w,int h,float t,int hi,int sec) {
        float z=Math.min(w,h);
        for(int k=0;k<3;k++) {
            float cx=w*(.16f+.34f*k)+(float)Math.sin(t*.19f+k)*w*.10f;
            float cy=h*(.32f+.20f*(k%2))+(float)Math.cos(t*.13f+k)*h*.07f;
            float radius=z*(.40f+noise(k+44)*.22f);
            color(Color.WHITE,27);
            int rgb=(k%2==0?hi:sec);
            p.setShader(new RadialGradient(cx,cy,radius,new int[]{rgb,rgb & 0x00ffffff},null,Shader.TileMode.CLAMP));
            c.drawCircle(cx,cy,radius,p);
            p.setShader(null);
        }
        // Fine glowing threads sweep below the center to create a layered scene.
        for(int j=0;j<3;j++){
            float top=h*(.67f+j*.018f);
            color(j==1?hi:sec,30+j*8);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(z*(.003f+j*.001f));
            path.reset();path.moveTo(-w*.10f,top);
            path.cubicTo(w*.25f,top-z*.13f+(float)Math.sin(t*.3f+j)*z*.02f,w*.72f,top+z*.12f,w*1.1f,top-z*.03f);
            c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void drawKnowledgePulse(Canvas c,int w,int h,int theme,float t,int hi,int sec) {
        float z=Math.min(w,h);
        if(theme==MATHS) {
            // Live sine wave: a visible link between algebra and trigonometry.
            float base=h*.70f;
            line(c,w*.19f,base,w*.81f,base,sec,95,1.4f);
            line(c,w*.50f,base-z*.048f,w*.50f,base+z*.048f,sec,95,1.4f);
            float lastX=0,lastY=0;
            for(int j=0;j<=60;j++) {
                float x=w*(.19f+.62f*j/60f);
                float y=base-(float)Math.sin(j*.31f+t*.65f)*z*.029f;
                if(j>0)line(c,lastX,lastY,x,y,hi,165,2.1f);
                lastX=x;lastY=y;
            }
            dot(c,lastX,lastY,z*.007f,hi,230);
            text(c,"f(x) = sin(x)",w*.50f,base+z*.090f,z*.030f,sec,165,true);
        } else if(theme==SCIENCE) {
            // Two animated complementary strands as a stylized DNA helix.
            float top=h*.65f, bottom=h*.748f;
            for(int j=0;j<=18;j++) {
                float y=top+(bottom-top)*j/18f;
                float sn=(float)Math.sin(j*.62f+t*.78f);
                float x1=w*.50f+sn*z*.083f, x2=w*.50f-sn*z*.083f;
                if(j%2==0)line(c,x1,y,x2,y,sec,105,2f);
                dot(c,x1,y,z*.010f,hi,210);
                dot(c,x2,y,z*.010f,sec,210);
            }
            text(c,"DNA  •  LIFE SCIENCE",w*.50f,h*.775f,z*.025f,Color.WHITE,145,true);
        } else if(theme==COSMOS) {
            // Repeating comet with a tapered tail, independent of network or video assets.
            float phase=frac(t*.048f);
            float x=w*(-.17f+1.43f*phase),y=h*(.25f+.18f*phase);
            for(int j=9;j>=0;j--) {
                float px=x-j*z*.029f,py=y-j*z*.011f;
                dot(c,px,py,z*(.0025f+.0009f*(10-j)),j%3==0?hi:sec,(int)((10-j)*18));
            }
            dot(c,x,y,z*.012f,Color.WHITE,236);
            // Slow orbital guide around a nearby star.
            color(sec,92);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);
            oval.set(w*.23f,h*.65f,w*.77f,h*.74f);c.drawOval(oval,p);p.setStyle(Paint.Style.FILL);
            float theta=t*.48f;
            dot(c,w*.50f+(float)Math.cos(theta)*w*.27f,h*.695f+(float)Math.sin(theta)*h*.045f,z*.010f,hi,230);
            text(c,"KEEP EXPLORING",w*.50f,h*.79f,z*.026f,Color.WHITE,170,true);
        } else {
            // Countable glowing bubbles for younger learners.
            String[] nums={"1","2","3","4","5"};
            for(int j=0;j<5;j++){
                float x=w*(.24f+j*.13f);
                float y=h*.70f+(float)Math.sin(t*.72f+j*.7f)*z*.015f;
                dot(c,x,y,z*.040f,j%2==0?hi:sec,100);
                text(c,nums[j],x,y+z*.012f,z*.040f,Color.WHITE,235,true);
            }
            text(c,"COUNT • DISCOVER • GROW",w*.50f,h*.787f,z*.025f,Color.WHITE,160,true);
        }
    }
    private void drawTouchMagic(Canvas c,int w,int h,float x,float y,long time,int theme,int hi,int sec) {
        float elapsed=(android.os.SystemClock.uptimeMillis()-time)/1000f;
        if(elapsed<0 || elapsed>1.65f)return;
        float z=Math.min(w,h),progress=elapsed/1.65f;
        float opacity=(1f-progress)*(1f-progress);
        for(int k=0;k<3;k++){
            float f=progress-k*.16f;
            if(f<0)continue;
            color(k==0?hi:sec,(int)(210*opacity));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(z*(.008f-.0016f*k));
            c.drawCircle(x,y,z*(.08f+f*.45f),p);
            p.setStyle(Paint.Style.FILL);
        }
        for(int j=0;j<22;j++){
            float angle=(float)(j*Math.PI*2/22+noise(j+777)*.18f);
            float reach=z*(.025f+progress*(.28f+.12f*noise(j+321)));
            float px=x+(float)Math.cos(angle)*reach;
            float py=y+(float)Math.sin(angle)*reach;
            dot(c,px,py,z*(.0045f+.006f*(1f-progress)),j%3==0?hi:sec,(int)(235*opacity));
        }
        if(elapsed<1.25f){
            final String[] facts={"π  ≈  3.14159","ATOM  •  MATTER","THE UNIVERSE IS VAST","LEARN  •  PLAY  •  GROW"};
            float tx=Math.max(w*.20f,Math.min(w*.80f,x));
            float ty=Math.max(h*.21f,Math.min(h*.83f,y-z*.19f));
            float half=z*.21f;
            color(0xff061b42,(int)(183*opacity));
            oval.set(tx-half,ty-z*.065f,tx+half,ty+z*.035f);
            c.drawRoundRect(oval,z*.036f,z*.036f,p);
            text(c,facts[theme],tx,ty,z*.034f,Color.WHITE,(int)(240*opacity),true);
        }
    }
}
