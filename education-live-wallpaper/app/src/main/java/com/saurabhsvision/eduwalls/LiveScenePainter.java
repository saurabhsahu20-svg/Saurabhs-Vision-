package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * V6 separate LIVE scene engine: these 50 animations do NOT reuse the v5 static
 * posters. Five educational worlds have ten separately themed motion compositions.
 * Entirely offline procedural artwork, capped at a fixed number of drawing objects.
 */
public final class LiveScenePainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private final RectF oval=new RectF();
    private final int[] DARK={0xff060e2d,0xff061d2c,0xff070a33,0xff261042,0xff20122b};
    private final int[] LIGHT={0xff0b48a7,0xff096f78,0xff452c91,0xff9a3c9c,0xff6a3395};
    private final String[][] SPARKS={
        {"π","∑","∞","x²","√","Δ","a+b","sin"},
        {"H₂O","DNA","O₂","Fe","NaCl","ATP","λ","E=mc²"},
        {"★","☾","✦","π","⋆","∞","☄","R²"},
        {"ABC","123","★","✦","READ","A+","BOOK","∞"},
        {"FOCUS","GO!","LEARN","GROW","A+","WIN","PLAN","TRY"}
    };

    private static float noise(float x){
        double k=Math.sin(x*92.833+17.23)*40713.14;
        return (float)(k-Math.floor(k));
    }
    private void ink(int rgb,int alpha){
        p.reset();p.setAntiAlias(true);
        p.setColor(rgb);p.setAlpha(Math.max(0,Math.min(255,alpha)));
    }
    private void circle(Canvas c,float x,float y,float r,int rgb,int alpha){
        ink(rgb,alpha);c.drawCircle(x,y,Math.max(.1f,r),p);
    }
    private void stroke(Canvas c,float x,float y,float xx,float yy,int rgb,float width,int a){
        ink(rgb,a);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x,y,xx,yy,p);
    }
    private void text(Canvas c,String s,float x,float y,float sz,int rgb,boolean bold,int alpha){
        ink(rgb,alpha);p.setTextSize(sz);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
    private void frame(Canvas c,float l,float top,float r,float bottom,int fill,int radius){
        ink(fill,215);oval.set(l,top,r,bottom);c.drawRoundRect(oval,radius,radius,p);
    }
    public void draw(Canvas c,int w,int h,float seconds,int requested,float tx,float ty,long tapped) {
        if(w<=0 || h<=0)return;
        final int id=LiveSceneCatalog.norm(requested),group=id/10,variation=id%10;
        final int hi=LiveSceneCatalog.color(id),second=(hi & 0xff00ff00) | 0x0040a5ff;
        final float t=seconds*.85f;
        c.save();
        c.scale(w/360f,h/640f);

        ink(Color.WHITE,255);
        p.setShader(new LinearGradient(0,0,360,640,new int[]{DARK[group],LIGHT[group],DARK[group]},
            new float[]{0f,.55f,1f},Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,p);p.setShader(null);
        // Each scene has a different atmospheric light arrangement.
        for(int j=0;j<4;j++){
            float x=50+noise(id*9+j)*265+(float)Math.sin(t*.24f+j)*20;
            float y=110+noise(id*8+j+57)*350+(float)Math.cos(t*.20f+j)*16;
            float radius=62+noise(j+id)*81;
            ink(hi,42);
            p.setShader(new RadialGradient(x,y,radius,
                new int[]{(hi&0x00ffffff)|0x55000000,(hi&0x00ffffff)|0x00000000},
                null,Shader.TileMode.CLAMP));
            c.drawCircle(x,y,radius,p);p.setShader(null);
        }
        // Star cloud, gentle parallax and motion with deterministic placement.
        for(int j=0;j<54;j++){
            float x=noise(id*32+j*1.3f)*360;
            float y=noise(id*23+j*5.1f)*640;
            float flicker=.63f+.37f*(float)Math.sin(t*(.34f+variation*.055f)+j*.41f);
            circle(c,x,y,.45f+noise(j+id)*1.8f,j%4==0?hi:Color.WHITE,
                (int)((44+j%5*17)*flicker));
        }

        // Vertical title and large central subject are different from still wallpapers.
        frame(c,22,35,338,83,0xff081a40,18);
        text(c,LiveSceneCatalog.group(id),180,53,11,hi,true,239);
        text(c,LiveSceneCatalog.name(id),180,73,19,Color.WHITE,true,247);
        drawWorld(c,t,group,variation,hi,second);
        drawMovingOverlay(c,t,id,hi,second);
        frame(c,32,551,328,604,0xff07122e,18);
        text(c,"SAURABH'S VISION",180,576,17,Color.WHITE,true,250);
        text(c,"LEARN TODAY  •  LEAD TOMORROW",180,592,10,hi,true,245);

        if(tapped>0)drawTouch(c,tx*360f/w,ty*640f/h,tapped,hi,variation);
        c.restore();
    }
    private void drawWorld(Canvas c,float t,int group,int v,int hi,int sec){
        final float cx=180,cy=320;
        final float sway=(float)Math.sin(t*.75f+v)*9f;
        if(group==0){ // MATHEMATICS: ten animated educational concepts.
            switch(v){
                case 0: case 5: {
                    text(c,"π",cx,cy+45,124,hi,true,250);
                    ring(c,cx,cy,128,52,t,sec);
                    text(c,v==0?"3.14159…":"CIRCUMFERENCE",cx,cy+112,17,Color.WHITE,true,225);
                    break;
                }
                case 1: case 6: {
                    for(int j=0;j<12;j++){
                        float theta=(float)(j*Math.PI/6+t*.25f);
                        float r=22+j*8.5f;
                        text(c,""+(j+1),(float)(cx+Math.cos(theta)*r),(float)(cy+Math.sin(theta)*r),22+(j%3)*3,j%2==0?hi:Color.WHITE,true,224);
                    }
                    break;
                }
                case 2: case 7: {
                    polygon(c,cx,cy,111,3+(v%3),t*.12f,hi);
                    polygon(c,cx,cy,71,6,t*.19f,sec);
                    text(c,v==2?"GEOMETRY":"POLYGONS",cx,cy+154,21,hi,true,220);
                    break;
                }
                case 3: case 8: {
                    axes(c,58,230,304,390,sec);
                    for(int j=1;j<=85;j++){
                        float x=63+j*2.72f;
                        float y=310-(float)Math.sin((j*.13f)+(t*.64f))*61f;
                        float xx=x-2.72f,yy=310-(float)Math.sin(((j-1)*.13f)+(t*.64f))*61f;
                        stroke(c,xx,yy,x,y,hi,2.3f,238);
                    }
                    text(c,v==3?"sin x":"f(x) = A sin(x)",180,445,19,Color.WHITE,true,240);
                    break;
                }
                default:{
                    book(c,cx,cy+15,hi,t);
                    text(c,"x² + y² = z²",cx,cy-102+sway,24,Color.WHITE,true,220);
                    break;
                }
            }
        } else if(group==1){ // SCIENCE: atomic, molecular, DNA, lab, and physics scenes.
            if(v==0||v==5||v==9){
                atom(c,cx,cy,hi,sec,t,v%2==0?4:3);
                text(c,v==5?"MOTION  •  ENERGY":"ATOMIC WORLD",cx,475,18,Color.WHITE,true,220);
            }else if(v==1||v==6){
                for(int j=0;j<28;j++){
                    float y=188+j*9.4f;
                    float a=j*.43f+t*.73f;
                    float x1=180+(float)Math.sin(a)*70, x2=180-(float)Math.sin(a)*70;
                    if(j%2==0)stroke(c,x1,y,x2,y,hi,2,160);
                    circle(c,x1,y,5,hi,230);circle(c,x2,y,5,sec,230);
                }
                text(c,"LIVING DNA",180,493,18,Color.WHITE,true,205);
            }else if(v==2||v==7){
                for(int j=0;j<4;j++){
                    float x=88+j*61;
                    stroke(c,x,262,x,362,sec,3,202);
                    frame(c,x-17,330,x+17,388,hi,12);
                    circle(c,x,334+(float)Math.sin(t+j)*9,6,Color.WHITE,220);
                }
                text(c,"SCIENCE LAB",180,460,22,Color.WHITE,true,230);
            }else{
                for(int j=0;j<6;j++){
                    float angle=(float)(j*Math.PI/3+t*.22f);
                    float x=cx+(float)Math.cos(angle)*90,y=cy+(float)Math.sin(angle)*74;
                    circle(c,x,y,18,j%2==0?sec:hi,203);
                    stroke(c,cx,cy,x,y,Color.WHITE,1.9f,100);
                }
                circle(c,cx,cy,34,hi,230);
                text(c,v==3?"H₂O":v==4?"F = ma":"NaCl",cx,cy+9,24,Color.WHITE,true,245);
            }
        }else if(group==2){ // COSMOS: orbiting worlds, moving comets, rockets.
            planet(c,cx,cy,94,hi,sec,t);
            if(v%3==0) {
                for(int j=0;j<3;j++){
                    float a=t*.22f+j*2.1f;
                    circle(c,cx+(float)Math.cos(a)*143,cy+(float)Math.sin(a)*91,12+j*3,sec,230);
                }
            }else if(v%3==1){
                float x=75+(t*15)%220,y=170+(float)Math.sin(t)*45;
                rocket(c,x,y,hi);
            }else {
                for(int j=0;j<14;j++){
                    float a=(float)(j*2*Math.PI/14+t*.31f);
                    float x=cx+(float)Math.cos(a)*139,y=cy+(float)Math.sin(a)*158;
                    circle(c,x,y,2.1f,hi,225);
                }
            }
            text(c,v==8?"SOLAR SYSTEM":v==6?"GALAXY QUEST":"EXPLORE THE STARS",180,501,17,Color.WHITE,true,214);
        }else if(group==3){ // MAGICAL LEARNING: flying pages, glowing books and letters.
            book(c,cx,cy+52,hi,t);
            String[] symbols={"ABC","123","∞","★","READ","LEARN","A+","✦"};
            for(int j=0;j<14;j++){
                float x=58+noise(v*21+j)*245;
                float y=210+noise(v*7+j+29)*185+(float)Math.sin(t*.70f+j)*10;
                text(c,symbols[(j+v)%symbols.length],x,y,15+(j%4)*4,j%2==0?hi:Color.WHITE,true,190);
            }
            if(v%2==0)for(int j=0;j<7;j++){
                float a=t*.31f+j*.89f;
                circle(c,cx+(float)Math.cos(a)*113,cy+(float)Math.sin(a)*86,2.8f,sec,200);
            }
            text(c,v<5?"KNOWLEDGE IS MAGIC":"IMAGINE • DISCOVER",cx,486,18,Color.WHITE,true,232);
        }else{ // MOTIVATION: progress ladder, goal halos, lightbulbs.
            if(v%3==0){
                for(int j=0;j<5;j++){
                    int height=30+j*22;
                    frame(c,64+j*43,410-height,104+j*43,412,hi,8);
                    text(c,""+(j+1),84+j*43,399-height,16,Color.WHITE,true,222);
                }
                text(c,"STEP BY STEP",180,470,22,Color.WHITE,true,235);
            }else if(v%3==1){
                for(int j=0;j<5;j++)ring(c,cx,cy,50+j*20,35+j*11,t*.12f+j,hi);
                text(c,"FOCUS",cx,cy+13,36,Color.WHITE,true,250);
            }else{
                circle(c,cx,cy-33,66,hi,220);
                frame(c,cx-21,cy+30,cx+21,cy+92,sec,10);
                for(int j=0;j<12;j++){
                    float a=(float)(Math.PI*2*j/12+t*.06f);
                    stroke(c,cx+(float)Math.cos(a)*78,cy-33+(float)Math.sin(a)*78,
                        cx+(float)Math.cos(a)*101,cy-33+(float)Math.sin(a)*101,hi,3,205);
                }
                text(c,"BRIGHT IDEAS",cx,483,22,Color.WHITE,true,242);
            }
        }
    }
    private void drawMovingOverlay(Canvas c,float t,int id,int hi,int sec){
        int v=id%10,g=id/10;
        switch((v+g)%5){
            case 0:{
                for(int j=0;j<13;j++){
                    float y=110+(t*16+j*39)%440;
                    circle(c,42+noise(id*5+j)*278,y,1.6f+j%3*.7f,hi,170);
                }break;
            }
            case 1:{
                for(int j=0;j<11;j++){
                    float a=t*.50f+j*.55f;
                    float r=86+j*3;
                    circle(c,180+(float)Math.cos(a)*r,325+(float)Math.sin(a)*r,2.5f,sec,175);
                }break;
            }
            case 2:{
                for(int j=0;j<40;j++){
                    float x=24+j*8,y=500+(float)Math.sin(j*.22f+t*.77f)*10;
                    circle(c,x,y,1.6f,hi,120);
                }break;
            }
            case 3:{
                float x=25+(t*30)%320,y=168+(float)Math.sin(t*.84f)*20;
                for(int j=0;j<12;j++)circle(c,x-j*6,y+j*2,1.7f,hi,205-j*15);
                break;
            }
            default:{
                for(int j=0;j<9;j++){
                    float phase=(t*.28f+j/9f)%1f;
                    circle(c,55+noise(id+j)*250,550-phase*400,2.5f,hi,(int)(90+phase*110));
                }
            }
        }
    }
    private void drawTouch(Canvas c,float px,float py,long at,int hi,int v){
        float elapsed=(android.os.SystemClock.uptimeMillis()-at)/1000f;
        if(elapsed<0||elapsed>1.55f)return;
        float fade=1-elapsed/1.55f;
        ink(hi,(int)(185*fade));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        c.drawCircle(px,py,23+elapsed*90,p);p.setStyle(Paint.Style.FILL);
        for(int j=0;j<16;j++){
            float a=(float)(j*Math.PI/8+v*.35f);
            float r=12+elapsed*(85+j%3*12);
            circle(c,px+(float)Math.cos(a)*r,py+(float)Math.sin(a)*r,2.4f,hi,(int)(220*fade));
        }
    }
    private void axes(Canvas c,float l,float top,float r,float b,int color){
        stroke(c,l,(top+b)/2,r,(top+b)/2,color,1.8f,164);
        stroke(c,(l+r)/2,top,(l+r)/2,b,color,1.8f,164);
    }
    private void polygon(Canvas c,float x,float y,float radius,int sides,float spin,int color){
        shape.reset();
        for(int i=0;i<sides;i++){
            float a=(float)(Math.PI*2*i/sides+spin-Math.PI*.5);
            float px=x+(float)Math.cos(a)*radius,py=y+(float)Math.sin(a)*radius;
            if(i==0)shape.moveTo(px,py);else shape.lineTo(px,py);
        }shape.close();
        ink(color,211);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
    }
    private void atom(Canvas c,float cx,float cy,int hi,int sec,float t,int orbits){
        for(int j=0;j<orbits;j++){
            c.save();c.rotate(j*180f/orbits,cx,cy);
            ink(j%2==0?hi:sec,208);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.7f);
            oval.set(cx-110,cy-38,cx+110,cy+38);
            c.drawOval(oval,p);p.setStyle(Paint.Style.FILL);
            float a=t*.67f+j*.9f;
            circle(c,cx+(float)Math.cos(a)*110,cy+(float)Math.sin(a)*38,7,sec,242);
            c.restore();
        }
        circle(c,cx,cy,26,hi,236);
        circle(c,cx-9,cy-5,8,Color.WHITE,190);
    }
    private void ring(Canvas c,float cx,float cy,float rx,float ry,float t,int color){
        c.save();c.rotate(t*7,cx,cy);
        ink(color,169);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);
        oval.set(cx-rx,cy-ry,cx+rx,cy+ry);
        c.drawOval(oval,p);p.setStyle(Paint.Style.FILL);
        c.restore();
    }
    private void planet(Canvas c,float cx,float cy,float rad,int hi,int sec,float t){
        ink(hi,255);
        p.setShader(new RadialGradient(cx-33,cy-40,rad*1.9f,
            new int[]{0xffffffff,hi,0xff132676},null,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,rad,p);p.setShader(null);
        c.save();c.rotate(-21+(float)Math.sin(t*.25f)*5,cx,cy);
        ink(sec,222);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);
        oval.set(cx-rad*1.55f,cy-rad*.39f,cx+rad*1.55f,cy+rad*.39f);
        c.drawOval(oval,p);p.setStyle(Paint.Style.FILL);c.restore();
        circle(c,cx+rad*.45f,cy-rad*.40f,10,Color.WHITE,126);
    }
    private void rocket(Canvas c,float x,float y,int hi){
        c.save();c.translate(x,y);c.rotate(30);
        shape.reset();shape.moveTo(0,-34);shape.lineTo(-13,-10);shape.lineTo(-13,23);
        shape.lineTo(13,23);shape.lineTo(13,-10);shape.close();
        ink(Color.WHITE,245);c.drawPath(shape,p);
        shape.reset();shape.moveTo(0,-34);shape.lineTo(-13,-10);shape.lineTo(13,-10);shape.close();
        ink(hi,240);c.drawPath(shape,p);
        circle(c,0,-5,6,0xff187acb,255);
        polygon(c,-16,18,13,3,0,hi);
        polygon(c,16,18,13,3,0,hi);
        ink(0xffffd44e,218);
        shape.reset();shape.moveTo(-8,23);shape.lineTo(0,53);shape.lineTo(8,23);shape.close();
        c.drawPath(shape,p);c.restore();
    }
    private void book(Canvas c,float x,float y,int hi,float t){
        float bob=(float)Math.sin(t*.70f)*7;
        c.save();c.translate(x,y+bob);
        shape.reset();shape.moveTo(0,-4);
        shape.cubicTo(-40,-25,-85,-20,-120,-13);
        shape.lineTo(-120,52);shape.cubicTo(-62,34,-30,36,0,58);shape.close();
        ink(Color.WHITE,252);c.drawPath(shape,p);
        shape.reset();shape.moveTo(0,-4);
        shape.cubicTo(40,-25,85,-20,120,-13);
        shape.lineTo(120,52);shape.cubicTo(62,34,30,36,0,58);shape.close();
        ink(0xffc9e8ff,248);c.drawPath(shape,p);
        stroke(c,0,-4,0,58,hi,3,241);
        for(int j=0;j<3;j++){
            stroke(c,-98,5+j*13,-22,12+j*13,0xff256cc1,1,160);
            stroke(c,22,12+j*13,98,5+j*13,0xff256cc1,1,160);
        }
        for(int j=0;j<9;j++){
            float theta=(float)(j*Math.PI/4.5+t*.20f);
            float r=63+(j%3)*16;
            circle(c,(float)Math.cos(theta)*r,(float)Math.sin(theta)*55-76,3,hi,190);
        }
        c.restore();
    }
}
