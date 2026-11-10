package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int NAVY=0xff071b49,BLUE=0xff1746d2,GOLD=0xffffd700,INK=0xff172b52,MUTED=0xff56698b;
    private static final String[] TITLES={"Maths Galaxy","Science Lab","Cosmic Classroom","Junior Learning"};
    private static final String[] SUBS={"Formulas • geometry • algebra","Atoms • molecules • physics","Planets • stars • exploration","Letters • numbers • happy learning"};
    private final Button[] themeButtons=new Button[4];
    private SharedPreferences prefs;
    private Preview preview;
    private TextView speedLabel;
    private int theme=0,speed=1;
    private boolean interactive=true,customArt=false;
    private static final int PICK_ART=8021;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        prefs=getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE);
        theme=prefs.getInt(EduWallpaperService.THEME,0);
        speed=prefs.getInt(EduWallpaperService.SPEED,1);
        interactive=prefs.getBoolean(EduWallpaperService.INTERACTIVE,true);
        customArt=prefs.getBoolean(EduWallpaperService.CUSTOM_ART,false);
        buildUI();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable rounded(int fill,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(2),stroke);
        return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        v.setGravity(Gravity.START);return v;
    }
    private LinearLayout stack(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private void addSpace(LinearLayout root,int h){View v=new View(this);root.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void add(LinearLayout container, View child, int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(top);container.addView(child,p);
    }
    private Button button(String label,int bg,int fg){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(rounded(bg,16,0));
        b.setMinHeight(dp(52));b.setPadding(dp(18),dp(10),dp(18),dp(10));return b;
    }
    private void buildUI(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff1f5fd);
        LinearLayout all=stack();scroll.addView(all);setContentView(scroll);
        LinearLayout top=stack();top.setBackgroundColor(NAVY);top.setPadding(dp(20),dp(28),dp(20),dp(26));
        add(top,text("✦ SAURABH’S VISION™",14,GOLD,true),0);
        add(top,text("VISION EDU WALLS 3.0",28,Color.WHITE,true),8);
        add(top,text("पढ़ाई की दुनिया, अब आपके फ़ोन की स्क्रीन पर!",15,0xffdbe9ff,false),8);
        add(top,text("HD Art Studio • Live Particles • Touch Magic • Offline",12,0xffb9d7f5,false),12);
        all.addView(top);
        LinearLayout body=stack();body.setPadding(dp(16),dp(20),dp(16),dp(34));all.addView(body);

        FrameLayout frame=new FrameLayout(this);frame.setBackground(rounded(NAVY,25,0));frame.setClipToOutline(true);
        preview=new Preview();frame.addView(preview,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(340));
        body.addView(frame,fp);
        add(body,text("👆 Touch the preview for knowledge sparks · Swipe horizontally to change theme.",13,MUTED,false),10);

        Button apply=button("✨ Set as Live Wallpaper",GOLD,NAVY);
        add(body,apply,16);
        apply.setOnClickListener(v->applyWallpaper());
        add(body,text("Choose a wallpaper theme",20,INK,true),24);
        add(body,text("Themes update your active wallpaper automatically.",13,MUTED,false),6);

        for(int i=0;i<4;i++){
            final int n=i;Button b=button((i+1)+".  "+TITLES[i]+"   ·   "+SUBS[i],0xfff8fbff,INK);
            b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            b.setTextSize(13);
            themeButtons[i]=b;add(body,b,9);
            b.setOnClickListener(v->selectTheme(n));
        }
        refreshSelected();
        add(body,text("Animation speed",20,INK,true),22);
        speedLabel=text("",14,MUTED,false);add(body,speedLabel,7);refreshSpeed();
        SeekBar slider=new SeekBar(this);slider.setMax(2);slider.setProgress(Math.max(0,Math.min(2,speed)));
        add(body,slider,8);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                speed=value;prefs.edit().putInt(EduWallpaperService.SPEED,speed).apply();refreshSpeed();preview.invalidate();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        Switch gestures=new Switch(this);
        gestures.setText("✨ Touch magic & gesture effects");
        gestures.setTextSize(15);gestures.setTextColor(INK);
        gestures.setChecked(interactive);
        gestures.setPadding(dp(10),dp(12),dp(12),dp(12));
        gestures.setBackground(rounded(Color.WHITE,15,0xffdbe4f4));
        add(body,gestures,18);
        gestures.setOnCheckedChangeListener((b,checked)->{
            interactive=checked;
            prefs.edit().putBoolean(EduWallpaperService.INTERACTIVE,checked).apply();
            preview.invalidate();
        });
        add(body,text("Tap for particles + a fun fact. Double-tap or swipe the wallpaper to switch worlds where your launcher supports touch gestures.",12,MUTED,false),10);
        LinearLayout privacy=stack();privacy.setPadding(dp(17),dp(14),dp(17),dp(14));
        privacy.setBackground(rounded(0xffe7efff,16,0));add(privacy,text("🔒 Your privacy matters",15,INK,true),0);
        add(privacy,text("No internet permission, no account and no personal data collection. Wallpapers are drawn directly on your phone. Animation pauses when hidden.",12,MUTED,false),8);
        add(body,privacy,19);
        add(body,text("Battery mode: animations slow automatically when Android Battery Saver is on. Your wallpaper pauses when hidden.",12,MUTED,false),13);
    }
    private void selectTheme(int selected){
        theme=Math.max(0,Math.min(3,selected));
        prefs.edit().putInt(EduWallpaperService.THEME,theme).apply();
        refreshSelected();
        if(preview!=null)preview.invalidate();
    }
    private void refreshSelected(){
        for(int i=0;i<themeButtons.length;i++) {
            themeButtons[i].setBackground(rounded(i==theme?0xffe3edff:Color.WHITE,14,i==theme?BLUE:0xffdbe4f4));
        }
    }
    private void refreshSpeed(){
        if(speedLabel!=null)speedLabel.setText(new String[]{"Slow • battery friendly","Normal • balanced","Fast • more movement"}[Math.max(0,Math.min(2,speed))]);
    }
    private void applyWallpaper(){
        Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,EduWallpaperService.class));
        try {startActivity(i);}
        catch(Exception ex) {
            try {startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(Exception ignored) {
                Toast.makeText(this,"Open Android Wallpapers and choose Vision Edu Walls.",Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override protected void onResume(){
        super.onResume();
        theme=prefs.getInt(EduWallpaperService.THEME,theme);
        refreshSelected();
        if(preview!=null)preview.begin();
    }
    @Override protected void onPause(){if(preview!=null)preview.end();super.onPause();}

    private final class Preview extends View {
        private final EducationPainter painter=new EducationPainter(MainActivity.this);
        private boolean active;
        private float downX,downY,touchX=-1000f,touchY=-1000f;
        private long touchAt=0L,lastTapAt=0L;
        Preview(){
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setClickable(true);
            setContentDescription("Interactive Education Live Wallpaper preview. Tap for sparks or swipe left and right to change themes.");
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            painter.draw(c,getWidth(),getHeight(),SystemClock.uptimeMillis()/1000f,
                theme,speed,touchX,touchY,touchAt,interactive,0f,customArt);
            if(active)postInvalidateDelayed(speed==0?90:(speed==2?38:54));
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(!interactive)return super.onTouchEvent(e);
            float x=e.getX(),y=e.getY();
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                downX=x;downY=y;
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE){
                if(Math.abs(x-downX)>dp(28) && Math.abs(x-downX)>Math.abs(y-downY)*1.3f){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
            if(action==MotionEvent.ACTION_UP){
                float dx=x-downX,dy=y-downY;
                if(Math.abs(dx)>dp(64)&&Math.abs(dx)>Math.abs(dy)*1.4f){
                    selectTheme((theme+(dx<0?1:3))%4);
                } else if(Math.abs(dx)<dp(45)&&Math.abs(dy)<dp(45)){
                    long now=SystemClock.uptimeMillis();
                    if(now-lastTapAt<330){
                        selectTheme((theme+1)%4);
                        lastTapAt=0L;
                    }else lastTapAt=now;
                    touchX=x;touchY=y;touchAt=now;
                }
                invalidate();
                performClick();
                return true;
            }
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        void begin(){active=true;invalidate();}
        void end(){active=false;}
    }
}
