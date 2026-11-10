package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
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
    private final CollectionPainter painter=new CollectionPainter();
    private SharedPreferences prefs;
    private LinearLayout gallery,actions;
    private TextView label,subtitle;
    private WallpaperPreview preview;
    private Button[] tabs=new Button[3];
    private boolean playing;
    private int mode=0,selectedWallpaper=0,selectedWidget=0;
    private long lastTap=0L;
    private float touchX=-1000f,touchY=-1000f,downX,downY;
    private long touchAt=0L;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        prefs=getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE);
        selectedWallpaper=Math.max(0,Math.min(CollectionCatalog.COUNT-1,prefs.getInt(EduWallpaperService.COLLECTION,0)));
        selectedWidget=getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).getInt("pending",0);
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
        add(head,txt("VISION EDU WALLS 5.0",26,Color.WHITE,true),8);
        add(head,txt("50 Wallpapers  •  50 Live Presets  •  50 Widgets",14,0xffd8e8ff,true),9);
        add(head,txt("Offline educational designs • Tap effects • Free",12,0xffa9cafa,false),7);
        add(root,head,12);

        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"🖼️  50 WALLS","✨  50 LIVE","⏰  50 WIDGETS"};
        for(int k=0;k<3;k++){
            final int n=k;
            Button tab=btn(names[k],n==0?BLUE:Color.WHITE,n==0?Color.WHITE:NAVY);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1f);
            lp.setMargins(dp(2),0,dp(2),0);nav.addView(tab,lp);tabs[k]=tab;
            tab.setOnClickListener(v->showMode(n));
        }
        add(root,nav,18);

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
        Button hd=btn("📦 IMPORT ALL 50 HD WALLPAPERS FROM ZIP",0xffe1efff,NAVY);
        add(root,hd,19);
        hd.setOnClickListener(v->startActivity(new Intent(this,ImagePackActivity.class)));
        Button old=btn("📚 Open Classic Wallpapers (Version 4 Collection)",0xffe1eaff,NAVY);
        add(root,old,25);old.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
        TextView note=txt("Original poster-inspired artwork. The 50 live entries are motion variants of the 50 built-in static compositions; widgets offer 50 configurable themes from one Android widget provider. Some launchers do not forward wallpaper swipes.",12,0xff587091,false);
        add(root,note,16);
        showMode(0);
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
            TextView help=txt("Tap the wallpaper to trigger sparks; double tap or swipe in your launcher to change scenes if supported.",12,0xff47668f,false);
            add(actions,help,8);
        }else{
            Button pin=btn("📌 Add Selected Widget to Home Screen",GOLD,NAVY);
            add(actions,pin,0);pin.setOnClickListener(v->pinWidget());
            TextView hint=txt("Widgets contain working time/date displays, education facts and tappable study messages. Choose a design, then use the Android launcher to add the widget.",12,0xff47668f,false);
            add(actions,hint,8);
        }
        for(int start=0;start<50;start+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=start;j<start+2;j++){
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
                String name=mode==2?WidgetCatalog.name(index):CollectionCatalog.name(index);
                TextView t=txt(String.format(Locale.US,"%02d. ",index+1)+name,12,NAVY,true);
                t.setGravity(Gravity.CENTER);
                t.setMaxLines(2);
                tile.addView(t,new LinearLayout.LayoutParams(-1,dp(41)));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
                lp.setMargins(dp(3),dp(3),dp(3),dp(5));row.addView(tile,lp);
                tile.setOnClickListener(v->{
                    if(mode==2)selectedWidget=index;else selectedWallpaper=index;
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
            subtitle.setText("Widget "+(selectedWidget+1)+" of 50  ·  "+WidgetCatalog.category(selectedWidget));
        }else{
            label.setText("✨ "+CollectionCatalog.name(selectedWallpaper));
            subtitle.setText((mode==0?"Static wallpaper ":"Live wallpaper ")+(selectedWallpaper+1)+" of 50  ·  "+CollectionCatalog.category(selectedWallpaper));
        }
        if(preview!=null)preview.invalidate();
    }
    private Bitmap makeStill(){
        Bitmap bmp=Bitmap.createBitmap(720,1280,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        painter.draw(c,720,1280,0,selectedWallpaper,false,-1000,-1000,0L);
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
    private void setLive(){
        prefs.edit().putInt(EduWallpaperService.COLLECTION,selectedWallpaper)
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
            painter.draw(c,getWidth(),getHeight(),0,index,false,-1000,-1000,0);
        }
    }
    private final class WallpaperPreview extends View{
        WallpaperPreview(){super(V5StudioActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(mode==2){
                Bitmap bmp=WidgetCatalog.background(selectedWidget,Math.max(1,getWidth()),Math.max(1,getHeight()));
                c.drawBitmap(bmp,0,0,new Paint(Paint.ANTI_ALIAS_FLAG));bmp.recycle();
                Paint p=new Paint(3);p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(27));
                c.drawText(WidgetCatalog.name(selectedWidget),dp(20),getHeight()*.58f,p);
                p.setTextSize(dp(15));
                c.drawText(WidgetCatalog.subtitle(selectedWidget),dp(20),getHeight()*.68f,p);
            }else {
                painter.draw(c,getWidth(),getHeight(),SystemClock.uptimeMillis()/1000f,selectedWallpaper,mode==1,touchX,touchY,touchAt);
                if(playing&&mode==1)postInvalidateDelayed(52);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(mode==2)return super.onTouchEvent(e);
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                float dx=e.getX()-downX;
                if(Math.abs(dx)>dp(55)){
                    selectedWallpaper=(selectedWallpaper+(dx<0?1:49))%50;
                    updateSelection();
                }else if(mode==1){
                    touchX=e.getX();touchY=e.getY();touchAt=SystemClock.uptimeMillis();
                    if(touchAt-lastTap<350){selectedWallpaper=(selectedWallpaper+1)%50;updateSelection();}
                    lastTap=touchAt;
                }
                invalidate();performClick();return true;
            }
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
