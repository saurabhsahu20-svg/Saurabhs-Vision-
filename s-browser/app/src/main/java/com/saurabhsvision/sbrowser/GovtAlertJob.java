package com.saurabhsvision.sbrowser;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Polls official HTML pages with bounded network use in a background JobService.
 * Only reports that a PORTAL PAGE changed, never that a vacancy is active.
 * A baseline is required before any change alert.
 */
public final class GovtAlertJob extends JobService {
    private volatile boolean stopped=false;
    @Override public boolean onStartJob(JobParameters params){
        stopped=false;
        new Thread(()->{
            try{scan();}
            catch(Exception ignored){}
            finally{jobFinished(params,false);}
        },"SBrowser-GovtPortalCheck").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){
        stopped=true;
        return true; // retry only when Android permits
    }
    private void scan(){
        SharedPreferences prefs=GovtAlerts.prefs(this);
        String area=prefs.getString("job_area","All Chhattisgarh");
        String category=prefs.getString("job_type","All");
        List<GovtSources.Source> sources=GovtSources.filter(area,category,"");
        if(sources.isEmpty())return;
        // Scan a rotating group to preserve bandwidth and battery; Android
        // scheduling is best-effort and may defer for multiple hours.
        int cursor=Math.abs(prefs.getInt("scan_cursor",0)%sources.size());
        int number=Math.min(12,sources.size());
        ArrayList<String> changed=new ArrayList<>();
        int scanned=0;
        long started=SystemClock.elapsedRealtime();
        for(int i=0;i<number&&!stopped;i++){
            if(SystemClock.elapsedRealtime()-started>110000L)break;
            GovtSources.Source source=sources.get((cursor+i)%sources.size());
            String signature=fingerprint(source.url);
            if(signature==null)continue;
            scanned++;
            String key="fingerprint_"+source.id;
            String previous=prefs.getString(key,null);
            prefs.edit().putString(key,signature).apply();
            // The first successful read is the baseline, not a new vacancy.
            if(previous!=null&&!previous.equals(signature))changed.add(source.title);
        }
        prefs.edit().putInt("scan_cursor",(cursor+number)%sources.size())
            .putLong("last_check",System.currentTimeMillis())
            .putInt("last_scanned",scanned).apply();
        GovtAlerts.showUpdateNotification(this,changed);
    }
    private String fingerprint(String address){
        HttpURLConnection connection=null;
        try{
            URL url=new URL(address);
            if(!"https".equalsIgnoreCase(url.getProtocol()))return null;
            connection=(HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(6500);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("User-Agent","SBrowser/1.2 (official notice update checker)");
            connection.setInstanceFollowRedirects(true);
            if(connection.getResponseCode()!=200)return null;
            if(!"https".equalsIgnoreCase(connection.getURL().getProtocol()))return null;
            String contentType=connection.getContentType();
            if(contentType==null||!contentType.toLowerCase(Locale.ROOT).contains("text/html"))return null;
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096];
            try(InputStream in=connection.getInputStream()){
                int n;
                while((n=in.read(buffer))!=-1 && output.size()<180000&&!stopped){
                    output.write(buffer,0,Math.min(n,180000-output.size()));
                }
            }
            String html=output.toString(StandardCharsets.UTF_8.name());
            // Exclude incidental CSS/JS data where possible.
            html=html.replaceAll("(?is)<!--.*?-->"," ")
                .replaceAll("(?is)<script[^>]*>.*?</script>"," ")
                .replaceAll("(?is)<style[^>]*>.*?</style>"," ")
                .replaceAll("(?is)<[^>]+>"," ")
                .replaceAll("\\s+"," ").trim();
            if(html.length()<80)return null;
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] bytes=digest.digest(html.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();
            for(byte b:bytes)hex.append(String.format(Locale.ROOT,"%02x",b&255));
            return hex.toString();
        }catch(Exception ignored){return null;}
        finally{if(connection!=null)connection.disconnect();}
    }
}
