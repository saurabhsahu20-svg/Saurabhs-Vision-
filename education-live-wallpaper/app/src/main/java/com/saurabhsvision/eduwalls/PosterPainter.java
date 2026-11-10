package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Eight original animated educational poster artworks.
 * Draws directly on Canvas, offline, without third-party image licensing concerns.
 */
public final class PosterPainter {
    public static final String[] NAMES={
        "Magical Maths","Rainbow Numbers","Green Chalkboard","Neon Science",
        "Success Steps","Storybook Dreams","Vintage Geometry","Space School"
    };
    public static final String[] DETAILS={
        "A golden book and floating equations","Bright, cheerful colourful arithmetic",
        "Blackboard geometry and school tools","Atoms, DNA and glowing science",
        "Study • practice • achieve","Magical books and learning stars",
        "Classic notebook and mathematical diagrams","Rockets, planets and knowledge"
    };
    private static final int[] TOP={
        0xff05133d,0xff059ec7,0xff102b24,0xff140b3c,
        0xff0a305d,0xff28174e,0xffd3b287,0xff05133b
    };
    private static final int[] BOTTOM={
        0xff42278a,0xff7d2caa,0xff255847,0xff254bb7,
        0xffe78b40,0xffbd4a8d,0xffebd6a8,0xff093a91
    };
    private static final int[] GLOW={
        0xffffd455,0xffffeb83,0xffc6f9c6,0xff00ecff,
        0xffffef8b,0xffffd7fc,0xff79563f,0xfffbd16e
    };
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private final RectF bounds=new RectF();

    private void fill(int color,int alpha){
        paint.reset();paint.setAntiAlias(true);paint.setColor(color);paint.setAlpha(Math.max(0,Math.min(255,alpha)));
    }
    private void circle(Canvas c,float x,float y,float r,int color,int alpha){
        fill(color,alpha);c.drawCircle(x,y,r,paint);
    }
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width,int alpha){
        fill(color,alpha);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,x2,y2,paint);
    }
    private void box(Canvas c,float l,float t,float r,float b,float radius,int color,int alpha){
        fill(color,alpha);bounds.set(l,t,r,b);c.drawRoundRect(bounds,radius,radius,paint);
    }
    private void txt(Canvas c,String text,float x,float y,float size,int color,boolean bold){
        fill(color,255);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        paint.setTextSize(size);c.drawText(text,x,y,paint);
    }
    private float n(float s){float f=(float)Math.sin(s*127.1f+1.57f)*43758.547f;return f-(float)Math.floor(f);}

    public void draw(Canvas original,int width,int height,float t,int index,float parallax){
        if(width<=0||height<=0)return;
        int id=Math.max(0,Math.min(7,index)),hi=GLOW[id];
        original.save();
        original.scale(width/360f,height/640f);
        Canvas c=original;
        fill(Color.WHITE,255);
        paint.setShader(new LinearGradient(0,0,360,640,TOP[id],BOTTOM[id],Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,paint);paint.setShader(null);
        // Transparent decorative rays provide depth while avoiding expensive bitmaps.
        for(int j=0;j<3;j++){
            float x=60+j*115+(float)Math.sin(t*.14f+j)*18;
            float y=140+j*124;
            float r=125+j*23;
            fill(Color.WHITE,37);
            paint.setShader(new RadialGradient(x,y,r,hi & 0x55ffffff,0x00000000,Shader.TileMode.CLAMP));
            c.drawCircle(x,y,r,paint);paint.setShader(null);
        }
        // The center is offset subtly as the launcher moves between home screens.
        c.save();c.translate(parallax*8f,0f);
        if(id==0)magicalMath(c,t,hi);
        else if(id==1)rainbow(c,t,hi);
        else if(id==2)chalkboard(c,t,hi);
        else if(id==3)science(c,t,hi);
        else if(id==4)success(c,t,hi);
        else if(id==5)storybook(c,t,hi);
        else if(id==6)vintage(c,t,hi);
        else space(c,t,hi);
        c.restore();
        dust(c,t,id,hi);
        original.restore();
    }
    private void dust(Canvas c,float t,int seed,int hi){
        int count=seed==6?22:39;
        for(int i=0;i<count;i++){
            float x=n(i+seed*29)*354+3;
            float y=((n(i+seed*37+110)*680-(t*(4+n(i+99)*5)))%680+680)%680-20;
            float r=.7f+2*n(i+seed+400);
            int a=45+(int)(110*(.55+.45*Math.sin(t*.7+i*1.12)));
            circle(c,x,y,r,i%5==0?hi:Color.WHITE,a);
            if(i%9==0) {
                line(c,x-r*2,y,x+r*2,y,hi,.8f,110);
                line(c,x,y-r*2,x,y+r*2,hi,.8f,110);
            }
        }
    }
    private void page(Canvas c,float x,float y,float w,float h,int color){
        shape.reset();shape.moveTo(x,y);
        shape.cubicTo(x+w*.34f,y-h*.22f,x+w*.68f,y-h*.15f,x+w,y);
        shape.lineTo(x+w,y+h);
        shape.cubicTo(x+w*.7f,y+h*.86f,x+w*.3f,y+h*.85f,x,y+h*.97f);
        shape.close();fill(color,255);c.drawPath(shape,paint);
    }
    private void book(Canvas c,float x,float y,float scale,float t){
        c.save();c.translate(x,y);c.scale(scale,scale);
        float bob=(float)Math.sin(t*.9f)*3.5f;
        box(c,-130,39+bob,132,51+bob,8,0xff23387a,205);
        page(c,-3,bob,128,59,0xffffe5a4);
        c.save();c.scale(-1,1);page(c,3,bob,128,59,0xffffffff);c.restore();
        line(c,0,bob,0,52+bob,0xffffa417,3,245);
        for(int k=0;k<4;k++){
            float yy=13+k*10+bob;
            line(c,20,yy,111,yy+3,0xffc59043,1,95);
            line(c,-21,yy,-110,yy+3,0xff6a8cc9,1,115);
        }
        circle(c,0,-16+bob,22,0xffffa330,35);
        c.restore();
    }
    private void atom(Canvas c,float x,float y,float r,float t,int hi){
        for(int j=0;j<3;j++){
            c.save();c.rotate(60*j+t*7,x,y);
            fill(j==0?hi:0xff72deff,225);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.8f);
            bounds.set(x-r,y-r*.4f,x+r,y+r*.4f);c.drawOval(bounds,paint);paint.setStyle(Paint.Style.FILL);
            double a=t*.45+j*2.1;
            circle(c,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r*.4f,5,j==0?hi:0xff7cf9ff,240);
            c.restore();
        }
        circle(c,x,y,15,0xffffe369,245);circle(c,x-4,y-5,5,Color.WHITE,165);
    }
    private void star(Canvas c,float x,float y,float r,int color){
        shape.reset();
        for(int i=0;i<10;i++){
            float a=(float)(-Math.PI/2+Math.PI*i/5);
            float rr=i%2==0?r:r*.45f;
            float px=x+(float)Math.cos(a)*rr,py=y+(float)Math.sin(a)*rr;
            if(i==0)shape.moveTo(px,py);else shape.lineTo(px,py);
        }
        shape.close();fill(color,235);c.drawPath(shape,paint);
    }
    private void triangle(Canvas c,float x,float y,float r,int color){
        shape.reset();shape.moveTo(x,y-r);shape.lineTo(x-r*.86f,y+r*.58f);shape.lineTo(x+r*.86f,y+r*.58f);shape.close();
        fill(color,205);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);c.drawPath(shape,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void pencil(Canvas c,float x,float y,float angle,int color){
        c.save();c.rotate(angle,x,y);
        box(c,x-8,y-53,x+8,y+52,4,color,255);
        box(c,x-8,y-53,x+8,y-43,2,0xffffd670,235);
        shape.reset();shape.moveTo(x-8,y+52);shape.lineTo(x+8,y+52);shape.lineTo(x,y+69);shape.close();
        fill(0xffffdba2,255);c.drawPath(shape,paint);
        c.restore();
    }
    private void heading(Canvas c,String first,String second,int hi){
        txt(c,first,180,104,26,Color.WHITE,true);
        txt(c,second,180,136,17,hi,true);
        line(c,86,149,274,149,hi,2.5f,170);
    }
    private void magicalMath(Canvas c,float t,int hi){
        heading(c,"MAGICAL WORLD OF","MATHEMATICS",hi);
        float wave=(float)Math.sin(t*.65f);
        for(int j=0;j<7;j++){
            float a=j*6.28f/7+t*.12f;
            float x=180+(float)Math.cos(a)*125f,y=337+(float)Math.sin(a)*91f;
            txt(c,new String[]{"π","1","2","3","∞","√","Σ"}[j],x,y,26+(j%2)*8,
                j%2==0?0xff74eaff:0xffffd455,true);
        }
        line(c,95,270,265,270,0xffbce8ff,2,100);
        txt(c,"a²+b²=c²",180,247,23,Color.WHITE,true);
        triangle(c,179,327,50,0xff8ff6ff);
        star(c,180,320,16,hi);
        book(c,180,488,1f,t);
        txt(c,"Numbers meet imagination",180,590,17,Color.WHITE,true);
    }
    private void rainbow(Canvas c,float t,int hi){
        box(c,25,67,335,170,25,0xff05387a,220);
        txt(c,"MATH IS FUN!",180,112,39,0xfffff07c,true);
        txt(c,"LEARN  •  PLAY  •  GROW",180,143,15,Color.WHITE,true);
        String[] n={"1","2","3","4","5","6","7","8","9"};
        int[] colors={0xffffd04e,0xffff7c87,0xff6ae5ff,0xff70ffa4,0xffffa251,0xffdb94ff,0xffffff77,0xff9fd2ff,0xffff5ccf};
        for(int j=0;j<9;j++){
            float x=72+(j%3)*108,y=235+(j/3)*90+(float)Math.sin(t*.9f+j)*7;
            circle(c,x,y,33,colors[j],225);
            circle(c,x-10,y-10,7,Color.WHITE,105);
            txt(c,n[j],x,y+12,36,0xff102f55,true);
        }
        box(c,22,512,338,594,22,0xfffff4df,238);
        txt(c,"2 + 3 = 5",180,557,36,0xff2b5f9d,true);
        pencil(c,39,530,25,0xffe85e69);
        pencil(c,319,537,-24,0xff34b2ea);
    }
    private void chalkboard(Canvas c,float t,int hi){
        box(c,14,49,346,603,8,0xff985d37,255);
        box(c,22,57,338,593,4,0xff163f37,255);
        for(int j=0;j<16;j++)line(c,30+j*18,80,30+j*18,568,Color.WHITE,.45f,10);
        txt(c,"MATHEMATICS",180,126,33,0xffe3fbd8,true);
        txt(c,"The world of shapes",180,157,17,0xffffe7aa,false);
        txt(c,"a² + b² = c²",180,224,26,Color.WHITE,true);
        triangle(c,125,316,63,0xffd7fdec);
        txt(c,"a",61,347,18,0xfffdf2ad,true);
        txt(c,"b",175,357,18,0xfffdf2ad,true);
        txt(c,"π  ≈  3.14159",180,426,29,0xffd8f8fc,true);
        for(int j=0;j<3;j++){
            float x=70+j*101;
            float y=490+(float)Math.sin(t*.5f+j)*3;
            circle(c,x,y,25,j==0?0xffffdc75:j==1?0xffb0f6f1:0xffff9e8a,170);
            txt(c,new String[]{"√","∑","∞"}[j],x,y+8,26,0xff16413c,true);
        }
        box(c,80,581,280,598,4,0xffa77755,245);
    }
    private void science(Canvas c,float t,int hi){
        heading(c,"SCIENCE IS","EVERYWHERE",hi);
        atom(c,180,328,91,t,0xff6cf9ff);
        txt(c,"H₂O",68,274,28,0xff85e9ff,true);
        txt(c,"CO₂",294,267,26,0xffffa9ec,true);
        txt(c,"DNA",277,429,25,0xffffe9a4,true);
        txt(c,"E = mc²",91,460,23,Color.WHITE,true);
        float px=62,py=381;
        line(c,px,py,px+33,py-26,0xff80e8ff,3,230);
        line(c,px+33,py-26,px+66,py,0xff80e8ff,3,230);
        circle(c,px,py,10,0xffffaeda,220);
        circle(c,px+33,py-26,10,0xff63f5f2,220);
        circle(c,px+66,py,10,0xffffdb76,220);
        book(c,180,522,.83f,t);
        txt(c,"EXPLORE • EXPERIMENT",180,600,17,Color.WHITE,true);
    }
    private void success(Canvas c,float t,int hi){
        txt(c,"SMALL STEPS",180,112,33,Color.WHITE,true);
        txt(c,"BIG DREAMS",180,151,39,0xffffef89,true);
        star(c,180,209,35,0xffffe57c);
        txt(c,"YOU CAN DO IT!",180,268,22,Color.WHITE,true);
        for(int j=0;j<5;j++){
            float y=520-j*51, x=35+j*18;
            box(c,x,y,x+250,y+43,9,0xff234376,190);
            box(c,x+7,y+6,x+243,y+37,7,j%2==0?0xffffd78f:0xffe7fff7,210);
            txt(c,new String[]{"START","LEARN","PRACTICE","FOCUS","SUCCEED"}[j],x+125,y+29,16,0xff1b3159,true);
        }
        circle(c,263,337+(float)Math.sin(t*.8f)*5,28,0xffffe889,235);
        txt(c,"★",263,348,29,0xffbd6b15,true);
    }
    private void storybook(Canvas c,float t,int hi){
        heading(c,"OPEN A BOOK","EXPLORE A WORLD",hi);
        float bob=(float)Math.sin(t*.78f)*6;
        for(int j=0;j<11;j++){
            float a=(float)(j*6.28f/11+t*.06f);
            float x=180+(float)Math.cos(a)*128f,y=320+(float)Math.sin(a)*130f+bob;
            txt(c,new String[]{"A","B","C","π","1","2","★","∞","3","4","✓"}[j],x,y,25,j%2==0?0xffffd982:0xffa7ecff,true);
        }
        circle(c,180,300+bob,89,0xff84a7ff,35);
        star(c,180,287+bob,42,0xffffdb75);
        txt(c,"IMAGINE",180,372+bob,26,Color.WHITE,true);
        book(c,180,511,1.1f,t);
        txt(c,"Every page holds a surprise",180,605,14,Color.WHITE,false);
    }
    private void vintage(Canvas c,float t,int hi){
        box(c,18,48,342,598,13,0xffefe2c8,221);
        for(int i=0;i<19;i++){
            float y=84+i*26;
            line(c,28,y,331,y,0xff8b9983,.7f,45);
        }
        txt(c,"MATHEMATICS",180,111,30,0xff604b36,true);
        txt(c,"THINK • SOLVE • IMPROVE",180,143,13,0xff66523f,true);
        triangle(c,180,267,84,0xff765741);
        txt(c,"a² + b² = c²",180,205,23,0xff6b493d,true);
        txt(c,"π",89,408,65,0xffa55b30,true);
        txt(c,"√",255,399,60,0xff60756e,true);
        book(c,180,493,.88f,t);
        txt(c,"Keep your curiosity alive",180,581,16,0xff5e4731,true);
    }
    private void space(Canvas c,float t,int hi){
        heading(c,"SPACE SCHOOL","LEARN BEYOND STARS",hi);
        circle(c,173,327,100,0xff4192ff,195);
        circle(c,147,305,51,0xff81c4ff,102);
        c.save();c.rotate(-23,180,326);
        fill(0xffffd377,231);paint.setStrokeWidth(9);paint.setStyle(Paint.Style.STROKE);
        bounds.set(41,295,311,362);c.drawOval(bounds,paint);paint.setStyle(Paint.Style.FILL);c.restore();
        circle(c,244,360,13,0xffe4fffe,220);
        float x=85+(float)Math.sin(t*.3f)*8,y=187-(float)Math.cos(t*.35f)*8;
        shape.reset();shape.moveTo(x,y-35);shape.lineTo(x+21,y+7);shape.lineTo(x-21,y+7);shape.close();
        fill(Color.WHITE,247);c.drawPath(shape,paint);
        circle(c,x,y-9,7,0xff67cfff,242);
        star(c,275,234,18,0xffffef6f);
        txt(c,"π",66,399,33,0xffcffdff,true);
        txt(c,"E = mc²",257,444,22,Color.WHITE,true);
        book(c,180,519,.92f,t);
        txt(c,"DREAM • DISCOVER • DO",180,598,18,Color.WHITE,true);
    }
}
