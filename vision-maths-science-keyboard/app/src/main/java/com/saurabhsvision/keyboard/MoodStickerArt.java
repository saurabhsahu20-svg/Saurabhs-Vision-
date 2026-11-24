package com.saurabhsvision.keyboard;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Original S Board "Moodlings": cheerful offline vector stickers. Designed
 * specifically for this keyboard, not extracted from other sticker apps.
 * All 72 new artworks have transparent backgrounds and contain no user data.
 */
public final class MoodStickerArt {
    private MoodStickerArt(){}
    private static final int[][] PALETTE={
        {0xffff9fc3,0xffec3883,0xffffdfec}, // Love
        {0xffffd06f,0xffff8b42,0xfffff0b1}, // Morning
        {0xffaf95ff,0xff7755d8,0xffefcbff}, // Laugh
        {0xff92bdff,0xff4f77dd,0xffd6eaff}, // Sadness
        {0xffaaa7e9,0xff6967b2,0xffd8d3ff}, // Sleep
        {0xff75e5d2,0xff32a2a7,0xffcdfaf1}  // Other
    };
    private static final int INK=0xff343252;
    private static final int WHITE=0xffffffff;
    private static final int ROSE=0xffff4d78;

    private static Paint brush(int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }
    private static int mix(int a,int b,float q){
        int r=(int)(((a>>16)&255)*(1-q)+((b>>16)&255)*q);
        int g=(int)(((a>>8)&255)*(1-q)+((b>>8)&255)*q);
        int blue=(int)((a&255)*(1-q)+(b&255)*q);
        return Color.rgb(r,g,blue);
    }
    private static void oval(Canvas c,Paint p,float l,float t,float r,float b){
        c.drawOval(new RectF(l,t,r,b),p);
    }
    private static void round(Canvas c,Paint p,float l,float t,float r,float b,float rad){
        c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);
    }
    private static void gradient(Canvas c,Path path,int top,int bottom){
        Paint p=brush(WHITE);
        p.setShader(new LinearGradient(0,63,0,254,top,bottom,Shader.TileMode.CLAMP));
        p.setShadowLayer(6,0,6,0x400c1b37);
        c.drawPath(path,p);
        p.clearShadowLayer();p.setShader(null);
    }
    private static void circleGradient(Canvas c,float x,float y,float rad,int top,int bottom){
        Paint p=brush(WHITE);
        p.setShader(new LinearGradient(x-rad,y-rad,x+rad,y+rad,top,bottom,
            Shader.TileMode.CLAMP));
        p.setShadowLayer(7,0,7,0x420c1b37);
        c.drawCircle(x,y,rad,p);
        p.clearShadowLayer();p.setShader(null);
    }
    private static void heart(Canvas c,float x,float y,float s,int color){
        Path path=new Path();
        path.moveTo(x,y+s*.88f);
        path.cubicTo(x-s*.8f,y+s*.36f,x-s*.80f,y-s*.43f,x,y-s*.10f);
        path.cubicTo(x+s*.80f,y-s*.43f,x+s*.80f,y+s*.36f,x,y+s*.88f);
        path.close();
        Paint p=brush(color);
        p.setShadowLayer(3,0,2,0x3c000000);
        c.drawPath(path,p);p.clearShadowLayer();
        p.setColor(0x88ffffff);
        oval(c,p,x-s*.36f,y-s*.14f,x-s*.09f,y+s*.04f);
    }
    private static void star(Canvas c,float x,float y,float radius,Paint p){
        Path path=new Path();
        for(int i=0;i<10;i++){
            double a=-Math.PI/2+i*Math.PI/5;
            float r=i%2==0?radius:radius*.55f;
            float px=x+(float)Math.cos(a)*r;
            float py=y+(float)Math.sin(a)*r;
            if(i==0)path.moveTo(px,py);else path.lineTo(px,py);
        }
        path.close();c.drawPath(path,p);
    }
    private static void sparkle(Canvas c,float x,float y,float size,int color){
        Paint p=brush(color);
        Path path=new Path();
        path.moveTo(x,y-size);path.quadTo(x+size*.2f,y-size*.2f,x+size,y);
        path.quadTo(x+size*.2f,y+size*.2f,x,y+size);
        path.quadTo(x-size*.2f,y+size*.2f,x-size,y);
        path.quadTo(x-size*.2f,y-size*.2f,x,y-size);
        c.drawPath(path,p);
    }
    private static void stroke(Canvas c,Path path,int color,float width){
        Paint p=brush(color);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
        c.drawPath(path,p);
    }
    private static void body(Canvas c,int type,int first,int second,int pale){
        Paint p=brush(first);
        switch(type){
            case 0: // little pear creature, two leaf ears
                oval(c,brush(second),96,80,130,125);
                oval(c,brush(second),189,80,223,125);
                circleGradient(c,160,169,93,pale,first);
                oval(c,brush(0x55ffffff),113,95,163,113);
                break;
            case 1: // heart-shaped squishy character
                Path h=new Path();
                h.moveTo(160,247);
                h.cubicTo(111,203,49,158,82,109);
                h.cubicTo(105,75,143,91,160,116);
                h.cubicTo(178,88,225,76,243,119);
                h.cubicTo(259,174,205,213,160,247);h.close();
                gradient(c,h,pale,first);
                break;
            case 2: // five-point star sprite
                Path sp=new Path();
                for(int j=0;j<10;j++){
                    double a=-Math.PI/2+j*Math.PI/5;
                    float rad=j%2==0?104:59;
                    float px=160+(float)Math.cos(a)*rad;
                    float py=162+(float)Math.sin(a)*rad;
                    if(j==0)sp.moveTo(px,py);else sp.lineTo(px,py);
                }
                sp.close();gradient(c,sp,pale,first);
                break;
            case 3: // rounded cat with pointy ears
                Path ear=new Path();
                ear.moveTo(82,133);ear.lineTo(93,73);ear.lineTo(130,99);
                ear.lineTo(190,99);ear.lineTo(227,73);ear.lineTo(237,136);
                ear.close();gradient(c,ear,first,second);
                circleGradient(c,160,168,82,pale,first);
                oval(c,brush(0x99ffacc8),98,94,113,117);
                oval(c,brush(0x99ffacc8),207,94,222,117);
                break;
            case 4: // friendly flower with six petals
                for(int j=0;j<7;j++){
                    double a=j*2*Math.PI/7;
                    float x=160+(float)Math.cos(a)*75;
                    float y=160+(float)Math.sin(a)*75;
                    circleGradient(c,x,y,39,mix(pale,WHITE,.38f),first);
                }
                circleGradient(c,160,163,66,pale,second);
                break;
            case 5: // fluffy cloud creature
                for(int j=0;j<5;j++){
                    int[] xs={101,135,184,215,160};
                    int[] ys={175,129,132,169,184};
                    circleGradient(c,xs[j],ys[j],48,pale,first);
                }
                break;
            case 6: // cup character with steam curl
                Paint saucer=brush(0x666979c5);
                oval(c,saucer,76,242,253,260);
                Path cup=new Path();
                cup.moveTo(93,117);cup.lineTo(226,117);
                cup.quadTo(228,209,198,240);cup.quadTo(159,257,123,237);
                cup.quadTo(95,212,93,117);cup.close();
                gradient(c,cup,pale,first);
                Paint handle=brush(second);
                handle.setStyle(Paint.Style.STROKE);handle.setStrokeWidth(15);
                c.drawArc(new RectF(206,148,264,213),-89,230,false,handle);
                oval(c,brush(second),94,111,226,137);
                oval(c,brush(0xff634e52),109,113,211,128);
                break;
            case 7: // bright curved moon, self-contained silhouette
                Path moon=new Path();
                moon.moveTo(151,55);
                moon.cubicTo(89,78,64,148,94,208);
                moon.cubicTo(124,259,199,269,247,214);
                moon.cubicTo(163,232,119,164,151,55);moon.close();
                gradient(c,moon,pale,first);
                break;
            case 8: // smiling rounded toast with a darker crust
                Paint crust=brush(second);
                round(c,crust,84,78,236,255,40);
                Paint toast=brush(pale);
                toast.setShader(new LinearGradient(0,95,0,248,pale,first,
                    Shader.TileMode.CLAMP));
                round(c,toast,96,89,223,240,35);
                break;
            case 9: // little mushroom friend
                Paint stalk=brush(pale);
                round(c,stalk,126,158,204,242,28);
                Path hat=new Path();
                hat.moveTo(61,161);
                hat.cubicTo(71,52,237,53,259,161);
                hat.quadTo(161,195,61,161);hat.close();
                gradient(c,hat,first,second);
                Paint spot=brush(0xafffffff);
                c.drawCircle(152,100,16,spot);
                c.drawCircle(99,144,13,spot);
                c.drawCircle(204,139,11,spot);
                break;
            case 10: // sleepy owl-like round character
                oval(c,brush(second),70,142,117,229);
                oval(c,brush(second),204,142,251,229);
                circleGradient(c,160,169,80,pale,first);
                oval(c,brush(0x70ffffff),112,205,211,243);
                Path beak=new Path();beak.moveTo(150,181);beak.lineTo(170,181);
                beak.lineTo(160,196);beak.close();c.drawPath(beak,brush(0xffffad57));
                break;
            default: // bow-wrapped surprise box character
                Paint box=brush(second);
                round(c,box,82,112,239,249,19);
                Paint face=brush(pale);
                round(c,face,95,132,227,235,14);
                Paint ribbon=brush(first);
                round(c,ribbon,152,102,168,249,7);
                Path bow=new Path();
                bow.moveTo(160,111);
                bow.cubicTo(97,64,103,134,160,119);
                bow.cubicTo(229,78,219,62,160,111);bow.close();
                c.drawPath(bow,brush(0xffff89b6));
                break;
        }
    }
    private static void face(Canvas c,int category,int type,int index){
        float centerX=160f,eyeY=166f;
        if(type==6)eyeY=182;
        if(type==7){centerX=154;eyeY=189;}
        if(type==8)eyeY=167;
        if(type==9)eyeY=189;
        if(type==10)eyeY=163;
        if(type==11)eyeY=170;
        float dx=27;
        Paint ink=brush(INK);
        if(category==2||category==4){ // laughing / sleeping eyelid curves
            ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(6);
            for(int side:new int[]{-1,1}){
                float x=centerX+side*dx;
                Path eye=new Path();
                eye.moveTo(x-13,eyeY+2);
                eye.quadTo(x,eyeY+(category==4?-12:13),x+13,eyeY+2);
                c.drawPath(eye,ink);
            }
            ink.setStyle(Paint.Style.FILL);
        }else{
            oval(c,ink,centerX-dx-9,eyeY-12,centerX-dx+9,eyeY+13);
            oval(c,ink,centerX+dx-9,eyeY-12,centerX+dx+9,eyeY+13);
            Paint shine=brush(WHITE);
            c.drawCircle(centerX-dx-3,eyeY-5,4,shine);
            c.drawCircle(centerX+dx-3,eyeY-5,4,shine);
        }
        Paint blush=brush(category==3?0x80cc92c9:0xaaff7aa8);
        oval(c,blush,centerX-64,eyeY+11,centerX-41,eyeY+23);
        oval(c,blush,centerX+41,eyeY+11,centerX+64,eyeY+23);
        Path mouth=new Path();
        float y=eyeY+33;
        if(category==3){
            mouth.moveTo(centerX-16,y+8);
            mouth.quadTo(centerX,y-6,centerX+17,y+9);
        }else if(category==4){
            mouth.moveTo(centerX-8,y);
            mouth.quadTo(centerX,y+6,centerX+9,y);
        }else if(category==2){
            mouth.moveTo(centerX-18,y-4);
            mouth.quadTo(centerX,y+34,centerX+18,y-4);
            mouth.quadTo(centerX,y+4,centerX-18,y-4);
            c.drawPath(mouth,brush(0xff9d3359));
            return;
        }else{
            mouth.moveTo(centerX-17,y);
            mouth.quadTo(centerX,y+23,centerX+17,y);
        }
        stroke(c,mouth,INK,5f);
        if(category==3){ // shiny teardrops
            Paint tear=brush(0xff6fbeff);
            oval(c,tear,centerX-50,eyeY+18,centerX-37,eyeY+43);
        }
    }
    private static void decoration(Canvas c,int category,int variant){
        int shift=variant%4;
        switch(category){
            case 0: // Love: hearts and charm sparks
                heart(c,235,82+shift*3,22,ROSE);
                heart(c,80,93,13,0xffff80a0);
                sparkle(c,257,170,11,0xffffc8db);break;
            case 1: // Morning: rising light rays and leaves
                for(int k=0;k<5;k++){
                    double a=k*Math.PI/4-.35;
                    float x=160+(float)Math.cos(a)*128;
                    float y=167+(float)Math.sin(a)*118;
                    sparkle(c,x,y,9,k%2==0?0xffffcc57:0xffffa277);
                }
                break;
            case 2: // Laugh: party confetti
                for(int k=0;k<6;k++){
                    float x=60+(k*43+variant*13)%205;
                    float y=67+(k*49)%77;
                    sparkle(c,x,y,9,k%2==0?0xffffa2c1:0xff73dcf9);
                }
                break;
            case 3: // Sadness: gentle drops, still hopeful
                oval(c,brush(0xff75b8f0),63,90,76,111);
                oval(c,brush(0xff75b8f0),246,99,259,122);
                sparkle(c,246,215,10,0xffd8f0ff);break;
            case 4: // Sleep: stars and floating z letters
                sparkle(c,72,107,13,0xffffd989);
                sparkle(c,242,97,12,0xffc5baff);
                Paint z=brush(0xff7777b7);
                z.setTextSize(30);z.setFakeBoldText(true);
                c.drawText("z",233,70,z);
                c.drawText("z",253,45,z);
                break;
            default: // Other: bubble confetti
                sparkle(c,81,88,12,0xffffce4a);
                sparkle(c,241,102,15,0xffe0a3ff);
                c.drawCircle(76,230,7,brush(0xffe99ccd));
                c.drawCircle(246,236,6,brush(0xff81cdeb));
        }
    }
    public static Bitmap draw(int mood,int variation,int wantedSize){
        int category=Math.floorMod(mood,6),variant=Math.floorMod(variation,12);
        int size=Math.max(100,Math.min(480,wantedSize));
        Bitmap result=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(result);
        canvas.scale(size/320f,size/320f);
        int[] scheme=PALETTE[category];
        int first=variant%2==0?scheme[0]:mix(scheme[0],scheme[1],.35f);
        int second=scheme[1],pale=scheme[2];
        body(canvas,variant%12,first,second,pale);
        face(canvas,category,variant%12,variant);
        decoration(canvas,category,variant);
        // Upper-left highlight adds a friendly glass-like finish, never a copy
        // of another app's sticker asset.
        Paint shine=brush(0x77ffffff);
        oval(canvas,shine,108,100,141,113);
        return result;
    }
}
