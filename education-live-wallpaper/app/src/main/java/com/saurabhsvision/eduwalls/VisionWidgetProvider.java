package com.saurabhsvision.eduwalls;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 160 configurable Android dashboards: 60 classic + 100 built-in 3D artwork skins.
 * All styles show genuine device time, date and battery plus optional weather.
 * Time is rendered continuously by Android's system TextClock.
 */
public final class VisionWidgetProvider extends AppWidgetProvider {
    public static final String PREFS="vision_widget_settings";
    private static final String NEXT="com.saurabhsvision.eduwalls.NEXT_FACT";
    private static final String REFRESH="com.saurabhsvision.eduwalls.REFRESH_SMART_WIDGETS";
    private static final int FLAGS=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;

    public static void setStyle(Context context,int id,int style){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putInt("style_"+id,WidgetCatalog.norm(style)).apply();
        updateWidget(context,AppWidgetManager.getInstance(context),id);
    }
    /** Each Home Screen widget keeps its own chosen built-in artwork. */
    public static void setWidget(Context context,int id,int style,int art){
        SharedPreferences.Editor editor=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putInt("style_"+id,WidgetCatalog.norm(style));
        if(BuiltIn3DWidgetArt.valid(art))editor.putInt("art_"+id,art);
        else editor.remove("art_"+id);
        editor.apply();
        updateWidget(context,AppWidgetManager.getInstance(context),id);
    }
    public static void updateAll(Context context){
        Context app=context.getApplicationContext();
        AppWidgetManager manager=AppWidgetManager.getInstance(app);
        int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(app,VisionWidgetProvider.class));
        if(ids!=null)for(int id:ids)updateWidget(app,manager,id);
    }
    private static String battery(Context ctx){
        Intent battery=ctx.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery==null)return "🔋 Battery --";
        int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        if(level<0 || scale<=0)return "🔋 Battery --";
        int value=Math.round(level*100f/scale);
        int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
        boolean charge=status==BatteryManager.BATTERY_STATUS_CHARGING ||
                       status==BatteryManager.BATTERY_STATUS_FULL;
        return (charge?"⚡ ":"🔋 ")+value+"%";
    }
    public static void updateWidget(Context ctx,AppWidgetManager manager,int widgetId){
        SharedPreferences pref=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        int style=WidgetCatalog.norm(pref.getInt("style_"+widgetId,pref.getInt("pending",0)));
        int category=style/10, accent=WidgetCatalog.accent(style);
        RemoteViews views=new RemoteViews(ctx.getPackageName(),R.layout.vision_widget);
        // Per-widget 3D artwork takes precedence, so two widgets can use DIFFERENT
        // images from the 100-picture gallery without replacing each other.
        int art=pref.getInt("art_"+widgetId,-1);
        Bitmap bg=BuiltIn3DWidgetArt.background(ctx,art,480,280);
        if(bg==null)bg=PremiumWidgetArt.background(ctx,480,280);
        if(bg==null)bg=WidgetCatalog.background(style,480,280);
        views.setImageViewBitmap(R.id.widget_background,bg);
        views.setTextViewText(R.id.widget_title,
            BuiltIn3DWidgetArt.valid(art)?BuiltIn3DWidgetArt.title(ctx,art):WidgetCatalog.name(style));
        views.setTextColor(R.id.widget_title,accent);
        views.setTextViewText(R.id.widget_city,WeatherRepository.city(ctx));
        views.setTextViewText(R.id.widget_weather,WeatherRepository.display(ctx));
        views.setTextViewText(R.id.widget_battery,battery(ctx));
        views.setTextViewText(R.id.widget_date,new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new Date()));
        views.setTextViewText(R.id.widget_label,WidgetCatalog.subtitle(style));
        views.setViewVisibility(R.id.widget_clock,View.VISIBLE);
        views.setTextColor(R.id.widget_label,0xffffffff);
        views.setTextColor(R.id.widget_date,0xffffffff);
        // Make all 50 styles show time, battery, weather and date. Their title,
        // background and learning message remain individual to each style.
        Intent rotate=new Intent(ctx,VisionWidgetProvider.class);
        rotate.setAction(NEXT);
        rotate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
        views.setOnClickPendingIntent(R.id.widget_label,
            PendingIntent.getBroadcast(ctx,widgetId,rotate,FLAGS));
        Intent weather=new Intent(ctx,WeatherSettingsActivity.class);
        weather.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.widget_weather,
            PendingIntent.getActivity(ctx,widgetId+100000,weather,FLAGS));
        views.setOnClickPendingIntent(R.id.widget_city,
            PendingIntent.getActivity(ctx,widgetId+100000,weather,FLAGS));
        Intent open=new Intent(ctx,Premium100Activity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("edit_widget_id",widgetId);
        views.setOnClickPendingIntent(R.id.widget_title,
            PendingIntent.getActivity(ctx,widgetId+200000,open,FLAGS));
        Intent refresh=new Intent(ctx,VisionWidgetProvider.class);
        refresh.setAction(REFRESH);
        views.setOnClickPendingIntent(R.id.widget_battery,
            PendingIntent.getBroadcast(ctx,widgetId+300000,refresh,FLAGS));
        manager.updateAppWidget(widgetId,views);
        bg.recycle(); // release only after the RemoteViews bitmap has been submitted
    }
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        for(int id:ids)updateWidget(context,manager,id);
        WeatherRepository.refresh(context,false);
        schedule(context);
    }
    @Override public void onEnabled(Context context){super.onEnabled(context);schedule(context);}
    @Override public void onDisabled(Context context){cancel(context);super.onDisabled(context);}
    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent);
        String action=intent.getAction();
        if(NEXT.equals(action)){
            int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
            if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return;
            SharedPreferences pref=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            int style=WidgetCatalog.norm(pref.getInt("style_"+id,0));
            int next=style/10*10+(style%10+1)%10;
            setStyle(context,id,next);
        }else if(REFRESH.equals(action)){
            updateAll(context);
            WeatherRepository.refresh(context,false);
        }else if(Intent.ACTION_BOOT_COMPLETED.equals(action)){
            schedule(context);
            updateAll(context);
            WeatherRepository.refresh(context,false);
        }else if(Intent.ACTION_POWER_CONNECTED.equals(action)||Intent.ACTION_POWER_DISCONNECTED.equals(action)){
            updateAll(context);
        }
    }
    private static PendingIntent alarmIntent(Context ctx){
        Intent intent=new Intent(ctx,VisionWidgetProvider.class);
        intent.setAction(REFRESH);
        return PendingIntent.getBroadcast(ctx,905, intent,FLAGS);
    }
    private static void schedule(Context ctx){
        AlarmManager alarm=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(alarm!=null){
            alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime()+15*60*1000L,
                15*60*1000L,alarmIntent(ctx));
        }
    }
    private static void cancel(Context ctx){
        AlarmManager alarm=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(alarm!=null)alarm.cancel(alarmIntent(ctx));
    }
    @Override public void onDeleted(Context ctx,int[] ids){
        SharedPreferences.Editor editor=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        for(int id:ids){
            editor.remove("style_"+id);
            editor.remove("art_"+id);
        }
        editor.apply();
        super.onDeleted(ctx,ids);
    }
}
