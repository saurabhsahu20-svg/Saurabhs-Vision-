package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.content.ContentResolver;
import java.io.InputStream;

/** Loads only an explicitly chosen photo; no broad storage permission or uploads. */
final class PhotoThemeArt {
    static final String PREFS="vision_keyboard_settings";
    static final String PHOTO_URI="theme_photo_uri";
    private PhotoThemeArt(){}

    static Bitmap load(Context context,int maxSize){
        String address=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .getString(PHOTO_URI,null);
        if(address==null||address.isEmpty())return null;
        try{
            Uri uri=Uri.parse(address);
            if(!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))return null;
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            try(InputStream in=context.getContentResolver().openInputStream(uri)){
                BitmapFactory.decodeStream(in,null,bounds);
            }
            if(bounds.outWidth<=0||bounds.outHeight<=0)return null;
            int max=Math.max(bounds.outWidth,bounds.outHeight);
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=1;
            while(max/options.inSampleSize>maxSize && options.inSampleSize<64)
                options.inSampleSize*=2;
            options.inPreferredConfig=Bitmap.Config.RGB_565;
            try(InputStream in=context.getContentResolver().openInputStream(uri)){
                return BitmapFactory.decodeStream(in,null,options);
            }
        }catch(Exception ignored){
            return null; // Gone photo / revoked permission: fallback to gradient.
        }
    }
    static void saveChosenUri(Context context,Uri uri){
        context.getContentResolver().takePersistableUriPermission(
            uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            .edit().putString(PHOTO_URI,uri.toString())
            .putInt("theme",ThemePalette.PHOTO_THEME).apply();
    }
}
