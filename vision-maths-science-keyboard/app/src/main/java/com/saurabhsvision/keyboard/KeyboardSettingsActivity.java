package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Original, accessible S Board controls; no third-party account or sync. */
public final class KeyboardSettingsActivity extends Activity {
    private static final int BG=0xff10151d, PANEL=0xff1e2632, TEXT=0xffeff6ff,
        MUTED=0xffb3c3d6, BLUE=0xff6b9cff;
    private final String PREFS="vision_keyboard_settings";
    private SharedPreferences prefs;
    private LinearLayout content;
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable box(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);
        g.setCornerRadius(dp(radius));return g;
    }
    private TextView label(String text,int sp,boolean bold,int color){
        TextView t=new TextView(this);t.setText(text);t.setTextSize(sp);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        LinearLayout frame=new LinearLayout(this);
        frame.setBackgroundColor(BG);frame.setOrientation(LinearLayout.VERTICAL);
        setContentView(frame);
        TextView header=label("‹    S Board settings",24,true,TEXT);
        header.setPadding(dp(20),dp(6),0,0);
        header.setOnClickListener(v->finish());
        frame.addView(header,new LinearLayout.LayoutParams(-1,dp(70)));
        ScrollView scroll=new ScrollView(this);
        frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14),dp(8),dp(14),dp(25));
        scroll.addView(content);
        section("⌨  Layout & keys");
        toggle("Number row","Always show 1–0 above the letters",
            "number_row",false);
        toggle("Long-press symbols","Hold letters for extra symbols; normal typing stays fast",
            "long_press_symbols",false);
        toggle("Accented characters","Hold a vowel to enter an accented letter",
            "accented_characters",true);
        toggle("Arrow keys","Show left and right cursor arrows on the ABC bottom row",
            "arrow_keys",true);
        toggle("Key pop-ups","Show an enlarged key label while pressing",
            "key_popups",true);
        toggle("Large key text","Make letters on keycaps easier to read",
            "large_key_text",false);
        seek("Long-press duration","Choose between 250 and 700 milliseconds",
            "long_press_duration",450,250,700);
        section("🎵  Sound & vibration");
        toggle("Key sound","Optional sound feedback on a typed key",
            "key_sound",false);
        toggle("Key vibration","Optional vibration; disabled by default for faster typing",
            "key_haptics",false);
        section("🎨  Themes and stickers");
        navigation("Explore Themes & Create a Photo Theme",
            "35+ free original designs, previews and your own photo",ThemeGalleryActivity.class);
        TextView stickers=label("Open the 🧩 tab on S Board for offline original stickers.",13,false,MUTED);
        pad(stickers,12);
        content.addView(stickers);
        section("🔐  Privacy");
        TextView privacy=label(
            "No sign-in is needed. Text typing is not uploaded. Clipboard history is opt-in, encrypted and has a Clear control in S Board. Speech dictation uses your Android recognition provider only after you tap the microphone.",13,false,MUTED);
        pad(privacy,14);content.addView(privacy);
        TextView footer=label("Changes apply when you close and reopen S Board.",13,false,BLUE);
        pad(footer,20);content.addView(footer);
    }
    private void pad(TextView v,int top){
        v.setPadding(dp(10),dp(top),dp(10),dp(7));
    }
    private void section(String name){
        TextView heading=label(name,18,true,TEXT);
        heading.setPadding(dp(8),dp(19),dp(8),dp(13));
        content.addView(heading);
    }
    private void toggle(String title,String detail,String key,boolean initial){
        LinearLayout boxView=new LinearLayout(this);
        boxView.setGravity(Gravity.CENTER_VERTICAL);
        boxView.setPadding(dp(14),dp(10),dp(14),dp(10));
        boxView.setBackground(box(PANEL,15));
        LinearLayout text=new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView heading=label(title,16,true,TEXT);
        TextView description=label(detail,12,false,MUTED);
        description.setPadding(0,dp(3),0,0);
        text.addView(heading);
        text.addView(description);
        boxView.addView(text,new LinearLayout.LayoutParams(0,-2,1f));
        Switch sw=new Switch(this);
        sw.setContentDescription(title);
        sw.setChecked(prefs.getBoolean(key,initial));
        sw.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key,on).apply());
        boxView.addView(sw);
        boxView.setOnClickListener(v->sw.setChecked(!sw.isChecked()));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,dp(9));
        content.addView(boxView,lp);
    }
    private void seek(String title,String detail,String key,int initial,int min,int max){
        LinearLayout block=new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(14),dp(12),dp(14),dp(11));
        block.setBackground(box(PANEL,15));
        int value=Math.max(min,Math.min(max,prefs.getInt(key,initial)));
        TextView heading=label(title+" — "+value+" ms",16,true,TEXT);
        block.addView(heading);
        block.addView(label(detail,12,false,MUTED));
        SeekBar slider=new SeekBar(this);
        slider.setMax(max-min);
        slider.setProgress(value-min);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar v,int progress,boolean fromUser){
                int ms=min+progress;
                heading.setText(title+" — "+ms+" ms");
                if(fromUser)prefs.edit().putInt(key,ms).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar v){}
            @Override public void onStopTrackingTouch(SeekBar v){}
        });
        block.addView(slider);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(9);content.addView(block,lp);
    }
    private void navigation(String title,String detail,Class<?> target){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(title+"\n"+detail);b.setTextSize(14);
        b.setTextColor(Color.WHITE);b.setBackground(box(0xff29549e,14));
        b.setOnClickListener(v->startActivity(new Intent(this,target)));
        content.addView(b,new LinearLayout.LayoutParams(-1,dp(76)));
    }
}
