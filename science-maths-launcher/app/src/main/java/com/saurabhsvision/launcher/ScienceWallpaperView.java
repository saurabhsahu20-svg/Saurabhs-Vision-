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
        "Cosmic Mathematics","Atomic Science","DNA Galaxy","Golden Equations",
        "Pearl Light • Maths & Science"
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
    /** Clean white/sky-blue/gold science wallpaper behind REAL UI controls.
     * Intentionally no mock status bar, fake Google UI or fake weather.
     */
    private void drawPearlLight(Canvas canvas,int w,int h){
        float scale=Math.min(w/410f,h/830f);
        color(0xfff7fcff,0);
        paint.setShader(new LinearGradient(0,0,w,h,
            new int[]{0xffd8edff,0xffffffff,0xffe9f6ff,0xffd9f1ff},
            null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);

        // Premium gold and royal-blue swooshes at top and footer.
        Path top=new Path();
        top.moveTo(0,0);top.lineTo(w,0);top.lineTo(w,h*.075f);
        top.cubicTo(w*.64f,h*.005f,w*.34f,h*.16f,0,h*.09f);top.close();
        color(0xff1166bf,0);canvas.drawPath(top,paint);
        Path band=new Path();
        band.moveTo(0,h*.045f);band.cubicTo(w*.35f,h*.13f,w*.65f,h*.015f,
            w,h*.09f);
        band.lineTo(w,h*.12f);
        band.cubicTo(w*.63f,h*.04f,w*.36f,h*.15f,0,h*.075f);band.close();
        color(0xfffbc942,0);canvas.drawPath(band,paint);
        Path bottom=new Path();
        bottom.moveTo(0,h*.92f);
        bottom.cubicTo(w*.35f,h*.99f,w*.7f,h*.89f,w,h*.94f);
        bottom.lineTo(w,h);bottom.lineTo(0,h);bottom.close();
        color(0xff2088db,0);canvas.drawPath(bottom,paint);
        Path bottomGold=new Path();
        bottomGold.moveTo(0,h*.975f);
        bottomGold.cubicTo(w*.32f,h*.93f,w*.75f,h*.99f,w,h*.95f);
        bottomGold.lineTo(w,h*.974f);
        bottomGold.cubicTo(w*.72f,h,w*.3f,h*.96f,0,h);bottomGold.close();
        color(0xffffc944,0);canvas.drawPath(bottomGold,paint);

        // Subtle sparkles and science formulae.
        Random rand=new Random(27044);
        for(int i=0;i<55;i++){
            float x=rand.nextFloat()*w,y=h*.18f+rand.nextFloat()*h*.65f;
            color(i%6==0?0x55ffca5b:0x3463b9fc,0);
            canvas.drawCircle(x,y,scale*(i%6==0?1.9f:.8f),paint);
        }
        color(0x663268b6,0);
        paint.setTextSize(scale*23);paint.setTypeface(
            android.graphics.Typeface.create("serif",
                android.graphics.Typeface.ITALIC));
        canvas.drawText("E = mc²",w*.63f,h*.46f,paint);
        canvas.drawText("π",w*.08f,h*.74f,paint);
        paint.setTextSize(scale*16);
        canvas.drawText("∑ n²",w*.78f,h*.72f,paint);

        // Atom on left. Use strokes and central nucleus.
        float ax=w*.21f,ay=h*.47f;
        for(int i=0;i<3;i++){
            canvas.save();canvas.rotate(i*60,ax,ay);
            color(0xff1387dc,scale*2.35f);
            canvas.drawOval(new RectF(ax-scale*47,ay-scale*19,
                ax+scale*47,ay+scale*19),paint);
            canvas.restore();
        }
        color(0xffffb53c,0);canvas.drawCircle(ax,ay,scale*9,paint);
        color(0xff21b6e1,0);canvas.drawCircle(ax+scale*29,ay-scale*17,scale*4,paint);

        // DNA helix on right, away from launcher action buttons.
        float cx=w*.83f,yStart=h*.30f,yEnd=h*.60f;
        for(int i=0;i<=36;i++){
            float t=i/36f,y=yStart+t*(yEnd-yStart);
            float offset=(float)Math.sin(t*Math.PI*4)*scale*28;
            float x1=cx+offset,x2=cx-offset;
            color(0xb91b8bd9,scale*2.1f);canvas.drawLine(x1,y,x2,y,paint);
            color(0xff0467bc,0);canvas.drawCircle(x1,y,scale*2.6f,paint);
            color(0xff2bc2e2,0);canvas.drawCircle(x2,y,scale*2.6f,paint);
        }

        // Scientific open book at lower center, with glowing golden spine.
        float bookY=h*.695f;
        Path leftPage=new Path();
        leftPage.moveTo(w*.5f,bookY+scale*58);
        leftPage.cubicTo(w*.34f,bookY+scale*28,w*.21f,bookY+scale*29,
            w*.11f,bookY+scale*51);
        leftPage.lineTo(w*.13f,bookY+scale*14);
        leftPage.cubicTo(w*.30f,bookY-scale*4,w*.44f,bookY+scale*4,
            w*.5f,bookY+scale*47);
        leftPage.close();
        color(0xfffdfdfd,0);canvas.drawPath(leftPage,paint);
        color(0xff2569b2,scale*1.5f);canvas.drawPath(leftPage,paint);
        Path rightPage=new Path();
        rightPage.moveTo(w*.5f,bookY+scale*58);
        rightPage.cubicTo(w*.66f,bookY+scale*28,w*.79f,bookY+scale*29,
            w*.89f,bookY+scale*51);
        rightPage.lineTo(w*.87f,bookY+scale*14);
        rightPage.cubicTo(w*.70f,bookY-scale*4,w*.56f,bookY+scale*4,
            w*.5f,bookY+scale*47);
        rightPage.close();
        color(0xfffdfdfd,0);canvas.drawPath(rightPage,paint);
        color(0xff2569b2,scale*1.5f);canvas.drawPath(rightPage,paint);
        color(0xffffc63f,0);
        canvas.drawCircle(w*.5f,bookY+scale*52,scale*5,paint);
        color(0xffd8a12d,scale*2.3f);
        canvas.drawLine(w*.5f,bookY+scale*10,w*.5f,bookY+scale*54,paint);

        // Blue flask on the left of the book, with small bubbles.
        float fx=w*.27f,fy=bookY-scale*35;
        Path flask=new Path();
        flask.moveTo(fx-scale*8,fy-scale*25);
        flask.lineTo(fx+scale*8,fy-scale*25);
        flask.lineTo(fx+scale*8,fy-scale*5);
        flask.lineTo(fx+scale*27,fy+scale*30);
        flask.quadTo(fx+scale*28,fy+scale*37,fx+scale*18,fy+scale*37);
        flask.lineTo(fx-scale*18,fy+scale*37);
        flask.quadTo(fx-scale*28,fy+scale*37,fx-scale*27,fy+scale*30);
        flask.lineTo(fx-scale*8,fy-scale*5);flask.close();
        color(0x8036ccf4,0);canvas.drawPath(flask,paint);
        color(0xff0078d4,scale*2f);canvas.drawPath(flask,paint);
        color(0xff16ace3,0);
        for(int i=0;i<5;i++){
            float x=fx+scale*(-14+i*6);
            canvas.drawCircle(x,fy+scale*(17+i%2*5),scale*2,paint);
        }

        // Light molecule on the right of book.
        float mx=w*.72f,my=bookY-scale*22;
        float[][] pts={{-26,-5},{0,-25},{21,-1},{2,25},{38,20}};
        int[][] links={{0,1},{1,2},{0,3},{3,4},{2,4}};
        for(int[] link:links){
            color(0xff349bd8,scale*2);
            canvas.drawLine(mx+pts[link[0]][0]*scale,my+pts[link[0]][1]*scale,
                mx+pts[link[1]][0]*scale,my+pts[link[1]][1]*scale,paint);
        }
        for(int i=0;i<pts.length;i++){
            color(i%2==0?0xffee72b5:0xff177bdc,0);
            canvas.drawCircle(mx+pts[i][0]*scale,my+pts[i][1]*scale,
                scale*5.2f,paint);
        }
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        if(theme==4){drawPearlLight(canvas,w,h);return;}
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
