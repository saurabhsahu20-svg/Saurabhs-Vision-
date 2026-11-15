package com.saurabhsvision.sbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Bounded on-device history and bookmarks. No account, analytics, or sync. */
public final class BrowserStore {
    public static final class Page {
        public final String title,url;
        Page(String title,String url) {this.title=title;this.url=url;}
    }
    private static final String PREFS="sbrowser_data_v1";
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static List<Page> list(Context c,String key){
        List<Page> result=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(prefs(c).getString(key,"[]"));
            for(int i=0;i<array.length();i++){
                JSONObject item=array.optJSONObject(i);if(item==null)continue;
                String url=item.optString("u","");
                if(UrlTools.validated(url)!=null)result.add(new Page(item.optString("t",url),url));
            }
        }catch(Exception ignored){}
        return result;
    }
    public static void put(Context c,String key,String title,String url,int limit){
        if(UrlTools.validated(url)==null)return;
        List<Page> existing=list(c,key);
        JSONArray out=new JSONArray();
        out.put(make(title,url));
        int count=1;
        for(Page p:existing){
            if(p.url.equals(url))continue;
            if(count++>=limit)break;
            out.put(make(p.title,p.url));
        }
        prefs(c).edit().putString(key,out.toString()).apply();
    }
    private static JSONObject make(String title,String url){
        JSONObject obj=new JSONObject();
        try{obj.put("u",url);obj.put("t",title==null||title.trim().isEmpty()?url:title);}catch(Exception ignored){}
        return obj;
    }
    public static void remove(Context c,String key,String url){
        JSONArray arr=new JSONArray();
        for(Page p:list(c,key))if(!p.url.equals(url))arr.put(make(p.title,p.url));
        prefs(c).edit().putString(key,arr.toString()).apply();
    }
    public static boolean has(Context c,String key,String url){
        for(Page p:list(c,key))if(p.url.equals(url))return true;
        return false;
    }
    public static void clear(Context c,String key){prefs(c).edit().remove(key).apply();}
    public static void setSetting(Context c,String key,String value){prefs(c).edit().putString(key,value).apply();}
    public static String getSetting(Context c,String key,String value){return prefs(c).getString(key,value);}
}
