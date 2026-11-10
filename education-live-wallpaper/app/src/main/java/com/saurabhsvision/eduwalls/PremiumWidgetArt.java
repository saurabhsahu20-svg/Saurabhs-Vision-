package com.saurabhsvision.eduwalls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Makes a compact user-selected Premium HD background for REAL Android widgets.
 * Numbers, time, battery, date, and weather remain Android RemoteViews/clock UI,
 * not part of the picture. User images never leave the phone.
 */
public final class PremiumWidgetArt {
    private static final String FILE_NAME="premium_widget_art.jpg";
    private PremiumWidgetArt(){}
    public static boolean available(Context ctx){return new File(ctx.getFilesDir(),FILE_NAME).isFile();}
    public static Bitmap background(Context ctx,int width,int height) {
        File f=new File(ctx.getFilesDir(),FILE_NAME);
        if(!f.isFile())return null;
        Bitmap raw=null;
        try{
            BitmapFactory.Options sample=new BitmapFactory.Options();
            sample.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(f.getAbsolutePath(),sample);
            if(sample.outWidth<=0||sample.outHeight<=0)return null;
            sample.inJustDecodeBounds=false;
            sample.inPreferredConfig=Bitmap.Config.RGB_565;
            sample.inSampleSize=1;
            while(sample.outWidth/sample.inSampleSize>1200||sample.outHeight/sample.inSampleSize>2000)sample.inSampleSize*=2;
            raw=BitmapFactory.decodeFile(f.getAbsolutePath(),sample);
            if(raw==null)return null;
            width=Math.max(100,Math.min(720,width));height=Math.max(75,Math.min(360,height));
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(result);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            float imageRatio=raw.getWidth()/(float)raw.getHeight();
            float targetRatio=width/(float)height;
            Rect crop;
            // Crop central subject with an upper-biased position for dashboard layouts.
            if(imageRatio>targetRatio){
                int srcWidth=Math.round(raw.getHeight()*targetRatio);
                int left=(raw.getWidth()-srcWidth)/2;
                crop=new Rect(left,0,left+srcWidth,raw.getHeight());
            }else{
                int srcHeight=Math.round(raw.getWidth()/targetRatio);
                int top=Math.round((raw.getHeight()-srcHeight)*.32f);
                crop=new Rect(0,top,raw.getWidth(),Math.min(raw.getHeight(),top+srcHeight));
            }
            c.drawBitmap(raw,crop,new RectF(0,0,width,height),p);
            p.reset();p.setAntiAlias(true);
            p.setShader(new LinearGradient(0,0,width,height,
                new int[]{0xe807122b,0x9f06142c,0xb0081030,0xdf06152a},
                new float[]{0f,.33f,.66f,1f},Shader.TileMode.CLAMP));
            c.drawRect(0,0,width,height,p);
            p.setShader(null);
            p.setColor(0xff6eeaff);p.setAlpha(112);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.5f,width*.004f));
            c.drawRoundRect(width*.012f,height*.03f,width*.988f,height*.97f,
                width*.04f,width*.04f,p);
            p.setStyle(Paint.Style.FILL);
            return result;
        }catch(OutOfMemoryError|RuntimeException e){
            return null;
        }finally{
            if(raw!=null&&!raw.isRecycled())raw.recycle();
        }
    }
    public static void choose(Context ctx,File source) throws IOException {
        if(source==null||!source.isFile())throw new IOException("Missing picture");
        BitmapFactory.Options sample=new BitmapFactory.Options();
        sample.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(source.getAbsolutePath(),sample);
        if(sample.outWidth<300||sample.outHeight<300)throw new IOException("Image too small");
        sample.inJustDecodeBounds=false;
        sample.inPreferredConfig=Bitmap.Config.RGB_565;
        sample.inSampleSize=1;
        while(sample.outWidth/sample.inSampleSize>1080||sample.outHeight/sample.inSampleSize>1920)sample.inSampleSize*=2;
        Bitmap bitmap=BitmapFactory.decodeFile(source.getAbsolutePath(),sample);
        if(bitmap==null)throw new IOException("Invalid image");
        File target=new File(ctx.getFilesDir(),FILE_NAME);
        File tmp=new File(ctx.getFilesDir(),FILE_NAME+".tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)){
            if(!bitmap.compress(Bitmap.CompressFormat.JPEG,83,out))throw new IOException("Encoding failed");
        }finally{bitmap.recycle();}
        if(target.exists()&&!target.delete()){tmp.delete();throw new IOException("Cannot overwrite widget art");}
        if(!tmp.renameTo(target)){tmp.delete();throw new IOException("Cannot finish widget art");}
        VisionWidgetProvider.updateAll(ctx);
    }
    public static void clear(Context ctx){
        File f=new File(ctx.getFilesDir(),FILE_NAME);
        if(f.exists())f.delete();
        VisionWidgetProvider.updateAll(ctx);
    }
}
