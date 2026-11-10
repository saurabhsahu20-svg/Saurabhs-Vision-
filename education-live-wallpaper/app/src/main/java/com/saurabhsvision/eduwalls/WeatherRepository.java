package com.saurabhsvision.eduwalls;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicitly opt-in Open-Meteo weather; time and battery never require network access. */
public final class WeatherRepository {
    public static final String PREFS="vision_weather";
    private static final long REFRESH_MS=30*60*1000L;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private WeatherRepository(){}

    public interface Result { void onResult(boolean success,String text); }

    public static SharedPreferences state(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    public static void saveLocation(Context context,double latitude,double longitude,String name){
        if(latitude < -90 || latitude>90 || longitude < -180 || longitude>180) throw new IllegalArgumentException("Invalid location");
        String label=name==null?"My area":name.trim();
        state(context).edit().putLong("lat",Double.doubleToRawLongBits(latitude))
            .putLong("lon",Double.doubleToRawLongBits(longitude))
            .putString("city",label)
            .remove("weather").remove("time").apply();
        VisionWidgetProvider.updateAll(context);
        refresh(context,true);
    }
    public static boolean hasLocation(Context c){return state(c).contains("lat")&&state(c).contains("lon");}
    public static String city(Context c){
        return state(c).getString("city","Set weather city");
    }
    public static String display(Context c){
        SharedPreferences p=state(c);
        if(!hasLocation(c))return "☀  Set weather location";
        String weather=p.getString("weather",null);
        if(weather==null)return "☁  Weather updating…";
        long age=System.currentTimeMillis()-p.getLong("time",0);
        if(age>3*60*60*1000L)return "↻  "+weather+" (old)";
        return weather;
    }

    public static void searchCity(Context c,String rawCity,Result cb) {
        final String query=rawCity==null?"":rawCity.trim();
        if(query.length()<2) {MAIN.post(()->cb.onResult(false,"Type a city name first."));return;}
        Context app=c.getApplicationContext();
        IO.execute(()->{
            try {
                String url="https://geocoding-api.open-meteo.com/v1/search?name="
                    +URLEncoder.encode(query,"UTF-8")+"&count=1&language=en&format=json";
                JSONObject doc=new JSONObject(get(url));
                JSONArray list=doc.optJSONArray("results");
                if(list==null||list.length()==0){MAIN.post(()->cb.onResult(false,"City not found. Try city and state."));return;}
                JSONObject top=list.getJSONObject(0);
                String name=top.optString("name",query);
                String region=top.optString("admin1","");
                String country=top.optString("country","");
                String label=name+(region.isEmpty()?"":", "+region)+(country.isEmpty()?"":", "+country);
                double lat=top.getDouble("latitude");
                double lon=top.getDouble("longitude");
                MAIN.post(()->{saveLocation(app,lat,lon,label);cb.onResult(true,"Weather location: "+label);});
            }catch(Exception e){MAIN.post(()->cb.onResult(false,"Weather city lookup unavailable. Check Internet."));}
        });
    }
    public static void refresh(Context context,boolean force){
        Context app=context.getApplicationContext();
        SharedPreferences p=state(app);
        if(!hasLocation(app))return;
        if(!force && System.currentTimeMillis()-p.getLong("attempt",0)<REFRESH_MS)return;
        if(!BUSY.compareAndSet(false,true))return;
        p.edit().putLong("attempt",System.currentTimeMillis()).apply();
        IO.execute(()->{
            try{
                double lat=Double.longBitsToDouble(state(app).getLong("lat",0));
                double lon=Double.longBitsToDouble(state(app).getLong("lon",0));
                String url=String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&timezone=auto",lat,lon);
                JSONObject current=new JSONObject(get(url)).getJSONObject("current");
                int code=current.getInt("weather_code");
                double temp=current.getDouble("temperature_2m");
                String label=icon(code)+" "+Math.round(temp)+"°C  "+condition(code);
                state(app).edit().putString("weather",label).putLong("time",System.currentTimeMillis()).apply();
            }catch(Exception ignored){
                // Preserve previous weather instead of fabricating data when offline.
            }finally{
                BUSY.set(false);
                MAIN.post(()->VisionWidgetProvider.updateAll(app));
            }
        });
    }
    private static String icon(int code){
        if(code==0)return "☀";if(code<=3)return "⛅";
        if(code<=48)return "☁";if(code<=67)return "☂";
        if(code<=77)return "❄";if(code<=82)return "🌧";return "⛈";
    }
    private static String condition(int code){
        if(code==0)return "Clear";if(code<=3)return "Cloudy";
        if(code<=48)return "Fog";if(code<=67)return "Rain";
        if(code<=77)return "Snow";if(code<=82)return "Showers";
        if(code<=86)return "Snow";return "Thunder";
    }
    private static String get(String url) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(8500);connection.setReadTimeout(8500);
        connection.setRequestProperty("Accept","application/json");
        connection.setRequestProperty("User-Agent","VisionEduWalls/5.1 (Education widget)");
        try{
            int status=connection.getResponseCode();
            if(status<200||status>=300)throw new IllegalStateException("HTTP "+status);
            try(InputStream in=connection.getInputStream();
                BufferedReader reader=new BufferedReader(new InputStreamReader(in,"UTF-8"))){
                StringBuilder sb=new StringBuilder();String line;
                while((line=reader.readLine())!=null){
                    sb.append(line);if(sb.length()>65536)throw new IllegalArgumentException("Weather response too large");
                }
                return sb.toString();
            }
        }finally{connection.disconnect();}
    }
}
