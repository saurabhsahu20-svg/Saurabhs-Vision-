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
    private static final int ABC=0,NUMBER=1,MATH=2,SCIENCE=3,EMOJI=4;
    private static final int EMOJIS_PER_PAGE=32;
    private int emojiCategory=0,emojiPage=0;
    private static final int[] ACCENTS={0xff65e9ff,0xffffd55c,0xff9affd4,0xffeea8ff,0xff2563eb};
    private static final int[] DARK={0xff071537,0xff1c1641,0xff061e32,0xff1a1040,0xffffffff};
    private static final int[] KEY_COLORS={0xd91b3561,0xdb45375e,0xdb21485b,0xdb3c2b67,0xfff8fbff};
    private KeyboardPanel keyboardFrame;
    private LinearLayout keysContent;
    private int mode=ABC,theme=0;
    private boolean shifted=false,caps=false;
    private long lastShift=0L;
    private final Handler repeatHandler=new Handler(Looper.getMainLooper());
    private boolean backspaceRepeated=false;
    private Runnable backspaceRepeat;
    private boolean haptics=true;

    @Override public void onCreate(){
        super.onCreate();
        theme=Math.floorMod(getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_THEME,0),5);
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
        super.onStartInput(info,restarting);
        if(!restarting){
            mode=info!=null && (info.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_NUMBER
                ?NUMBER:ABC;
            shifted=false;caps=false;
        }
    }
    @Override public void onStartInputView(EditorInfo info,boolean restarting){
        super.onStartInputView(info,restarting);
        theme=Math.floorMod(settings().getInt(KEY_THEME,0),5);
        if(keyboardFrame!=null)rebuild();
    }
    @Override public void onFinishInputView(boolean finishingInput){
        stopRepeat();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onDestroy(){
        stopRepeat();
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
        keysContent.setPadding(dp(3),dp(3),dp(3),dp(4));
        addToolbar();
        if(mode==ABC){
            addRow("Q W E R T Y U I O P");
            addRow("A S D F G H J K L");
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
    private void addToolbar(){
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        String[] ids={"ABC","123","∑","⚛","😊","☀","⚙"};
        for(int i=0;i<ids.length;i++){
            final int idx=i;
            TextView tab=new TextView(this);
            tab.setText(ids[i]);
            tab.setTextSize(i==2||i==3?17:13);
            tab.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setTextColor(i<5 && mode==i?DARK[theme]:(light()?0xff253955:Color.WHITE));
            GradientDrawable surface=new GradientDrawable();
            boolean selected=i<5 && mode==i;
            surface.setColor(selected?ACCENTS[theme]:(light()?0xffe4ebf6:0x66243659));
            surface.setCornerRadius(dp(14));
            surface.setStroke(dp(1),selected?ACCENTS[theme]:(light()?0xffd0dced:0x556edfff));
            tab.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light()?0x402563eb:0x40ffffff),surface,null));
            tab.setElevation(dp(light()?1.2f:.7f));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(37),i==2||i==3?1.1f:1f);
            lp.setMargins(dp(2),0,dp(2),dp(5));
            bar.addView(tab,lp);
            tab.setContentDescription(i==5?"Change keyboard theme":i==6?"Keyboard settings":"Switch keyboard to "+ids[i]);
            tab.setOnClickListener(v->{
                feedback(v);
                if(idx<5){mode=idx;shifted=false;rebuild();}
                else if(idx==5){theme=(theme+1)%5;settings().edit().putInt(KEY_THEME,theme).apply();rebuild();}
                else{Intent intent=new Intent(this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try{startActivity(intent);}catch(RuntimeException ignored){}}
            });
        }
        keysContent.addView(bar,new LinearLayout.LayoutParams(-1,dp(41)));
    }
    private void addRow(String keys){
        String[] labels=keys.split(" ");
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        boolean landscape=getResources().getConfiguration().orientation==2;
        int height=dp(landscape?40:48);
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
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        int height=dp(getResources().getConfiguration().orientation==2?43:51);
        if(mode!=ABC)addKey(row,"ABC","ABC",1.10f,height,true);
        else addKey(row,"123","123",1.05f,height,true);
        if(mode==ABC)addKey(row,"∑","MATH",.95f,height,true);
        else if(mode==MATH)addKey(row,"⚛","SCI",.85f,height,true);
        else addKey(row,"π","MATH",.85f,height,true);
        addKey(row,"😊","EMOJI",.74f,height,true);
        addKey(row,",",",",.60f,height,false);
        addKey(row,"SPACE","SPACE",3.75f,height,true);
        addKey(row,".",".",.6f,height,false);
        addKey(row,"↵","ENTER",1.05f,height,true);
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(3)));
    }
    private static boolean isAction(String value){
        return value.equals("⇧")||value.equals("⌫")||value.equals("ABC")||
            value.equals("123")||value.equals("MATH")||value.equals("SCI")||
            value.equals("EMOJI")||value.equals("EMOJI_PREV")||value.equals("EMOJI_NEXT")||
            value.equals("SPACE")||value.equals("ENTER")||value.equals("←");
    }
    private void addKey(LinearLayout row,String display,String output,float weight,int height,boolean special){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(display);
        b.setIncludeFontPadding(false);
        int length=display.length();
        b.setTextSize("SPACE".equals(output)?14f:
            (mode==EMOJI? (length>4?18f:23f) :
            length>=5?11.5f:length>=4?13f:length>=3?15f:20f));
        b.setTypeface(Typeface.create(special?"sans-serif-medium":"sans-serif",
            special||isAction(output)?Typeface.BOLD:Typeface.NORMAL));
        final boolean enter="ENTER".equals(output);
        final boolean functionKey=special||isAction(output);
        b.setTextColor(light()?
            (enter?Color.WHITE:(functionKey?0xff2056bb:0xff162338)):
            (enter?0xff072043:(functionKey?ACCENTS[theme]:Color.WHITE)));
        b.setPadding(0,0,0,0);b.setMinimumHeight(0);b.setMinHeight(0);
        b.setMinimumWidth(0);b.setMinWidth(0);
        int upper,lower,stroke;
        if(light()){
            upper=enter?0xff3c82f6:(functionKey?0xffe6eefb:0xffffffff);
            lower=enter?0xff1d55cf:(functionKey?0xffdbe7f8:0xfff7faff);
            stroke=enter?0xff245fd7:0xffcbd6e6;
        }else{
            upper=enter?ACCENTS[theme]:(functionKey?0xff2e507b:KEY_COLORS[theme]);
            lower=enter?0xff78e5ff:(functionKey?0xff15365c:0xff10284e);
            stroke=functionKey?ACCENTS[theme]:0x608db9e9;
        }
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{upper,lower});
        shape.setCornerRadius(dp(13));
        shape.setStroke(dp(1),stroke);
        b.setBackground(new RippleDrawable(
            ColorStateList.valueOf(light()?0x452563eb:0x6affffff),shape,null));
        b.setElevation(dp(light()?1.7f:1.2f));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,height-dp(4),weight);
        lp.setMargins(dp(2.4f),dp(1.1f),dp(2.4f),dp(2.5f));
        row.addView(b,lp);
        b.setContentDescription("⌫".equals(output)?"Delete last character":("SPACE".equals(output)?"Spacebar - insert space":output));
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
        if(android.os.Build.VERSION.SDK_INT>=24)conn.deleteSurroundingTextInCodePoints(1,0);
        else conn.deleteSurroundingText(1,0);
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
        if(code.equals("ABC")){mode=ABC;shifted=false;rebuild();return;}
        if(code.equals("123")){mode=NUMBER;shifted=false;rebuild();return;}
        if(code.equals("MATH")){mode=MATH;shifted=false;rebuild();return;}
        if(code.equals("SCI")){mode=SCIENCE;shifted=false;rebuild();return;}
        if(code.equals("EMOJI")){mode=EMOJI;emojiPage=0;shifted=false;rebuild();return;}
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
            int key=KeyEvent.KEYCODE_DPAD_LEFT;
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));
            conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));return;
        }
        if(code.equals("SPACE")){conn.commitText(" ",1);return;}
        if(code.equals("ENTER")){
            EditorInfo info=getCurrentInputEditorInfo();
            int action=info==null?EditorInfo.IME_ACTION_NONE:
                (info.imeOptions&EditorInfo.IME_MASK_ACTION);
            if(action!=EditorInfo.IME_ACTION_NONE && action!=EditorInfo.IME_ACTION_UNSPECIFIED)
                conn.performEditorAction(action);
            else conn.commitText("\n",1);
            return;
        }
        conn.commitText(code,1);
        if(mode==ABC && shifted && !caps && code.length()==1 && Character.isLetter(code.charAt(0))){
            shifted=false;rebuild();
        }
    }
}
