package com.saurabhsvision.sbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A clearly labelled, locally cached backup for temporary RSS network failures.
 * Never presents cached headlines as a fresh network response.
 */
public final class CityNewsCache {
    private static final String PREF="s_browser_city_news_cache_v1";
    private static final long MAX_CACHE_AGE_MS=12L*60L*60L*1000L;
    private CityNewsCache(){}

    private static String key(String city,boolean hindi,int days){
        String value=CityNewsSources.normalize(city)+"|"+hindi+"|"+days;
        return "feed_"+Integer.toHexString(value.hashCode());
    }
    public static void save(Context context,String city,boolean hindi,int days,
                            CityNewsFeed.Result result){
        if(result==null||result.stories.isEmpty())return;
        try{
            JSONObject object=new JSONObject();
            object.put("checkedAt",result.checkedAt);
            JSONArray rows=new JSONArray();
            for(CityNewsFeed.Story story:result.stories){
                if(rows.length()>=25)break;
                JSONObject row=new JSONObject();
                row.put("title",story.title);
                row.put("source",story.source);
                row.put("url",story.url);
                row.put("published",story.published);
                rows.put(row);
            }
            object.put("stories",rows);
            context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putString(key(city,hindi,days),object.toString()).apply();
        }catch(Exception ignored){}
    }
    public static CityNewsFeed.Result load(Context context,String city,
                                          boolean hindi,int days,long now){
        try{
            String raw=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
                .getString(key(city,hindi,days),null);
            if(raw==null)return null;
            JSONObject object=new JSONObject(raw);
            long checked=object.optLong("checkedAt",0L);
            if(checked<=0L||checked>now+60000L||now-checked>MAX_CACHE_AGE_MS)
                return null;
            JSONArray rows=object.optJSONArray("stories");
            if(rows==null)return null;
            List<CityNewsFeed.Story> stories=new ArrayList<>();
            for(int i=0;i<rows.length()&&stories.size()<25;i++){
                JSONObject row=rows.optJSONObject(i);
                if(row==null)continue;
                String title=row.optString("title","");
                String source=row.optString("source","Unknown publisher");
                String url=row.optString("url","");
                long published=row.optLong("published",0L);
                if(title.length()>8&&CityNewsFeed.isWebUrl(url)&&
                    CityNewsSources.inWindow(published,now,days)){
                    stories.add(new CityNewsFeed.Story(title,source,url,published));
                }
            }
            return stories.isEmpty()?null:new CityNewsFeed.Result(stories,checked);
        }catch(Exception ignored){return null;}
    }
}
