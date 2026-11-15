package com.saurabhsvision.sbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Local-only storage. No syncing, analytics, logins or network transmission. */
public final class BrowserStore {
    public static final class Site {
        public final String url,title;
        Site(String u,String t){url=u;title=t;}
    }
    private final SharedPreferences prefs;
    public BrowserStore(Context context){
        prefs=context.getSharedPreferences("s_browser_local_v1",Context.MODE_PRIVATE);
    }
    public String searchEngine(){return prefs.getString("search_engine","google");}
    public void setSearchEngine(String value){prefs.edit().putString("search_engine",value).apply();}
    public boolean focusMode(){return prefs.getBoolean("focus",false);}
    public void setFocusMode(boolean enabled){prefs.edit().putBoolean("focus",enabled).apply();}
    private JSONArray load(String key){
        try{return new JSONArray(prefs.getString(key,"[]"));}
        catch(Exception ignored){return new JSONArray();}
    }
    private void save(String key,JSONArray data){prefs.edit().putString(key,data.toString()).apply();}
    private Site parse(JSONObject j){return new Site(j.optString("url",""),j.optString("title","Page"));}
    private JSONObject json(String url,String title){
        JSONObject j=new JSONObject();
        try{j.put("url",url);j.put("title",title);}catch(Exception ignored){}
        return j;
    }
    public List<Site> list(String key){
        JSONArray array=load(key);
        List<Site> out=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            JSONObject value=array.optJSONObject(i);
            if(value!=null&&!value.optString("url","").isEmpty())out.add(parse(value));
        }
        return out;
    }
    public void history(String url,String title){
        if(url==null||!(url.startsWith("http://")||url.startsWith("https://")))return;
        JSONArray output=new JSONArray();
        output.put(json(url,title==null||title.isEmpty()?url:title));
        for(Site item:list("history")){
            if(!item.url.equals(url)&&output.length()<250)output.put(json(item.url,item.title));
        }
        save("history",output);
    }
    public boolean bookmarked(String url){
        for(Site item:list("bookmarks"))if(item.url.equals(url))return true;
        return false;
    }
    public boolean toggleBookmark(String url,String title){
        if(url==null||!url.startsWith("http"))return false;
        boolean exists=bookmarked(url);
        JSONArray output=new JSONArray();
        if(!exists)output.put(json(url,title));
        for(Site item:list("bookmarks"))if(!item.url.equals(url))output.put(json(item.url,item.title));
        save("bookmarks",output);
        return !exists;
    }
    public void clearHistory(){prefs.edit().remove("history").apply();}
    public void clearBookmarks(){prefs.edit().remove("bookmarks").apply();}
    public String note(String url){return prefs.getString("note_"+key(url),"");}
    public void saveNote(String url,String note){
        prefs.edit().putString("note_"+key(url),note).apply();
    }
    public void saveSession(List<String> urls){
        JSONArray j=new JSONArray();
        for(String url:urls){if(url!=null&&url.startsWith("http"))j.put(url);}
        prefs.edit().putString("session",j.toString()).apply();
    }
    public List<String> session(){
        JSONArray j=load("session");List<String> out=new ArrayList<>();
        for(int i=0;i<j.length()&&i<8;i++){String u=j.optString(i,"");
            if(u.startsWith("http"))out.add(u);}
        return out;
    }
    private static String key(String input){
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(
                (input==null?"":input).getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(int i=0;i<10;i++)out.append(String.format("%02x",bytes[i]&255));
            return out.toString();
        }catch(Exception e){return "unknown";}
    }
}
