package com.saurabhsvision.caller;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Phone number lookup and optional full-screen caller photo are local-only. */
public final class PhotoStore {
    public static final class Match {
        public final String name;
        public final Uri photo;
        Match(String name,Uri photo){this.name=name;this.photo=photo;}
    }
    // Short-lived, bounded in-memory cache avoids repeated Contacts Provider
    // lookups on every Telecom call-state notification. No network/storage.
    private static final long CACHE_TTL_MS=30000L;
    private static final class CacheItem {
        final Match match;
        final long time;
        CacheItem(Match match,long time){this.match=match;this.time=time;}
    }
    private static final LinkedHashMap<String,CacheItem> CACHE=
        new LinkedHashMap<String,CacheItem>(64,.75f,true){
            @Override protected boolean removeEldestEntry(
                Map.Entry<String,CacheItem> eldest){return size()>48;}
        };
    private static Match cache(String key,Match result){
        synchronized(CACHE){CACHE.put(key,new CacheItem(result,
            android.os.SystemClock.elapsedRealtime()));}
        return result;
    }
    private PhotoStore(){}

    public static Uri chosenPhoto(Context context,String phone){
        if(TextUtils.isEmpty(phone))return null;
        android.content.SharedPreferences prefs=context.getSharedPreferences(
            "caller_photos",Context.MODE_PRIVATE);
        String value=prefs.getString(CallerNumberKey.key(phone),null);
        if(value==null)value=prefs.getString(CallerNumberKey.legacyKey(phone),null);
        if(value==null)return null;
        try{return Uri.parse(value);}catch(Exception e){return null;}
    }
    public static void savePhoto(Context context,String phone,Uri uri){
        if(TextUtils.isEmpty(phone)||uri==null)return;
        context.getSharedPreferences("caller_photos",Context.MODE_PRIVATE).edit()
            .putString(CallerNumberKey.key(phone),uri.toString()).apply();
        synchronized(CACHE){CACHE.clear();}
    }
    public static void removePhoto(Context context,String phone){
        if(TextUtils.isEmpty(phone))return;
        context.getSharedPreferences("caller_photos",Context.MODE_PRIVATE).edit()
            .remove(CallerNumberKey.key(phone))
            .remove(CallerNumberKey.legacyKey(phone)).apply();
        synchronized(CACHE){CACHE.clear();}
    }
    public static Match resolve(Context context,String phone){
        final String raw=phone==null?"":phone;
        final boolean canRead=context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
            ==PackageManager.PERMISSION_GRANTED;
        final String key=(canRead?"c:":"n:")+CallerNumberKey.normalized(raw);
        long now=android.os.SystemClock.elapsedRealtime();
        synchronized(CACHE){
            CacheItem item=CACHE.get(key);
            if(item!=null&&now-item.time<CACHE_TTL_MS)return item.match;
            if(item!=null)CACHE.remove(key);
        }
        Uri custom=chosenPhoto(context,raw);
        String fallback=TextUtils.isEmpty(raw)?"Unknown Caller":raw;
        if(!canRead||TextUtils.isEmpty(raw))
            return cache(key,new Match(fallback,custom));
        Uri lookup=Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(raw));
        String[] columns={ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI};
        try(Cursor cursor=context.getContentResolver().query(
                lookup,columns,null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){
                String name=cursor.getString(0);
                String picture=cursor.getString(1);
                Uri photo=custom!=null?custom:(picture==null?null:Uri.parse(picture));
                return cache(key,new Match(TextUtils.isEmpty(name)?fallback:name,photo));
            }
        }catch(Exception ignored){}
        return cache(key,new Match(fallback,custom));
    }
    public static Bitmap loadScaled(Context context,Uri uri,int maxDimension){
        if(uri==null)return null;
        try{
            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inJustDecodeBounds=true;
            try(InputStream stream=context.getContentResolver().openInputStream(uri)){
                if(stream==null)return null;
                BitmapFactory.decodeStream(stream,null,opts);
            }
            if(opts.outWidth<=0||opts.outHeight<=0)return null;
            int sample=1;
            while(Math.max(opts.outWidth/sample,opts.outHeight/sample)>maxDimension)
                sample*=2;
            opts.inSampleSize=sample;opts.inJustDecodeBounds=false;
            try(InputStream stream=context.getContentResolver().openInputStream(uri)){
                if(stream==null)return null;
                return BitmapFactory.decodeStream(stream,null,opts);
            }
        }catch(Exception ignored){return null;}
    }
}
