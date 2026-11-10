package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.util.Locale;

/** User-facing premium 50+50+50 collection, preserving the v4 classic studio. */
public final class V5StudioActivity extends Activity {
    private static final int NAVY=0xff071b49, BLUE=0xff1746d2, GOLD=0xffffd54f;
    private static final int SAVE_PNG=5042;
    private final StillScenePainter stillPainter=new StillScenePainter();
    private final LiveScenePainter livePainter=new LiveScenePainter();
    private final BiologyPainter biologyPainter=new BiologyPainter();
    private static final int STATIC_COUNT=CollectionCatalog.COUNT+BiologyCatalog.COUNT;
    private static final int LIVE_COUNT=LiveSceneCatalog.COUNT+BiologyCatalog.COUNT;
    private SharedPreferences prefs;
    private LinearLayout gallery,actions;
    private TextView label,subtitle;
    private WallpaperPreview preview;
    private Button[] tabs=new Button[3];
    private boolean playing;
    private int mode=0,selectedWallpaper=0,selectedLive=0,selectedWidget=0;
    private long lastTap=0L;
    private float touchX=-1000f,touchY=-1000f,downX,downY;
    private long touchAt=0L;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        prefs=getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE);
        selectedWallpaper=Math.max(0,Math.min(STATIC_COUNT-1,prefs.getInt(EduWallpaperService.COLLECTION,0)));
        selectedWidget=getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).getInt("pending",0);
        selectedLive=Math.max(0,Math.min(LIVE_COUNT-1,prefs.getInt(EduWallpaperService.LIVE_ID,0)));
        buildUI();
    }
    private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
    private GradientDrawable round(int color,int r){
        GradientDrawable gd=new GradientDrawable();gd.setColor(color);gd.setCornerRadius(dp(r));return gd;
    }
    private TextView txt(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;
    }
    private void add(LinearLayout c,View v,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);c.addView(v,lp);
    }
    private Button btn(String name,int color,int textColor){
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);
        b.setBackground(round(color,12));b.setTextColor(textColor);
        b.setMinHeight(dp(48));b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return b;
    }
    private void buildUI(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(0xffeef3fc);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),0,dp(14),dp(26));sc.addView(root);setContentView(sc);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18),dp(21),dp(18),dp(20));head.setBackground(round(NAVY,18));
        add(head,txt("🎓  SAURABH'S VISION",14,GOLD,true),0);
        add(head,txt("VISION EDU WALLS 8.0",26,Color.WHITE,true),8);
        add(head,txt("66 Static  •  66 Live  •  60 Digital Widgets",14,0xffd8e8ff,true),9);
        add(head,txt("NEW: Premium 3D HD Art Studio • Moving Comets • Glass Widgets",12,0xffa9cafa,false),7);
        add(root,head,12);

        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"🖼️  66 WALLS","✨  66 LIVE","⏰  60 WIDGETS"};
        for(int k=0;k<3;k++){
            final int n=k;
            Button tab=btn(names[k],n==0?BLUE:Color.WHITE,n==0?Color.WHITE:NAVY);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1f);
            lp.setMargins(dp(2),0,dp(2),0);nav.addView(tab,lp);tabs[k]=tab;
            tab.setOnClickListener(v->showMode(n));
        }
        add(root,nav,18);

        Button premium=btn("🌌 OPEN PREMIUM 3D HD ART STUDIO",GOLD,NAVY);
        add(root,premium,14);
        premium.setMinHeight(dp(60));
        premium.setOnClickListener(v->openPremiumStudio());
        TextView premiumInfo=txt("Import the HD art pack once. Use glowing images as static wallpaper, animated live wallpaper, or as the background for real clock/date/weather/battery widgets.",12,0xff49668c,false);
        add(root,premiumInfo,8);

        label=txt("",21,NAVY,true);add(root,label,18);
        subtitle=txt("",12,0xff587091,false);add(root,subtitle,7);
        FrameLayout frame=new FrameLayout(this);frame.setClipToOutline(true);frame.setBackground(round(0xff040d28,18));
        preview=new WallpaperPreview();frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams f=new LinearLayout.LayoutParams(-1,dp(340));
        f.topMargin=dp(12);root.addView(frame,f);
        actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);
        add(root,actions,12);
        TextView heading=txt("CHOOSE YOUR DESIGN",19,NAVY,true);add(root,heading,24);
        gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);
        add(root,gallery,12);
        Button hd=btn("📦 Premium 3D HD Art Pack / Import ZIP",0xffe1efff,NAVY);
        add(root,hd,19);
        hd.setOnClickListener(v->openPremiumStudio());
        Button old=btn("📚 Open Classic Wallpapers (Version 4 Collection)",0xffe1eaff,NAVY);
        add(root,old,25);old.setOnClickListener(v->{prefs.edit().putBoolean(EduWallpaperService.V6_LIVE_ENABLED,false).apply();startActivity(new Intent(this,MainActivity.class));});
        TextView note=txt("Static: 50 educational posters + 16 new Biology diagrams. Live: 50 animations + 16 interactive Biology studies with touch labels. Widgets: 60 working digital styles, including 10 new Biology facts. These are original educational illustrations, not clinical medical images.",12,0xff587091,false);
        add(root,note,16);
        showMode(0);
    }
    private void openPremiumStudio(){
        prefs.edit().putBoolean(EduWallpaperService.V6_LIVE_ENABLED,false).apply();
        startActivity(new Intent(this,ImagePackActivity.class));
    }
    private void showMode(int m){
        mode=m;
        for(int k=0;k<tabs.length;k++){
            tabs[k].setBackground(round(k==mode?BLUE:Color.WHITE,12));
            tabs[k].setTextColor(k==mode?Color.WHITE:NAVY);
        }
        actions.removeAllViews();
        gallery.removeAllViews();
        if(mode==0){
            Button set=btn("🖼️ Apply Static Wallpaper (Home)",GOLD,NAVY);
            add(actions,set,0);set.setOnClickListener(v->setStill());
            Button save=btn("💾 Save Wallpaper as PNG",0xffdce7ff,NAVY);
            add(actions,save,8);save.setOnClickListener(v->exportStill());
        }else if(mode==1){
            Button apply=btn("✨ Set as LIVE Wallpaper",GOLD,NAVY);
            add(actions,apply,0);apply.setOnClickListener(v->setLive());
            TextView help=txt("66 live scenes, including 16 anatomy/botany models. Tap to reveal parts, double-tap/swipe to switch scenes where your launcher supports wallpaper gestures.",12,0xff47668f,false);
            add(actions,help,8);
        }else{
            Button pin=btn("📌 Add Selected Smart Widget to Home Screen",GOLD,NAVY);
            add(actions,pin,0);pin.setOnClickListener(v->pinWidget());
            Button weather=btn("☀ Set Weather City / Use Phone Location",0xffdce7ff,NAVY);
            add(actions,weather,8);
            Button hdWidget=btn("🌌 Choose Premium 3D Art for Digital Widgets",0xff1b3165,Color.WHITE);
            add(actions,hdWidget,8);hdWidget.setOnClickListener(v->openPremiumStudio());
            weather.setOnClickListener(v->startActivity(new Intent(this,WeatherSettingsActivity.class)));
            TextView hint=txt("All 60 widget styles use real Android digital time, date, battery percentage and optional Internet weather. In Premium 3D HD Studio choose any imported image as a widget background. Text remains live; it is not printed onto the artwork.",12,0xff47668f,false);
            add(actions,hint,8);
        }
        int count=mode==0?STATIC_COUNT:(mode==1?LIVE_COUNT:WidgetCatalog.COUNT);
        if(mode!=2){
            Button biology=btn("🧬 Jump to 16 New Biology Wallpapers",0xffd6f6e9,NAVY);
            add(actions,biology,9);
            biology.setOnClickListener(v->{
                if(mode==0)selectedWallpaper=CollectionCatalog.COUNT;
                else selectedLive=LiveSceneCatalog.COUNT;
                updateSelection();
            });
        }
        for(int start=0;start<count;start+=2){
            if(start==50 && mode!=2) {
                TextView section=txt("🧬 NEW BIOLOGY ATLAS · 16 ORGAN & PLANT STUDIES",16,NAVY,true);
                add(gallery,section,14);
            }else if(start==50){
                TextView section=txt("🧫 NEW BIOLOGY · 10 DIGITAL WIDGETS",16,NAVY,true);
                add(gallery,section,14);
            }
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=start;j<Math.min(start+2,count);j++){
                final int index=j;
                LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
                tile.setPadding(dp(5),dp(5),dp(5),dp(10));
                tile.setBackground(round(Color.WHITE,14));tile.setClickable(true);tile.setFocusable(true);
                if(mode==2){
                    TextView icon=txt("◷  "+WidgetCatalog.category(index),13,WidgetCatalog.accent(index),true);
                    icon.setGravity(Gravity.CENTER);
                    icon.setBackground(round(0xff101d40,11));
                    tile.addView(icon,new LinearLayout.LayoutParams(-1,dp(95)));
                    TextView sub=txt(WidgetCatalog.subtitle(index),11,0xff526789,false);
                    sub.setGravity(Gravity.CENTER);
                    tile.addView(sub,new LinearLayout.LayoutParams(-1,dp(48)));
                }else{
                    Miniature tiny=new Miniature(index);
                    tile.addView(tiny,new LinearLayout.LayoutParams(-1,dp(190)));
                }
                String name=mode==2?WidgetCatalog.name(index):
                    mode==1?(index>=LiveSceneCatalog.COUNT?BiologyCatalog.name(index-LiveSceneCatalog.COUNT):LiveSceneCatalog.name(index)):
                    (index>=CollectionCatalog.COUNT?BiologyCatalog.name(index-CollectionCatalog.COUNT):CollectionCatalog.name(index));
                TextView t=txt(String.format(Locale.US,"%02d. ",index+1)+name,12,NAVY,true);
                t.setGravity(Gravity.CENTER);
                t.setMaxLines(2);
                tile.addView(t,new LinearLayout.LayoutParams(-1,dp(41)));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
                lp.setMargins(dp(3),dp(3),dp(3),dp(5));row.addView(tile,lp);
                tile.setOnClickListener(v->{
                    if(mode==2)selectedWidget=index;else if(mode==1)selectedLive=index;else selectedWallpaper=index;
                    updateSelection();preview.invalidate();
                });
            }
            add(gallery,row,0);
        }
        updateSelection();
    }
    private void updateSelection(){
        if(mode==2){
            label.setText("⏰ "+WidgetCatalog.name(selectedWidget));
            subtitle.setText("Widget "+(selectedWidget+1)+" of "+WidgetCatalog.COUNT+"  ·  "+WidgetCatalog.category(selectedWidget));
        }else{
            if(mode==1){
                boolean biology=selectedLive>=LiveSceneCatalog.COUNT;
                label.setText("🌌 "+(biology?BiologyCatalog.name(selectedLive-LiveSceneCatalog.COUNT):LiveSceneCatalog.name(selectedLive)));
                subtitle.setText("Live animation "+(selectedLive+1)+" of "+LIVE_COUNT+" · "+(biology?"INTERACTIVE BIOLOGY":LiveSceneCatalog.group(selectedLive)));
            }else {
                boolean biology=selectedWallpaper>=CollectionCatalog.COUNT;
                label.setText("🎨 "+(biology?BiologyCatalog.name(selectedWallpaper-CollectionCatalog.COUNT):CollectionCatalog.name(selectedWallpaper)));
                subtitle.setText("Static artwork "+(selectedWallpaper+1)+" of "+STATIC_COUNT+" · "+(biology?"BIOLOGY ATLAS":CollectionCatalog.category(selectedWallpaper)));
            }
        }
        if(preview!=null)preview.invalidate();
    }
    private Bitmap makeStill(){
        Bitmap bmp=Bitmap.createBitmap(720,1280,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        paintStill(c,720,1280,selectedWallpaper);
        return bmp;
    }
    private void setStill(){
        Bitmap bmp=null;
        try {
            bmp=makeStill();
            WallpaperManager.getInstance(this).setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM);
            Toast.makeText(this,"Static wallpaper applied!",Toast.LENGTH_LONG).show();
        }catch(Exception ex){
            Toast.makeText(this,"Couldn't apply wallpaper. Try PNG export instead.",Toast.LENGTH_LONG).show();
        }finally{if(bmp!=null)bmp.recycle();}
    }
    private void exportStill(){
        try {
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_TITLE,"Vision_Edu_Wall_"+String.format(Locale.US,"%02d",selectedWallpaper+1)+".png");
            startActivityForResult(i,SAVE_PNG);
        }catch(Exception ex){Toast.makeText(this,"Save picker unavailable.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int r,int result,Intent data){
        super.onActivityResult(r,result,data);
        if(r!=SAVE_PNG||result!=RESULT_OK||data==null||data.getData()==null)return;
        Bitmap bmp=null;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
            if(out==null)throw new IllegalStateException("Cannot write");
            bmp=makeStill();
            if(!bmp.compress(Bitmap.CompressFormat.PNG,100,out))throw new IllegalStateException("Image export failed");
            Toast.makeText(this,"Saved original wallpaper PNG.",Toast.LENGTH_LONG).show();
        }catch(Exception ex){Toast.makeText(this,"Could not save this wallpaper.",Toast.LENGTH_LONG).show();}
        finally{if(bmp!=null)bmp.recycle();}
    }
    private void paintStill(Canvas c,int w,int h,int n){
        if(n>=CollectionCatalog.COUNT)
            biologyPainter.draw(c,w,h,n-CollectionCatalog.COUNT,false,0,-1000,-1000,0);
        else stillPainter.draw(c,w,h,n);
    }
    private void paintLive(Canvas c,int w,int h,int n,float sec,float x,float y,long at){
        if(n>=LiveSceneCatalog.COUNT)
            biologyPainter.draw(c,w,h,n-LiveSceneCatalog.COUNT,true,sec,x,y,at);
        else livePainter.draw(c,w,h,sec,n,x,y,at);
    }
    private String phoneBattery(){
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery==null)return "Battery unavailable";
        int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        if(level<0||scale<=0)return "Battery unavailable";
        return "🔋 Battery "+Math.round(level*100f/scale)+"%";
    }
    private void setLive(){
        prefs.edit().putInt(EduWallpaperService.LIVE_ID,selectedLive)
            .putBoolean(EduWallpaperService.V6_LIVE_ENABLED,true)
            .putInt(EduWallpaperService.COLLECTION,-1)
            .putInt(EduWallpaperService.POSTER,-1)
            .putBoolean(EduWallpaperService.CUSTOM_ART,false).apply();
        Intent intent=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,EduWallpaperService.class));
        try{startActivity(intent);}
        catch(Exception ex){
            try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(Exception ignored){Toast.makeText(this,"Use Android Wallpapers > Vision Edu Walls.",Toast.LENGTH_LONG).show();}
        }
    }
    private void pinWidget(){
        getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).edit().putInt("pending",selectedWidget).apply();
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(Build.VERSION.SDK_INT>=26 && manager.isRequestPinAppWidgetSupported()){
            boolean accepted=manager.requestPinAppWidget(new ComponentName(this,VisionWidgetProvider.class),null,null);
            if(!accepted)Toast.makeText(this,"Home screen cannot pin widgets. Long-press Home Screen > Widgets.",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"Long-press Home Screen > Widgets > Vision Edu Walls > select a style.",Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onResume(){super.onResume();playing=true;if(preview!=null)preview.invalidate();}
    @Override protected void onPause(){playing=false;super.onPause();}
    private final class Miniature extends View{
        private final int index;
        Miniature(int n){super(V5StudioActivity.this);index=n;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(mode==1)paintLive(c,getWidth(),getHeight(),index,7.5f,-1000,-1000,0L);
            else paintStill(c,getWidth(),getHeight(),index);
        }
    }
    private final class WallpaperPreview extends View{
        WallpaperPreview(){super(V5StudioActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(mode==2){
                Bitmap bmp=PremiumWidgetArt.background(V5StudioActivity.this,Math.max(100,getWidth()),Math.max(100,getHeight()));
                if(bmp==null)bmp=WidgetCatalog.background(selectedWidget,Math.max(1,getWidth()),Math.max(1,getHeight()));
                c.drawBitmap(bmp,null,new android.graphics.RectF(0,0,getWidth(),getHeight()),
                    new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
                bmp.recycle();
                Paint p=new Paint(3);p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(dp(18));
                c.drawText(WidgetCatalog.name(selectedWidget),dp(16),getHeight()*.28f,p);
                p.setTextSize(dp(36));
                c.drawText(new java.text.SimpleDateFormat("h:mm a",Locale.getDefault()).format(new java.util.Date()),dp(18),getHeight()*.48f,p);
                p.setTextSize(dp(15));
                c.drawText(new java.text.SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new java.util.Date()),dp(18),getHeight()*.57f,p);
                c.drawText(WeatherRepository.display(V5StudioActivity.this),dp(18),getHeight()*.66f,p);
                c.drawText(phoneBattery(),dp(18),getHeight()*.74f,p);
                c.drawText(WidgetCatalog.subtitle(selectedWidget),dp(18),getHeight()*.82f,p);
                if(playing)postInvalidateDelayed(60000);
            }else {
                if(mode==1){
                    paintLive(c,getWidth(),getHeight(),selectedLive,SystemClock.uptimeMillis()/1000f,
                        touchX,touchY,touchAt);
                    if(playing)postInvalidateDelayed(52);
                }else{
                    paintStill(c,getWidth(),getHeight(),selectedWallpaper);
                }
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(mode==2)return super.onTouchEvent(e);
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                float dx=e.getX()-downX;
                if(Math.abs(dx)>dp(55)){
                    if(mode==1)selectedLive=(selectedLive+(dx<0?1:LIVE_COUNT-1))%LIVE_COUNT;
                    else selectedWallpaper=(selectedWallpaper+(dx<0?1:STATIC_COUNT-1))%STATIC_COUNT;
                    updateSelection();
                }else if(mode==1){
                    touchX=e.getX();touchY=e.getY();touchAt=SystemClock.uptimeMillis();
                    if(touchAt-lastTap<350){selectedLive=(selectedLive+1)%LIVE_COUNT;updateSelection();}
                    lastTap=touchAt;
                }
                invalidate();performClick();return true;
            }
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
