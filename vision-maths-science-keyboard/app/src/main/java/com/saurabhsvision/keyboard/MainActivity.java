package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

/** Native onboarding, theme settings and typing-test screen for the Android IME. */
public final class MainActivity extends Activity {
    private static final String PREFS="vision_keyboard_settings";
    private static final int NAVY=0xff061631;
    private static final int GOLD=0xffffd45c;
    private static final int[] ACCENTS={0xff65e9ff,0xffffd55c,0xff9affd4,0xffeea8ff,0xff2563eb};
    private static final String[] NAMES={"Cosmic Blue","Golden Mathematics","Science Lab","Purple Quantum","Pure White"};
    private LinearLayout root;
    private TextView themeStatus;
    private LinearLayout keyPreview;
    private final String[] previewKeys={"Q","W","E","R","T","Y","π","√","🧬","😊"};
    private int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        createUi();
    }
    private GradientDrawable box(int fill,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);g.setCornerRadius(dp(radius));return g;
    }
    private TextView text(String content,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(content);v.setTextSize(size);
        v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private void add(LinearLayout layout,View child,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);layout.addView(child,lp);
    }
    private Button button(String label,int color,int textColor){
        Button b=new Button(this);
        b.setText(label);b.setTextSize(16);b.setAllCaps(false);
        b.setTextColor(textColor);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setMinHeight(dp(56));b.setBackground(box(color,16));
        b.setElevation(dp(2));
        return b;
    }
    private void createUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setBackgroundColor(0xffeff5ff);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(30));
        scroll.addView(root);setContentView(scroll);
        FrameLayout hero=new FrameLayout(this);
        hero.setBackground(box(NAVY,24));
        hero.addView(new KeyboardBackdrop(this,0),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(22),dp(20),dp(22),dp(20));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-1);
        hero.addView(copy,cp);
        add(copy,text("🎓 SAURABH'S VISION™",14,GOLD,true),0);
        add(copy,text("MATHS & SCIENCE\nKEYBOARD",30,Color.WHITE,true),10);
        add(copy,text("Type Smarter. Explore Further.",15,0xffbde9ff,false),8);
        add(copy,text("π   √   ∑   ∞   🧬   ⚛️   😊   🥰   💯",25,0xff8df0ff,true),18);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(222));root.addView(hero,hp);

        add(root,text("⌨️ Set as your Android Keyboard",20,NAVY,true),25);
        add(root,text("एक बार keyboard enable करें, फिर default keyboard चुनें। उसके बाद WhatsApp, Notes और दूसरे text fields में भी इस्तेमाल कर पाएँगे।",14,0xff496284,false),10);
        Button enable=button("① Enable Vision Keyboard",0xff1949ad,Color.WHITE);
        add(root,enable,15);
        enable.setOnClickListener(v->{
            try{startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));}
            catch(RuntimeException e){showMessage("Keyboard Settings नहीं खुल सकीं। Android Settings → System → Keyboard पर जाएँ।");}
        });
        Button choose=button("② Choose Default Keyboard",GOLD,NAVY);
        add(root,choose,10);
        choose.setOnClickListener(v->{
            InputMethodManager manager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(manager!=null)manager.showInputMethodPicker();
            else showMessage("Android keyboard picker उपलब्ध नहीं है।");
        });

        add(root,text("🌐 Indian Languages • हिन्दी / Hinglish / English",21,NAVY,true),26);
        add(root,text("Choose English, Roman Hinglish or phonetic typing in any of India's 22 scheduled languages. Type with normal English keys; in Hindi mode, namaste → नमस्ते. No Internet needed.",14,0xff496284,false),8);
        final TextView langPreview=text("",15,0xff2153ad,true);
        String[] languageOptions=new String[IndicLanguages.count()];
        for(int i=0;i<languageOptions.length;i++)languageOptions[i]=IndicLanguages.label(i);
        final Spinner languageSpinner=new Spinner(this);
        ArrayAdapter<String> langAdapter=new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,languageOptions);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        languageSpinner.setBackground(box(Color.WHITE,15));
        languageSpinner.setPadding(dp(12),dp(10),dp(12),dp(10));
        add(root,languageSpinner,12);
        add(root,langPreview,10);
        int initialLanguage=IndicLanguages.norm(
            getSharedPreferences(PREFS,MODE_PRIVATE).getInt("language",IndicLanguages.ENGLISH));
        languageSpinner.setSelection(initialLanguage);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                int lang=IndicLanguages.norm(position);
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt("language",lang).apply();
                langPreview.setText("Example: namaste → "+
                    OfflinePhonetic.convert("namaste",lang)+
                    (lang==IndicLanguages.HINGLISH?" (Roman Hinglish)":
                     lang==IndicLanguages.ENGLISH?" (English)":
                     " · "+IndicLanguages.nativeName(lang)));
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        LinearLayout quickLanguages=new LinearLayout(this);
        quickLanguages.setOrientation(LinearLayout.HORIZONTAL);
        add(root,quickLanguages,12);
        int[] quickIds={IndicLanguages.ENGLISH,IndicLanguages.HINGLISH,IndicLanguages.HINDI};
        String[] quickNames={"ABC English","Hinglish","हिन्दी"};
        for(int i=0;i<quickIds.length;i++){
            final int selected=quickIds[i];
            Button quick=button(quickNames[i],i==2?0xff235dcc:0xffe6eefb,i==2?Color.WHITE:NAVY);
            quick.setTextSize(12);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(49),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            quickLanguages.addView(quick,lp);
            quick.setOnClickListener(v->languageSpinner.setSelection(selected));
        }
        add(root,text("ℹ️ Roman phonetic input writes sounds in another script; it does NOT translate sentence meaning. Non-Hindi spellings, Urdu and minority scripts are basic approximations.",12,0xff607590,false),9);

        add(root,text("📏 Keyboard Height & Spacebar",20,NAVY,true),26);
        add(root,text("Compact height is recommended on phones where the bottom SPACE / ENTER row was hidden. Both options automatically fit their rows inside the available screen height.",14,0xff496284,false),7);
        TextView heightStatus=text("",14,0xff1848a0,true);
        add(root,heightStatus,9);
        Runnable refreshHeight=()->heightStatus.setText(
            getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("compact_height",true)
                ?"Selected: Compact (Recommended) — Spacebar visible"
                :"Selected: Comfortable — adjusts to available screen height");
        Button compact=button("✅ Compact Keyboard (Recommended)",0xff2360c6,Color.WHITE);
        add(root,compact,9);
        compact.setOnClickListener(v->{
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("compact_height",true).apply();
            refreshHeight.run();
            showMessage("Compact keyboard enabled. Close and reopen the keyboard.");
        });
        Button comfortable=button("Comfortable / Larger Keyboard",0xffe5edfb,NAVY);
        add(root,comfortable,7);
        comfortable.setOnClickListener(v->{
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("compact_height",false).apply();
            refreshHeight.run();
            showMessage("Comfortable layout enabled. The Spacebar will still auto-fit.");
        });
        refreshHeight.run();

        add(root,text("🎨 Premium Keyboard Themes",21,NAVY,true),29);
        add(root,text("5 professional themes • 4 Neon + 1 Pure White / Light",13,0xff54708e,false),5);
        themeStatus=text("",15,0xff16519a,true);
        add(root,themeStatus,10);
        LinearLayout themeColumn=new LinearLayout(this);
        themeColumn.setOrientation(LinearLayout.VERTICAL);
        add(root,themeColumn,8);
        for(int i=0;i<NAMES.length;i++){
            final int index=i;
            LinearLayout card=new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable cardBg=box(i==4?0xfffcfdff:(i%2==0?Color.WHITE:0xffe5ebff),16);
            cardBg.setStroke(dp(index==getSharedPreferences(PREFS,MODE_PRIVATE).getInt("theme",0)?2:1),
                index==getSharedPreferences(PREFS,MODE_PRIVATE).getInt("theme",0)?0xff285cba:0xffd7dfed);
            card.setBackground(cardBg);
            card.setElevation(dp(1.5f));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(68));
            p.topMargin=dp(7);
            themeColumn.addView(card,p);
            TextView circle=text(i==0?"π":i==1?"∑":i==2?"⚛":i==3?"Ω":"☀",27,i==4?0xff2563eb:ACCENTS[i],true);
            circle.setGravity(Gravity.CENTER);
            circle.setBackground(box(0xff11234b,13));
            LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(55),dp(55));
            ilp.leftMargin=dp(6);card.addView(circle,ilp);
            TextView label=text(NAMES[i],16,NAVY,true);
            LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(0,-2,1);
            t.leftMargin=dp(14);card.addView(label,t);
            Button apply=button("Use",0xff1746b1,Color.WHITE);apply.setTextSize(13);
            LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(dp(71),dp(48));
            blp.rightMargin=dp(7);
            card.addView(apply,blp);
            apply.setOnClickListener(v->{
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt("theme",index).apply();
                showTheme();
                showMessage(NAMES[index]+" applied. Reopen keyboard to see the design.");
            });
        }
        add(root,text("⌨️ Theme Key Preview",17,NAVY,true),19);
        keyPreview=new LinearLayout(this);
        keyPreview.setOrientation(LinearLayout.HORIZONTAL);
        keyPreview.setPadding(dp(6),dp(11),dp(6),dp(11));
        add(root,keyPreview,7);
        showTheme();

        add(root,text("😊 New Emoji Keyboard",21,NAVY,true),27);
        add(root,text("Emoji tab खोलें, Faces, Love, Feelings, Hands, People, Animals, Nature, Food, Fun, Symbols और Science चुनें। ◀ ▶ से नए emoji pages देखें। कुल 923 entries हैं; कुछ emojis Android version के अनुसार अलग दिख सकते हैं।",14,0xff496284,false),8);
        add(root,text("🧪 Test Your Keyboard",21,NAVY,true),30);
        add(root,text("नीचे बॉक्स पर टैप करें और Maths/Science keyboard से लिखकर देखें:",14,0xff496284,false),8);
        EditText test=new EditText(this);
        test.setTextSize(17);test.setTextColor(NAVY);
        test.setHintTextColor(0xff6785a0);
        test.setHint("Try: namaste → नमस्ते, π × r², H₂O, 😊");
        test.setSingleLine(false);test.setMinLines(3);
        test.setPadding(dp(15),dp(12),dp(15),dp(12));
        test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        test.setBackground(box(Color.WHITE,15));
        add(root,test,12);

        LinearLayout features=new LinearLayout(this);features.setOrientation(LinearLayout.VERTICAL);
        features.setPadding(dp(17),dp(16),dp(17),dp(16));
        features.setBackground(box(Color.WHITE,18));
        add(root,features,27);
        add(features,text("✨ What's Inside",18,NAVY,true),0);
        add(features,text("• 🌐 22 Indian languages + English and Hinglish Roman typing\n• Hindi phonetic: namaste → नमस्ते, mera → मेरा\n• Compact keyboard height: no more full-screen empty background\n• ABC QWERTY, Shift/Caps, Numbers, Maths and Science\n• 😊 Emoji keyboard with 923 options across 11 categories\n• Faces, love, feelings, gestures, people, animals and more\n• Previous/next emoji pages and responsive key touch effects\n• Backspace repeat, cursor arrows, Space and Enter",14,0xff3d5a7b,false),11);

        add(root,text("🔒 Your Typing Stays Private",20,NAVY,true),25);
        add(root,text("इस keyboard को Internet permission नहीं दी गई है। यह आपकी typing history, passwords या clipboard को save या किसी server पर upload नहीं करता। आपके typed characters सिर्फ खुले हुए text field तक भेजे जाते हैं। Android नए keyboard को enable करते समय सामान्य सुरक्षा चेतावनी दिखाता है।",14,0xff445d7f,false),9);

        Button instagram=button("📷 Follow @saurabhs_vision on Instagram",0xffddd9fc,NAVY);
        add(root,instagram,21);
        instagram.setOnClickListener(v->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.instagram.com/saurabhs_vision/")));}
            catch(RuntimeException ex){showMessage("Instagram link नहीं खुल पाया।");}
        });
        add(root,text("SAURABH'S VISION™ • Learn Today, Lead Tomorrow!",12,0xff5c7190,true),24);
    }
    private void showTheme(){
        int current=Math.max(0,Math.min(4,getSharedPreferences(PREFS,MODE_PRIVATE).getInt("theme",0)));
        if(themeStatus!=null)themeStatus.setText("Selected: "+NAMES[current]+(current==4?"  •  LIGHT MODE":""));
        if(keyPreview==null)return;
        keyPreview.removeAllViews();
        keyPreview.setBackground(box(current==4?0xffe8effa:0xff0b2248,16));
        for(String key:previewKeys){
            TextView item=text(key,key.length()>1?14:17,current==4?0xff14243b:Color.WHITE,true);
            item.setGravity(Gravity.CENTER);
            GradientDrawable bg=box(current==4?Color.WHITE:0xff1b385f,10);
            bg.setStroke(dp(1),current==4?0xffc8d7e9:ACCENTS[current]);
            item.setBackground(bg);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(39),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            keyPreview.addView(item,lp);
        }
    }
    private void showMessage(String message){
        android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
    }
}
