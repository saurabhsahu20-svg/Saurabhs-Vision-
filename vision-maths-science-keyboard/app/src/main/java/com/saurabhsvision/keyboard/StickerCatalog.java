package com.saurabhsvision.keyboard;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** 126 offline PNG stickers: keep the original 54 IDs and add 72 original Moodlings. */
public final class StickerCatalog {
    private StickerCatalog(){}
    private static final int ORIGINAL_PACKS=9;
    private static final int ORIGINAL_COUNT=54;
    private static final int MOOD_ITEMS_PER_PACK=12;
    private static final android.util.LruCache<String,Bitmap> PREVIEWS=
        new android.util.LruCache<String,Bitmap>(3072){
            @Override protected int sizeOf(String key,Bitmap bitmap){
                return Math.max(1,bitmap.getByteCount()/1024);
            }
        };
    static final String[] PACKS={"Study Crew","Animal Moods","Science Fun",
        "Good Vibes","Celebration","Comic Faces","Festive Fun",
        "Nature Notes","Sport Sparks",
        "Love","Morning","Laugh","Sadness","Sleep","Other"};
    private static final String[][] LABELS={
        {"Great job!","Study time","Super smart","Keep going","Homework hero","A+ effort"},
        {"Happy paws","Purrfect!","Cool cat","Oh wow!","Tiny joy","Let's play"},
        {"Eureka!","Lab day","Space trip","Brain power","Bright idea","Hello Mars"},
        {"You got this","Be kind","Thank you","Good morning","Well done","All smiles"},
        {"Hooray!","Happy day","Let's dance","Best wishes","Big win","Party time"},
        {"No way!","So funny","Oops!","Epic win","Amazing","So cute"},
        {"Shine bright","Happy lights","Cheers!","Surprise!","Celebrate","Make a wish"},
        {"Go green","Bloom","Fresh air","Save trees","Rainy day","Sunny vibes"},
        {"Go team!","Goal!","Champion","Keep playing","Full power","Game on"},
        {"Heart hug","Pink promise","Star affection","Cozy kitten","Petal love","Cloud cuddle",
         "Sweet tea","Moonlight love","Love toast","Shy mushroom","Owl affection","Gift of care"},
        {"Hello sunrise","Wake up smile","Morning spark","Sunny cat","Fresh flowers","Cloudy tea",
         "First coffee","Bright crescent","Breakfast toast","Mushroom sunrise","Early owl","New day gift"},
        {"Giggle berry","Laughing heart","Fun star","Silly kitten","Happy petals","Giggly cloud",
         "Coffee joke","Funny moon","Toast LOL","Laughing mushroom","Jolly owl","Surprise laughter"},
        {"Gentle hug","Mending heart","Quiet star","Little rain cat","Droopy flower","Rain cloud",
         "Warm comfort","Pensive moon","Blue toast","Moody mushroom","Thoughtful owl","Kindness gift"},
        {"Dreamy berry","Sleepy heart","Goodnight star","Purring kitty","Drowsy blossom","Soft cloud",
         "Bedtime cocoa","Moon dreams","Sleepy toast","Night mushroom","Drowsy owl","Sweet dreams"},
        {"Magic moment","Joyful heart","Wish star","Curious kitten","Mystery flower","Wonder cloud",
         "Lucky cup","Cosmic moon","Surprise toast","Party mushroom","Wise owl","Mystery gift"}
    };
    private static final String[][] EMOJI={
        {"⭐","📚","🧠","🚀","✏️","🏅"},
        {"🐾","🐈","😺","🐼","🐣","🐶"},
        {"💡","🧪","🛸","🔬","⚡","🪐"},
        {"🌈","💖","🙏","☀️","👏","😊"},
        {"🎉","🎁","💃","🎈","🏆","🥳"},
        {"😲","🤣","🙈","😎","🤩","🥹"},
        {"✨","🪔","🎇","🎊","🎆","🌠"},
        {"🌿","🌸","🍃","🌳","🌧️","☀️"},
        {"🏏","⚽","🏆","🏃","💪","🎮"},
        {"💗","💝","💖","😻","🌸","🥰","💌","🌙","💕","🍄","🦉","🎁"},
        {"🌄","☀️","✨","🐈","🌻","☁️","☕","🌅","🍞","🍄","🦉","🎀"},
        {"😆","🤣","⭐","😹","🌼","😄","😂","😜","😁","🍄","🦉","🎉"},
        {"🥹","💔","🌧️","😿","🥀","🌧️","🫶","🌙","😔","🍄","🦉","💙"},
        {"😴","💤","🌟","😽","🌼","☁️","☕","🌜","🍞","🌙","🦉","🛏️"},
        {"🎇","💫","⭐","🐱","🌺","☁️","☕","🪐","😮","🍄","🦉","🎁"}
    };
    private static final int[] BASE={
        0xff3769c9,0xffa84c8d,0xff267d8b,0xff975eb6,0xffde8051,
        0xff963c78,0xffc38132,0xff288b68,0xff356fc2,
        0xffec6395,0xffec9c36,0xff9762d7,0xff618fd3,0xff7975bf,0xff30b7a8
    };
    static int packCount(){return PACKS.length;}
    static int count(){return ORIGINAL_COUNT+(PACKS.length-ORIGINAL_PACKS)*MOOD_ITEMS_PER_PACK;}
    static int clamp(int i){return Math.floorMod(i,count());}
    static int itemsInPack(int pack){return Math.floorMod(pack,PACKS.length)<ORIGINAL_PACKS?
        6:MOOD_ITEMS_PER_PACK;}
    static int packFor(int item){
        int n=clamp(item);
        return n<ORIGINAL_COUNT?n/6:ORIGINAL_PACKS+(n-ORIGINAL_COUNT)/MOOD_ITEMS_PER_PACK;
    }
    static int id(int pack,int offset){
        int p=Math.floorMod(pack,packCount());
        int start=p<ORIGINAL_PACKS?p*6:ORIGINAL_COUNT+
            (p-ORIGINAL_PACKS)*MOOD_ITEMS_PER_PACK;
        return start+Math.floorMod(offset,itemsInPack(p));
    }
    static String text(int i){
        int n=clamp(i),p=packFor(n);
        return LABELS[p][n-id(p,0)];
    }
    static String emoji(int i){
        int n=clamp(i),p=packFor(n);
        return EMOJI[p][n-id(p,0)];
    }
    /** Bounded preview cache: never store clipboard data or user-generated files. */
    static Bitmap preview(int id,int width){
        int n=clamp(id),size=Math.min(480,Math.max(100,width));
        String key=n+"_"+size;
        synchronized(PREVIEWS){
            Bitmap cached=PREVIEWS.get(key);
            if(cached!=null&&!cached.isRecycled())return cached;
        }
        Bitmap bitmap=draw(n,size);
        synchronized(PREVIEWS){PREVIEWS.put(key,bitmap);}
        return bitmap;
    }
    private static int blend(int a,int b,float t){
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int blue=(int)((a&255)*(1-t)+(b&255)*t);
        return Color.rgb(r,g,blue);
    }
    static Bitmap draw(int idx,int width){
        int id=clamp(idx),pack=packFor(id);
        int size=Math.min(480,Math.max(100,width));
        if(id>=ORIGINAL_COUNT)return MoodStickerArt.draw(pack-ORIGINAL_PACKS,
            id-id(pack,0),size);
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
