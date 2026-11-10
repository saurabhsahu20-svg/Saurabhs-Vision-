package com.saurabhsvision.eduwalls;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** One fully functional configurable Android widget provider with 50 design presets. */
public final class VisionWidgetProvider extends AppWidgetProvider {
    public static final String PREFS="vision_widget_settings";
    private static final String ACTION_NEXT="com.saurabhsvision.eduwalls.NEXT_FACT";
    private static final int PENDING_FLAGS=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;

    public static void setStyle(Context context,int widgetId,int style){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
            .putInt("style_"+widgetId,WidgetCatalog.norm(style)).apply();
        updateWidget(context,AppWidgetManager.getInstance(context),widgetId);
    }
    public static void updateWidget(Context ctx,AppWidgetManager manager,int widgetId){
        SharedPreferences pref=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        int style=pref.getInt("style_"+widgetId,pref.getInt("pending",0));
        int category=style/10,accent=WidgetCatalog.accent(style);
        RemoteViews views=new RemoteViews(ctx.getPackageName(),R.layout.vision_widget);
        Bitmap bg=WidgetCatalog.background(style,500,280);
        views.setImageViewBitmap(R.id.widget_background,bg);
        views.setTextViewText(R.id.widget_title,WidgetCatalog.name(style));
        views.setTextViewText(R.id.widget_label,WidgetCatalog.subtitle(style));
        views.setTextColor(R.id.widget_clock,0xffffffff);
        views.setTextColor(R.id.widget_title,accent);
        views.setTextColor(R.id.widget_label,0xffffffff);
        views.setViewVisibility(R.id.widget_clock,category==0||category==1?View.VISIBLE:View.GONE);
        views.setViewVisibility(R.id.widget_label,View.VISIBLE);
        if(category==1){
            String date=new SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.widget_label,date);
        }else if(category==0){
            String date=new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.widget_label,date+"  •  "+WidgetCatalog.subtitle(style));
        }
        // Touching a widget cycles educational content in its own group.
        Intent next=new Intent(ctx,VisionWidgetProvider.class);
        next.setAction(ACTION_NEXT);
        next.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
        PendingIntent cycle=PendingIntent.getBroadcast(ctx,widgetId,next,PENDING_FLAGS);
        views.setOnClickPendingIntent(R.id.widget_background,cycle);
        views.setOnClickPendingIntent(R.id.widget_label,cycle);
        Intent open=new Intent(ctx,V5StudioActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openApp=PendingIntent.getActivity(ctx,widgetId+100000,open,PENDING_FLAGS);
        views.setOnClickPendingIntent(R.id.widget_title,openApp);
        manager.updateAppWidget(widgetId,views);
        // RemoteViews has now been serialized for the home-screen host.
        bg.recycle();
    }
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        for(int id:ids)updateWidget(context,manager,id);
    }
    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent);
        if(ACTION_NEXT.equals(intent.getAction())){
            int widgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
            if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID)return;
            SharedPreferences pref=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            int style=pref.getInt("style_"+widgetId,0), next=style/10*10+(style%10+1)%10;
            setStyle(context,widgetId,next);
        }
    }
    @Override public void onDeleted(Context context,int[] ids){
        SharedPreferences.Editor edit=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        for(int id:ids)edit.remove("style_"+id);
        edit.apply();
        super.onDeleted(context,ids);
    }
}
