package com.saurabhsvision.eduwalls;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** 50 selectable real home-screen widget configurations (5 content types × 10 skins). */
public final class WidgetCatalog {
    private WidgetCatalog(){}
    public static final int COUNT=50;
    public static final String[] GROUPS={"LIVE CLOCK","TODAY'S DATE","MATHS FACTS","SCIENCE FACTS","STUDY MOTIVATION"};
    private static final String[][] NAMES={
        {"Golden Clock","Galaxy Time","Blue Hour","Study Time","Aurora Clock","Rainbow Hours","Future Time","Star Clock","Cosmic Hour","Midnight Clock"},
        {"School Calendar","Bright Date","Galaxy Calendar","Daily Planner","Academic Date","New Day","Calendar Glow","Golden Day","Cosmic Date","Fresh Start"},
        {"Pi Facts","Algebra Bits","Geometry Facts","Prime Numbers","Fraction Facts","Maths Magic","Calculus Idea","Number Power","Maths Explorer","Quick Equations"},
        {"Atom Facts","DNA Facts","Space Science","Chemistry Facts","Life Science","Physics Fact","Periodic Table","Solar System","Energy Facts","Science Explorer"},
        {"Stay Curious","Keep Learning","Daily Practice","Dream Big","Study Smart","Small Steps","Focus Today","Learn & Lead","Believe & Grow","Progress Daily"}
    };
    private static final String[][] DETAILS={
        {"Your learning hour","Time to explore","A bright minute","Make today count","Minutes matter","Your colourful time","Build your future","Shine every hour","Explore the day","Keep moving forward"},
        {"Make today count","One new chance","Discover today","Plan with care","Your next chapter","Learn something new","Every day adds up","A fresh beginning","Today's knowledge","Start again"},
        {"π ≈ 3.14159","2(a + b) = 2a + 2b","Triangle angles = 180°","2, 3, 5, 7 are prime","½ = 0.5","√64 = 8","Derivative of x² is 2x","9 × 9 = 81","A square has 4 equal sides","3² + 4² = 5²"},
        {"Atoms build matter","DNA stores genetic instructions","Earth orbits the Sun","H₂O is water","Cells are units of life","Force = mass × acceleration","Atomic number = protons","Earth is a planet","Sunlight is renewable","Plants use light to make sugars"},
        {"Stay curious","Every day, learn a little","Practice builds understanding","Your ideas matter","Ask thoughtful questions","Small steps are progress","Focus on one concept","Learn today, lead tomorrow","Keep exploring","Progress takes practice"}
    };
    private static final int[] PALETTE={0xffffd75e,0xff75d9ff,0xffb6aaff,0xffa0edcb,0xffff9b91,
        0xffeaa2fc,0xffbaf274,0xffffc087,0xff95dfe7,0xffffe8ad};
    public static int norm(int n){return Math.max(0,Math.min(49,n));}
    public static String name(int n){n=norm(n);return NAMES[n/10][n%10];}
    public static String category(int n){return GROUPS[norm(n)/10];}
    public static String subtitle(int n){n=norm(n);return DETAILS[n/10][n%10];}
    public static int accent(int n){return PALETTE[norm(n)%10];}
    /** Distinctive digital dashboard art: 5 subjects × 10 separate visual compositions. */
    public static Bitmap background(int n,int width,int height){
        n=norm(n);
        width=Math.max(10,Math.min(width,720));height=Math.max(10,Math.min(height,720));
        final int group=n/10,v=n%10;
        Bitmap bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final int[][] bases={
            {0xff041a39,0xff104d9c},{0xff102344,0xff25516f},
            {0xff130d44,0xff463b91},{0xff0a2b2e,0xff216f78},
            {0xff24173c,0xff633e83}};
        final int hi=accent(n);
        p.setShader(new LinearGradient(0,0,width,height,
            new int[]{bases[group][0],bases[group][1],bases[group][0]},
            new float[]{0f,.59f,1f},Shader.TileMode.CLAMP));
        c.drawRect(0,0,width,height,p);p.setShader(null);
        p.setColor(hi);p.setAlpha(34+(v%4)*9);
        c.drawCircle(width*(.75f+.06f*(v%3)),height*.27f,
            width*(.24f+.04f*(v%5)),p);
        p.setColor(0xffffffff);p.setAlpha(23);
        c.drawCircle(width*.08f,height*.89f,width*.38f,p);

        // The five categories each have a genuinely different background subject.
        if(group==0) { // Time: concentric clock rings and minute markers
            float x=width*.82f,y=height*.57f,rad=width*.16f;
            p.setColor(hi);p.setAlpha(100);p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1.5f,width*.004f));
            c.drawCircle(x,y,rad,p);
            c.drawCircle(x,y,rad*.75f,p);
            p.setStyle(Paint.Style.FILL);
            for(int j=0;j<12;j++){
                float a=(float)(j*Math.PI/6);
                p.setColor(hi);p.setAlpha(140);
                c.drawCircle(x+(float)Math.cos(a)*rad*.9f,y+(float)Math.sin(a)*rad*.9f,
                    Math.max(1,width*.004f),p);
            }
        }else if(group==1) { // Date: abstract calendar grid
            float xx=width*.70f, yy=height*.23f, wd=width*.26f,ht=height*.54f;
            p.setColor(hi);p.setAlpha(76);
            c.drawRoundRect(xx,yy,xx+wd,yy+ht,width*.03f,width*.03f,p);
            p.setColor(Color.WHITE);p.setAlpha(92);
            for(int j=0;j<9;j++){
                float x=xx+wd*(.22f+(j%3)*.28f);
                float y=yy+ht*(.26f+(j/3)*.23f);
                c.drawRoundRect(x-4,y-4,x+4,y+4,2,2,p);
            }
        }else if(group==2) { // Maths: coordinate graph and formula ribbon
            p.setColor(hi);p.setAlpha(92);p.setStrokeWidth(2);
            for(int j=0;j<12;j++){
                float x=width*(.57f+.035f*j);
                float y=height*(.54f-.15f*(float)Math.sin(j*.47f+v*.36f));
                if(j>0){
                    float lastX=width*(.57f+.035f*(j-1));
                    float lastY=height*(.54f-.15f*(float)Math.sin((j-1)*.47f+v*.36f));
                    c.drawLine(lastX,lastY,x,y,p);
                }
                c.drawCircle(x,y,width*.005f,p);
            }
        }else if(group==3) { // Science: molecule constellation
            for(int j=0;j<7;j++){
                float a=(float)(j*Math.PI*2/7),x=width*(.78f+.13f*(float)Math.cos(a)),
                    y=height*(.54f+.23f*(float)Math.sin(a));
                p.setColor(hi);p.setAlpha(100);p.setStrokeWidth(2);
                c.drawLine(width*.78f,height*.54f,x,y,p);
                p.setColor(j%2==0?hi:Color.WHITE);p.setAlpha(96);
                c.drawCircle(x,y,width*.028f,p);
            }
            p.setColor(hi);p.setAlpha(150);
            c.drawCircle(width*.78f,height*.54f,width*.042f,p);
        }else{ // Motivation: rising progress bars with flag and stars
            for(int j=0;j<5;j++){
                float x=width*(.58f+j*.077f);
                float y=height*(.83f-(j+2)*.09f);
                p.setColor(hi);p.setAlpha(74+j*17);
                c.drawRoundRect(x,y,x+width*.06f,height*.88f,
                    width*.014f,width*.014f,p);
            }
        }

        // Ten independent frame geometries and decorative arrangements per category.
        p.setColor(hi);p.setAlpha(176);p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.8f,width*.004f));
        final float inset=width*(.023f+.002f*(v%3));
        float corner=(v%4+1)*width*.018f;
        if(v%3==0){
            c.drawRoundRect(inset,inset,width-inset,height-inset,corner,corner,p);
        }else if(v%3==1){
            for(int j=0;j<4;j++){
                float x=j%2==0?inset:width-inset;
                float y=j<2?inset:height-inset;
                float dx=j%2==0?1:-1,dy=j<2?1:-1;
                c.drawLine(x,y,x+dx*width*.12f,y,p);
                c.drawLine(x,y,x,y+dy*height*.18f,p);
            }
        }else{
            float top=height*.05f;
            c.drawArc(inset,top,width-inset,height-top,190+v*3,110,false,p);
            c.drawArc(inset,top,width-inset,height-top,10+v*3,110,false,p);
        }
        p.setStyle(Paint.Style.FILL);
        for(int j=0;j<13;j++){
            double a=(j*2.7+n*1.13)*Math.PI/8;
            float x=(float)(width*(.50+.41*Math.sin(a+j*.13)));
            float y=(float)(height*(.50+.41*Math.cos(a)));
            p.setColor(j%4==0?Color.WHITE:hi);
            p.setAlpha(40+(j*17+n*3)%112);
            c.drawCircle(x,y,Math.max(1.5f,width*.003f*(1+j%3)),p);
        }
        return bmp;
    }
}
