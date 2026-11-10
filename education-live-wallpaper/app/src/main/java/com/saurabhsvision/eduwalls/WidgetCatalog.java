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
    public static Bitmap background(int n,int width,int height){
        n=norm(n);
        width=Math.max(10,Math.min(width,720));height=Math.max(10,Math.min(height,720));
        Bitmap bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        int hi=accent(n),dark=n%2==0?0xff071b49:0xff181045;
        p.setShader(new LinearGradient(0,0,width,height,dark,n%2==0?0xff194ca9:0xff423095,Shader.TileMode.CLAMP));
        c.drawRect(0,0,width,height,p);p.setShader(null);
        p.setColor(hi);p.setAlpha(30+n%5*5);
        c.drawCircle(width*.87f,height*.19f,width*.39f,p);
        c.drawCircle(width*.10f,height*.84f,width*.29f,p);
        for(int j=0;j<16;j++){
            double angle=(j*2.4+n)*Math.PI/8;
            float x=(float)(width*(.50+.4*Math.sin(angle+j*.15)));
            float y=(float)(height*(.5+.42*Math.cos(angle)));
            p.setColor(j%3==0?Color.WHITE:hi);p.setAlpha(55+(j*11)%110);
            c.drawCircle(x,y,Math.max(1.5f,width*.004f*(1+j%3)),p);
        }
        p.setColor(hi);p.setAlpha(175);p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.8f,width*.005f));
        c.drawRoundRect(width*.03f,height*.045f,width*.97f,height*.955f,width*.068f,width*.068f,p);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setAlpha(255);
        return bmp;
    }
}
