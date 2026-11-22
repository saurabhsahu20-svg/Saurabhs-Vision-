package com.saurabhsvision.keyboard;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** 30 original offline PNG stickers, rendered with Canvas and Unicode emoji. */
public final class StickerCatalog {
    private StickerCatalog(){}
    static final String[] PACKS={"Study Crew","Animal Moods","Science Fun",
        "Good Vibes","Celebration"};
    private static final String[][] LABELS={
        {"Great job!","Study time","Super smart","Keep going","Homework hero","A+ effort"},
        {"Happy paws","Purrfect!","Cool cat","Oh wow!","Tiny joy","Let's play"},
        {"Eureka!","Lab day","Space trip","Brain power","Bright idea","Hello Mars"},
        {"You got this","Be kind","Thank you","Good morning","Well done","All smiles"},
        {"Hooray!","Happy day","Let's dance","Best wishes","Big win","Party time"}
    };
    private static final String[][] EMOJI={
        {"⭐","📚","🧠","🚀","✏️","🏅"},
        {"🐾","🐈","😺","🐼","🐣","🐶"},
        {"💡","🧪","🛸","🔬","⚡","🪐"},
        {"🌈","💖","🙏","☀️","👏","😊"},
        {"🎉","🎁","💃","🎈","🏆","🥳"}
    };
    private static final int[] BASE={
        0xff3769c9,0xffa84c8d,0xff267d8b,0xff975eb6,0xffde8051
    };
    static int packCount(){return PACKS.length;}
    static int count(){return PACKS.length*6;}
    static int clamp(int i){return Math.floorMod(i,count());}
    static int packFor(int item){return clamp(item)/6;}
    static int id(int pack,int offset){return Math.floorMod(pack,packCount())*6+Math.floorMod(offset,6);}
    static String text(int i){int id=clamp(i);return LABELS[id/6][id%6];}
    static String emoji(int i){int id=clamp(i);return EMOJI[id/6][id%6];}
    private static int blend(int a,int b,float t){
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int blue=(int)((a&255)*(1-t)+(b&255)*t);
        return Color.rgb(r,g,blue);
    }
    static Bitmap draw(int idx,int width){
        int id=clamp(idx),pack=id/6;
        int size=Math.min(480,Math.max(100,width));
        Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bitmap);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float unit=size/320f;
        int start=BASE[pack];
        int end=blend(BASE[pack],0xff111633,.35f);
        p.setShader(new LinearGradient(0,0,size,size,start,end,Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(unit*10,unit*10,size-unit*10,size-unit*10),
            unit*52,unit*52,p);p.setShader(null);
        // Decorative confetti is deterministic: no downloaded artwork or tracking.
        for(int k=0;k<14;k++){
            float x=(20+(k*59+id*31)%274)*unit;
            float y=(20+(k*37+id*43)%226)*unit;
            p.setColor((k%3)==0?0xffffdb81:(k%3)==1?0xff9beffa:0xffffb1de);
            p.setAlpha(155);
            c.drawCircle(x,y,unit*(k%3+2.5f),p);
        }
        p.setAlpha(255);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        p.setTextSize(unit*137);
        p.setColor(Color.WHITE);
        c.drawText(emoji(id),size*.5f,size*.56f,p);
        p.setColor(0x9e0a1b35);
        c.drawRoundRect(new RectF(unit*27,size-unit*105,size-unit*27,size-unit*29),
            unit*23,unit*23,p);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.create("sans-serif-rounded",Typeface.BOLD));
        p.setTextSize(unit*(text(id).length()>12?22:27));
        c.drawText(text(id),size*.5f,size-unit*58,p);
        return bitmap;
    }
}
