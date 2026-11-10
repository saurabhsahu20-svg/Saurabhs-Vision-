package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Separate collection of FIFTY STATIC poster compositions, not animation captures. */
public final class StillScenePainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final RectF rect=new RectF();
    private static final int[][] COLORS={
        {0xff06245b,0xff2975de,0xffffd55b},
        {0xff0b3141,0xff207b8e,0xff84ffdb},
        {0xff160942,0xff5047b4,0xffffc67f},
        {0xff56255d,0xffcd54aa,0xffffe0ae},
        {0xff24314b,0xff537aca,0xffffe194}
    };
    private static final String[][] MOTIFS={
        {"π","x²","1 2 3","△","Σ","∫","√","sin","∞","%" },
        {"⚛","DNA","H₂O","Fe","O₂","NaCl","pH","F = ma","ATP","λ"},
        {"☀","★","☾","⋆","∞","☄","SATURN","EARTH","GALAXY","ASTRO"},
        {"ABC","BOOKS","READ","STORY","A+","IDEAS","123","LEARN","✦","IMAGINE"},
        {"FOCUS","DREAM","PRACTICE","GOAL","GROW","PLAN","TRY","LEAD","SUCCESS","CREATE"}
    };
    private static final String[][] SUBS={
        {"Numbers become possibilities","Solve it step by step","Learn the language of numbers",
         "Shapes tell a story","Think with patterns","Discover the unknown",
         "Every problem has a path","Functions draw a picture","Explore the infinite","Find the fraction"},
        {"Explore the atom","The code of life","Science is everywhere","Elements around us",
         "Discover oxygen","Molecules make matter","Test and observe","Motion has meaning",
         "Energy powers life","Waves teach us"},
        {"Discover the Sun","Explore distant stars","Moonlight learning","Dream beyond limits",
         "The cosmos awaits","Travel through knowledge","Planets in motion","Our blue planet",
         "Welcome to the galaxy","Explore space"},
        {"Every letter opens a door","Books are portals","Reading is adventure","Create your story",
         "Your next bright idea","Learn something new","Count and create","Turn the page",
         "A little magic in learning","Ideas have no limits"},
        {"Stay curious","Big dreams start small","One question at a time","Set goals with care",
         "Progress grows slowly","Plan, learn, reflect","Try again and grow","Learn today, lead tomorrow",
         "Celebrate your progress","Create something wonderful"}
    };
    private static float rand(int i){double x=Math.sin(i*31.91+5.13)*45821.379;return (float)(x-Math.floor(x));}
    private void style(int rgb,int alpha) {p.reset();p.setAntiAlias(true);p.setColor(rgb);p.setAlpha(alpha);}
    private void txt(Canvas c,String s,float x,float y,float size,int rgb,boolean bold,int alpha){
        style(rgb,alpha);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextSize(size);c.drawText(s,x,y,p);
    }
    private void line(Canvas c,float x,float y,float xx,float yy,int rgb,float wd,int alpha){
        style(rgb,alpha);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(wd);c.drawLine(x,y,xx,yy,p);
    }
    private void dot(Canvas c,float x,float y,float radius,int col,int alpha){
        style(col,alpha);c.drawCircle(x,y,radius,p);
    }
    private void box(Canvas c,float l,float t,float r,float b,float round,int col,int alpha){
        style(col,alpha);rect.set(l,t,r,b);c.drawRoundRect(rect,round,round,p);
    }
    private void polygon(Canvas c,float x,float y,float r,int sides,float rotate,int rgb,int alpha){
        path.reset();
        for(int j=0;j<sides;j++){
            float a=(float)(Math.PI*2*j/sides+rotate-Math.PI*.5);
            float xx=x+(float)Math.cos(a)*r, yy=y+(float)Math.sin(a)*r;
            if(j==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);
        }path.close();style(rgb,alpha);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }
    private void book(Canvas c,float x,float y,float scale,int accent){
        c.save();c.translate(x,y);c.scale(scale,scale);
        path.reset();path.moveTo(0,-12);path.cubicTo(-36,-35,-79,-28,-108,-15);
        path.lineTo(-108,52);path.cubicTo(-64,35,-30,36,0,57);path.close();
        style(Color.WHITE,250);c.drawPath(path,p);
        path.reset();path.moveTo(0,-12);path.cubicTo(36,-35,79,-28,108,-15);
        path.lineTo(108,52);path.cubicTo(64,35,30,36,0,57);path.close();
        style(0xffd8edff,251);c.drawPath(path,p);
        line(c,0,-12,0,57,accent,3,255);
        for(int k=0;k<3;k++){
            line(c,15,1+k*13,94,-3+k*13,0xff406aa2,1.5f,125);
            line(c,-94,-3+k*13,-15,1+k*13,0xff406aa2,1.5f,125);
        }
        c.restore();
    }
    public void draw(Canvas c,int width,int height,int requested){
        if(width<=0||height<=0)return;
        final int id=CollectionCatalog.norm(requested),group=id/10,v=id%10;
        final int accent=CollectionCatalog.accent(id),background=COLORS[group][0];
        c.save();c.scale(width/360f,height/640f);
        style(Color.WHITE,255);
        int lighter=COLORS[group][1];
        p.setShader(new LinearGradient(0,0,360,640,
            new int[]{background,lighter,background},
            new float[]{0,.56f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,p);p.setShader(null);

        // Dramatic unique rainbow ribbons for this poster, away from the icon safe zone.
        for(int ribbon=0;ribbon<3;ribbon++){
            style(ribbon==0?accent:COLORS[group][2],25+ribbon*9);
            path.reset();
            float shift=(v*13+ribbon*27)%80;
            path.moveTo(-90,170+shift);
            path.cubicTo(110,60+shift,245,285+shift,440,105+shift);
            path.lineTo(440,195+shift);
            path.cubicTo(200,360+shift,110,130+shift,-90,280+shift);
            path.close();c.drawPath(path,p);
        }
        for(int i=0;i<60;i++){
            float x=rand(id*19+i*3)*355+2,y=rand(id*11+i*13+17)*590+15;
            dot(c,x,y,.8f+rand(i+id)*2.4f,i%7==0?accent:Color.WHITE,55+i%4*29);
        }
        box(c,17,26,343,96,20,0xff061a42,197);
        txt(c,CollectionCatalog.category(id)+"  •  "+String.format(java.util.Locale.US,"%02d",id+1),180,46,11,accent,true,252);
        txt(c,CollectionCatalog.name(id),180,74,22,Color.WHITE,true,252);
        // Different compositions for each of the ten entries in a subject group.
        drawComposition(c,group,v,accent);
        box(c,20,545,340,609,20,0xff071735,212);
        txt(c,SUBS[group][v],180,569,12,Color.WHITE,true,246);
        txt(c,"SAURABH'S VISION  •  LEARN TODAY, LEAD TOMORROW",180,591,10,accent,true,254);
        c.restore();
    }
    private void drawComposition(Canvas c,int group,int v,int accent){
        float cx=180,cy=321;
        if(group==0){
            if(v==0){ // Gold pi and 3d-inspired number halos
                dot(c,cx,cy,124,accent,35);
                txt(c,"π",cx,cy+66,178,accent,true,240);
                txt(c,"3.14159265",cx,477,22,Color.WHITE,true,245);
            }else if(v==1){
                box(c,44,212,316,430,22,0xff093552,238);
                for(int j=0;j<6;j++)txt(c,(j+1)+"x + "+(j+2)+" = ?",180,255+j*29,17,j%2==0?Color.WHITE:accent,true,224);
            }else if(v==2){
                for(int j=0;j<12;j++){
                    float angle=(float)(j*Math.PI/6),r=122;
                    dot(c,cx+(float)Math.cos(angle)*r,cy+(float)Math.sin(angle)*r,24,j%2==0?accent:Color.WHITE,170);
                    txt(c,""+(j+1),cx+(float)Math.cos(angle)*r,cy+(float)Math.sin(angle)*r+8,22,0xff08285a,true,252);
                }
                txt(c,"NUMBERS",180,335,26,Color.WHITE,true,243);
            }else if(v==3){
                for(int j=0;j<6;j++)polygon(c,180,315,35+j*22,3+j,0, j%2==0?accent:Color.WHITE,200);
                txt(c,"GEOMETRY",180,478,24,Color.WHITE,true,240);
            }else if(v==4){
                box(c,89,191,271,450,25,0xff071d41,248);
                box(c,105,217,256,282,9,0xffa7edff,231);
                txt(c,"12 × 8",180,256,29,0xff123368,true,255);
                for(int j=0;j<12;j++){
                    float x=126+(j%3)*52,y=325+(j/3)*31;
                    dot(c,x,y,15,j%3==0?accent:0xff7baeff,247);
                }
            }else if(v==5){
                txt(c,"Σ",cx,cy+70,180,accent,true,248);
                for(int j=0;j<8;j++)txt(c,""+(j+1),65+j*33,453,17,Color.WHITE,true,185);
            }else if(v==6){
                txt(c,"√",cx-24,cy+65,160,accent,true,240);
                txt(c,"64  =  8",cx+47,cy+65,37,Color.WHITE,true,240);
            }else if(v==7){
                line(c,60,355,308,355,Color.WHITE,2,151);
                line(c,185,211,185,452,Color.WHITE,2,151);
                for(int j=0;j<59;j++){
                    float x=70+j*4,y=342-(float)Math.sin(j*.18)*72;
                    dot(c,x,y,2,accent,242);
                }
                txt(c,"f(x) = sin x",180,477,20,Color.WHITE,true,236);
            }else if(v==8){
                txt(c,"∞",180,cy+55,168,accent,true,246);
                polygon(c,180,315,135,8,.22f,Color.WHITE,184);
            }else{
                pie(c,180,315,110,accent);
                txt(c,"¼   +   ¾   = 1",180,492,27,Color.WHITE,true,231);
            }
        }else if(group==1){
            if(v==0||v==5){
                for(int j=0;j<4;j++){c.save();c.rotate(j*45,180,315);
                    style(accent,222);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
                    rect.set(57,280,303,350);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);c.restore();}
                dot(c,180,315,28,accent,250);
                txt(c,"ATOMS",180,486,27,Color.WHITE,true,246);
            }else if(v==1||v==8){
                for(int j=0;j<26;j++){
                    float y=185+j*10,x=180+(float)Math.sin(j*.5)*72;
                    if(j%2==0)line(c,x,y,360-x,y,accent,2,175);
                    dot(c,x,y,7,accent,239);dot(c,360-x,y,7,Color.WHITE,220);
                }
                txt(c,v==1?"DNA • LIFE":"BIOLOGY",180,493,20,Color.WHITE,true,240);
            }else if(v==2||v==6){
                for(int j=0;j<4;j++){
                    float x=68+j*74;box(c,x-21,293,x+21,430,11,Color.WHITE,170);
                    box(c,x-18,352+j%2*19,x+18,422,9,accent,193);
                    dot(c,x,344,12,Color.WHITE,224);
                }
                txt(c,"LABORATORY",180,253,23,Color.WHITE,true,235);
            }else if(v==3){
                for(int j=0;j<6;j++){
                    float a=(float)(j*Math.PI/3),x=180+(float)Math.cos(a)*97,y=327+(float)Math.sin(a)*80;
                    dot(c,x,y,23,j%2==0?accent:Color.WHITE,224);
                    line(c,180,327,x,y,accent,2,185);
                }txt(c,"MOLECULES",180,477,24,Color.WHITE,true,236);
            }else if(v==4){
                for(int j=0;j<7;j++)polygon(c,180,315,33+j*21,4,.25f,accent,180);
                txt(c,"MAGNETISM",180,475,23,Color.WHITE,true,240);
            }else if(v==7){
                dot(c,180,320,117,0xffa4eedc,134);
                for(int j=0;j<14;j++)dot(c,180+(float)Math.cos(j*.46)*82,320+(float)Math.sin(j*.46)*84,6,accent,223);
                txt(c,"CELL BIOLOGY",180,492,24,Color.WHITE,true,239);
            }else{
                for(int j=0;j<16;j++){
                    float x=58+(j%4)*75,y=215+(j/4)*65;
                    box(c,x,y,x+60,y+53,10,j%2==0?accent:Color.WHITE,178);
                    txt(c,new String[]{"H","He","Li","Be","C","N","O","F","Na","Mg","Al","Si","P","S","Cl","Ar"}[j],x+30,y+35,22,0xff12264a,true,254);
                }
            }
        }else if(group==2){
            dot(c,180,318,108,accent,233);
            dot(c,147,279,29,Color.WHITE,98);
            for(int j=0;j<5;j++){
                float a=(float)(j*1.256+v*.23);
                dot(c,180+(float)Math.cos(a)*144,318+(float)Math.sin(a)*144,7+j%3*3,j%2==0?Color.WHITE:accent,229);
            }
            if(v%3==0){
                style(Color.WHITE,220);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);
                rect.set(26,280,334,360);c.save();c.rotate(-24,180,318);c.drawOval(rect,p);c.restore();p.setStyle(Paint.Style.FILL);
            }else if(v%3==1){
                path.reset();path.moveTo(180,204);path.lineTo(150,280);path.lineTo(210,280);path.close();
                style(Color.WHITE,233);c.drawPath(path,p);
                box(c,162,279,198,401,9,Color.WHITE,224);
                txt(c,"ROCKET",180,486,23,Color.WHITE,true,225);
            }else{
                for(int j=0;j<9;j++){
                    float a=(float)(j*Math.PI*2/9);
                    line(c,180,318,180+(float)Math.cos(a)*155,318+(float)Math.sin(a)*155,accent,2,115);
                }
            }
        }else if(group==3){
            book(c,180,353,1.26f,accent);
            for(int j=0;j<10;j++){
                float a=(float)(j*.62+v*.3);
                float x=180+(float)Math.cos(a)*133,y=278+(float)Math.sin(a)*118;
                txt(c,new String[]{"A","B","C","1","2","3","✦","★","π","?"}[(j+v)%10],x,y,19+(j%3)*5,j%2==0?accent:Color.WHITE,true,242);
            }
            if(v%3==0)for(int j=0;j<5;j++){
                float x=63+j*58;
                line(c,x,178,x,216,accent,2,150);
                dot(c,x,174,6,Color.WHITE,221);
            }
            txt(c, v<4?"MAGIC OF READING":v<7?"A WORLD OF IDEAS":"CREATE & DISCOVER",180,478,22,Color.WHITE,true,233);
        }else{
            if(v==0||v==7){
                for(int j=0;j<6;j++){
                    float y=455-j*52;
                    box(c,49+j*22,y,310-j*22,y+33,10,j%2==0?accent:Color.WHITE,182);
                    txt(c,j%2==0?"LEARN":"GROW",180,y+24,15,0xff142b4c,true,247);
                }
            }else if(v==1||v==3){
                dot(c,180,325,104,accent,166);
                for(int j=0;j<16;j++){
                    float a=(float)(j*Math.PI/8);
                    line(c,180+(float)Math.cos(a)*116,325+(float)Math.sin(a)*116,
                        180+(float)Math.cos(a)*137,325+(float)Math.sin(a)*137,Color.WHITE,3,175);
                }
                txt(c,v==1?"DREAM":"DISCIPLINE",180,339,26,0xff06274c,true,248);
            }else if(v==2||v==5){
                polygon(c,180,320,131,9,.25f,accent,220);
                polygon(c,180,320,79,6,0,Color.WHITE,210);
                txt(c,v==2?"PRACTICE":"PLAN TODAY",180,333,25,Color.WHITE,true,249);
            }else if(v==4||v==8){
                for(int j=0;j<7;j++){
                    float a=(float)(j*Math.PI*2/7);
                    dot(c,180+(float)Math.cos(a)*112,320+(float)Math.sin(a)*119,17,accent,211);
                }
                txt(c,v==4?"PROGRESS":"SUCCESS",180,329,30,Color.WHITE,true,241);
            }else{
                book(c,180,340,1.2f,accent);
                txt(c,v==6?"TRY AGAIN":"CREATE MORE",180,249,24,Color.WHITE,true,236);
            }
        }
    }
    private void pie(Canvas c,float cx,float cy,float r,int accent){
        for(int i=0;i<4;i++){
            style(i%2==0?accent:Color.WHITE,224);
            rect.set(cx-r,cy-r,cx+r,cy+r);
            c.drawArc(rect,i*90,87,true,p);
        }
    }
}
