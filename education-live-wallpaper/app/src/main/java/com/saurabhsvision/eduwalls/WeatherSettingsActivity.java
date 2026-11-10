package com.saurabhsvision.eduwalls;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.CancellationSignal;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

/** Weather is opt-in; phone time, date, and battery widgets work without a location. */
public final class WeatherSettingsActivity extends Activity {
    private static final int ASK_COARSE=1405;
    private TextView status;
    private EditText city;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean returned=new AtomicBoolean(false);
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(15));return g;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff071b49);
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);sc.setBackgroundColor(0xffedf3ff);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));
        sc.addView(root);setContentView(sc);
        TextView heading=new TextView(this);
        heading.setText("☀  WEATHER & SMART WIDGETS");
        heading.setTextSize(24);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        heading.setTextColor(0xff071b49);root.addView(heading);
        TextView intro=new TextView(this);
        intro.setText("⏰ Time + 📅 Date + 🔋 Phone Battery work automatically. Weather needs an Internet connection and a city or optional phone location.");
        intro.setTextSize(15);intro.setTextColor(0xff284373);
        add(root,intro,14);
        status=new TextView(this);status.setTextSize(15);status.setTextColor(0xff144fa1);
        status.setPadding(dp(15),dp(15),dp(15),dp(15));
        status.setBackground(bg(Color.WHITE));add(root,status,20);
        showStatus();

        TextView manual=new TextView(this);
        manual.setText("🌆 Choose a city (no location permission)");
        manual.setTextSize(18);manual.setTextColor(0xff071b49);
        manual.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        add(root,manual,23);
        city=new EditText(this);
        city.setSingleLine(true);city.setTextSize(16);
        city.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        city.setHint("City name, e.g. Raipur or Delhi");
        city.setBackground(bg(Color.WHITE));city.setPadding(dp(15),dp(14),dp(15),dp(14));
        add(root,city,10);
        Button set=button("🔎 Find City & Show Weather",0xff1746d2,Color.WHITE);
        add(root,set,9);
        set.setOnClickListener(v->{
            String q=city.getText().toString().trim();
            if(q.length()<2){status.setText("Enter a city name.");return;}
            status.setText("Looking up city…");
            WeatherRepository.searchCity(this,q,(ok,message)->{
                status.setText(message+(ok?"\n"+WeatherRepository.display(this):""));
                if(ok)showStatus();
            });
        });

        TextView auto=new TextView(this);
        auto.setText("📍 Or use approximate phone location");
        auto.setTextSize(18);auto.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        auto.setTextColor(0xff071b49);add(root,auto,26);
        TextView explain=new TextView(this);
        explain.setText("Optional: Android asks for location permission. Only the selected approximate coordinates are used to request weather from Open-Meteo, and are stored on your phone.");
        explain.setTextSize(13);explain.setTextColor(0xff526a8a);add(root,explain,7);
        Button phone=button("📍 Use Phone's Approximate Location",0xffffd54f,0xff071b49);
        add(root,phone,12);phone.setOnClickListener(v->beginLocation());

        Button refresh=button("↻ Refresh Weather Now",Color.WHITE,0xff1746d2);
        add(root,refresh,22);
        refresh.setOnClickListener(v->{
            WeatherRepository.refresh(this,true);
            status.setText("Requesting updated weather…");
            handler.postDelayed(this::showStatus,3500);
        });
        TextView credit=new TextView(this);
        credit.setText("Weather: Open-Meteo · Requires Internet. Updated approximately every 30 minutes; stale data is marked when offline. Your clock follows the phone time zone. Battery may refresh periodically rather than every second.");
        credit.setTextSize(13);credit.setTextColor(0xff617390);add(root,credit,20);
    }
    private void add(LinearLayout root,View v,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);root.addView(v,lp);
    }
    private Button button(String title,int color,int ink){
        Button b=new Button(this);b.setText(title);b.setTextColor(ink);
        b.setTextSize(15);b.setAllCaps(false);b.setBackground(bg(color));b.setMinHeight(dp(52));
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return b;
    }
    private void showStatus(){
        if(status!=null)status.setText("📍 "+WeatherRepository.city(this)
            +"\n"+WeatherRepository.display(this)+"\n⏰ Clock and 🔋 Battery read from this device.");
    }
    private void beginLocation(){
        if(Build.VERSION.SDK_INT>=23 &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},ASK_COARSE);
        }else{useLocation();}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=ASK_COARSE)return;
        if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)useLocation();
        else status.setText("Location not allowed. You can enter a city without granting permission.");
    }
    private void useLocation(){
        if(Build.VERSION.SDK_INT>=23 &&
             checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            status.setText("Location permission is required. Or enter a city.");return;
        }
        LocationManager manager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if(manager==null){status.setText("Phone location service unavailable. Enter city.");return;}
        String provider=LocationManager.NETWORK_PROVIDER;
        try {
            if(!manager.isProviderEnabled(provider)){
                status.setText("Enable Android Location, or enter a city name.");return;
            }
            Location cached=manager.getLastKnownLocation(provider);
            if(cached!=null && System.currentTimeMillis()-cached.getTime()<60*60*1000L){
                setPhoneLocation(cached);return;
            }
            status.setText("Getting approximate phone location…");
            returned.set(false);
            if(Build.VERSION.SDK_INT>=30){
                CancellationSignal signal=new CancellationSignal();
                manager.getCurrentLocation(provider,signal,getMainExecutor(),location->{
                    if(returned.compareAndSet(false,true)){
                        if(location==null)status.setText("No location available. Enter city name.");
                        else setPhoneLocation(location);
                    }
                });
                handler.postDelayed(()->{
                    if(returned.compareAndSet(false,true)){
                        signal.cancel();status.setText("Could not obtain location. Enter a city instead.");
                    }
                },13000);
            }else{
                LocationListener listener=new LocationListener(){
                    @Override public void onLocationChanged(Location location){
                        if(returned.compareAndSet(false,true))setPhoneLocation(location);
                    }
                };
                manager.requestSingleUpdate(provider,listener,Looper.getMainLooper());
                handler.postDelayed(()->{
                    if(returned.compareAndSet(false,true)){
                        manager.removeUpdates(listener);
                        status.setText("Could not obtain phone location. Enter a city instead.");
                    }
                },13000);
            }
        }catch(SecurityException | IllegalArgumentException ex){
            status.setText("Could not read device location. Enter a city instead.");
        }
    }
    private void setPhoneLocation(Location location){
        WeatherRepository.saveLocation(this,location.getLatitude(),location.getLongitude(),"My approximate area");
        showStatus();
        status.append("\nRefreshing current weather…");
    }
}
