package com.saurabhsvision.eduwalls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;

/** Illustrated educational models, not diagnostic or medical images. */
public final class BiologyPainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private final RectF oval=new RectF();
    private static final int NAVY=0xff092448,INK=0xff143653,WHITE=Color.WHITE;
    private float t; private boolean live;private int accent,ink;
    private static float sin(float v){return (float)Math.sin(v);}
    private static float cos(float v){return (float)Math.cos(v);}
    private void ink(int color,int alpha){
        p.reset();p.setAntiAlias(true);p.setColor(color);p.setAlpha(Math.max(0,Math.min(255,alpha)));
    }
    private void dot(Canvas c,float x,float y,float r,int color,int a){
        ink(color,a);c.drawCircle(x,y,Math.max(.25f,r),p);
    }
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width,int a){
        ink(color,a);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);
    }
    private void words(Canvas c,String value,float x,float y,float size,int color,boolean bold,int a){
        ink(color,a);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(value,x,y,p);
    }
    private void box(Canvas c,float l,float top,float r,float bottom,int color,int radius,int alpha){
        ink(color,alpha);oval.set(l,top,r,bottom);
        c.drawRoundRect(oval,radius,radius,p);
    }
    private void oval(Canvas c,float cx,float cy,float rx,float ry,int color,int alpha,boolean outline){
        ink(color,alpha);p.setStyle(outline?Paint.Style.STROKE:Paint.Style.FILL);
        p.setStrokeWidth(outline?2.4f:1);
        this.oval.set(cx-rx,cy-ry,cx+rx,cy+ry);c.drawOval(this.oval,p);p.setStyle(Paint.Style.FILL);
    }
    private void curve(Canvas c,float x1,float y1,float cx1,float cy1,float cx2,float cy2,float x2,float y2,int color,float width,int alpha){
        shape.reset();shape.moveTo(x1,y1);shape.cubicTo(cx1,cy1,cx2,cy2,x2,y2);
        ink(color,alpha);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width);
        c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
    }
    private void arc(Canvas c,float cx,float cy,float radius,float start,float sweep,int color,float wd){
        ink(color,210);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(wd);
        oval.set(cx-radius,cy-radius,cx+radius,cy+radius);
        c.drawArc(oval,start,sweep,false,p);p.setStyle(Paint.Style.FILL);
    }
    private void movingDot(Canvas c,float x,float y,int color,int seed){
        float pulse=(sin(t*1.6f+seed)+1)*.5f;
        dot(c,x,y,3.4f+pulse*2,color,200);
        dot(c,x,y,8+pulse*5,color,32);
    }
    private void arrow(Canvas c,float x1,float y1,float x2,float y2,int color){
        line(c,x1,y1,x2,y2,color,2.6f,219);
        float a=(float)Math.atan2(y2-y1,x2-x1);
        line(c,x2,y2,x2-cos(a-.7f)*9,y2-sin(a-.7f)*9,color,2.5f,219);
        line(c,x2,y2,x2-cos(a+.7f)*9,y2-sin(a+.7f)*9,color,2.5f,219);
    }
    private void diagramLabel(Canvas c,String label,float x,float y,float targetX,float targetY){
        int panel=live?0xff122d54:0xffe6f2f6;
        line(c,x,y-4,targetX,targetY,accent,1.25f,155);
        box(c,x-51,y-15,x+51,y+5,panel,7,232);
        words(c,label,x,y,10,ink,true,250);
    }
    public void draw(Canvas c,int width,int height,int requested,boolean animate,float seconds,
                     float touchX,float touchY,long touchAt){
        if(width<=0||height<=0)return;
        final int id=BiologyCatalog.norm(requested);
        this.live=animate;this.t=animate?seconds:0;
        this.accent=BiologyCatalog.accent(id);
        this.ink=animate?WHITE:INK;
        c.save();c.scale(width/360f,height/640f);
        paintBackdrop(c,id);
        box(c,18,28,342,92,live?0xff071a36:WHITE,19,live?226:243);
        words(c,live?"BIOLOGY  •  INTERACTIVE":"BIOLOGY  •  ILLUSTRATED",180,48,12,accent,true,247);
        words(c,BiologyCatalog.name(id),180,76,21,ink,true,250);
        switch(id){
            case 0:dna(c);break;
            case 1:heart(c);break;
            case 2:kidney(c);break;
            case 3:nephron(c);break;
            case 4:leaf(c);break;
            case 5:plantTissue(c);break;
            case 6:animalTissue(c);break;
            case 7:neuron(c);break;
            case 8:lungs(c);break;
            case 9:brain(c);break;
            case 10:plantCell(c);break;
            case 11:flower(c);break;
            case 12:chloroplast(c);break;
            case 13:stomata(c);break;
            case 14:roots(c);break;
            default:blood(c);break;
        }
        // Accessible facts, no unsupported diagnoses or medical claims.
        box(c,24,500,336,609,live?0xff071b38:WHITE,18,live?220:238);
        words(c,"SAURABH'S VISION",180,526,15,accent,true,250);
        String fact=BiologyCatalog.fact(id);
        int split=fact.lastIndexOf(' ',Math.min(30,fact.length()-1));
        if(fact.length()>38 && split>8){
            words(c,fact.substring(0,split),180,554,12,ink,true,246);
            words(c,fact.substring(split+1),180,573,12,ink,true,246);
        }else words(c,fact,180,562,12,ink,true,246);
        words(c,live?"TOUCH TO DISCOVER  •  SWIPE TO EXPLORE":"LEARN TODAY  •  LEAD TOMORROW",
              180,593,9.5f,accent,true,241);
        if(live && touchAt>0)touchReveal(c,id,Math.max(0,Math.min(360,touchX*360/width)),
            Math.max(0,Math.min(640,touchY*640/height)),touchAt);
        c.restore();
    }
    private void paintBackdrop(Canvas c,int id){
        ink(WHITE,255);
        int[] colors=live?
            new int[]{0xff05152c,0xff0c3a53,0xff07122e}:
            new int[]{0xffe9f8ff,0xffd3ecdf,0xffe9f3ff};
        p.setShader(new LinearGradient(0,0,360,640,colors,new float[]{0,.48f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,360,640,p);p.setShader(null);
        dot(c,290,194,98,accent,live?22:33);
        dot(c,64,426,72,accent,live?15:26);
        for(int j=0;j<27;j++){
            float x=24+(j*61+id*17)%320,y=102+(j*37+id*13)%390;
            float move=live?sin(t*.52f+j)*3:0;
            dot(c,x+move,y,1.2f+(j%3)*.7f,accent,80+j%3*35);
        }
        for(int n=0;n<3;n++){
            arc(c,180,295,128+n*34,15,105,accent,1.1f);
        }
    }
    private void dna(Canvas c){
        float cx=180,top=135;
        // Two strands with accurately paired crossbars; colorful DNA base pairs.
        for(int j=0;j<42;j++){
            float y=top+j*7.7f;
            float phase=j*.31f+(live?t*.52f:0);
            float left=cx+sin(phase)*88,right=cx-sin(phase)*88;
            if(j>0){
                float prev=(j-1)*.31f+(live?t*.52f:0);
                line(c,cx+sin(prev)*88,y-7.7f,left,y,0xff4cceea,4,244);
                line(c,cx-sin(prev)*88,y-7.7f,right,y,0xfff19ed4,4,235);
            }
            if(j%2==0){
                line(c,left,y,right,y,j%4==0?0xffffd86b:0xffa6f5ba,3.6f,199);
                dot(c,left,y,4.6f,0xff4cceea,240);dot(c,right,y,4.6f,0xfff19ed4,240);
            }
        }
        if(live){float j=(t*9)%40,y=top+j*7.7f;movingDot(c,cx+sin(j*.31f+t*.52f)*88,y,WHITE,0);}
        diagramLabel(c,"BASE PAIRS",91,467,132,302);
        diagramLabel(c,"BACKBONES",269,466,247,303);
    }
    private void heart(Canvas c){
        c.save();c.translate(180,318);
        float pulse=live?1+.034f*sin(t*4.3f):1;
        c.scale(pulse,pulse);
        shape.reset();
        shape.moveTo(0,126);
        shape.cubicTo(-28,109,-124,15,-105,-66);
        shape.cubicTo(-91,-135,-21,-128,0,-79);
        shape.cubicTo(30,-134,95,-134,108,-72);
        shape.cubicTo(135,-5,61,85,0,126);
        shape.close();
        ink(0xffed607e,242);c.drawPath(shape,p);
        ink(0xffa63761,220);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
        // Schematic four chambers, not a detailed anatomical reconstruction.
        oval(c,-51,-37,38,34,0xffffb8ad,222,false);
        oval(c,52,-37,38,34,0xffa7d8ff,226,false);
        oval(c,-48,43,41,63,0xffe84d69,237,false);
        oval(c,48,43,41,63,0xff678fdc,241,false);
        curve(c,-5,-100,-15,-168,28,-149,36,-102,0xffe8f4ff,12,238);
        curve(c,21,-108,53,-155,76,-145,82,-112,0xffffb64f,9,245);
        line(c,0,-62,0,111,0xfff6d6de,3,185);
        c.restore();
        if(live){
            float px=180+sin(t*1.5f)*40,py=300+sin(t*1.5f)*52;
            movingDot(c,px,py,WHITE,2);
        }
        diagramLabel(c,"ATRIUM",79,458,132,286);
        diagramLabel(c,"VENTRICLE",281,458,225,366);
    }
    private void kidney(Canvas c){
        // Symmetric, stylized kidney diagrams and cutaway segments.
        for(int side=0;side<2;side++){
            float cx=side==0?106:252,cy=309;
            c.save();c.translate(cx,cy);
            if(side==1)c.scale(-1,1);
            shape.reset();shape.moveTo(5,-116);
            shape.cubicTo(-112,-125,-113,121,3,123);
            shape.cubicTo(87,103,15,37,21,4);
            shape.cubicTo(32,-37,81,-93,5,-116);
            shape.close();
            ink(0xffb84c78,244);c.drawPath(shape,p);
            ink(0xffe78dac,242);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
            for(int j=0;j<7;j++){
                float a=(float)(j*.67-2),x=-25+cos(a)*48,y=sin(a)*79;
                shape.reset();shape.moveTo(14,0);shape.lineTo(x-14,y-9);shape.lineTo(x+5,y+9);shape.close();
                ink(0xffedb3b7,206);c.drawPath(shape,p);
            }
            curve(c,22,3,39,-2,52,23,52,62,0xffffedaf,11,224);
            c.restore();
            curve(c,cx+21,cy+4,cx+20,cy+101,cx+40,cy+111,cx+32,cy+155,0xffe6dd93,6,231);
        }
        if(live)for(int j=0;j<2;j++)movingDot(c,j==0?125:269,280+sin(t+j)*34,WHITE,j);
        diagramLabel(c,"CORTEX",86,467,67,258);
        diagramLabel(c,"MEDULLA",269,467,236,330);
    }
    private void nephron(Canvas c){
        // Glomerulus in Bowman's capsule => descending and ascending loop => distal segment.
        oval(c,104,199,49,48,0xffa8e8fc,156,false);
        oval(c,104,199,39,37,0xffd78cac,146,false);
        for(int j=0;j<16;j++){
            float angle=(float)(j*.99);
            dot(c,104+cos(angle)*24,199+sin(angle)*22,5,0xffbd476e,223);
        }
        curve(c,153,199,207,175,215,246,162,266,accent,9,245);
        curve(c,162,266,79,292,105,359,151,346,accent,9,245);
        curve(c,151,346,201,345,148,466,193,446,0xff78dbe8,9,246);
        curve(c,193,446,244,419,183,339,227,321,0xff78dbe8,9,245);
        curve(c,227,321,280,296,214,253,250,231,0xffffb17e,9,245);
        curve(c,250,231,276,217,299,244,293,314,0xffffb17e,8,246);
        curve(c,293,314,290,390,303,418,304,479,0xffe59e6c,8,240);
        if(live){
            float u=(t*.30f)%1f;
            movingDot(c,162+sin(u*6.2f)*36,266+u*170,WHITE,1);
        }
        diagramLabel(c,"GLOMERULUS",104,135,104,198);
        diagramLabel(c,"LOOP OF HENLE",229,479,192,424);
    }
    private void leaf(Canvas c){
        c.save();c.translate(180,306);c.rotate(-21);
        shape.reset();shape.moveTo(-100,112);
        shape.cubicTo(-122,-76,4,-170,113,-136);
        shape.cubicTo(141,-19,61,114,-100,112);
        shape.close();
        ink(0xff3fae70,246);c.drawPath(shape,p);
        ink(0xffc3ff88,218);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
        curve(c,-99,111,-41,10,50,-91,113,-136,0xffe3ffab,5,249);
        for(int k=0;k<7;k++){
            float x=-77+k*22,y=100-k*32;
            line(c,x,y,x+65,y+39,0xffbbff8c,2,188);
            line(c,x,y,x-29,y-32,0xffbbff8c,2,188);
        }
        c.restore();
        if(live){
            for(int j=0;j<9;j++){
                float a=t*.64f+j*.74f;
                dot(c,115+(j%3)*37+sin(a)*12,239+(j/3)*61+cos(a)*12,3.2f,WHITE,214);
            }
        }
        diagramLabel(c,"LEAF BLADE",88,457,160,320);
        diagramLabel(c,"LEAF VEINS",263,457,195,340);
    }
    private void plantTissue(Canvas c){
        drawGrid(c,50,155,260,127,5,4,0xff74d19c,0xffd9fbb1);
        for(int j=0;j<5;j++){
            float x=76+j*49;
            box(c,x,315,x+24,456,0xffddac6a,7,239);
            line(c,x+12,319,x+12,451,0xffffe6a7,3,204);
            box(c,x+30,326,x+41,449,0xff96df99,5,210);
        }
        if(live)for(int j=0;j<5;j++)movingDot(c,88+j*49,348+(t*19+j*25)%95,WHITE,j);
        diagramLabel(c,"PARENCHYMA",107,133,130,207);
        diagramLabel(c,"XYLEM/PHLOEM",257,473,261,395);
    }
    private void animalTissue(Canvas c){
        // Top: epithelia; bottom: smooth muscle and neuron in connective matrix.
        for(int j=0;j<8;j++){
            float x=45+j*37;
            box(c,x,174,x+32,272,j%2==0?0xffffb6c2:0xffd9a4e0,9,230);
            oval(c,x+16,224,10,15,0xff7952a2,242,false);
        }
        for(int j=0;j<6;j++){
            float y=321+j*19;
            curve(c,50,y,134,y-8,215,y+11,311,y,0xffda88a7,10,210);
        }
        oval(c,255,425,21,19,0xffa4b5ff,225,false);
        for(int j=0;j<5;j++){
            float a=(float)(j*1.24);
            line(c,255,425,255+cos(a)*58,425+sin(a)*48,0xffa4b5ff,3,219);
        }
        if(live)movingDot(c,230+sin(t)*20,428+cos(t)*13,WHITE,4);
        diagramLabel(c,"EPITHELIUM",107,136,132,210);
        diagramLabel(c,"MUSCLE",257,472,204,350);
    }
    private void neuron(Canvas c){
        oval(c,155,303,62,54,0xffc08be0,240,false);
        oval(c,155,303,23,23,0xff6e439f,231,false);
        for(int j=0;j<9;j++){
            float a=(float)(j*.698);
            float x=155+cos(a)*63,y=303+sin(a)*56;
            curve(c,x,y,x+cos(a)*39,y+sin(a)*42,
                x+cos(a)*85,y+sin(a)*74,
                x+cos(a)*119,y+sin(a)*98,0xffb28bf2,4,234);
        }
        curve(c,214,305,240,306,258,347,287,359,0xffffd37a,8,240);
        for(int j=0;j<5;j++){
            float x=238+j*22,y=312+j*11;
            oval(c,x,y,12,8,0xffffeac3,207,false);
        }
        for(int j=0;j<4;j++)
            curve(c,292,358,304,337+j*23,317,331+j*29,332,332+j*29,0xffffd37a,3,233);
        if(live){
            float r=(t*.4f)%1f;
            movingDot(c,214+r*94,305+r*51,WHITE,3);
        }
        diagramLabel(c,"DENDRITES",91,460,107,280);
        diagramLabel(c,"AXON",263,460,263,343);
    }
    private void lungs(Canvas c){
        curve(c,179,169,179,217,179,239,179,266,0xffb0e7fa,21,235);
        curve(c,178,264,142,276,131,300,100,324,0xff8ed8ff,11,238);
        curve(c,182,264,224,277,226,298,261,324,0xff8ed8ff,11,238);
        for(int side=0;side<2;side++){
            c.save();c.translate(side==0?111:247,345);
            float pulse=live?1+.035f*sin(t*1.35f):1;
            c.scale(pulse,pulse);
            shape.reset();
            if(side==0){
                shape.moveTo(47,-88);shape.cubicTo(5,-116,-61,-67,-66,1);
                shape.cubicTo(-84,116,-5,118,44,82);shape.close();
            }else{
                shape.moveTo(-47,-88);shape.cubicTo(-5,-116,61,-67,66,1);
                shape.cubicTo(84,116,5,118,-44,82);shape.close();
            }
            ink(0xfff79db6,231);c.drawPath(shape,p);
            c.restore();
            for(int j=0;j<5;j++){
                float x=side==0?113-j*8:247+j*8,y=303+j*22;
                line(c,side==0?120:240,290,x,y,0xffd85b84,2,161);
            }
        }
        if(live){
            movingDot(c,180,211+(t*25)%75,WHITE,1);
            movingDot(c,118,310+sin(t)*23,0xffb0f2ff,1);
        }
        diagramLabel(c,"BRONCHI",91,467,116,298);
        diagramLabel(c,"LUNG TISSUE",264,467,244,382);
    }
    private void brain(Canvas c){
        // Stylized cortical folds and cerebellum, not a clinical anatomical image.
        shape.reset();shape.moveTo(110,387);
        shape.cubicTo(63,369,52,320,77,291);
        shape.cubicTo(43,257,80,205,113,207);
        shape.cubicTo(132,162,174,183,187,190);
        shape.cubicTo(235,164,264,198,264,221);
        shape.cubicTo(309,230,306,290,280,307);
        shape.cubicTo(312,349,264,391,222,380);
        shape.cubicTo(189,422,148,391,110,387);shape.close();
        ink(0xffebb1db,241);c.drawPath(shape,p);
        for(int j=0;j<10;j++){
            float x=115+j%5*36,y=242+j/5*73;
            curve(c,x,y,x-22,y-17,x+25,y-23,x+13,y+9,0xffb872b2,2.8f,211);
        }
        oval(c,224,401,46,35,0xffb78ccd,232,false);
        curve(c,190,398,180,447,197,455,206,478,0xffbdb6e6,16,242);
        if(live)for(int j=0;j<6;j++)movingDot(c,135+j*23,252+sin(t*.8f+j)*25,0xffffffc4,j);
        diagramLabel(c,"CEREBRUM",105,463,131,282);
        diagramLabel(c,"CEREBELLUM",272,463,229,399);
    }
    private void plantCell(Canvas c){
        box(c,70,181,292,441,0xff3ca675,26,243);
        box(c,80,191,282,431,0xffb5efac,22,248);
        oval(c,180,319,75,75,0xffa6deeb,242,false);
        oval(c,134,251,24,24,0xff8f78c1,232,false);
        for(int j=0;j<9;j++){
            float x=111+(j%3)*73,y=222+(j/3)*92;
            oval(c,x,y,14,8,0xff31995c,221,false);
        }
        if(live)for(int j=0;j<4;j++)movingDot(c,135+j*30,320+sin(t+j)*27,WHITE,j);
        diagramLabel(c,"CELL WALL",102,464,83,319);
        diagramLabel(c,"VACUOLE",266,464,189,323);
    }
    private void flower(Canvas c){
        line(c,180,360,180,476,0xff419c5c,9,243);
        for(int j=0;j<6;j++){
            float a=(float)(j*Math.PI/3);
            float x=180+cos(a)*68,y=305+sin(a)*72;
            oval(c,x,y,43,25,j%2==0?0xffffbc6e:0xffff82b4,230,false);
        }
        dot(c,180,305,29,0xffffdd69,250);
        for(int j=0;j<7;j++)line(c,168+j*3,284,162+j*6,248+(j%2)*9,0xffffb555,2,211);
        shape.reset();shape.moveTo(179,433);shape.cubicTo(107,381,75,439,177,455);shape.close();
        ink(0xff5fba79,238);c.drawPath(shape,p);
        if(live)for(int j=0;j<7;j++){
            float a=t*.38f+j*.9f;dot(c,180+cos(a)*104,305+sin(a)*104,3.5f,WHITE,216);
        }
        diagramLabel(c,"PETALS",91,470,113,298);
        diagramLabel(c,"STAMENS",271,470,183,274);
    }
    private void chloroplast(Canvas c){
        oval(c,180,325,142,91,0xff52b36d,246,false);
        oval(c,180,325,130,79,0xffc1f59a,236,false);
        for(int j=0;j<7;j++){
            float x=100+j*27,y=300+(j%3)*20;
            for(int z=0;z<5;z++)box(c,x,y+z*5,x+21,y+z*5+3,0xff3e9466,3,238);
        }
        if(live)for(int j=0;j<7;j++){
            float a=t*.55f+j*.81f;
            movingDot(c,120+cos(a)*43,321+sin(a)*20,WHITE,j);
        }
        diagramLabel(c,"THYLAKOIDS",102,459,145,317);
        diagramLabel(c,"STROMA",264,459,258,336);
    }
    private void stomata(Canvas c){
        // Schematic paired guard cells surrounding the pore.
        for(int side=0;side<2;side++){
            float cx=side==0?126:235;
            oval(c,cx,310,55,130,0xff86cf7f,234,false);
            oval(c,cx+(side==0?15:-15),310,34,102,0xffc4f5a1,247,false);
            for(int j=0;j<6;j++){
                float y=229+j*31,x=cx+(side==0?-17:17)+sin(j*.8f)*9;
                oval(c,x,y,6,10,0xff479b66,232,false);
            }
        }
        float open=live?13+5*sin(t*.6f):18;
        oval(c,180,313,open,91,0xff082e48,243,false);
        if(live)for(int j=0;j<4;j++)movingDot(c,180,245+j*37+sin(t+j)*9,WHITE,j);
        diagramLabel(c,"GUARD CELL",99,465,117,328);
        diagramLabel(c,"STOMATAL PORE",264,465,180,314);
    }
    private void roots(Canvas c){
        line(c,180,210,180,431,0xffc38c59,11,241);
        for(int j=0;j<7;j++){
            float y=237+j*29;
            float dx=28+j%3*17;
            curve(c,180,y,163-dx,y+10,146-dx,y+21,138-dx,y+42,0xffc38c59,4,219);
            curve(c,180,y,202+dx,y+8,220+dx,y+23,234+dx,y+38,0xffc38c59,4,219);
        }
        for(int j=0;j<16;j++){
            float a=(float)(j*.62),x=180+sin(a)*71,y=302+(j%7)*19;
            line(c,x,y,x+sin(a)*25,y+11,0xffab865d,1.3f,173);
        }
        if(live)for(int j=0;j<5;j++)movingDot(c,180,238+(t*16+j*41)%215,0xff8ce8ff,j);
        diagramLabel(c,"ROOT HAIRS",93,467,100,380);
        diagramLabel(c,"XYLEM",269,467,180,276);
    }
    private void blood(Canvas c){
        for(int j=0;j<11;j++){
            float x=79+(j%4)*71,y=201+(j/4)*85;
            float bob=live?sin(t*.7f+j)*8:0;
            oval(c,x,y+bob,32,19,0xffe96b78,230,false);
            oval(c,x,y+bob,15,9,0xffb83b64,200,false);
        }
        oval(c,228,332,38,37,0xffe0d3ff,245,false);
        oval(c,228,332,22,21,0xff937ac9,243,false);
        for(int j=0;j<12;j++)dot(c,69+(j*41)%239,270+(j*37)%150,3.8f,0xffffe5ac,233);
        if(live)movingDot(c,225+sin(t)*10,330+cos(t)*9,WHITE,1);
        diagramLabel(c,"RED BLOOD CELL",105,466,107,290);
        diagramLabel(c,"WHITE BLOOD CELL",265,466,228,330);
    }
    private void drawGrid(Canvas c,float x,float y,float w,float h,int cols,int rows,int fill,int edge){
        box(c,x,y,x+w,y+h,fill,18,153);
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
            float xx=x+3+col*w/cols,yy=y+3+row*h/rows;
            box(c,xx,yy,xx+w/cols-7,yy+h/rows-7,edge,8,193);
            oval(c,xx+w/cols*.5f,yy+h/rows*.45f,7,9,fill,218,false);
        }
    }
    private void touchReveal(Canvas c,int id,float x,float y,long when){
        float age=(SystemClock.uptimeMillis()-when)/1000f;
        if(age<0||age>2.25f)return;
        float fade=1-age/2.25f;
        int side=x<180?0:1;
        String part=BiologyCatalog.part(id,side);
        ink(accent,(int)(185*fade));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f);
        c.drawCircle(x,y,14+age*68,p);p.setStyle(Paint.Style.FILL);
        for(int j=0;j<12;j++){
            float a=(float)(j*Math.PI/6);
            dot(c,x+cos(a)*(25+age*49),y+sin(a)*(25+age*49),2.6f,accent,(int)(215*fade));
        }
        box(c,41,409,319,461,live?0xff07172e:WHITE,18,(int)(245*fade));
        words(c,"EXPLORE: "+part,180,441,16,accent,true,(int)(250*fade));
    }
}
