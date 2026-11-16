package com.saurabhsvision.sbrowser;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Two independent network paths: a small GitHub Pages digest, then live RSS.
 * Do not call stored or scheduled content "live". Never invent headlines.
 */
public final class AssemblyNewsSource {
    public static final String DIGEST =
        "https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/s-browser-news/latest.json";
    // GitHub Pages may lag behind a successful news-refresh commit. The public
    // raw file is an independent delivery route, not an independent news source.
    private static final String RAW_DIGEST =
        "https://raw.githubusercontent.com/saurabhsahu20-svg/Saurabhs-Vision-/main/website/s-browser-news/latest.json";
    private static final long MAX_DIGEST_AGE=12L*60L*60L*1000L;
    private AssemblyNewsSource(){}

    public static final class Bulletin {
        public final CityNewsFeed.Result result;
        public final boolean scheduled;
        public Bulletin(CityNewsFeed.Result result,boolean scheduled){
            this.result=result;this.scheduled=scheduled;
        }
    }

    public static Bulletin fetch(String category,boolean hindi,boolean cg,int days)
        throws Exception {
        Exception digestFailure=null;
        try{
            CityNewsFeed.Result data=fetchDigest(category,hindi,cg,days);
            if(data!=null&&!data.stories.isEmpty())return new Bulletin(data,true);
        }catch(Exception e){digestFailure=e;}
        try{
            return new Bulletin(CityNewsFeed.fetchSchoolNewsRss(
                DailyNewsSources.feed(category,hindi,cg,days),days),false);
        }catch(Exception rssFailure){
            throw new Exception("Bulletin and live feed unavailable: "+
                (digestFailure==null?"No recent digest":shortError(digestFailure))+
                " / "+shortError(rssFailure));
        }
    }
    private static String shortError(Exception e){
        String s=e.getClass().getSimpleName();
        return s.length()>40?s.substring(0,40):s;
    }
    public static CityNewsFeed.Result fetchDigest(String category,boolean hindi,
        boolean cg,int days) throws Exception {
        String key=category+"_"+(cg?"cg":"india")+"_"+(hindi?"hi":"en");
        if(!DailyNewsSources.SPORTS.equals(category) &&
           !DailyNewsSources.BREAKING.equals(category))
            throw new IllegalArgumentException("Unknown news category");
        Exception firstError=null;
        // Prefer deployed Pages, but do not make Pages deployment a single point
        // of failure: the GitHub Actions job commits the same JSON to main.
        for(String endpoint:new String[]{DIGEST,RAW_DIGEST}){
            try{
                CityNewsFeed.Result result=readDigest(endpoint,key,days);
                if(result!=null && !result.stories.isEmpty())return result;
            }catch(Exception e){
                if(firstError==null)firstError=e;
            }
        }
        if(firstError!=null)throw firstError;
        return null;
    }

    private static CityNewsFeed.Result readDigest(String endpoint,String key,int days)
        throws Exception {
        URL url=new URL(endpoint);
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(4500);
        connection.setReadTimeout(4500);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept","application/json");
        connection.setRequestProperty("User-Agent","SBrowser/1.8 Android");
        try{
            if(connection.getResponseCode()!=200)
                throw new Exception("News bulletin HTTP "+connection.getResponseCode());
            // Defensive final URL check in case a CDN or proxy redirects us.
            if(!"https".equalsIgnoreCase(connection.getURL().getProtocol()))
                throw new Exception("Insecure bulletin redirect");
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){
                byte[] block=new byte[4096];int n;
                while((n=in.read(block))!=-1){
                    if(buffer.size()+n>210000)throw new Exception("Digest too large");
                    buffer.write(block,0,n);
                }
            }
            return parseDigest(new String(buffer.toByteArray(),"UTF-8"),
                key,days,System.currentTimeMillis());
        }finally{connection.disconnect();}
    }
    public static CityNewsFeed.Result parseDigest(String raw,String key,int days,long now)
        throws Exception {
        if(key==null||!key.matches("(sports|breaking)_(cg|india)_(hi|en)"))
            throw new IllegalArgumentException("Invalid digest key");
        JSONObject root=new JSONObject(raw);
        if(root.optInt("schema",0)!=1)throw new Exception("Unrecognized digest schema");
        JSONObject feed=root.optJSONObject("feeds");
        JSONObject row=feed==null?null:feed.optJSONObject(key);
        if(row==null)return null;
        long fetched=row.optLong("fetched_ms",0L);
        if(fetched<=0||fetched>now+60000L||now-fetched>MAX_DIGEST_AGE)
            return null; // never silently display old cached reports
        JSONArray articles=row.optJSONArray("stories");
        if(articles==null)return null;
        List<CityNewsFeed.Story> result=new ArrayList<>();
        HashSet<String> seen=new HashSet<>();
        for(int i=0;i<articles.length()&&result.size()<12;i++){
            JSONObject article=articles.optJSONObject(i);
            if(article==null)continue;
            String title=article.optString("title","").trim();
            String url=article.optString("url","");
            String publisher=article.optString("publisher","");
            String publisherUrl=article.optString("publisher_url","");
            long published=article.optLong("published_ms",0L);
            if(!NewsQualityGate.suitableHeadline(title) ||
               !NewsQualityGate.trustedPublisherUrl(publisherUrl) ||
               publisher.length()<2 ||
               !CityNewsSources.inWindow(published,now,days) ||
               !url.startsWith("https://") || !CityNewsFeed.isWebUrl(url))
                continue;
            String unique=title.toLowerCase(Locale.ROOT);
            if(seen.add(unique))
                result.add(new CityNewsFeed.Story(title,publisher,url,published));
        }
        Collections.sort(result,Comparator.comparingLong(
            (CityNewsFeed.Story item)->item.published).reversed());
        return result.isEmpty()?null:new CityNewsFeed.Result(result,fetched);
    }
}
