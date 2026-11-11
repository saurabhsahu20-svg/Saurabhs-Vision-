package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Fully native system keyboard / IME. No network, account login, logging,
 * clipboard access, or saved keystroke history.
 */
public final class VisionKeyboardService extends InputMethodService {
    private static final String PREFS="vision_keyboard_settings";
    private static final String KEY_THEME="theme";
    private static final int ABC=0,NUMBER=1,MATH=2,SCIENCE=3,EMOJI=4,LANGUAGE=5;
    private static final int EMOJIS_PER_PAGE=32;
    private int emojiCategory=0,emojiPage=0;
    private static final int[] ACCENTS={0xff65e9ff,0xffffd55c,0xff9affd4,0xffeea8ff,0xff2563eb};
    private static final int[] DARK={0xff071537,0xff1c1641,0xff061e32,0xff1a1040,0xffffffff};
    private static final int[] KEY_COLORS={0xd91b3561,0xdb45375e,0xdb21485b,0xdb3c2b67,0xfff8fbff};
    private KeyboardPanel keyboardFrame;
    private LinearLayout keysContent;
    private int mode=ABC,theme=0;
    private int selectedLanguage=IndicLanguages.ENGLISH;
    private boolean sensitiveField=false;
    private final StringBuilder romanComposing=new StringBuilder();
    private TextView romanPreview;
    private TextView[] suggestionChips;
    private boolean updatingSuggestions=false;
    private boolean shifted=false,caps=false;
    private long lastShift=0L;
    private final Handler repeatHandler=new Handler(Looper.getMainLooper());
    private boolean backspaceRepeated=false;
    private Runnable backspaceRepeat;
    private boolean haptics=true;

    @Override public void onCreate(){
        super.onCreate();
        theme=Math.floorMod(getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_THEME,0),5);
        selectedLanguage=IndicLanguages.norm(settings().getInt("language",IndicLanguages.ENGLISH));
    }
    private int dp(float value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    private SharedPreferences settings(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    @Override public View onCreateInputView(){
        keyboardFrame=new KeyboardPanel(this,theme);
        rebuild();
        return keyboardFrame;
    }
    /** Avoid Android's fullscreen extract editor and keep the app visible above the keyboard. */
    @Override public boolean onEvaluateFullscreenMode(){return false;}
    @Override public void onStartInput(EditorInfo info,boolean restarting){
        finishRomanWord();
        super.onStartInput(info,restarting);
        sensitiveField=isPasswordField(info);
        if(suggestionChips!=null)updateSuggestions();
        if(!restarting){
            int type=info==null?InputType.TYPE_CLASS_TEXT:(info.inputType&InputType.TYPE_MASK_CLASS);
            mode=(type==InputType.TYPE_CLASS_NUMBER || type==InputType.TYPE_CLASS_PHONE)
                ?NUMBER:ABC;
            shifted=false;caps=false;
        }
    }
    private boolean isPasswordField(EditorInfo info){
        if(info==null)return false;
        int type=info.inputType;
        int cls=type&InputType.TYPE_MASK_CLASS;
        int variation=type&InputType.TYPE_MASK_VARIATION;
        return (cls==InputType.TYPE_CLASS_TEXT &&
            (variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||
             variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||
             variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))||
            (cls==InputType.TYPE_CLASS_NUMBER &&
             variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }
    @Override public void onStartInputView(EditorInfo info,boolean restarting){
        super.onStartInputView(info,restarting);
        theme=Math.floorMod(settings().getInt(KEY_THEME,0),5);
        selectedLanguage=IndicLanguages.norm(settings().getInt("language",IndicLanguages.ENGLISH));
        sensitiveField=isPasswordField(info);
        if(keyboardFrame!=null)rebuild();
    }
    @Override public void onUpdateSelection(int oldSelStart,int oldSelEnd,
        int newSelStart,int newSelEnd,int candidatesStart,int candidatesEnd){
        super.onUpdateSelection(oldSelStart,oldSelEnd,
            newSelStart,newSelEnd,candidatesStart,candidatesEnd);
        // Word candidates are calculated locally only, never stored/logged.
        if(!updatingSuggestions && mode==ABC)updateSuggestions();
    }
    @Override public void onFinishInputView(boolean finishingInput){
        stopRepeat();
        finishRomanWord();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onDestroy(){
        stopRepeat();
        finishRomanWord();
        repeatHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    private boolean light(){return theme==4;}
    private void rebuild(){
        if(keyboardFrame==null)return;
        keyboardFrame.removeAllViews();
        keyboardFrame.setTheme(theme);
        // Default to screen-safe compact mode; optional normal size still auto-fits
        // if Android's IME container cannot accommodate the natural key heights.
        keyboardFrame.setCompact(settings().getBoolean("compact_height",true));
        keysContent=keyboardFrame;
        suggestionChips=null;
        romanPreview=null;
        // Keep the fourth, Space/Enter row above the device navigation inset.
        // Bottom inset is measured by KeyboardPanel and never overlays letter keys.
        keysContent.setPadding(dp(3),dp(5),dp(3),dp(18));
        addToolbar();
        if(mode==LANGUAGE){
            addLanguagePicker();
        }else if(mode==ABC){
            // Reference screenshot has a single tools row above the keys.
            // Local predictions remain available as an optional setting.
            if(settings().getBoolean("show_suggestions",false))addSuggestions();
            if(selectedLanguage!=IndicLanguages.ENGLISH)addLanguageStatus();

            addRow("Q W E R T Y U I O P");
            addRow("A S D F G H J K L");
            // Exactly three letter rows followed by a separate fourth action row.
            // Do NOT replace or shift any letters to make the Spacebar visible.
            addRow("⇧ Z X C V B N M ⌫");
            addBottom();
        }else if(mode==NUMBER){
            addRow("1 2 3 4 5 6 7 8 9 0");
            addRow("@ # ₹ % & - + ( ) /");
            addRow("* \" ' : ; ! ? = ← ⌫");
            addBottom();
        }else if(mode==MATH){
            addRow("π √ ∑ ∞ ≈ ≠ ≤ ≥ ± ×");
            addRow("÷ ∫ ∂ Δ θ α β γ Ω μ");
            addRow("sin( cos( tan( log( ln( x² x³ ^ → ⌫");
            addBottom();
        }else if(mode==SCIENCE){
            addRow("H₂O CO₂ O₂ NaCl H⁺ OH⁻ e⁻ pH DNA RNA");
            addRow("→ ⇌ ↑ ↓ °C K mol kg m/s N");
            addRow("α β γ λ μ Ω Δ ∝ ∴ ⌫");
            addBottom();
        }else{
            addEmojiCategories();
            addEmojiPage();
            addEmojiBottom();
        }
        keyboardFrame.requestLayout();
    }
    /**
     * Real keyboard toolbar: branded icon and scrollable functionality tabs.
     * Brand is part of InputMethodService input view, not just launcher preview.
     */
    private void addToolbar(){
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        scroller.setContentDescription("S Board toolbar; swipe horizontally for ABC, numbers, maths, science, emoji, languages, themes and settings");
        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        int screenWidthDp=getResources().getConfiguration().screenWidthDp;
        final boolean narrow=screenWidthDp<500;
        LinearLayout identity=new LinearLayout(this);
        identity.setOrientation(LinearLayout.HORIZONTAL);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.s_board_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("S Board logo by Saurabh's Vision");
        identity.addView(logo,new LinearLayout.LayoutParams(dp(narrow?33:43),dp(narrow?33:43)));
        LinearLayout brandWords=new LinearLayout(this);
        brandWords.setOrientation(LinearLayout.VERTICAL);
        brandWords.setGravity(Gravity.CENTER_VERTICAL);
        brandWords.setPadding(dp(5),0,dp(1),0);
        TextView brand=new TextView(this);
        brand.setText("S Board");
        brand.setTextColor(light()?0xff123b83:Color.WHITE);
        brand.setTextSize(narrow?16:20);
        brand.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        brand.setIncludeFontPadding(false);
        brandWords.addView(brand,new LinearLayout.LayoutParams(-1,-2));
        TextView caption=new TextView(this);
        caption.setText("by Saurabh's Vision™");
        caption.setTextColor(light()?0xff326fb4:0xff8ce4ff);
        caption.setTextSize(narrow?8:10);
        caption.setIncludeFontPadding(false);
        caption.setSingleLine(true);
        brandWords.addView(caption,new LinearLayout.LayoutParams(-1,-2));
        identity.addView(brandWords,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout.LayoutParams identityLp=new LinearLayout.LayoutParams(
            dp(narrow?151:205),dp(46));
        identityLp.setMargins(dp(1),0,dp(4),0);
        bar.addView(identity,identityLp);
        View separator=new View(this);
        separator.setBackgroundColor(light()?0xff438fcf:0xff5aeaff);
        LinearLayout.LayoutParams separatorLp=
            new LinearLayout.LayoutParams(dp(1),dp(32));
        separatorLp.setMargins(0,0,dp(8),0);
        bar.addView(separator,separatorLp);

        // Screenshot-inspired ordering: no redundant toolbar Space control.
        // The real wide Space key always lives in the bottom row.
        String[] ids={"ABC","123","∑ Math","⚛ Sci","😊","🌐","🎨","⚙"};
        View active=null;
        for(int i=0;i<ids.length;i++){
            final int index=i;
            TextView tab=new TextView(this);
            tab.setText(ids[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(i<2?15:((i==2||i==3)?13:20));
            tab.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
            boolean chosen=index<5&&mode==index;
            tab.setTextColor(chosen?(light()?Color.WHITE:0xff041b33):
                (light()?0xff193c69:Color.WHITE));
            GradientDrawable background=new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                chosen?
                    (light()?new int[]{0xff387cfa,0xff235dd1}:
                        new int[]{0xff7bffff,0xff0ed5ee}):
                    (light()?new int[]{0xfff4f8fd,0xffdce7f4}:
                        new int[]{0xff19476d,0xff061d3c}));
            background.setCornerRadius(dp(15));
            background.setStroke(dp(1),chosen?ACCENTS[theme]:
                (light()?0xffbfd4ed:0xff29cfff));
            tab.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light()?0x332b61c6:0x55b5ffff),
                background,null));
            tab.setElevation(dp(2));
            int width=(index==2||index==3)?dp(narrow?73:87):
                (index<2?dp(narrow?60:75):dp(narrow?51:63));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,dp(41));
            lp.setMargins(dp(3),0,dp(3),0);
            bar.addView(tab,lp);
            if(chosen)active=tab;
            tab.setContentDescription(index==5?"Choose Indian language":
                index==6?"Change keyboard theme":
                index==7?"Keyboard settings":"Switch keyboard to "+ids[i]);
            tab.setOnClickListener(v->{
                feedback(v);
                if(index<5){
                    finishRomanWord();mode=index;shifted=false;rebuild();
                }else if(index==5){
                    finishRomanWord();mode=LANGUAGE;rebuild();
                }else if(index==6){
                    theme=(theme+1)%5;
                    settings().edit().putInt(KEY_THEME,theme).apply();rebuild();
                }else{
                    finishRomanWord();
                    Intent intent=new Intent(this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try{startActivity(intent);}catch(RuntimeException ignored){}
                }
            });
        }
        scroller.addView(bar);
        int toolHeight=dp(getResources().getConfiguration().orientation==2?49:51);
        keysContent.addView(scroller,new LinearLayout.LayoutParams(-1,toolHeight));
        if(active!=null && mode!=ABC){
            final View item=active;
            scroller.post(()->scroller.smoothScrollTo(
                Math.max(0,item.getLeft()-dp(narrow?163:210)),0));
        }
    }
    /**
     * Gboard-style local completions for ABC/Hinglish and a phonetic preview.
     * All contents are read only from the active field, never written to disk.
     */
    private void addSuggestions(){
        LinearLayout strip=new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        suggestionChips=new TextView[3];
        for(int i=0;i<3;i++){
            final int position=i;
            TextView chip=new TextView(this);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            chip.setTextSize(14);
            chip.setTextColor(light()?0xff26364f:0xfff0f8ff);
            chip.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light()?0x332b61c6:0x44a4ecff),
                new GradientDrawable(),null));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(34),1f);
            lp.setMargins(dp(1),0,dp(1),0);
            strip.addView(chip,lp);
            chip.setOnClickListener(v->{
                if(sensitiveField)return;
                Object tag=chip.getTag();
                if(tag instanceof String)applySuggestion((String)tag);
            });
            suggestionChips[i]=chip;
            if(position<2){
                View divider=new View(this);
                divider.setBackgroundColor(light()?0xffc6d3e7:0x7048a5cb);
                strip.addView(divider,new LinearLayout.LayoutParams(dp(1),dp(17)));
            }
        }
        keysContent.addView(strip,new LinearLayout.LayoutParams(-1,dp(37)));
        updateSuggestions();
    }
    private void updateSuggestions(){
        if(suggestionChips==null||updatingSuggestions)return;
        updatingSuggestions=true;
        try{
            String typed="";
            InputConnection conn=getCurrentInputConnection();
            boolean phonetic=IndicLanguages.phonetic(selectedLanguage);
            if(phonetic){
                typed=romanComposing.toString();
            }else if(conn!=null&&!sensitiveField){
                typed=WordSuggestions.trailingWord(conn.getTextBeforeCursor(36,0));
            }
            String[] candidates=sensitiveField||mode!=ABC?
                new String[0]:WordSuggestions.get(typed,
                    selectedLanguage!=IndicLanguages.ENGLISH,3);
            String[] fallback=sensitiveField?
                new String[]{"Private typing","",""}:
                new String[]{"S Board","by Saurabh's Vision™","⌨ Type to see suggestions"};
            for(int i=0;i<suggestionChips.length;i++){
                TextView chip=suggestionChips[i];
                if(i<candidates.length){
                    String raw=candidates[i];
                    String label=phonetic?OfflinePhonetic.convert(raw,selectedLanguage):raw;
                    chip.setText(label);
                    chip.setTag(raw);
                    chip.setClickable(true);
                    chip.setTypeface(Typeface.DEFAULT,i==0?Typeface.BOLD:Typeface.NORMAL);
                }else{
                    chip.setText(candidates.length==0?fallback[i]:"");
                    chip.setTag(null);
                    chip.setClickable(false);
                    chip.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
                }
            }
        }finally{updatingSuggestions=false;}
    }
    private void applySuggestion(String suggestion){
        InputConnection conn=getCurrentInputConnection();
        if(conn==null||sensitiveField||suggestion==null||suggestion.isEmpty())return;
        if(phoneticEnabled()){
            conn.setComposingText(
                OfflinePhonetic.convert(suggestion,selectedLanguage),1);
            conn.finishComposingText();
            romanComposing.setLength(0);
            conn.commitText(" ",1);
        }else{
            String before=WordSuggestions.trailingWord(conn.getTextBeforeCursor(36,0));
            if(!before.isEmpty())conn.deleteSurroundingText(before.length(),0);
            conn.commitText(suggestion+" ",1);
        }
        updateSuggestions();
    }
    private void addRow(String keys){
        String[] labels=keys.split(" ");
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        boolean landscape=getResources().getConfiguration().orientation==2;
        int height=dp(landscape?49:55);
        for(String label:labels){
            String display=label;
            String output=label;
            if(mode==ABC && label.length()==1 && Character.isLetter(label.charAt(0))){
                output=(shifted||caps)?label.toUpperCase():label.toLowerCase();
                display=output;
            }
            float weight=("⇧".equals(label)||"⌫".equals(label))?1.25f:1f;
            addKey(row,display,output,weight,height,false);
        }
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(3)));
    }
    private void selectLanguage(int lang){
        finishRomanWord();
        selectedLanguage=IndicLanguages.norm(lang);
        settings().edit().putInt("language",selectedLanguage).apply();
        mode=ABC;shifted=false;caps=false;
        rebuild();
    }
    /**
     * Two horizontally scrollable rows cover English + Hinglish + all 22 scheduled
     * Indian languages, without growing the IME taller than the viewport.
     */
    private void addLanguagePicker(){
        TextView title=new TextView(this);
        title.setText("🌐 Select language · Swipe sideways to see all 24 options");
        title.setTextSize(12);title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTextColor(light()?0xff243f70:0xffe1edff);
        title.setPadding(dp(7),0,dp(4),0);
        keysContent.addView(title,new LinearLayout.LayoutParams(-1,dp(29)));
        for(int row=0;row<2;row++){
            HorizontalScrollView scroll=new HorizontalScrollView(this);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setFillViewport(false);
            LinearLayout choices=new LinearLayout(this);
            choices.setOrientation(LinearLayout.HORIZONTAL);
            choices.setGravity(Gravity.CENTER_VERTICAL);
            for(int i=row*12;i<Math.min((row+1)*12,IndicLanguages.count());i++){
                final int choice=i;
                TextView item=new TextView(this);
                item.setText(IndicLanguages.nativeName(i)+"\n"+IndicLanguages.name(i));
                item.setGravity(Gravity.CENTER);
                item.setTextSize(12);
                item.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                boolean current=i==selectedLanguage;
                item.setTextColor(current?DARK[theme]:(light()?0xff253955:Color.WHITE));
                GradientDrawable shape=new GradientDrawable();
                shape.setColor(current?ACCENTS[theme]:(light()?0xffe6edf9:0xaa20395c));
                shape.setCornerRadius(dp(11));
                shape.setStroke(dp(1),current?ACCENTS[theme]:0x6689b3df);
                item.setBackground(shape);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(120),dp(54));
                lp.setMargins(dp(3),dp(2),dp(3),dp(2));
                choices.addView(item,lp);
                item.setContentDescription("Choose "+IndicLanguages.name(i)+" keyboard");
                item.setOnClickListener(v->{feedback(v);selectLanguage(choice);});
            }
            scroll.addView(choices);
            keysContent.addView(scroll,new LinearLayout.LayoutParams(-1,dp(59)));
        }
        TextView note=new TextView(this);
        note.setText("English = Latin   •   Hinglish = Roman Hindi   •   Other languages = phonetic script");
        note.setTextSize(11);note.setTextColor(light()?0xff476183:0xffb8d8fc);
        note.setGravity(Gravity.CENTER);
        keysContent.addView(note,new LinearLayout.LayoutParams(-1,dp(37)));
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER);
        int height=dp(47);
        addKey(footer,"ABC","ABC",1.2f,height,true);
        addKey(footer,"🇮🇳 Hindi","LANG_HINDI",1.5f,height,true);
        addKey(footer,"Hinglish","LANG_HINGLISH",1.5f,height,true);
        addKey(footer,"English","LANG_ENGLISH",1.5f,height,true);
        keysContent.addView(footer,new LinearLayout.LayoutParams(-1,height+dp(2)));
    }
    private void addLanguageStatus(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=new TextView(this);
        name.setText(selectedLanguage==IndicLanguages.HINGLISH?"Hinglish":
            IndicLanguages.nativeName(selectedLanguage));
        name.setTextSize(12);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        name.setTextColor(light()?0xff2257b7:ACCENTS[theme]);
        name.setPadding(dp(4),0,dp(5),0);
        row.addView(name,new LinearLayout.LayoutParams(-2,-1));
        romanPreview=new TextView(this);
        romanPreview.setSingleLine(true);
        romanPreview.setTextSize(12);
        romanPreview.setGravity(Gravity.CENTER_VERTICAL);
        romanPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        romanPreview.setTextColor(light()?0xff213754:0xffeaf5ff);
        row.addView(romanPreview,new LinearLayout.LayoutParams(0,-1,1f));
        TextView swap=new TextView(this);
        swap.setText(selectedLanguage==IndicLanguages.HINDI?"Hinglish":"हिंदी");
        swap.setGravity(Gravity.CENTER);
        swap.setTextColor(light()?0xff1c5acc:ACCENTS[theme]);
        swap.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        swap.setTextSize(12);
        swap.setPadding(dp(7),0,dp(7),0);
        row.addView(swap,new LinearLayout.LayoutParams(-2,-1));
        swap.setContentDescription("Toggle between Hindi phonetic and Hinglish");
        swap.setOnClickListener(v->selectLanguage(selectedLanguage==IndicLanguages.HINDI?
            IndicLanguages.HINGLISH:IndicLanguages.HINDI));
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,dp(31)));
        refreshRomanPreview();
    }
    private void refreshRomanPreview(){
        if(romanPreview==null)return;
        if(sensitiveField){
            romanPreview.setText("Private field · English letters only");
        }else if(selectedLanguage==IndicLanguages.HINGLISH){
            romanPreview.setText("Type Hindi in English letters: namaste");
        }else if(romanComposing.length()>0){
            romanPreview.setText(romanComposing+" → "+
                OfflinePhonetic.convert(romanComposing.toString(),selectedLanguage));
        }else{
            romanPreview.setText("Type namaste → "+
                OfflinePhonetic.convert("namaste",selectedLanguage));
        }
    }
    private void finishRomanWord(){
        if(romanComposing.length()==0)return;
        InputConnection connection=getCurrentInputConnection();
        if(connection!=null)connection.finishComposingText();
        romanComposing.setLength(0);
        refreshRomanPreview();
    }
    private boolean phoneticEnabled(){
        return mode==ABC && IndicLanguages.phonetic(selectedLanguage) && !sensitiveField;
    }
    private boolean isAsciiLetter(String code){
        return code.length()==1 && (
            (code.charAt(0)>='a'&&code.charAt(0)<='z')||
            (code.charAt(0)>='A'&&code.charAt(0)<='Z'));
    }
    private void appendRoman(String code,InputConnection connection){
        if(romanComposing.length()>=48)finishRomanWord();
        romanComposing.append(code.toLowerCase(java.util.Locale.ROOT));
        connection.setComposingText(
            OfflinePhonetic.convert(romanComposing.toString(),selectedLanguage),1);
        refreshRomanPreview();
        updateSuggestions();
    }
    private void addEmojiCategories(){
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setFillViewport(false);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setHorizontalFadingEdgeEnabled(true);
        LinearLayout categories=new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categories.setGravity(Gravity.CENTER_VERTICAL);
        String[] icons={"😀","🥰","😢","👋","🧑","🐼","🌿","🍕","🎉","💯","🧬"};
        for(int i=0;i<EmojiCatalog.count();i++){
            final int category=i;
            TextView chip=new TextView(this);
            chip.setGravity(Gravity.CENTER);
            chip.setText(icons[i]+" "+EmojiCatalog.title(i));
            chip.setTextSize(12);
            chip.setTextColor(category==emojiCategory?DARK[theme]:(light()?0xff263a56:Color.WHITE));
            chip.setContentDescription(EmojiCatalog.title(i)+" emojis");
            GradientDrawable shape=new GradientDrawable();
            shape.setColor(i==emojiCategory?ACCENTS[theme]:(light()?0xffe2eaf5:0x7a1a3560));
            shape.setStroke(dp(1),light()?0xffc9d7ec:0x477ab5d9);
            shape.setCornerRadius(dp(12));
            chip.setBackground(shape);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(34));
            lp.setMargins(dp(3),dp(2),dp(3),dp(2));
            chip.setPadding(dp(9),0,dp(9),0);
            categories.addView(chip,lp);
            chip.setOnClickListener(v->{
                emojiCategory=category;emojiPage=0;rebuild();
            });
        }
        scroller.addView(categories);
        keysContent.addView(scroller,new LinearLayout.LayoutParams(-1,dp(38)));
        // Reopen the selection near the currently chosen category.
        final int selected=emojiCategory;
        scroller.post(()->{
            if(selected>=0&&selected<categories.getChildCount()){
                View child=categories.getChildAt(selected);
                scroller.scrollTo(Math.max(0,child.getLeft()-dp(22)),0);
            }
        });
    }
    private void addEmojiPage(){
        String[] emoji=EmojiCatalog.items(emojiCategory);
        int start=emojiPage*EMOJIS_PER_PAGE;
        boolean landscape=getResources().getConfiguration().orientation==2;
        int height=dp(landscape?34:40);
        for(int r=0;r<4;r++){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            for(int col=0;col<8;col++){
                int index=start+r*8+col;
                if(index<emoji.length){
                    String e=emoji[index];
                    addKey(row,e,e,1f,height,false);
                }else{
                    TextView empty=new TextView(this);
                    row.addView(empty,new LinearLayout.LayoutParams(0,height,1f));
                }
            }
            keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(2)));
        }
    }
    private void addEmojiBottom(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        int height=dp(getResources().getConfiguration().orientation==2?38:47);
        addKey(row,"ABC","ABC",.9f,height,true);
        addKey(row,"◀","EMOJI_PREV",.55f,height,true);
        int pages=(EmojiCatalog.items(emojiCategory).length+EMOJIS_PER_PAGE-1)/EMOJIS_PER_PAGE;
        TextView count=new TextView(this);
        count.setGravity(Gravity.CENTER);count.setTextColor(light()?0xff245bcc:ACCENTS[theme]);
        count.setTextSize(12);count.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        count.setText((emojiPage+1)+"/"+Math.max(1,pages));
        row.addView(count,new LinearLayout.LayoutParams(0,height,.6f));
        addKey(row,"▶","EMOJI_NEXT",.55f,height,true);
        addKey(row,"space","SPACE",2.4f,height,true);
        addKey(row,"⌫","⌫",.85f,height,true);
        addKey(row,"↵","ENTER",.75f,height,true);
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(2)));
    }
    private void addBottom(){
        // Gboard-like bottom row, with mandatory wide Space key in all modes.
        LinearLayout row=new LinearLayout(this);
        row.setTag("sboard_space_action");
        row.setGravity(Gravity.CENTER);
        int height=dp(getResources().getConfiguration().orientation==2?57:62);
        addKey(row,mode==ABC?"?123":"ABC",mode==ABC?"123":"ABC",1.02f,height,true);
        addKey(row,"☺","EMOJI",.82f,height,true);
        addKey(row,"A↔अ","LANG_TOGGLE",.85f,height,true);
        addKey(row,"Space","SPACE",4.35f,height,true);
        addKey(row,",",",",.60f,height,false);
        addKey(row,".",".",.60f,height,false);
        addKey(row,"↵","ENTER",1.16f,height,true);
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(3)));
    }
    private static boolean isAction(String value){
        return value.equals("⇧")||value.equals("⌫")||value.equals("ABC")||
            value.equals("123")||value.equals("MATH")||value.equals("SCI")||
            value.equals("EMOJI")||value.equals("EMOJI_PREV")||value.equals("EMOJI_NEXT")||
            value.equals("SPACE")||value.equals("ENTER")||value.equals("←")||
            value.startsWith("LANG_");
    }
    private void addKey(LinearLayout row,String display,String output,float weight,int height,boolean special){
        SBoardKeyButton b=new SBoardKeyButton(this);
        b.setSpaceIndicator("SPACE".equals(output),light());
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(display);
        b.setIncludeFontPadding(false);
        int length=display.length();
        b.setTextSize("SPACE".equals(output)?17f:
            (mode==EMOJI? (length>4?18f:23f) :
            length>=5?11.5f:length>=4?13f:length>=3?15f:22f));
        b.setTypeface(Typeface.create("sans-serif-medium",
            special||isAction(output)?Typeface.BOLD:Typeface.NORMAL));
        final boolean enter="ENTER".equals(output);
        final boolean functionKey=special||isAction(output);
        b.setTextColor(light()?
            (enter?Color.WHITE:(functionKey?0xff2056bb:0xff162338)):
            (enter?0xff082440:("SPACE".equals(output)?Color.WHITE:
                (functionKey?0xffadf9ff:Color.WHITE))));
        b.setPadding(0,0,0,0);b.setMinimumHeight(0);b.setMinHeight(0);
        b.setMinimumWidth(0);b.setMinWidth(0);
        int upper,lower,stroke;
        if(light()){
            upper=enter?0xff3c82f6:(functionKey?0xffe6eefb:0xffffffff);
            lower=enter?0xff1d55cf:(functionKey?0xffdbe7f8:0xfff7faff);
            stroke=enter?0xff245fd7:0xffcbd6e6;
        }else{
            // Glossy neon navy keycaps like the supplied visual design.
            upper=enter?0xff68ffff:("SPACE".equals(output)?0xff2865a4:
                (functionKey?0xff1c4a80:0xff2d6397));
            lower=enter?0xff0bbce9:("SPACE".equals(output)?0xff082753:
                (functionKey?0xff0c2c51:0xff071d40));
            stroke=enter?0xffb0ffff:0xff3cdbff;
        }
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{upper,lower});
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1),stroke);
        b.setBackground(new RippleDrawable(
            ColorStateList.valueOf(light()?0x452563eb:0x6affffff),shape,null));
        b.setElevation(dp(light()?1.7f:2.3f));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,height-dp(4),weight);
        lp.setMargins(dp(2.4f),dp(1.1f),dp(2.4f),dp(2.5f));
        row.addView(b,lp);
        b.setContentDescription("⌫".equals(output)?"Delete last character":("SPACE".equals(output)?"Spacebar - insert space":output));
        if(mode==ABC && output.length()==1){
            int index="qwertyuiop".indexOf(output.toLowerCase(java.util.Locale.ROOT));
            if(index>=0){
                final String num=index==9?"0":String.valueOf(index+1);
                b.setOnLongClickListener(v->{feedback(v);press(num);return true;});
                b.setLongClickable(true);
            }
        }
        if("⌫".equals(output)){
            b.setOnTouchListener((view,event)->{
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        backspaceRepeated=false;view.setPressed(true);startRepeat();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        stopRepeat();view.setPressed(false);return true;
                    case MotionEvent.ACTION_UP:
                        stopRepeat();view.setPressed(false);
                        if(!backspaceRepeated){feedback(view);deleteOne();}
                        return true;
                    default:return true;
                }
            });
        }else{
            b.setOnTouchListener((view,event)->{
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                    view.animate().scaleX(.96f).scaleY(.94f).setDuration(65).start();
                }else if(event.getActionMasked()==MotionEvent.ACTION_UP||
                         event.getActionMasked()==MotionEvent.ACTION_CANCEL){
                    view.animate().scaleX(1f).scaleY(1f).setDuration(85).start();
                }
                return false;
            });
            b.setOnClickListener(v->{feedback(v);press(output);});
        }
    }
    private void startRepeat(){
        stopRepeat();
        backspaceRepeat=new Runnable(){
            boolean first=true;
            @Override public void run(){
                if(first){first=false;backspaceRepeated=true;}
                deleteOne();
                repeatHandler.postDelayed(this,75L);
            }
        };
        repeatHandler.postDelayed(backspaceRepeat,430L);
    }
    private void stopRepeat(){
        if(backspaceRepeat!=null)repeatHandler.removeCallbacks(backspaceRepeat);
        backspaceRepeat=null;
    }
    private void feedback(View v){
        if(haptics)v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
    private void deleteOne(){
        InputConnection conn=getCurrentInputConnection();
        if(conn==null)return;
        if(phoneticEnabled() && romanComposing.length()>0){
            romanComposing.deleteCharAt(romanComposing.length()-1);
            if(romanComposing.length()==0){
                conn.setComposingText("",1);conn.finishComposingText();
            }else{
                conn.setComposingText(
                    OfflinePhonetic.convert(romanComposing.toString(),selectedLanguage),1);
            }
            refreshRomanPreview();
            updateSuggestions();
            return;
        }
        if(android.os.Build.VERSION.SDK_INT>=24)conn.deleteSurroundingTextInCodePoints(1,0);
        else conn.deleteSurroundingText(1,0);
        updateSuggestions();
    }
    private void press(String code){
        InputConnection conn=getCurrentInputConnection();
        if(code.equals("⇧")){
            long now=SystemClock.uptimeMillis();
            if(now-lastShift<350){caps=!caps;shifted=caps;}
            else if(caps){caps=false;shifted=false;}
            else shifted=!shifted;
            lastShift=now;rebuild();return;
        }
        if(code.equals("LANG_TOGGLE")){
            selectLanguage(selectedLanguage==IndicLanguages.HINDI?
                IndicLanguages.ENGLISH:IndicLanguages.HINDI);return;
        }
        if(code.equals("LANG_HINDI")){selectLanguage(IndicLanguages.HINDI);return;}
        if(code.equals("LANG_HINGLISH")){selectLanguage(IndicLanguages.HINGLISH);return;}
        if(code.equals("LANG_ENGLISH")){selectLanguage(IndicLanguages.ENGLISH);return;}
        if(code.equals("ABC")){finishRomanWord();mode=ABC;shifted=false;rebuild();return;}
        if(code.equals("123")){finishRomanWord();mode=NUMBER;shifted=false;rebuild();return;}
        if(code.equals("MATH")){finishRomanWord();mode=MATH;shifted=false;rebuild();return;}
        if(code.equals("SCI")){finishRomanWord();mode=SCIENCE;shifted=false;rebuild();return;}
        if(code.equals("EMOJI")){finishRomanWord();mode=EMOJI;emojiPage=0;shifted=false;rebuild();return;}
        if(code.equals("EMOJI_PREV")){
            int n=(EmojiCatalog.items(emojiCategory).length+EMOJIS_PER_PAGE-1)/EMOJIS_PER_PAGE;
            emojiPage=(emojiPage+n-1)%Math.max(1,n);rebuild();return;
        }
        if(code.equals("EMOJI_NEXT")){
            int n=(EmojiCatalog.items(emojiCategory).length+EMOJIS_PER_PAGE-1)/EMOJIS_PER_PAGE;
            emojiPage=(emojiPage+1)%Math.max(1,n);rebuild();return;
        }
        if(conn==null)return;
        if(code.equals("←")){
            finishRomanWord();
            int key=KeyEvent.KEYCODE_DPAD_LEFT;
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));return;
        }
        if(code.equals("SPACE")){finishRomanWord();conn.commitText(" ",1);updateSuggestions();return;}
        if(code.equals("ENTER")){
            finishRomanWord();
            EditorInfo info=getCurrentInputEditorInfo();
            int action=info==null?EditorInfo.IME_ACTION_NONE:
                (info.imeOptions&EditorInfo.IME_MASK_ACTION);
            if(action!=EditorInfo.IME_ACTION_NONE && action!=EditorInfo.IME_ACTION_UNSPECIFIED)
                conn.performEditorAction(action);
            else conn.commitText("\n",1);
            return;
        }
        if(phoneticEnabled() && isAsciiLetter(code)){
            appendRoman(code,conn);
        }else{
            finishRomanWord();
            conn.commitText(code,1);
        }
        updateSuggestions();
        if(mode==ABC && shifted && !caps && code.length()==1 && Character.isLetter(code.charAt(0))){
            shifted=false;rebuild();
        }
    }
}
