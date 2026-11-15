package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Fully bundled offline school dictionary with definitions, Hindi, and TTS. */
public final class DictionaryActivity extends Activity {
    private static final int BACK=0xff081629, PANEL=0xff132a42, INK=0xffeef9ff,
        MUTED=0xffb0c8d8, CYAN=0xff5be7ec;
    private List<DictionaryCore.Entry> dictionary;
    private LinearLayout results;
    private TextToSpeech voice;
    private EditText search;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable refresh;

    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BACK);getWindow().setNavigationBarColor(BACK);
        try(InputStream input=getAssets().open("dictionary.tsv")){
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
            while((n=input.read(chunk))!=-1)buffer.write(chunk,0,n);
            dictionary=DictionaryCore.parse(buffer.toString(StandardCharsets.UTF_8.name()));
        }catch(Exception e){dictionary=java.util.Collections.emptyList();}
        voice=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS&&voice!=null)voice.setLanguage(Locale.ENGLISH);});
        LinearLayout base=new LinearLayout(this);base.setOrientation(LinearLayout.VERTICAL);
        base.setPadding(dp(18),dp(15),dp(18),dp(12));base.setBackgroundColor(BACK);setContentView(base);
        TextView title=text("‹   Dictionary",26,INK,true);title.setPadding(0,dp(12),0,dp(7));base.addView(title);
        title.setOnClickListener(v->finish());
        TextView caption=text("OFFLINE  •  ENGLISH + HINDI  •  STUDY WORDS",11,CYAN,true);
        base.addView(caption);
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(18);
        search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("Search a word...");
        search.setBackground(bg(PANEL,18));search.setPadding(dp(15),0,dp(13),0);
        LinearLayout.LayoutParams inputP=new LinearLayout.LayoutParams(-1,dp(57));inputP.topMargin=dp(16);
        base.addView(search,inputP);
        TextView count=text(dictionary.size()+" built-in words • Tap a result for meaning",12,MUTED,false);
        count.setPadding(0,dp(12),0,dp(8));base.addView(count);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        base.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int af){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                if(refresh!=null)handler.removeCallbacks(refresh);
                final String q=s.toString();refresh=()->render(q);handler.postDelayed(refresh,90);
            }
            public void afterTextChanged(Editable e){}
        });
        String prefill=getIntent().getStringExtra("word");
        if(prefill!=null)search.setText(prefill.replaceAll("[^\\p{L} -]"," ").trim());
        render(search.getText().toString());
    }
    private void render(String input){
        if(isFinishing()||results==null)return;
        results.removeAllViews();
        List<DictionaryCore.Entry> items=DictionaryCore.search(dictionary,input,60);
        if(items.isEmpty()){
            TextView empty=text("No offline match yet. Try another English word.",15,MUTED,false);
            empty.setPadding(dp(10),dp(28),dp(10),dp(10));results.addView(empty);return;
        }
        for(DictionaryCore.Entry word:items){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(15),dp(13),dp(14),dp(13));row.setBackground(bg(PANEL,17));
            TextView heading=text(word.word,19,INK,true);row.addView(heading);
            TextView desc=text(word.meaning,13,MUTED,false);desc.setMaxLines(2);
            LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,-2);dp1.topMargin=dp(5);row.addView(desc,dp1);
            TextView hin=text(word.hindi+"   •   "+word.category,12,CYAN,false);
            LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(-1,-2);dp2.topMargin=dp(5);row.addView(hin,dp2);
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(9);
            results.addView(row,layout);row.setOnClickListener(v->showWord(word));
        }
    }
    private void showWord(DictionaryCore.Entry e){
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle(e.word+"  •  "+e.category)
            .setMessage(e.meaning+"\n\nHindi: "+e.hindi)
            .setPositiveButton("Pronounce",(d,w)->speak(e.word))
            .setNegativeButton("Close",null).create();
        dialog.show();
    }
    private void speak(String word){
        if(voice!=null)voice.speak(word,TextToSpeech.QUEUE_FLUSH,null,"dictionary_word");
    }
    @Override protected void onDestroy(){
        if(refresh!=null)handler.removeCallbacks(refresh);
        if(voice!=null){voice.stop();voice.shutdown();voice=null;}
        super.onDestroy();
    }
}
