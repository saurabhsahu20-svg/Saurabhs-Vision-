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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

/**
 * Draws each of the ONE HUNDRED packaged HD wallpapers behind a real Android
 * RemoteViews dashboard. Images do not contain fake weather/clock/battery text.
 * Widgets can store independent art_<widgetId> selections.
 */
public final class BuiltIn3DWidgetArt {
    private static JSONArray catalog;
    private BuiltIn3DWidgetArt(){}
    public static boolean valid(int id){return id>=0 && id<100;}
    private static synchronized JSONArray catalog(Context ctx)throws Exception {
        if(catalog==null){
            try(InputStream in=ctx.getAssets().open("premium100/catalog.json")){
                byte[] payload=new byte[160000];int len=0,k;
                while((k=in.read(payload,len,payload.length-len))>0){
                    len+=k;if(len==payload.length)throw new IOException("3D gallery catalog too large");
                }
                catalog=new JSONArray(new String(payload,0,len,"UTF-8"));
                if(catalog.length()!=100){catalog=null;throw new IOException("Artwork catalog incomplete");}
            }
        }
        return catalog;
    }
    public static String title(Context ctx,int index){
        if(!valid(index))return "VISION 3D";
        try{return catalog(ctx).getJSONObject(index).optString("name","3D ART "+(index+1));}
        catch(Exception ex){return "3D ART "+(index+1);}
    }
    public static String category(Context ctx,int index){
        if(!valid(index))return "3D";
        try{return catalog(ctx).getJSONObject(index).optString("category","3D");}
        catch(Exception ex){return "3D";}
    }
    public static Bitmap thumbnail(Context ctx,int index,int sample){
        if(!valid(index))return null;
        try{
            String filename=catalog(ctx).getJSONObject(index).getString("file");
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=Math.max(1,sample);
            options.inPreferredConfig=Bitmap.Config.RGB_565;
            try(InputStream in=ctx.getAssets().open("premium100/"+filename)){
                return BitmapFactory.decodeStream(in,null,options);
            }
        }catch(Exception|OutOfMemoryError ignored){return null;}
    }
    public static Bitmap background(Context ctx,int index,int width,int height) {
        if(!valid(index))return null;
        Bitmap source=thumbnail(ctx,index,3);
        if(source==null)return null;
        try{
            width=Math.max(100,Math.min(640,width));height=Math.max(90,Math.min(340,height));
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(result);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            // Center-crop the same picture found in the 3D wallpaper gallery.
            int bw=source.getWidth(),bh=source.getHeight();
            float ratio=width/(float)height;
            Rect crop;
            if(bw/(float)bh>ratio){
                int cw=Math.max(1,Math.min(bw,Math.round(bh*ratio)));
                crop=new Rect((bw-cw)/2,0,(bw+cw)/2,bh);
            }else{
                int ch=Math.max(1,Math.min(bh,Math.round(bw/ratio)));
                int top=Math.max(0,Math.min(bh-ch,Math.round((bh-ch)*.39f)));
                crop=new Rect(0,top,bw,top+ch);
            }
            c.drawBitmap(source,crop,new RectF(0,0,width,height),p);
            // Constant dark glass overlay for readable dynamic data.
            p.reset();p.setAntiAlias(true);
            p.setShader(new LinearGradient(0,0,width,height,
                new int[]{0xe5041531,0x95061636,0xa9051236,0xd807132c},
                new float[]{0f,.29f,.65f,1f},Shader.TileMode.CLAMP));
            c.drawRect(0,0,width,height,p);p.setShader(null);
            int[] accents={0xff56e7ff,0xffffc968,0xffff92dd,0xffa3f5b0,0xffa6afff,
                0xff6edafa,0xffa9ffcd,0xffffbc8b,0xffdfaaff,0xfff9e0ad};
            int color=accents[index/10];
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,width*.0045f));
            p.setColor(color);p.setAlpha(191);
            float inset=width*.014f;
            c.drawRoundRect(inset,height*.035f,width-inset,height*.965f,
                width*.054f,width*.054f,p);
            p.setStyle(Paint.Style.FILL);
            for(int j=0;j<11;j++){
                float x=width*((13+j*31+index*7)%97)/100f;
                float y=height*((17+j*37+index*11)%91)/100f;
                p.setColor(j%3==0?Color.WHITE:color);p.setAlpha(80+(j*9)%80);
                c.drawCircle(x,y,Math.max(1,width*.003f),p);
            }
            return result;
        }catch(OutOfMemoryError | RuntimeException ex){return null;}
        finally{source.recycle();}
    }
}
