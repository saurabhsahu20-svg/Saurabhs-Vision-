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
import android.widget.FrameLayout;
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
    private static final int ABC=0,NUMBER=1,MATH=2,SCIENCE=3;
    private static final int[] ACCENTS={0xff65e9ff,0xffffd55c,0xff9affd4,0xffeea8ff};
    private static final int[] DARK={0xff071537,0xff1c1641,0xff061e32,0xff1a1040};
    private static final int[] KEY_COLORS={0xd91b3561,0xdb45375e,0xdb21485b,0xdb3c2b67};
    private FrameLayout keyboardFrame;
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
        theme=getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_THEME,0)%4;
    }
    private int dp(float value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    private SharedPreferences settings(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    @Override public View onCreateInputView(){
        keyboardFrame=new FrameLayout(this);
        rebuild();
        return keyboardFrame;
    }
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
        theme=Math.floorMod(settings().getInt(KEY_THEME,0),4);
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
    private void rebuild(){
        if(keyboardFrame==null)return;
        keyboardFrame.removeAllViews();
        keyboardFrame.addView(new KeyboardBackdrop(this,theme),
            new FrameLayout.LayoutParams(-1,-1));
        keysContent=new LinearLayout(this);
        keysContent.setOrientation(LinearLayout.VERTICAL);
        keysContent.setPadding(dp(3),dp(5),dp(3),dp(7));
        keyboardFrame.addView(keysContent,new FrameLayout.LayoutParams(-1,-2));
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
        }else{
            addRow("H₂O CO₂ O₂ NaCl H⁺ OH⁻ e⁻ pH DNA RNA");
            addRow("→ ⇌ ↑ ↓ °C K mol kg m/s N");
            addRow("α β γ λ μ Ω Δ ∝ ∴ ⌫");
            addBottom();
        }
        keyboardFrame.requestLayout();
    }
    private void addToolbar(){
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        String[] ids={"ABC","123","∑ MATH","⚛ SCI","🎨","⚙"};
        for(int i=0;i<ids.length;i++){
            final int idx=i;
            TextView tab=new TextView(this);
            tab.setText(ids[i]);
            tab.setTextSize(i==2||i==3?12:13);
            tab.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setTextColor(i<4 && mode==i?DARK[theme]:Color.WHITE);
            GradientDrawable surface=new GradientDrawable();
            boolean selected=i<4 && mode==i;
            surface.setColor(selected?ACCENTS[theme]:0x66243659);
            surface.setCornerRadius(dp(12));
            surface.setStroke(dp(1),selected?ACCENTS[theme]:0x556edfff);
            tab.setBackground(surface);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(41),i==2||i==3?1.4f:1f);
            lp.setMargins(dp(2),0,dp(2),dp(5));
            bar.addView(tab,lp);
            tab.setContentDescription(i==4?"Change keyboard theme":i==5?"Keyboard settings":"Switch keyboard to "+ids[i]);
            tab.setOnClickListener(v->{
                feedback(v);
                if(idx<4){mode=idx;shifted=false;rebuild();}
                else if(idx==4){theme=(theme+1)%4;settings().edit().putInt(KEY_THEME,theme).apply();rebuild();}
                else{Intent intent=new Intent(this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try{startActivity(intent);}catch(RuntimeException ignored){}}
            });
        }
        keysContent.addView(bar,new LinearLayout.LayoutParams(-1,dp(46)));
    }
    private void addRow(String keys){
        String[] labels=keys.split(" ");
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        boolean landscape=getResources().getConfiguration().orientation==2;
        int height=dp(landscape?45:55);
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
    private void addBottom(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        int height=dp(getResources().getConfiguration().orientation==2?45:55);
        if(mode!=ABC)addKey(row,"ABC","ABC",1.10f,height,true);
        else addKey(row,"123","123",1.05f,height,true);
        if(mode==ABC)addKey(row,"∑","MATH",.95f,height,true);
        else if(mode==MATH)addKey(row,"⚛","SCI",.85f,height,true);
        else addKey(row,"π","MATH",.85f,height,true);
        addKey(row,",",",",.60f,height,false);
        addKey(row,"space","SPACE",3.6f,height,true);
        addKey(row,".",".",.6f,height,false);
        addKey(row,"↵","ENTER",1.05f,height,true);
        keysContent.addView(row,new LinearLayout.LayoutParams(-1,height+dp(3)));
    }
    private static boolean isAction(String value){
        return value.equals("⇧")||value.equals("⌫")||value.equals("ABC")||
            value.equals("123")||value.equals("MATH")||value.equals("SCI")||
            value.equals("SPACE")||value.equals("ENTER")||value.equals("←");
    }
    private void addKey(LinearLayout row,String display,String output,float weight,int height,boolean special){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(display);
        b.setIncludeFontPadding(false);
        int length=display.length();
        b.setTextSize(length>=5?11.5f:length>=4?13f:length>=3?15f:20f);
        b.setTypeface(Typeface.create("sans-serif",special||isAction(output)?Typeface.BOLD:Typeface.NORMAL));
        b.setTextColor(special?ACCENTS[theme]:Color.WHITE);
        b.setPadding(0,0,0,0);b.setMinimumHeight(0);b.setMinHeight(0);b.setMinimumWidth(0);b.setMinWidth(0);
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{special?0xeb293c68:KEY_COLORS[theme],special?0xea102345:0xed0c2146});
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1),special?ACCENTS[theme]:0x4b79bdff);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(ACCENTS[theme]),shape,null));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,height-dp(3),weight);
        lp.setMargins(dp(2),dp(1),dp(2),dp(2));
        row.addView(b,lp);
        b.setContentDescription("⌫".equals(output)?"Delete last character":("SPACE".equals(output)?"Space":output));
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
