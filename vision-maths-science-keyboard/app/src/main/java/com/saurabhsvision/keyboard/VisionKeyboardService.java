package com.saurabhsvision.keyboard;

import android.Manifest;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.text.Html;
import android.widget.ScrollView;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Bitmap;
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
import android.view.inputmethod.InputContentInfo;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Fully native system keyboard / IME. No network, account login, logging,
 * clipboard access without explicit keyboard visibility, or saved keystroke history.
 */
public final class VisionKeyboardService extends InputMethodService {
    private static final String PREFS="vision_keyboard_settings";
    private static final String KEY_THEME="theme";
    private static final int ABC=0,NUMBER=1,MATH=2,SCIENCE=3,EMOJI=4,LANGUAGE=5,SYMBOLS=6,CLIPBOARD=7,STICKERS=8;
    private static final int EMOJIS_PER_PAGE=32;
    private int emojiCategory=0,emojiPage=0;
    private static final int[] ACCENTS=ThemePalette.ACCENTS;
    private KeyboardPanel keyboardFrame;
    private LinearLayout keysContent;
    private int mode=ABC,theme=4;
    private boolean toolbarExpanded=false;
    private int selectedLanguage=IndicLanguages.ENGLISH;
    private boolean sensitiveField=false;
    private final StringBuilder romanComposing=new StringBuilder();
    private TextView romanPreview;
    private TextView[] suggestionChips;
    private boolean updatingSuggestions=false;
    // Coalesce redundant cursor/keypress callbacks into one display refresh.
    private final Handler suggestionHandler=new Handler(Looper.getMainLooper());
    private boolean suggestionsQueued=false;
    private final Runnable suggestionRefresh=()->{
        suggestionsQueued=false;
        updateSuggestions();
    };
    private void requestSuggestions(){
        if(suggestionChips==null||suggestionsQueued)return;
        suggestionsQueued=true;
        suggestionHandler.postDelayed(suggestionRefresh,16L);
    }
    // A cursor-change callback often repeats the same suggestion query after a keypress.
    private String lastSuggestionKey=null;
    private boolean shifted=false,caps=false;
    private long lastShift=0L;
    private final Handler repeatHandler=new Handler(Looper.getMainLooper());
    private boolean backspaceRepeated=false;
    private Runnable backspaceRepeat;
    private boolean haptics=false;
    private VoiceDictation voiceDictation;
    private TextView voiceMicButton;
    private String voiceStatus="";
    // Invalidate speech callbacks as soon as an editor changes or the IME closes.
    private long voiceSessionToken=0L;
    private ClipboardManager clipboardManager;
    private ClipboardHistoryStore clipboardHistory;
    private TextView clipboardShortcut;
    private boolean clipboardObserverRegistered=false;
    private boolean keyboardVisible=false;
    private int stickerPack=0;
    private final ClipboardManager.OnPrimaryClipChangedListener clipboardObserver=()->onClipboardChanged();

    @Override public void onCreate(){
        super.onCreate();
        theme=ThemePalette.norm(getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_THEME,4));
        selectedLanguage=IndicLanguages.norm(settings().getInt("language",IndicLanguages.ENGLISH));
        haptics=settings().getBoolean("key_haptics",false);
        clipboardManager=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardHistory=new ClipboardHistoryStore(this);
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
        cancelVoiceDictation();
        sensitiveField=isPasswordField(info);
        lastSuggestionKey=null; // Never reuse suggestions from a different editor.
        if(suggestionChips!=null)updateSuggestions();
        if(!restarting){
            int type=info==null?InputType.TYPE_CLASS_TEXT:(info.inputType&InputType.TYPE_MASK_CLASS);
            mode=(type==InputType.TYPE_CLASS_NUMBER || type==InputType.TYPE_CLASS_PHONE)
                ?NUMBER:ABC;
            shifted=false;caps=false;toolbarExpanded=false;
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
        theme=ThemePalette.norm(settings().getInt(KEY_THEME,4));
        selectedLanguage=IndicLanguages.norm(settings().getInt("language",IndicLanguages.ENGLISH));
        sensitiveField=isPasswordField(info);
        haptics=settings().getBoolean("key_haptics",false);
        keyboardVisible=true;
        if(clipboardManager!=null&&!clipboardObserverRegistered){
            clipboardManager.addPrimaryClipChangedListener(clipboardObserver);
            clipboardObserverRegistered=true;
        }
        if(keyboardFrame!=null)rebuild();
        updateClipboardShortcut();
    }
    @Override public void onUpdateSelection(int oldSelStart,int oldSelEnd,
        int newSelStart,int newSelEnd,int candidatesStart,int candidatesEnd){
        super.onUpdateSelection(oldSelStart,oldSelEnd,
            newSelStart,newSelEnd,candidatesStart,candidatesEnd);
        // Word candidates are calculated locally only, never stored/logged.
        if(!updatingSuggestions && mode==ABC)requestSuggestions();
    }
    @Override public void onFinishInputView(boolean finishingInput){
        keyboardVisible=false;
        if(clipboardManager!=null&&clipboardObserverRegistered){
            clipboardManager.removePrimaryClipChangedListener(clipboardObserver);
            clipboardObserverRegistered=false;
        }
        clipboardShortcut=null;
        cancelVoiceDictation();
        suggestionHandler.removeCallbacks(suggestionRefresh);suggestionsQueued=false;
        stopRepeat();
        finishRomanWord();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onDestroy(){
        keyboardVisible=false;
        if(clipboardManager!=null&&clipboardObserverRegistered){
            clipboardManager.removePrimaryClipChangedListener(clipboardObserver);
            clipboardObserverRegistered=false;
        }
        clipboardShortcut=null;
        cancelVoiceDictation();
        suggestionHandler.removeCallbacksAndMessages(null);
        stopRepeat();
        finishRomanWord();
        repeatHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    private boolean light(){return ThemePalette.isLight(theme);}
    private void rebuild(){
        if(keyboardFrame==null)return;
        suggestionHandler.removeCallbacks(suggestionRefresh);suggestionsQueued=false;
        keyboardFrame.removeAllViews();
        keyboardFrame.setTheme(theme);
        // Default to screen-safe compact mode; optional normal size still auto-fits
        // if Android's IME container cannot accommodate the natural key heights.
        keyboardFrame.setCompact(settings().getBoolean("compact_height",true));
        keysContent=keyboardFrame;
        suggestionChips=null;
        voiceMicButton=null;
        clipboardShortcut=null;
        lastSuggestionKey=null;
        romanPreview=null;
        // The compat IME window sits ABOVE Android's navigation bar.
        // Avoid adding a second oversized bottom inset: it squeezed the
        // fourth Space/Enter row under the bottom of the real viewport.
        keysContent.setPadding(dp(3),dp(4),dp(3),dp(8));
        // Compact suggestions bar by default; the grid exposes all original tabs.
        if((mode!=ABC && mode!=CLIPBOARD && mode!=STICKERS) || toolbarExpanded)addToolbar();
        if(mode==STICKERS){
            addStickerPanel();
        }else if(mode==CLIPBOARD){
            addClipboardPanel();
        }else if(mode==LANGUAGE){
            addLanguagePicker();
        }else if(mode==ABC){
            // Reference screenshot has a single tools row above the keys.
            // Local predictions remain available as an optional setting.
            if(settings().getBoolean("show_suggestions",true))addSuggestions();
            else addVoiceOnlyRow();
            if(selectedLanguage!=IndicLanguages.ENGLISH)addLanguageStatus();

            if(settings().getBoolean("number_row",false))
                addRow("1 2 3 4 5 6 7 8 9 0");
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
        }else if(mode==SYMBOLS){
            addRow("_ ~ ` | • § © ® ™ ✓");
            addRow("[ ] { } \\ ^ € £ ¥ ¢");
            addRow("« » ‹ › ° ± × ÷ ≈ ⌫");
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
        // The S Board brand is also a clearly accessible close/return control.
        identity.setClickable(true);
        identity.setContentDescription("S Board, close tools and return to typing");
        identity.setOnClickListener(v->{
            feedback(v);
            finishRomanWord();
            mode=ABC;
            toolbarExpanded=false;
            rebuild();
        });
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
        String[] ids={"ABC","123","∑ Math","⚛ Sci","😊","🌐","🎨","⚙","📋","🧩"};
        View active=null;
        for(int i=0;i<ids.length;i++){
            final int index=i;
            TextView tab=new TextView(this);
            tab.setText(ids[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(i<2?15:((i==2||i==3)?13:20));
            tab.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
            boolean chosen=index<5&&mode==index;
            tab.setTextColor(chosen?(theme>=5?ThemePalette.ON_ACCENT[theme]:
                    (light()?Color.WHITE:0xff041b33)):
                (light()?0xff193c69:Color.WHITE));
            GradientDrawable background=new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                chosen?
                    (theme>=5?new int[]{ThemePalette.ENTER_TOP[theme],ThemePalette.ENTER_BOTTOM[theme]}:
                        (light()?new int[]{0xff387cfa,0xff235dd1}:
                            new int[]{0xff7bffff,0xff0ed5ee})):
                    (theme>=5?
                        new int[]{ThemePalette.FUNCTION_TOP[theme],ThemePalette.FUNCTION_BOTTOM[theme]}:
                        (light()?new int[]{0xfff4f8fd,0xffdce7f4}:
                            new int[]{0xff19476d,0xff061d3c})));
            background.setCornerRadius(dp(15));
            background.setStroke(dp(1),chosen?ACCENTS[theme]:
                (theme>=5?ThemePalette.STROKE[theme]:(light()?0xffbfd4ed:0xff29cfff)));
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
                index==7?"Keyboard settings":index==8?"Open clipboard with paste and history options":index==9?"Original sticker packs":"Switch keyboard to "+ids[i]);
            tab.setOnClickListener(v->{
                feedback(v);
                if(index<5){
                    finishRomanWord();mode=index;shifted=false;toolbarExpanded=false;rebuild();
                }else if(index==5){
                    finishRomanWord();mode=LANGUAGE;rebuild();
                }else if(index==6){
                    Intent gallery=new Intent(this,ThemeGalleryActivity.class);
                    gallery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try{startActivity(gallery);}catch(RuntimeException ignored){}
                }else if(index==8){
                    openClipboardPanel();
                }else if(index==9){
                    finishRomanWord();
                    mode=STICKERS;stickerPack=0;toolbarExpanded=false;rebuild();
                }else{
                    finishRomanWord();
                    Intent intent=new Intent(this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try{startActivity(intent);}catch(RuntimeException ignored){}
                }
            });
            if(index==6){
                tab.setOnLongClickListener(v->{
                    theme=(theme+1)%ThemePalette.count();
                    settings().edit().putInt(KEY_THEME,theme).apply();
                    feedback(v);rebuild();
                    return true;
                });
            }
        }
        scroller.addView(bar);
        int toolHeight=dp(getResources().getConfiguration().orientation==2?49:51);
        keysContent.addView(scroller,new LinearLayout.LayoutParams(-1,toolHeight));
        if(active!=null && mode!=ABC && mode!=NUMBER && mode!=SYMBOLS){
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
        TextView grid=new TextView(this);
        grid.setText("▦");grid.setTextSize(26);
        grid.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        grid.setGravity(Gravity.CENTER);
        grid.setTextColor(light()?0xff303542:Color.WHITE);
        grid.setContentDescription("Show S Board tools, maths, science, languages, emoji, settings");
        strip.addView(grid,new LinearLayout.LayoutParams(dp(42),dp(39)));
        grid.setOnClickListener(v->{feedback(v);toolbarExpanded=!toolbarExpanded;rebuild();});
        suggestionChips=new TextView[4];
        for(int i=0;i<suggestionChips.length;i++){
            TextView chip=new TextView(this);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            chip.setTextSize(14);
            chip.setTextColor(light()?0xff272a35:0xfff0f8ff);
            chip.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light()?0x222d3545:0x44a4ecff),
                new GradientDrawable(),null));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(36),1f);
            lp.setMargins(dp(1),0,dp(1),0);
            strip.addView(chip,lp);
            chip.setOnClickListener(v->{
                if(sensitiveField)return;
                Object tag=chip.getTag();
                if(tag instanceof String)applySuggestion((String)tag);
            });
            suggestionChips[i]=chip;
            if(i<suggestionChips.length-1){
                View divider=new View(this);
                divider.setBackgroundColor(light()?0xff949baa:0x7048a5cb);
                strip.addView(divider,new LinearLayout.LayoutParams(dp(1),dp(23)));
            }
        }
        TextView mic=new TextView(this);
        mic.setText("🎙");mic.setTextSize(23);
        mic.setTextColor(light()?0xff303542:Color.WHITE);
        mic.setGravity(Gravity.CENTER);
        mic.setContentDescription("Tap to dictate your message using microphone");
        attachClipboardShortcut(strip);
        strip.addView(mic,new LinearLayout.LayoutParams(dp(44),dp(39)));
        voiceMicButton=mic;
        mic.setOnClickListener(v->{feedback(v);toggleVoiceDictation();});
        refreshVoiceMic();
        keysContent.addView(strip,new LinearLayout.LayoutParams(-1,dp(42)));
        updateSuggestions();
    }
    /** The microphone remains accessible when word suggestions are disabled. */
    private void addVoiceOnlyRow(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView openTools=new TextView(this);
        openTools.setText("▦");openTools.setTextSize(24);
        openTools.setGravity(Gravity.CENTER);
        openTools.setTextColor(light()?0xff303542:Color.WHITE);
        openTools.setContentDescription("Show S Board keyboard tools");
        row.addView(openTools,new LinearLayout.LayoutParams(dp(44),dp(40)));
        openTools.setOnClickListener(v->{feedback(v);toolbarExpanded=!toolbarExpanded;rebuild();});
        TextView hint=new TextView(this);
        hint.setText("🎙 Tap microphone to dictate");
        hint.setGravity(Gravity.CENTER);
        hint.setTextSize(13);
        hint.setTextColor(light()?0xff303542:Color.WHITE);
        row.addView(hint,new LinearLayout.LayoutParams(0,dp(40),1f));
        TextView mic=new TextView(this);
        mic.setText("🎙");mic.setTextSize(23);
        mic.setGravity(Gravity.CENTER);
        attachClipboardShortcut(row);
        row.addView(mic,new LinearLayout.LayoutParams(dp(48),dp(40)));
        voiceMicButton=mic;
        mic.setOnClickListener(v->{feedback(v);toggleVoiceDictation();});
        refreshVoiceMic();
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));
    }

    /** Clipboard content is accessed only while S Board is the visible IME. */
    private ClipDescription safeClipDescription(){
        if(clipboardManager==null||!keyboardVisible)return null;
        try{return clipboardManager.getPrimaryClipDescription();}
        catch(RuntimeException ignored){return null;}
    }
    private static boolean sensitiveClip(ClipDescription d){
        return d!=null && android.os.Build.VERSION.SDK_INT>=24 &&
            d.getExtras()!=null &&
            d.getExtras().getBoolean("android.content.extra.IS_SENSITIVE",false);
    }
    private boolean clipboardTextAvailable(){
        if(sensitiveField)return false;
        ClipDescription d=safeClipDescription();
        return d!=null&&!sensitiveClip(d)&&
            (d.hasMimeType("text/plain")||d.hasMimeType("text/html"));
    }
    private String plainClipboardText(){
        if(!clipboardTextAvailable())return null;
        try{
            ClipData data=clipboardManager.getPrimaryClip();
            if(data==null||data.getItemCount()<1)return null;
            ClipData.Item item=data.getItemAt(0);
            if(item.getText()!=null)return item.getText().toString();
            String html=item.getHtmlText();
            if(html==null)return null;
            return android.os.Build.VERSION.SDK_INT>=24?
                Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString():
                Html.fromHtml(html).toString();
        }catch(RuntimeException error){return null;}
    }
    private void attachClipboardShortcut(LinearLayout row){
        TextView b=new TextView(this);
        b.setText("📋");
        b.setTextSize(21);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(light()?0xff293b60:Color.WHITE);
        b.setContentDescription("Tap to paste copied text. Long press for clipboard options.");
        row.addView(b,new LinearLayout.LayoutParams(dp(39),dp(39)));
        clipboardShortcut=b;
        b.setOnClickListener(v->{feedback(v);pasteCurrent(true);});
        b.setOnLongClickListener(v->{feedback(v);openClipboardPanel();return true;});
        updateClipboardShortcut();
    }
    private void updateClipboardShortcut(){
        if(clipboardShortcut!=null)
            clipboardShortcut.setVisibility(clipboardTextAvailable()?View.VISIBLE:View.GONE);
    }
    private void onClipboardChanged(){
        if(Looper.myLooper()!=Looper.getMainLooper()){
            suggestionHandler.post(this::onClipboardChanged);return;
        }
        if(!keyboardVisible)return;
        // Disabled by default. Do not inspect clipboard contents just to show a shortcut.
        if(!sensitiveField&&clipboardHistory!=null&&clipboardHistory.enabled()){
            String text=plainClipboardText();
            if(text!=null&&text.length()<=ClipboardHistoryStore.MAX_TEXT)
                clipboardHistory.add(text);
        }
        updateClipboardShortcut();
        if(mode==CLIPBOARD)rebuild();
    }
    private void openClipboardPanel(){
        if(sensitiveField){
            Toast.makeText(this,"Clipboard history is hidden in password fields.",
                Toast.LENGTH_SHORT).show();return;
        }
        cancelVoiceDictation();
        finishRomanWord();
        mode=CLIPBOARD;
        toolbarExpanded=false;
        rebuild();
    }
    private void closeClipboardPanel(){
        mode=ABC;toolbarExpanded=false;rebuild();
    }
    /** Forward styled paste to the receiving editor; that app chooses what to retain. */
    private void pasteCurrent(boolean preserveFormatting){
        InputConnection conn=getCurrentInputConnection();
        if(conn==null)return;
        if(voiceDictation!=null&&voiceDictation.isActive())cancelVoiceDictation();
        finishRomanWord();
        if(preserveFormatting){
            ClipDescription clip=safeClipDescription();
            if(clip==null){
                Toast.makeText(this,"Nothing copied yet.",Toast.LENGTH_SHORT).show();
                return;
            }
            boolean accepted=conn.performContextMenuAction(android.R.id.paste);
            if(!accepted){
                Toast.makeText(this,"This app did not accept formatted paste. Try Paste plain.",
                    Toast.LENGTH_LONG).show();
                return;
            }
        }else{
            String copied=plainClipboardText();
            if(copied==null||copied.isEmpty()){
                Toast.makeText(this,"No copied text available.",Toast.LENGTH_SHORT).show();
                return;
            }
            conn.commitText(copied,1);
        }
        if(mode==CLIPBOARD)closeClipboardPanel();
        else requestSuggestions();
    }

    private TextView clipboardButton(String label,int textSize){
        TextView b=new TextView(this);
        b.setText(label);
        b.setTextSize(textSize);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.setPadding(dp(5),0,dp(5),0);
        b.setTextColor(light()?0xff18365a:Color.WHITE);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(light()?0xffedf2fc:0xff244b66);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1),light()?0xffb6cce6:0xff45bfd6);
        b.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x334a86ee),bg,null));
        return b;
    }
    private void clipboardRow(LinearLayout row,TextView child,float weight){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),weight);
        lp.setMargins(dp(2),0,dp(2),0);
        row.addView(child,lp);
    }
    private void addClipboardPanel(){
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=clipboardButton("ABC",15);
        clipboardRow(header,back,.65f);
        back.setOnClickListener(v->{feedback(v);closeClipboardPanel();});
        TextView title=new TextView(this);
        title.setText("📋 Clipboard");
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setTextColor(light()?0xff213954:Color.WHITE);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(42),1.35f));
        final boolean historyOn=clipboardHistory!=null&&clipboardHistory.enabled();
        TextView toggle=clipboardButton(historyOn?"24h: ON":"24h: OFF",12);
        clipboardRow(header,toggle,1f);
        toggle.setContentDescription(historyOn?
            "Disable and erase encrypted clipboard history":
            "Enable encrypted clipboard history for up to 24 hours");
        toggle.setOnClickListener(v->{
            if(clipboardHistory==null)return;
            boolean enable=!clipboardHistory.enabled();
            clipboardHistory.setEnabled(enable);
            if(enable){
                String now=plainClipboardText();
                if(now!=null)clipboardHistory.add(now);
                Toast.makeText(this,"History ON. Copies made while S Board is open are saved encrypted for up to 24 hours.",
                    Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,"History OFF. Saved clips deleted.",
                    Toast.LENGTH_SHORT).show();
            }
            rebuild();
        });
        keysContent.addView(header,new LinearLayout.LayoutParams(-1,dp(46)));

        TextView details=new TextView(this);
        details.setText("Paste with formatting depends on app support. History saves plain text only.");
        details.setTextSize(11);
        details.setGravity(Gravity.CENTER);
        details.setMaxLines(2);
        details.setTextColor(light()?0xff49617b:0xffc1dff1);
        keysContent.addView(details,new LinearLayout.LayoutParams(-1,dp(32)));

        LinearLayout pastes=new LinearLayout(this);
        pastes.setGravity(Gravity.CENTER);
        TextView formatted=clipboardButton("Paste with formatting",12);
        TextView plain=clipboardButton("Paste plain text",12);
        clipboardRow(pastes,formatted,1f);
        clipboardRow(pastes,plain,1f);
        formatted.setOnClickListener(v->{feedback(v);pasteCurrent(true);});
        plain.setOnClickListener(v->{feedback(v);pasteCurrent(false);});
        keysContent.addView(pastes,new LinearLayout.LayoutParams(-1,dp(46)));

        TextView current=new TextView(this);
        String clip=plainClipboardText();
        current.setText(clip==null||clip.trim().isEmpty()?
            "Current clipboard: empty or private":
            "Current: "+(clip.length()>65?clip.substring(0,65)+"…":clip));
        current.setTextColor(light()?0xff254463:0xffd0e9fa);
        current.setTextSize(12);
        current.setSingleLine(true);
        current.setEllipsize(android.text.TextUtils.TruncateAt.END);
        current.setPadding(dp(9),0,dp(9),0);
        current.setGravity(Gravity.CENTER_VERTICAL);
        keysContent.addView(current,new LinearLayout.LayoutParams(-1,dp(29)));

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        java.util.ArrayList<ClipboardHistoryStore.Entry> entries=clipboardHistory==null?
            new java.util.ArrayList<>():clipboardHistory.entries();
        if(!historyOn||entries.isEmpty()){
            TextView empty=new TextView(this);
            empty.setText(historyOn?
                "No saved copies yet. Copy text while S Board is open.":
                "History OFF by default. Enable 24h history above.");
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(light()?0xff50647e:0xffbedbea);
            list.addView(empty,new LinearLayout.LayoutParams(-1,dp(95)));
        }else{
            for(ClipboardHistoryStore.Entry entry:entries){
                final String copied=entry.text;
                String preview=copied.replace('\n',' ').replace('\r',' ');
                TextView item=clipboardButton(preview,12);
                item.setContentDescription("Paste saved clipboard text");
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(40));
                lp.setMargins(dp(4),dp(2),dp(4),dp(2));
                list.addView(item,lp);
                item.setOnClickListener(v->{
                    InputConnection conn=getCurrentInputConnection();
                    if(conn!=null&&!sensitiveField){
                        finishRomanWord();
                        conn.commitText(copied,1);
                    }
                    closeClipboardPanel();
                });
            }
        }
        scroller.addView(list);
        keysContent.addView(scroller,new LinearLayout.LayoutParams(-1,dp(117)));

        LinearLayout actions=new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        TextView clear=clipboardButton("Clear saved history",12);
        clipboardRow(actions,clear,1f);
        clear.setOnClickListener(v->{
            if(clipboardHistory!=null)clipboardHistory.clear();
            rebuild();
        });
        TextView returnButton=clipboardButton("↵ Back to keyboard",12);
        clipboardRow(actions,returnButton,1f);
        returnButton.setOnClickListener(v->closeClipboardPanel());
        keysContent.addView(actions,new LinearLayout.LayoutParams(-1,dp(43)));
    }

    /** Original sticker art stays on-device until the user explicitly taps one. */
    private void addStickerPanel(){
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView abc=clipboardButton("ABC",15);
        clipboardRow(header,abc,.70f);
        abc.setOnClickListener(v->{feedback(v);mode=ABC;rebuild();});
        TextView title=new TextView(this);
        title.setText("🧩 "+StickerCatalog.PACKS[stickerPack]);
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(light()?0xff173354:Color.WHITE);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(42),2.0f));
        TextView prev=clipboardButton("‹",22);
        TextView next=clipboardButton("›",22);
        clipboardRow(header,prev,.45f);
        clipboardRow(header,next,.45f);
        prev.setContentDescription("Previous sticker pack");
        next.setContentDescription("Next sticker pack");
        prev.setOnClickListener(v->{stickerPack=Math.floorMod(stickerPack-1,StickerCatalog.packCount());rebuild();});
        next.setOnClickListener(v->{stickerPack=(stickerPack+1)%StickerCatalog.packCount();rebuild();});
        keysContent.addView(header,new LinearLayout.LayoutParams(-1,dp(45)));

        HorizontalScrollView chooser=new HorizontalScrollView(this);
        chooser.setHorizontalScrollBarEnabled(false);
        LinearLayout packs=new LinearLayout(this);
        packs.setGravity(Gravity.CENTER_VERTICAL);
        for(int i=0;i<StickerCatalog.packCount();i++){
            final int pack=i;
            TextView tag=clipboardButton(StickerCatalog.PACKS[i],12);
            tag.setBackgroundColor(pack==stickerPack?
                (light()?0xffb2cbff:0xff295b8b):
                (light()?0xffe8eef8:0xff254d63));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(111),dp(36));
            lp.setMargins(dp(3),0,dp(3),0);
            packs.addView(tag,lp);
            tag.setOnClickListener(v->{stickerPack=pack;rebuild();});
        }
        chooser.addView(packs);
        keysContent.addView(chooser,new LinearLayout.LayoutParams(-1,dp(39)));

        int size=dp(getResources().getConfiguration().orientation==2?57:72);
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            for(int col=0;col<3;col++){
                int index=StickerCatalog.id(stickerPack,row*3+col);
                LinearLayout tile=new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setContentDescription("Insert "+StickerCatalog.text(index)+" sticker image");
                Bitmap bitmap=StickerCatalog.draw(index,184);
                ImageView imageView=new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                tile.addView(imageView,new LinearLayout.LayoutParams(-1,size));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,size+dp(4),1f);
                lp.setMargins(dp(3),0,dp(3),0);
                line.addView(tile,lp);
                tile.setOnClickListener(v->{feedback(v);sendSticker(index);});
            }
            keysContent.addView(line,new LinearLayout.LayoutParams(-1,size+dp(8)));
        }
        TextView hint=new TextView(this);
        hint.setText("Tap a sticker to send a PNG where supported; other apps get emoji text.");
        hint.setTextSize(11);hint.setGravity(Gravity.CENTER);
        hint.setMaxLines(2);
        hint.setTextColor(light()?0xff334b66:0xffc1e8f6);
        keysContent.addView(hint,new LinearLayout.LayoutParams(-1,dp(38)));
    }
    private void sendSticker(int index){
        if(sensitiveField){
            Toast.makeText(this,"Stickers are unavailable in password fields.",
                Toast.LENGTH_SHORT).show();return;
        }
        InputConnection connection=getCurrentInputConnection();
        if(connection==null)return;
        finishRomanWord();
        boolean inserted=false;
        if(android.os.Build.VERSION.SDK_INT>=25){
            EditorInfo info=getCurrentInputEditorInfo();
            String[] types=info==null?null:info.contentMimeTypes;
            boolean accepts=false;
            if(types!=null){
                for(String type:types){
                    if("image/png".equalsIgnoreCase(type)||"image/*".equalsIgnoreCase(type)){
                        accepts=true;break;
                    }
                }
            }
            if(accepts){
                try{
                    InputContentInfo sticker=new InputContentInfo(
                        StickerProvider.uriFor(index),
                        new ClipDescription("S Board original sticker",
                            new String[]{"image/png"}),null);
                    inserted=connection.commitContent(sticker,
                        InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,null);
                }catch(RuntimeException ignored){inserted=false;}
            }
        }
        if(!inserted){
            connection.commitText(StickerCatalog.emoji(index)+" "+
                StickerCatalog.text(index)+" ",1);
            Toast.makeText(this,
                "This app accepts sticker text, not keyboard PNG images.",
                Toast.LENGTH_SHORT).show();
        }
    }
    /** Start one speech recognition session on explicit tap, with runtime consent. */
    private void toggleVoiceDictation(){
        if(sensitiveField){
            Toast.makeText(this,"Voice typing is disabled in password fields.",
                Toast.LENGTH_SHORT).show();
            return;
        }
        if(voiceDictation!=null&&voiceDictation.isActive()){
            voiceDictation.stop();return;
        }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=
            PackageManager.PERMISSION_GRANTED){
            // Only a foreground Activity can display Android's permission dialog.
            Intent consent=new Intent(this,VoicePermissionActivity.class);
            consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try{startActivity(consent);}
            catch(RuntimeException error){
                Toast.makeText(this,"Open S Board settings and allow microphone permission.",
                    Toast.LENGTH_LONG).show();
            }
            return;
        }
        if(getCurrentInputConnection()==null){
            Toast.makeText(this,"Tap a message field before using voice typing.",
                Toast.LENGTH_SHORT).show();
            return;
        }
        if(voiceDictation!=null)cancelVoiceDictation();
        final long expectedVoiceSession=++voiceSessionToken;
        voiceDictation=new VoiceDictation(this,new VoiceDictation.Callback(){
            @Override public void onVoiceState(boolean active,String status){
                if(expectedVoiceSession!=voiceSessionToken)return;
                voiceStatus=active?status:"";
                refreshVoiceMic();
                if(!active && status!=null && !status.isEmpty()){
                    Toast.makeText(VisionKeyboardService.this,status,Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onVoiceText(String text){
                if(expectedVoiceSession!=voiceSessionToken)return;
                insertVoiceText(text);
            }
        });
        voiceDictation.start(VoiceLanguageTags.forLanguage(selectedLanguage));
    }
    private void cancelVoiceDictation(){
        voiceSessionToken++;
        if(voiceDictation!=null){
            VoiceDictation old=voiceDictation;
            voiceDictation=null;
            old.cancel();
        }
        voiceStatus="";
        refreshVoiceMic();
    }
    private void refreshVoiceMic(){
        if(voiceMicButton!=null){
            boolean live=voiceDictation!=null&&voiceDictation.isActive();
            voiceMicButton.setText(live?"■":"🎙");
            voiceMicButton.setContentDescription(live?
                (voiceStatus.isEmpty()?"Stop dictating and convert speech to text":voiceStatus):
                "Voice typing: tap to speak");
            voiceMicButton.setTextColor(live?0xffe34b57:
                (light()?0xff303542:Color.WHITE));
        }
    }
    /** Writes only final recognized text into the currently active editor. */
    private void insertVoiceText(String text){
        if(text==null||text.trim().isEmpty()||sensitiveField)return;
        InputConnection conn=getCurrentInputConnection();
        if(conn==null){
            Toast.makeText(this,"Message field closed. Please dictate again.",
                Toast.LENGTH_SHORT).show();
            return;
        }
        finishRomanWord();
        conn.beginBatchEdit();
        try{
            CharSequence before=conn.getTextBeforeCursor(1,0);
            String prefix=(before!=null && before.length()>0 &&
                !Character.isWhitespace(before.charAt(before.length()-1)))?" ":"";
            conn.commitText(prefix+text.trim()+" ",1);
        }finally{conn.endBatchEdit();}
        lastSuggestionKey=null;
        requestSuggestions();
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
            // The editor may fire onUpdateSelection for the same typed prefix
            // after press() has already drawn these chips. Skip re-rendering it.
            String lookupKey=selectedLanguage+":"+mode+":"+(sensitiveField?"private":typed);
            if(lookupKey.equals(lastSuggestionKey))return;
            lastSuggestionKey=lookupKey;
            String[] candidates=sensitiveField||mode!=ABC?
                new String[0]:WordSuggestions.get(typed,
                    selectedLanguage!=IndicLanguages.ENGLISH,4);
            String[] fallback=sensitiveField?
                new String[]{"Private typing","","",""}:
                new String[]{"S Board","Start typing","Suggestions appear here",""};
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
        if(voiceDictation!=null&&voiceDictation.isActive())cancelVoiceDictation();
        InputConnection conn=getCurrentInputConnection();
        if(conn==null||sensitiveField||suggestion==null||suggestion.isEmpty())return;
        if(phoneticEnabled()){
            conn.setComposingText(
                OfflinePhonetic.convert(suggestion,selectedLanguage),1);
            conn.finishComposingText();
            romanComposing.setLength(0);
            conn.commitText(" ",1);
        }else{
            // Keep replacement atomic for IME editors and avoid intermediate
            // partial words appearing in the host application.
            conn.beginBatchEdit();
            try{
                String before=WordSuggestions.trailingWord(conn.getTextBeforeCursor(36,0));
                if(!before.isEmpty())conn.deleteSurroundingText(before.length(),0);
                conn.commitText(suggestion+" ",1);
            }finally{
                conn.endBatchEdit();
            }
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
        mode=ABC;shifted=false;caps=false;toolbarExpanded=false;
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
                item.setTextColor(current?ThemePalette.ON_ACCENT[theme]:(light()?0xff253955:Color.WHITE));
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
        // press() refreshes suggestions once after committing this key.
        // Do not duplicate the same expensive InputConnection lookup here.
    }
    private void addEmojiCategories(){
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setFillViewport(false);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setHorizontalFadingEdgeEnabled(true);
        LinearLayout categories=new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categories.setGravity(Gravity.CENTER_VERTICAL);
        String[] icons={"😀","🥰","😢","👋","🧑","🐼","🌿","🍕","🎉","💯","🧬","🎭"};
        for(int i=0;i<EmojiCatalog.count();i++){
            final int category=i;
            TextView chip=new TextView(this);
            chip.setGravity(Gravity.CENTER);
            chip.setText(icons[i]+" "+EmojiCatalog.title(i));
            chip.setTextSize(12);
            chip.setTextColor(category==emojiCategory?ThemePalette.ON_ACCENT[theme]:(light()?0xff263a56:Color.WHITE));
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
        addKey(row,submitKeyLabel(),"ENTER",.75f,height,true);
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(2)));
    }
    /** Editor-specific submit key. Search fields use search; chats use Enter/Send. */
    private boolean isSearchEditor(){
        EditorInfo info=getCurrentInputEditorInfo();
        return info!=null &&
            (info.imeOptions&EditorInfo.IME_MASK_ACTION)==EditorInfo.IME_ACTION_SEARCH;
    }
    private String submitKeyLabel(){return isSearchEditor()?"⌕":"↵";}
    private void addBottom(){
        // One number-mode key plus a context-aware Enter/Search submit action.
        LinearLayout row=new LinearLayout(this);
        row.setTag("sboard_space_action");
        row.setGravity(Gravity.CENTER);
        int height=dp(getResources().getConfiguration().orientation==2?57:62);
        if(mode==ABC){
            // Keep only the left ?123; use right for Enter/Search instead.
            addKey(row,"?123","123",1.05f,height,true);
            addKey(row,"☺","EMOJI",.90f,height,true);
            if(settings().getBoolean("arrow_keys",true)){
                addKey(row,"Space","SPACE",5.7f,height,true);
                addKey(row,"◀","←",.70f,height,true);
                addKey(row,"▶","CURSOR_RIGHT",.70f,height,true);
            }else{
                addKey(row,"Space","SPACE",7.1f,height,true);
            }
            addKey(row,submitKeyLabel(),"ENTER",1.05f,height,true);
        }else if(mode==NUMBER||mode==SYMBOLS){
            // Familiar two-page symbol layout:
            // ABC always returns to letters. The separate adjacent key
            // switches between basic numbers and the extended symbols page.
            addKey(row,"ABC","ABC",1.12f,height,true);
            addKey(row,mode==NUMBER?"=<":"?123",
                mode==NUMBER?"SYM_MORE":"SYM_BACK",.84f,height,true);
            addKey(row,"☺","EMOJI",.78f,height,true);
            addKey(row,"Space","SPACE",4.35f,height,true);
            addKey(row,",",",",.60f,height,false);
            addKey(row,".",".",.60f,height,false);
            addKey(row,submitKeyLabel(),"ENTER",1.16f,height,true);
        }else{
            // Maths and Science keep their original quick language toggle.
            addKey(row,"ABC","ABC",1.02f,height,true);
            addKey(row,"☺","EMOJI",.82f,height,true);
            addKey(row,"A↔अ","LANG_TOGGLE",.85f,height,true);
            addKey(row,"Space","SPACE",4.35f,height,true);
            addKey(row,",",",",.60f,height,false);
            addKey(row,".",".",.60f,height,false);
            addKey(row,submitKeyLabel(),"ENTER",1.16f,height,true);
        }
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(3)));
    }
    private static boolean isAction(String value){
        return value.equals("⇧")||value.equals("⌫")||value.equals("ABC")||
            value.equals("123")||value.equals("MATH")||value.equals("SCI")||
            value.equals("EMOJI")||value.equals("EMOJI_PREV")||value.equals("EMOJI_NEXT")||
            value.equals("SPACE")||value.equals("ENTER")||value.equals("←")||value.equals("CURSOR_RIGHT")||
            value.equals("SYM_MORE")||value.equals("SYM_BACK")||
            value.startsWith("LANG_");
    }
    private void addKey(LinearLayout row,String display,String output,float weight,int height,boolean special){
        SBoardKeyButton b=new SBoardKeyButton(this);
        b.setSpaceIndicator("SPACE".equals(output),light(),ACCENTS[theme]);
        b.setKeyPopup(settings().getBoolean("key_popups",false)&&
            mode==ABC && output.length()==1 && Character.isLetter(output.charAt(0)),
            display);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(display);
        b.setIncludeFontPadding(false);
        int length=display.length();
        boolean largerLetters=settings().getBoolean("large_key_text",true);
        b.setTextSize("SPACE".equals(output)?17f:
            (mode==EMOJI? (length>4?18f:23f) :
            length>=5?11.5f:length>=4?13f:length>=3?15f:
                (largerLetters?22f:18f)));
        b.setTypeface(Typeface.create("sans-serif-medium",
            special||isAction(output)?Typeface.BOLD:Typeface.NORMAL));
        final boolean enter="ENTER".equals(output);
        final boolean functionKey=special||isAction(output);
        b.setTextColor(theme==4?
            (enter?Color.WHITE:("SPACE".equals(output)?0xff2a2d37:
                (functionKey?Color.WHITE:0xff242731))):theme>=5?
            (enter?ThemePalette.ON_ACCENT[theme]:ThemePalette.TEXT[theme]):
            (light()? (enter?Color.WHITE:(functionKey?0xff2056bb:0xff162338)):
             (enter?0xff082440:("SPACE".equals(output)?Color.WHITE:
                (functionKey?0xffadf9ff:Color.WHITE)))));
        // Never create per-key press animations; keep native ripple and fast tap dispatch.
        b.setStateListAnimator(null);
        // Disabling button-level audio/haptics avoids redundant system work
        // during rapid taps. The optional vibration is applied explicitly.
        b.setSoundEffectsEnabled(settings().getBoolean("key_sound",false));
        b.setHapticFeedbackEnabled(haptics);
        b.setPadding(0,0,0,0);b.setMinimumHeight(0);b.setMinHeight(0);
        b.setMinimumWidth(0);b.setMinWidth(0);
        int upper,lower,stroke;
        if(theme>=5){
            upper=enter?ThemePalette.ENTER_TOP[theme]:
                ("SPACE".equals(output)?ThemePalette.SPACE_TOP[theme]:
                    (functionKey?ThemePalette.FUNCTION_TOP[theme]:ThemePalette.KEY_TOP[theme]));
            lower=enter?ThemePalette.ENTER_BOTTOM[theme]:
                ("SPACE".equals(output)?ThemePalette.SPACE_BOTTOM[theme]:
                    (functionKey?ThemePalette.FUNCTION_BOTTOM[theme]:ThemePalette.KEY_BOTTOM[theme]));
            stroke=ThemePalette.STROKE[theme];
        }else if(theme==4){
            // Flat, high-contrast slate function keys and pure white letter keys.
            upper=enter?0xff747b8f:("SPACE".equals(output)?Color.WHITE:
                (functionKey?0xff747b8f:Color.WHITE));
            lower=enter?0xff697185:("SPACE".equals(output)?Color.WHITE:
                (functionKey?0xff697185:Color.WHITE));
            stroke=functionKey?0xff7a8090:0xffeceef4;
        }else if(light()){
            upper=enter?0xff3c82f6:(functionKey?0xffe6eefb:0xffffffff);
            lower=enter?0xff1d55cf:(functionKey?0xffdbe7f8:0xfff7faff);
            stroke=enter?0xff245fd7:0xffcbd6e6;
        }else{
            // Preserve the already-approved original glossy neon-blue keycaps.
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
        // Increase practical tap target without changing rows or key labels.
        lp.setMargins(dp(1.5f),dp(1.0f),dp(1.5f),dp(2.0f));
        row.addView(b,lp);
        b.setContentDescription("⌫".equals(output)?"Delete last character":
            ("SPACE".equals(output)?"Spacebar - insert space":
                ("ENTER".equals(output)?(isSearchEditor()?"Search":"Enter or send"):output)));
        // Long-press alternatives are handled on touch-down with the
        // user-selected timer; they never rely on Android's fixed timeout.
        final String[] alternatives=longPressAlternatives(output);
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
            // Key commits on ACTION_UP, with optional truly adjustable
            // long-press alternatives. No per-tap object allocations.
            b.setOnClickListener(v->{feedback(v);press(output);});
            final Runnable[] pending={null};
            final boolean[] held={false};
            b.setLongClickable(false); // Disable the unrelated OS timeout.
            b.setOnTouchListener((view,event)->{
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        held[0]=false;
                        view.setPressed(true);
                        if(alternatives!=null){
                            Runnable task=()->{
                                held[0]=true;
                                view.setPressed(false);
                                if("#LANG".equals(alternatives[0])){
                                    feedback(view);finishRomanWord();
                                    mode=LANGUAGE;rebuild();
                                }else{
                                    showLongPressChoices(view,alternatives);
                                }
                            };
                            pending[0]=task;
                            int ms=Math.max(250,Math.min(700,
                                settings().getInt("long_press_duration",450)));
                            repeatHandler.postDelayed(task,ms);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        boolean inBounds=event.getX()>=-dp(10) &&
                            event.getX()<view.getWidth()+dp(10) &&
                            event.getY()>=-dp(10) &&
                            event.getY()<view.getHeight()+dp(10);
                        view.setPressed(inBounds);
                        if(!inBounds&&pending[0]!=null){
                            repeatHandler.removeCallbacks(pending[0]);pending[0]=null;
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(pending[0]!=null){
                            repeatHandler.removeCallbacks(pending[0]);pending[0]=null;
                        }
                        boolean inside=event.getX()>=-dp(10) &&
                            event.getX()<view.getWidth()+dp(10) &&
                            event.getY()>=-dp(10) &&
                            event.getY()<view.getHeight()+dp(10);
                        view.setPressed(false);
                        if(inside&&!held[0])view.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        if(pending[0]!=null){
                            repeatHandler.removeCallbacks(pending[0]);pending[0]=null;
                        }
                        view.setPressed(false);return true;
                    default:return true;
                }
            });
        }
    }
    /** Long-press symbols and accented characters; never stored or uploaded. */
    private String[] longPressAlternatives(String output){
        if(mode!=ABC)return null;
        if("123".equals(output))return new String[]{"#LANG"};
        if(output.length()!=1)return null;
        String letter=output.toLowerCase(java.util.Locale.ROOT);
        boolean accents=settings().getBoolean("accented_characters",true);
        if(accents){
            switch(letter){
                case "a":return new String[]{"á","à","â","ä","ã","å","ā"};
                case "e":return new String[]{"é","è","ê","ë","ē"};
                case "i":return new String[]{"í","ì","î","ï","ī"};
                case "o":return new String[]{"ó","ò","ô","ö","õ","ō"};
                case "u":return new String[]{"ú","ù","û","ü","ū"};
                case "n":return new String[]{"ñ","ń"};
                case "c":return new String[]{"ç","ć"};
            }
        }
        if(settings().getBoolean("long_press_symbols",false)){
            String[] keys={"q","w","e","r","t","y","u","i","o","p",
                "s","d","f","g","h","j","k","l","z","x","v","b","m"};
            String[] choices={"1","2","3","4","5","6","7","8","9","0",
                "@","#","₹","%","&","*","(",")","!","?","/","=",":"};
            for(int i=0;i<keys.length;i++)
                if(keys[i].equals(letter))return new String[]{choices[i]};
        }else{
            int index="qwertyuiop".indexOf(letter);
            if(index>=0)return new String[]{index==9?"0":String.valueOf(index+1)};
        }
        return null;
    }
    private void showLongPressChoices(View key,String[] items){
        if(items==null||items.length==0)return;
        // One alternative can be inserted without creating a floating menu.
        if(items.length==1){feedback(key);press(items[0]);return;}
        try{
            PopupMenu menu=new PopupMenu(this,key);
            for(String value:items)menu.getMenu().add(value);
            menu.setOnMenuItemClickListener(item->{
                feedback(key);press(item.getTitle().toString());return true;
            });
            menu.show();
        }catch(RuntimeException ex){
            // Some IME windows disallow anchored submenus. Still make the key work.
            feedback(key);press(items[0]);
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
        if(voiceDictation!=null&&voiceDictation.isActive())cancelVoiceDictation();
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
        boolean deleted=false;
        if(android.os.Build.VERSION.SDK_INT>=24)
            deleted=conn.deleteSurroundingTextInCodePoints(1,0);
        if(!deleted)conn.deleteSurroundingText(1,0);
        requestSuggestions();
    }
    private void press(String code){
        if(voiceDictation!=null&&voiceDictation.isActive())cancelVoiceDictation();
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
        if(code.equals("ABC")){finishRomanWord();mode=ABC;shifted=false;toolbarExpanded=false;rebuild();return;}
        if(code.equals("SYM_MORE")){mode=SYMBOLS;rebuild();return;}
        if(code.equals("SYM_BACK")){mode=NUMBER;rebuild();return;}
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
        // Toolbar actions do not need an editor IPC round-trip.
        InputConnection conn=getCurrentInputConnection();
        if(conn==null)return;
        if(code.equals("←")||code.equals("CURSOR_RIGHT")){
            finishRomanWord();
            int key=code.equals("←")?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT;
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));return;
        }
        if(code.equals("SPACE")){finishRomanWord();conn.commitText(" ",1);requestSuggestions();return;}
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
        requestSuggestions();
        if(mode==ABC && shifted && !caps && code.length()==1 && Character.isLetter(code.charAt(0))){
            shifted=false;rebuild();
        }
    }
}
