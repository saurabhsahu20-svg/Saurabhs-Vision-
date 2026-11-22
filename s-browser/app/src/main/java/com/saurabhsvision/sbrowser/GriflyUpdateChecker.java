package com.saurabhsvision.sbrowser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads only the owner's PUBLIC GitHub release metadata; never installs anything. */
public final class GriflyUpdateChecker {
    public static final String RELEASES_URL=
        "https://api.github.com/repos/saurabhsahu20-svg/Saurabhs-Vision-/releases?per_page=100";
    private static final String RELEASE_PAGE_PREFIX=
        "https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/tag/";
    private static final String ASSET_PREFIX=
        "https://github.com/saurabhsahu20-svg/Saurabhs-Vision-/releases/download/";
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private GriflyUpdateChecker(){}
    public interface Callback {void completed(Release latest,String error);}
    public static final class Release{
        public final String tag,releasePage,downloadUrl;
        public final int[] version;
        private Release(String tag,String page,String download,int[] version){
            this.tag=tag;this.releasePage=page;this.downloadUrl=download;this.version=version;
        }
    }
    public static void check(Context ignored,Callback callback){
        EXECUTOR.execute(()->{
            Release best=null;String error=null;
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(RELEASES_URL).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept","application/vnd.github+json");
                connection.setRequestProperty("User-Agent","Grifly-Android-Update-Checker");
                connection.setInstanceFollowRedirects(false);
                if(connection.getResponseCode()!=200)throw new Exception("GitHub temporarily unavailable");
                ByteArrayOutputStream body=new ByteArrayOutputStream();
                try(InputStream input=connection.getInputStream()){
                    byte[] bytes=new byte[8192];int n;
                    while((n=input.read(bytes))!=-1){
                        if(body.size()+n>2_000_000)throw new Exception("Release catalog too large");
                        body.write(bytes,0,n);
                    }
                }
                JSONArray releases=new JSONArray(body.toString(StandardCharsets.UTF_8.name()));
                for(int i=0;i<releases.length();i++){
                    JSONObject item=releases.optJSONObject(i);
                    if(item==null||item.optBoolean("draft",false))continue;
                    String tag=item.optString("tag_name","");
                    int[] version=GriflyVersion.fromTag(tag);
                    if(version==null)continue;
                    String page=item.optString("html_url","");
                    if(!page.equals(RELEASE_PAGE_PREFIX+tag))continue;
                    JSONArray assets=item.optJSONArray("assets");
                    if(assets==null)continue;
                    String download=null;
                    for(int k=0;k<assets.length();k++){
                        JSONObject asset=assets.optJSONObject(k);
                        if(asset==null)continue;
                        String name=asset.optString("name","");
                        String url=asset.optString("browser_download_url","");
                        if(name.startsWith("Grifly_")&&name.endsWith(".apk")&&
                           url.equals(ASSET_PREFIX+tag+"/"+name)&&asset.optLong("size",0)>100000){
                            download=url;break;
                        }
                    }
                    if(download!=null&&(best==null||
                        GriflyVersion.compare(version,best.version)>0)){
                        best=new Release(tag,page,download,version);
                    }
                }
                if(best==null)error="No verified Grifly APK Release found";
            }catch(Exception ex){
                error="Cannot check GitHub releases right now";
            }finally{
                if(connection!=null)connection.disconnect();
            }
            final Release result=best;
            final String problem=error;
            MAIN.post(()->callback.completed(result,problem));
        });
    }
}
