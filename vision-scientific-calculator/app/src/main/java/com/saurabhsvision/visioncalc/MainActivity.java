package com.saurabhsvision.visioncalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * VISION CALC — scientific, offline, no advertising or login.
 *
 * Data: last 60 calculations and DEG/RAD + memory state in app-local storage.
 * Does not collect or transmit any entered expressions.
 */
public final class MainActivity extends Activity {
    private static final String PREFS="vision_calc_local";
    // Dynamic palette. The screenshot-inspired light appearance is the default;
    // the original dark neon navy look remains available with one tap.
    private int NAVY, PANEL, CARD, CYAN, GOLD, WHITE, MUTED, SOFT, RED;
    private boolean darkMode=false, scientificOpen=false;
    private int themeIndex=0;
    private ComfortTheme palette;
    private static final int THEME_DARK=4;
    private TextView themeToggle;
    private static final String[] TAB_NAMES={"CALCULATOR","SCIENTIFIC","CONVERTER","HISTORY"};
    private SharedPreferences prefs;
    private LinearLayout root, screen;
    private TextView expressionView,answerView,statusView,degreeButton;
    private ScrollView scroll;
    // Rows are resized from the ACTUAL visible viewport, not display metrics.
    private final List<LinearLayout> keypadRows=new ArrayList<>();
    private TextView keypadFooter;
    private int keyExpansionPx=0;
    private String expression="";
    // Last successful preview is reused on '=' and Copy, avoiding duplicate
    // parsing and formatting of the same keypress on the main UI thread.
    private String cachedExpression=null,cachedFormatted=null;
    private boolean cachedDegrees;
    private long cachedAnsBits,cachedMemoryBits;
    private double cachedResult=0;
    private double ans=0,memory=0;
    private boolean degrees=true;
    private int tab=0;
    private Spinner categorySpinner,fromSpinner,toSpinner;
    private EditText converterValue;
    private TextView converterResult,converterDetails;

    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+.5f);}
    private boolean landscape(){
        return getResources().getConfiguration().orientation
            ==Configuration.ORIENTATION_LANDSCAPE;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        // New comfort theme defaults to Soft Sky Blue for fresh and upgraded
        // installs. The user's explicit v1.6 theme selection is retained.
        themeIndex=ComfortTheme.clamp(prefs.getInt("comfortTheme",0));
        darkMode=(themeIndex==THEME_DARK);
        scientificOpen=prefs.getBoolean("scientificOpen",false);
        applyPalette();
        degrees=prefs.getBoolean("degrees",true);
        memory=Double.longBitsToDouble(prefs.getLong("memoryBits",Double.doubleToLongBits(0)));
        ans=Double.longBitsToDouble(prefs.getLong("ansBits",Double.doubleToLongBits(0)));
        if(!Double.isFinite(memory))memory=0;
        if(!Double.isFinite(ans))ans=0;
        if(saved!=null){
            expression=saved.getString("expression","");
            tab=saved.getInt("tab",0);
        }
        applySystemBars();
        buildUi();
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putString("expression",expression);
        state.putInt("tab",tab);
    }
    private void applyPalette(){
        palette=ComfortTheme.choose(themeIndex);
        darkMode=palette.dark();
        NAVY=palette.background; PANEL=palette.panel; CARD=palette.card;
        CYAN=palette.accent; GOLD=palette.detail; WHITE=palette.ink;
        MUTED=palette.muted; SOFT=palette.soft; RED=palette.error;
    }
    private void chooseComfortTheme(){
        new AlertDialog.Builder(this).setTitle("S CALC • Comfortable Colors")
            .setSingleChoiceItems(ComfortTheme.NAMES,themeIndex,
                (dialog,selected)->{
                    themeIndex=ComfortTheme.clamp(selected);
                    prefs.edit().putInt("comfortTheme",themeIndex).apply();
                    applyPalette();buildUi();
                    dialog.dismiss();
                })
            .setNegativeButton("Cancel",null).show();
    }
    private void applySystemBars(){
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        int visibility=darkMode?0:(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }
    private GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if(stroke!=0)drawable.setStroke(dp(1),stroke);
        return drawable;
    }
    private GradientDrawable gradient(int top,int bottom,int radius,int stroke){
        GradientDrawable drawable=new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        drawable.setCornerRadius(dp(radius));
        if(stroke!=0)drawable.setStroke(dp(1),stroke);
        return drawable;
    }
    private TextView text(String value,float size,int color,boolean bold){
        TextView text=new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setIncludeFontPadding(true);
        if(bold)text.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return text;
    }
    private void add(LinearLayout column,View view,int marginTop){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=dp(marginTop);
        column.addView(view,lp);
    }
    private Button button(String label,int top,int bottom,int border,int foreground){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);
        b.setTextSize(16);b.setTextColor(foreground);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        b.setPadding(dp(4),0,dp(4),0);
        b.setMinHeight(0);b.setMinimumHeight(0);
        b.setBackground(gradient(top,bottom,12,border));
        b.setElevation(dp(1.6f));
        b.setContentDescription(label);
        return b;
    }
    private void buildUi(){
        applySystemBars();
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(NAVY);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(11),dp(6),dp(11),dp(10));
        // Android 15 edge-to-edge: protect the title AND final equals key.
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            int navInset=insets.getSystemWindowInsetBottom();
            int statusInset=insets.getSystemWindowInsetTop();
            root.setPadding(dp(11),Math.max(dp(6),statusInset+dp(4)),
                dp(11),Math.max(dp(10),navInset+dp(5)));
            return insets;
        });
        scroll.addView(root);
        setContentView(scroll);
        buildHeader();
        buildTabs();
        screen=new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        keypadRows.clear();
        keypadFooter=null;
        keyExpansionPx=0;
        LinearLayout.LayoutParams fill=new LinearLayout.LayoutParams(-1,-2);
        fill.topMargin=dp(6);
        root.addView(screen,fill);
        showTab();
        if(tab<=1) scroll.post(this::expandKeypadToViewport);
    }

    /** Expand all five numeric rows equally into the viewport's unused bottom
     * area. This is calculated after real Android window insets and layout,
     * so it works on tall phones and tablets without obscuring '=' or
     * forcing a fixed-key-height layout on short/landscape displays.
     */
    private void expandKeypadToViewport(){
        if(tab>1||(keypadRows.size()!=4&&keypadRows.size()!=5)||
            keypadFooter==null||
            scroll.getHeight()<=0||screen.getParent()!=root)return;
        int footerBottom=screen.getTop()+keypadFooter.getBottom();
        int currentKeyHeight=((LinearLayout.LayoutParams)
            keypadRows.get(0).getChildAt(0).getLayoutParams()).height;
        int maxHeight=dp(landscape()?96:155);
        // On short screens the policy returns zero; the ScrollView retains
        // its full keypad, whereas tall screens expand each of the five rows.
        int extraPerRow=CalculatorLayoutPolicy.extraPerKeyRow(
            scroll.getHeight(),footerBottom,root.getPaddingBottom(),dp(10),
            currentKeyHeight,maxHeight,keypadRows.size());
        if(extraPerRow<=0)return;
        keyExpansionPx+=extraPerRow;
        for(LinearLayout row:keypadRows){
            for(int i=0;i<row.getChildCount();i++){
                View key=row.getChildAt(i);
                LinearLayout.LayoutParams keyParams=
                    (LinearLayout.LayoutParams)key.getLayoutParams();
                keyParams.height+=extraPerRow;
                key.setLayoutParams(keyParams);
            }
        }
    }
    private void buildHeader(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu=text("☰",23,WHITE,true);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(gradient(0xff234878,0xff0c234a,11,0xff528dff));
        menu.setContentDescription("Menu and settings");
        row.addView(menu,new LinearLayout.LayoutParams(dp(40),dp(40)));
        menu.setOnClickListener(v->showProMenu());

        ImageView icon=new ImageView(this);
        icon.setImageResource(R.drawable.ic_s_calc);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setContentDescription("S CALC app icon");
        LinearLayout.LayoutParams iLp=new LinearLayout.LayoutParams(dp(40),dp(40));
        iLp.leftMargin=dp(9);
        row.addView(icon,iLp);

        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(6),0,0,0);
        TextView brand=text("SAURABH'S VISION™",13,
            darkMode?0xffffd767:0xff35558a,true);
        titles.addView(brand);
        TextView sub=text("S CALC  •  Scientific Calculator",11,MUTED,false);
        titles.addView(sub);
        row.addView(titles,new LinearLayout.LayoutParams(0,-2,1f));

        TextView recent=text("◷",24,WHITE,true);
        recent.setGravity(Gravity.CENTER);
        recent.setBackground(gradient(0xff194a78,0xff0d2448,11,0xff328bd9));
        recent.setContentDescription("Open calculation history");
        LinearLayout.LayoutParams rLp=new LinearLayout.LayoutParams(dp(40),dp(40));
        rLp.rightMargin=dp(6);
        row.addView(recent,rLp);
        recent.setOnClickListener(v->{tab=3;buildUi();});

        themeToggle=text("⚙",24,WHITE,true);
        themeToggle.setGravity(Gravity.CENTER);
        themeToggle.setBackground(gradient(0xff194a78,0xff0d2448,11,0xff328bd9));
        themeToggle.setContentDescription("Calculator settings");
        row.addView(themeToggle,new LinearLayout.LayoutParams(dp(40),dp(40)));
        themeToggle.setOnClickListener(v->showProMenu());
        add(root,row,2);

        TextView ribbon=text("S CALC",25,Color.WHITE,true);
        ribbon.setGravity(Gravity.CENTER);
        ribbon.setLetterSpacing(.085f);
        ribbon.setBackground(gradient(
            darkMode?0xffd43c2c:0xffb35273,
            darkMode?0xff8c181d:0xff773459,17,0xffffc85b));
        ribbon.setContentDescription("S CALC by Saurabh's Vision");
        LinearLayout.LayoutParams ribbonParams=new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(43));
        ribbonParams.setMargins(dp(48),dp(9),dp(48),0);
        root.addView(ribbon,ribbonParams);
    }
    private void showProMenu(){
        String[] choices={
            "🎨 Color theme • "+ComfortTheme.NAMES[themeIndex],
            degrees?"Angle: Degrees (switch to RAD)":"Angle: Radians (switch to DEG)",
            "Open Calculation History", "Open Unit Converter",
            "Scientific Calculator", "Basic Calculator"
        };
        new AlertDialog.Builder(this).setTitle("S CALC • Settings")
            .setItems(choices,(dialog,which)->{
                switch(which){
                    case 0:chooseComfortTheme();break;
                    case 1:
                        degrees=!degrees;
                        prefs.edit().putBoolean("degrees",degrees).apply();
                        buildUi();break;
                    case 2:tab=3;buildUi();break;
                    case 3:tab=2;buildUi();break;
                    case 4:tab=1;buildUi();break;
                    case 5:tab=0;buildUi();break;
                    default:break;
                }
            }).show();
    }
    private void buildTabs(){
        LinearLayout tabs=new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(dp(2),dp(3),dp(2),dp(3));
        tabs.setBackground(shape(PANEL,12,0xff245782));
        String[] symbols={"⚗","∑","⇄","◷"};
        for(int i=0;i<TAB_NAMES.length;i++){
            final int idx=i;
            LinearLayout column=new LinearLayout(this);
            column.setGravity(Gravity.CENTER);
            column.setOrientation(LinearLayout.VERTICAL);
            int top=idx==tab?0xff9c22c8:0xff153d6b;
            int bottom=idx==tab?0xff53139a:0xff071b39;
            int outline=idx==tab?0xffee99ff:0xff2ea4ed;
            column.setBackground(gradient(top,bottom,12,outline));
            TextView mark=text(symbols[i],17,Color.WHITE,true);
            mark.setGravity(Gravity.CENTER);
            column.addView(mark);
            TextView name=text(TAB_NAMES[i],9,Color.WHITE,true);
            name.setGravity(Gravity.CENTER);
            column.addView(name);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);
            lp.setMargins(dp(2),dp(2),dp(2),dp(2));
            tabs.addView(column,lp);
            column.setContentDescription("Open "+TAB_NAMES[i]+" tab");
            column.setOnClickListener(v->{tab=idx;buildUi();});
        }
        add(root,tabs,9);
    }
    private void showTab(){
        screen.removeAllViews();
        if(tab==0)buildComfortCalculator();
        else if(tab==1)buildProCalculator();
        else if(tab==2)buildConverter();
        else buildHistory();
    }
    // The reference screenshot uses large, softly rounded 4-column keys.
    // Keep science tools available above the grid instead of squeezing the keys.
    private Button numericKey(String label,boolean operation,boolean clear,boolean equals){
        int top,bottom,stroke,ink;
        if(darkMode){
            top=equals?0xff67f5f9:clear?0xff844051:operation?0xff2a5a84:0xff234a74;
            bottom=equals?0xff10bbdb:clear?0xff632839:operation?0xff102d56:0xff0a264b;
            stroke=equals?0xffb7ffff:clear?0xffa55a68:0xff2e8cad;
            ink=equals?0xff032034:0xfff8fbff;
        }else{
            top=equals?0xff674868:clear?0xff6884bd:operation?0xff798096:0xffe7e7ee;
            bottom=equals?0xff503551:clear?0xff5e74a6:operation?0xff6b7288:0xffe1e2eb;
            stroke=0;
            ink=operation||clear||equals?Color.WHITE:0xff1c1c24;
        }
        Button b=button(label,top,bottom,stroke,ink);
        b.setTextSize(label.equals("()")?25:label.equals("⌫")?26:30);
        b.setElevation(dp(darkMode?1.6f:0));
        b.setBackground(gradient(top,bottom,27,stroke));
        b.setContentDescription(label.equals("⌫")?"Backspace":label.equals("=")?"Calculate result":
            label.equals("()")?"Open or close bracket":label.equals("AC")?"Clear all":
            label.equals("÷")?"Divide":label.equals("×")?"Multiply":
            label.equals("−")?"Subtract":label.equals("+")?"Add":label);
        return b;
    }
    private Button compactChip(String label){
        Button chip=button(label,
            darkMode?0xff1c4871:0xffe6e6f0,
            darkMode?0xff0c2e50:0xffdedfec,
            darkMode?0xff367f9b:0,WHITE);
        chip.setBackground(shape(darkMode?0xff1c4871:0xffe7e7f0,15,0));
        chip.setTextSize(13);
        chip.setElevation(0);
        return chip;
    }
    /** Distinct large scientific keys (trig teal, power indigo, constants
     * gold/violet), with legible labels and a minimum 48dp touch height.
     */
    private Button scientificChip(String token){
        boolean trig=token.equals("sin")||token.equals("cos")||token.equals("tan")||
            token.equals("asin")||token.equals("acos")||token.equals("atan");
        boolean constant=token.equals("π")||token.equals("e")||
            token.equals("τ")||token.equals("φ");
        int top,bottom,stroke,foreground;
        if(darkMode){
            top=trig?0xff147e91:constant?0xff7962a1:0xff345a97;
            bottom=trig?0xff0a4563:constant?0xff43356e:0xff203768;
            stroke=trig?0xff4af4e8:constant?0xffeac5ff:0xff74a7fb;
            foreground=0xffffffff;
        }else{
            top=trig?0xffd2f2f4:constant?0xfff1dff3:0xffe0e8fd;
            bottom=trig?0xffbee7eb:constant?0xffe4d3ea:0xffccd9f3;
            stroke=trig?0xff7ec5d3:constant?0xffcfabd4:0xffa5bce7;
            foreground=trig?0xff165d71:constant?0xff603d75:0xff294c81;
        }
        Button b=button(token,top,bottom,stroke,foreground);
        b.setBackground(gradient(top,bottom,18,stroke));
        b.setTextSize(token.length()>3?15:18);
        b.setElevation(dp(darkMode?1.0f:0.4f));
        b.setContentDescription("Scientific "+token);
        return b;
    }
    private String recentResult(){
        try{
            JSONArray items=new JSONArray(prefs.getString("history","[]"));
            JSONObject first=items.optJSONObject(0);
            if(first!=null)return first.optString("expression","")+
                "  =  "+first.optString("result","");
        }catch(Exception ignored){}
        return "Try sin(30) × 200  or  (2+3)²";
    }

    /**
     * S CALC Pro: designed from the user's neon scientific keypad reference.
     * All visible keys have real operations. Unsupported symbolic integration
     * or matrix solvers are NOT represented as functional controls.
     */
    /**
     * Comfortable basic calculator based on the user's supplied 4-column
     * circular keypad reference. Uses real ExpressionEngine operations.
     * Only S CALC branding is new; no fake display values or functionality.
     */
    private Button comfortKey(String label){
        boolean number=label.matches("[0-9.]");
        boolean equals=label.equals("=");
        boolean clear=label.equals("AC");
        boolean delete=label.equals("⌫");
        int top=number?palette.digit:equals?palette.equals:
            clear||delete?palette.clear:palette.operator;
        int bottom=top;
        int fg=darkMode?0xffF7FBFF:palette.ink;
        if(darkMode && equals)fg=0xff052031;
        int stroke=darkMode?0xff4D89AD:0xffCCDDE8;
        Button button=button(label,top,bottom,stroke,fg);
        button.setBackground(gradient(top,bottom,36,stroke));
        button.setTextSize(label.equals("()")?24:label.equals("⌫")?25:
            label.length()>2?22:31);
        button.setElevation(dp(darkMode?1.8f:0.8f));
        button.setContentDescription(label.equals("⌫")?
            "Delete last character":label.equals("AC")?
            "Clear calculation":label.equals("=")?"Calculate result":label);
        return button;
    }
    private void buildComfortCalculator(){
        final boolean compact=landscape()||
            getResources().getDisplayMetrics().heightPixels/
                getResources().getDisplayMetrics().density<705;

        LinearLayout display=new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setPadding(dp(17),dp(compact?10:18),dp(17),dp(compact?11:16));
        display.setBackground(gradient(palette.displayTop,palette.displayBottom,
            24,darkMode?0xff2E7A9F:palette.soft));

        LinearLayout labelRow=new LinearLayout(this);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("S VISION  •  LIVE RESULT",12,palette.muted,true);
        labelRow.addView(label,new LinearLayout.LayoutParams(0,dp(32),1));
        degreeButton=text(degrees?"DEG":"RAD",12,WHITE,true);
        degreeButton.setGravity(Gravity.CENTER);
        degreeButton.setBackground(shape(palette.soft,12,0));
        labelRow.addView(degreeButton,new LinearLayout.LayoutParams(dp(57),dp(32)));
        degreeButton.setOnClickListener(v->{
            degrees=!degrees;
            prefs.edit().putBoolean("degrees",degrees).apply();
            buildUi();
        });
        add(display,labelRow,0);

        expressionView=text(expression.isEmpty()?"0":displayExpression(expression),
            compact?25:30,palette.ink,true);
        expressionView.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);
        expressionView.setMinHeight(dp(compact?48:62));
        add(display,expressionView,dp(4));

        answerView=text("0",compact?35:46,darkMode?0xff91F1F3:palette.accent,true);
        answerView.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        answerView.setSingleLine(true);
        answerView.setEllipsize(TextUtils.TruncateAt.START);
        answerView.setMinHeight(dp(compact?49:67));
        add(display,answerView,0);

        statusView=text("OFFLINE • ACCURATE • PRIVATE",11,palette.muted,false);
        statusView.setGravity(Gravity.END);
        add(display,statusView,dp(6));
        add(screen,display,0);

        LinearLayout tools=new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        TextView science=text("∑  SCIENTIFIC ›",12,palette.accent,true);
        science.setGravity(Gravity.CENTER);
        science.setContentDescription("Open full scientific calculator");
        science.setBackground(shape(palette.soft,14,0));
        LinearLayout.LayoutParams sci=new LinearLayout.LayoutParams(0,dp(43),1);
        sci.rightMargin=dp(4);
        tools.addView(science,sci);
        science.setOnClickListener(v->{tab=1;buildUi();});
        TextView history=text("◷  HISTORY",12,palette.accent,true);
        history.setGravity(Gravity.CENTER);
        history.setContentDescription("Open calculation history");
        history.setBackground(shape(palette.soft,14,0));
        LinearLayout.LayoutParams his=new LinearLayout.LayoutParams(0,dp(43),1);
        his.leftMargin=dp(4);
        tools.addView(history,his);
        history.setOnClickListener(v->{tab=3;buildUi();});
        add(screen,tools,dp(10));

        String[][] rows={
            {"AC","()","%","÷"},
            {"7","8","9","×"},
            {"4","5","6","−"},
            {"1","2","3","+"},
            {"0",".","⌫","="}
        };
        int height=dp(compact?57:73);
        for(String[] items:rows){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            keypadRows.add(row);
            for(String key:items){
                Button b=comfortKey(key);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                    0,height,1f);
                p.setMargins(dp(3),dp(4),dp(3),dp(4));
                row.addView(b,p);
                b.setOnClickListener(v->onKey(key));
            }
            add(screen,row,0);
        }

        // Keep last row and equals fully visible above Android navigation.
        TextView footer=text("S CALC  •  SAURABH'S VISION™",10,palette.muted,true);
        footer.setGravity(Gravity.CENTER);
        add(screen,footer,8);
        keypadFooter=footer;
        updatePreview();
    }

    private Button proScientificKey(String label,boolean selected){
        boolean yellow=label.equals("DEG")||label.equals("RAD");
        int top=selected?0xffffd95a:darkMode?0xff24518b:0xffd9e8ff;
        int bottom=selected?0xffffa400:darkMode?0xff071c3f:0xffc4d5f5;
        int outline=yellow?0xffffc53f:darkMode?0xff35c6ff:0xff659fd2;
        int fg=selected?0xff182746:darkMode?0xffffffff:0xff16355d;
        Button b=button(label,top,bottom,outline,fg);
        b.setBackground(gradient(top,bottom,12,outline));
        b.setTextSize(label.length()>=4?12:label.length()>=3?14:17);
        b.setElevation(0);
        b.setContentDescription("Scientific "+label);
        return b;
    }
    private Button proNumberKey(String label){
        boolean digit=label.matches("[0-9.]")||label.equals("EXP");
        boolean equals=label.equals("=");
        boolean clear=label.equals("AC")||label.equals("⌫");
        int top,bottom,border,ink;
        if(equals){
            top=0xffffc33c;bottom=0xffe75b07;border=0xffffd771;ink=0xffffffff;
        }else if(clear){
            top=0xffb121d3;bottom=0xff5714a4;border=0xffff79f4;ink=0xffffffff;
        }else if(digit){
            top=0xfffcfeff;bottom=0xffc4d7f5;border=0xff8abaff;ink=0xff081c39;
        }else{
            top=0xff1767b3;bottom=0xff052250;border=0xff3bcbff;ink=0xffffffff;
        }
        Button b=button(label,top,bottom,border,ink);
        b.setTextSize(label.length()>2?17:25);
        b.setBackground(gradient(top,bottom,13,border));
        b.setContentDescription(label.equals("=")?"Calculate result":
            label.equals("⌫")?"Delete last character":
            label.equals("AC")?"Clear all":
            label.equals("EXP")?"Scientific exponent notation":label);
        return b;
    }
    private void pressProScience(String label){
        switch(label){
            case "DEG":
                degrees=true;
                prefs.edit().putBoolean("degrees",true).apply();
                buildUi();return;
            case "RAD":
                degrees=false;
                prefs.edit().putBoolean("degrees",false).apply();
                buildUi();return;
            case "10ˣ":appendFunction("10^x");return;
            case "eˣ":appendFunction("exp");return;
            case "x²":appendFunction("x²");return;
            case "x³":appendFunction("x³");return;
            case "√x":appendFunction("sqrt");return;
            case "∛x":appendFunction("cbrt");return;
            case "|x|":appendFunction("abs");return;
            case "x!":onKey("!");return;
            case "%":onKey("%");return;
            case "(":case ")":onKey(label);return;
            default:appendFunction(label);return;
        }
    }
    private void buildProCalculator(){
        boolean small=landscape()||
            getResources().getDisplayMetrics().heightPixels/
                getResources().getDisplayMetrics().density<710;
        // Icy display face — text is LIVE, never a hard-coded integral answer.
        LinearLayout display=new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setPadding(dp(14),dp(9),dp(14),dp(9));
        display.setBackground(gradient(
            darkMode?0xffd7efff:0xffeef8ff,
            darkMode?0xff8bc3ef:0xffc3e5ff,17,0xff46caff));
        LinearLayout headline=new LinearLayout(this);
        headline.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("S VISION  •  LIVE CALCULATION",12,0xff19416e,true);
        headline.addView(title,new LinearLayout.LayoutParams(0,dp(30),1));
        degreeButton=text(degrees?"DEG":"RAD",13,0xff0a315b,true);
        degreeButton.setGravity(Gravity.CENTER);
        degreeButton.setBackground(shape(0xffffd45a,9,0xffdeaa35));
        headline.addView(degreeButton,
            new LinearLayout.LayoutParams(dp(57),dp(30)));
        degreeButton.setOnClickListener(v->{
            degrees=!degrees;
            prefs.edit().putBoolean("degrees",degrees).apply();
            buildUi();
        });
        add(display,headline,0);

        expressionView=text(expression.isEmpty()?"0":displayExpression(expression),
            small?20:26,0xff0a2245,true);
        expressionView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);
        expressionView.setMinHeight(dp(small?29:37));
        add(display,expressionView,2);
        answerView=text("0",small?27:38,0xff0b386d,true);
        answerView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        answerView.setSingleLine(true);
        answerView.setEllipsize(TextUtils.TruncateAt.START);
        answerView.setMinHeight(dp(small?34:47));
        add(display,answerView,0);
        statusView=text("Scientific • Offline • DEG / RAD",10,0xff345c85,false);
        statusView.setGravity(Gravity.RIGHT);
        add(display,statusView,2);
        add(screen,display,0);

        String[][] sciences={
            {"sin","cos","tan","cot","sec","csc"},
            {"log","ln","10ˣ","eˣ","x²","x³"},
            {"√x","∛x","|x|","π","nCr","nPr"},
            {"(",")","x!","%","DEG","RAD"}
        };
        LinearLayout sciPanel=new LinearLayout(this);
        sciPanel.setOrientation(LinearLayout.VERTICAL);
        int sciHeight=dp(small?37:47);
        for(String[] line:sciences){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for(String key:line){
                boolean selected=(key.equals("DEG")&&degrees)||
                    (key.equals("RAD")&&!degrees);
                Button b=proScientificKey(key,selected);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,sciHeight,1);
                lp.setMargins(dp(2),dp(2),dp(2),dp(2));
                row.addView(b,lp);
                b.setOnClickListener(v->pressProScience(key));
            }
            sciPanel.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        add(screen,sciPanel,8);

        // Additional maths are real functions, not placeholders.
        if(tab==1||scientificOpen){
            HorizontalScrollView advanced=new HorizontalScrollView(this);
            advanced.setHorizontalScrollBarEnabled(false);
            LinearLayout options=new LinearLayout(this);
            for(String label:new String[]{"asin","acos","atan","sinh","cosh",
                    "tanh","abs","floor","ceil","mod","gcd","lcm","root",
                    "1/x","nCr",",","nPr","min","max","pow","φ","τ","e"}){
                Button b=proScientificKey(label,false);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(69),dp(43));
                lp.setMargins(dp(2),0,dp(4),0);
                options.addView(b,lp);
                b.setOnClickListener(v->appendFunction(label));
            }
            advanced.addView(options);
            add(screen,advanced,4);
        }

        String[][] numeric={
            {"7","8","9","AC","⌫"},
            {"4","5","6","×","÷"},
            {"1","2","3","+","−"},
            {"0",".","EXP","Ans","="}
        };
        int numberHeight=dp(small?48:65);
        for(String[] line:numeric){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            keypadRows.add(row);
            for(String key:line){
                Button b=proNumberKey(key);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,numberHeight,1);
                lp.setMargins(dp(2),dp(3),dp(2),dp(3));
                row.addView(b,lp);
                b.setOnClickListener(v->onKey(key));
            }
            add(screen,row,0);
        }

        // Functional shortcuts styled after the reference's bottom tool rail.
        LinearLayout shortcuts=new LinearLayout(this);
        String[] actions={"ƒ(x) FUNCTIONS","⇄ CONVERTER","◷ HISTORY","▣ COPY"};
        for(int i=0;i<actions.length;i++){
            final int id=i;
            TextView link=text(actions[i],9,0xffffdf7d,true);
            link.setGravity(Gravity.CENTER);
            link.setSingleLine(true);
            link.setBackground(gradient(0xff1b3e73,0xff081d43,10,0xff458fd8));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1);
            lp.setMargins(dp(2),0,dp(2),0);
            shortcuts.addView(link,lp);
            link.setContentDescription(actions[i]);
            link.setOnClickListener(v->{
                switch(id){
                    case 0:tab=1;buildUi();break;
                    case 1:tab=2;buildUi();break;
                    case 2:tab=3;buildUi();break;
                    case 3:onMemory("COPY");break;
                    default:break;
                }
            });
        }
        add(screen,shortcuts,9);
        TextView footer=text("S CALC  •  SAURABH'S VISION™",10,MUTED,true);
        footer.setGravity(Gravity.CENTER);
        add(screen,footer,7);
        keypadFooter=footer;
        updatePreview();
    }
    private void buildCalculator(){
        final boolean compact=landscape();
        LinearLayout display=new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setPadding(dp(16),dp(compact?7:8),dp(16),dp(8));
        display.setBackground(darkMode?
            gradient(0xff12355a,0xff081e3b,23,0xff235b80):
            gradient(0xffffffff,0xfff0eff8,23,0xffe5e3ee));

        LinearLayout displayHeading=new LinearLayout(this);
        displayHeading.setGravity(Gravity.CENTER_VERTICAL);
        // Compact S VISION logo is inside the calculator's result screen,
        // separate from the S CALC launcher icon used at the top.
        LinearLayout visionLogo=new LinearLayout(this);
        visionLogo.setGravity(Gravity.CENTER_VERTICAL);
        TextView monogram=text("S",17,0xffffd66d,true);
        monogram.setGravity(Gravity.CENTER);
        monogram.setBackground(gradient(0xff214f87,0xff091f48,9,0xffffd66d));
        monogram.setContentDescription("S Vision logo");
        visionLogo.addView(monogram,new LinearLayout.LayoutParams(dp(26),dp(26)));
        LinearLayout visionText=new LinearLayout(this);
        visionText.setOrientation(LinearLayout.VERTICAL);
        visionText.setPadding(dp(6),0,0,0);
        TextView logoName=text("S VISION",12,darkMode?0xffefc46d:0xff364c83,true);
        visionText.addView(logoName);
        TextView label=text("YOUR CALCULATION",9,MUTED,false);
        visionText.addView(label);
        visionLogo.addView(visionText,new LinearLayout.LayoutParams(-2,-2));
        displayHeading.addView(visionLogo,new LinearLayout.LayoutParams(0,dp(32),1f));
        degreeButton=text(degrees?"DEG":"RAD",13,
            darkMode?NAVY:Color.WHITE,true);
        degreeButton.setGravity(Gravity.CENTER);
        degreeButton.setBackground(shape(CYAN,12,0));
        degreeButton.setContentDescription(
            "Angle mode: "+(degrees?"degrees":"radians")+". Tap to switch.");
        displayHeading.addView(degreeButton,new LinearLayout.LayoutParams(dp(54),dp(32)));
        degreeButton.setOnClickListener(v->{
            degrees=!degrees;
            prefs.edit().putBoolean("degrees",degrees).apply();
            degreeButton.setText(degrees?"DEG":"RAD");
            degreeButton.setContentDescription(
                "Angle mode: "+(degrees?"degrees":"radians")+". Tap to switch.");
            updatePreview();
        });
        add(display,displayHeading,0);

        expressionView=text(expression.isEmpty()?"0":displayExpression(expression),
            compact?23:27,WHITE,true);
        expressionView.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);
        expressionView.setMinHeight(dp(compact?27:30));
        add(display,expressionView,2);

        answerView=text("0",compact?32:40,GOLD,true);
        answerView.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        answerView.setSingleLine(true);
        answerView.setEllipsize(TextUtils.TruncateAt.START);
        answerView.setMinHeight(dp(compact?38:43));
        add(display,answerView,0);

        statusView=text("OFFLINE  •  INSTANT RESULT",11,MUTED,false);
        statusView.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        add(display,statusView,0);
        add(screen,display,0);

        LinearLayout recall=new LinearLayout(this);
        recall.setGravity(Gravity.CENTER_VERTICAL);
        TextView historyLabel=text("↶  "+recentResult(),11,MUTED,false);
        historyLabel.setSingleLine(true);
        historyLabel.setEllipsize(TextUtils.TruncateAt.END);
        recall.addView(historyLabel,new LinearLayout.LayoutParams(0,dp(27),1f));
        TextView recallAction=text("HISTORY ›",10,CYAN,true);
        recallAction.setGravity(Gravity.CENTER);
        recall.addView(recallAction,new LinearLayout.LayoutParams(dp(72),dp(27)));
        recall.setContentDescription("Open recent calculations");
        recall.setOnClickListener(v->{tab=2;buildUi();});
        add(screen,recall,1);

        LinearLayout toolTitle=new LinearLayout(this);
        toolTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView scienceLabel=text("∑  SCIENTIFIC TOOLS",12,WHITE,true);
        toolTitle.addView(scienceLabel,new LinearLayout.LayoutParams(0,dp(26),1f));
        TextView expand=text(scientificOpen?"SHOW LESS  ⌃":"SHOW ALL  ⌄",11,CYAN,true);
        expand.setGravity(Gravity.CENTER);
        toolTitle.addView(expand,new LinearLayout.LayoutParams(dp(108),dp(26)));
        toolTitle.setContentDescription("Expand or collapse scientific functions");
        toolTitle.setOnClickListener(v->{
            scientificOpen=!scientificOpen;
            prefs.edit().putBoolean("scientificOpen",scientificOpen).apply();
            buildUi();
        });
        add(screen,toolTitle,1);

        String[] quick={"sin","cos","tan","√","π","x²","log","ln","nCr",",","nPr","e"};
        String[] extra={"asin","acos","atan","cbrt","abs","exp","sinh","cosh",
            "tanh","root","gcd","lcm","floor","ceil","mod","1/x","10^x","τ",
            "φ","rad","deg","max","min"};
        if(compact){
            // Landscape keeps science in one horizontally scrollable strip.
            HorizontalScrollView strip=new HorizontalScrollView(this);
            strip.setHorizontalScrollBarEnabled(false);
            LinearLayout row=new LinearLayout(this);
            for(String token:quick){
                Button chip=scientificChip(token);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(77),dp(48));
                lp.setMargins(dp(2),0,dp(4),0);
                row.addView(chip,lp);
                chip.setOnClickListener(v->appendFunction(token));
            }
            strip.addView(row);
            add(screen,strip,0);
        }else{
            // 4 × 3 visible science keys: no tiny buttons or hidden functions.
            LinearLayout scienceGrid=new LinearLayout(this);
            scienceGrid.setOrientation(LinearLayout.VERTICAL);
            for(int r=0;r<3;r++){
                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for(int c=0;c<4;c++){
                    final String token=quick[r*4+c];
                    Button chip=scientificChip(token);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(50),1);
                    lp.setMargins(dp(2),dp(3),dp(2),dp(3));
                    row.addView(chip,lp);
                    chip.setOnClickListener(v->appendFunction(token));
                }
                scienceGrid.addView(row,new LinearLayout.LayoutParams(-1,-2));
            }
            add(screen,scienceGrid,0);
        }
        if(scientificOpen){
            HorizontalScrollView extraScroll=new HorizontalScrollView(this);
            extraScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout row=new LinearLayout(this);
            for(String token:extra){
                Button chip=scientificChip(token);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                    dp(token.length()>4?86:77),dp(50));
                lp.setMargins(dp(2),0,dp(5),0);
                row.addView(chip,lp);
                chip.setOnClickListener(v->appendFunction(token));
            }
            extraScroll.addView(row);
            add(screen,extraScroll,5);
        }

        LinearLayout memoryRow=new LinearLayout(this);
        for(String token:new String[]{"MC","MR","M+","M−","MS","COPY"}){
            Button b=compactChip(token);
            b.setTextSize(11);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(31),1);
            lp.setMargins(dp(2),0,dp(2),0);
            memoryRow.addView(b,lp);
            b.setOnClickListener(v->onMemory(token));
        }
        add(screen,memoryRow,6);

        // Keyboard remains part of the scrollable content. Avoid clipping the
        // final '=' row when an OEM reports less vertical space than expected.
        // A small adaptive gap is used on tall screens, never forced overflow.
        float availableDp=getResources().getDisplayMetrics().heightPixels/
            getResources().getDisplayMetrics().density;
        int gapDp=landscape()?2:Math.max(2,(int)Math.min(10,(availableDp-760)/6));
        View flexibleSpace=new View(this);
        screen.addView(flexibleSpace,new LinearLayout.LayoutParams(-1,dp(gapDp)));

        String[][] rows={
            {"AC","()","%","÷"},
            {"7","8","9","×"},
            {"4","5","6","−"},
            {"1","2","3","+"},
            {"0",".","⌫","="}
        };
        float heightDp=getResources().getDisplayMetrics().heightPixels/
            getResources().getDisplayMetrics().density;
        float sized=(heightDp-(compact?235:425))/5f;
        int keyHeight=dp(Math.max(compact?48:53,Math.min(compact?65:81,sized)));
        for(String[] keys:rows){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            keypadRows.add(row);
            for(String key:keys){
                Button b=numericKey(key,
                    (key.equals("()")||("÷×−+%".contains(key)&&key.length()==1)),
                    key.equals("AC"),key.equals("="));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,keyHeight,1f);
                lp.setMargins(dp(2),dp(2),dp(2),dp(2));
                row.addView(b,lp);
                b.setOnClickListener(v->onKey(key));
            }
            add(screen,row,0);
        }
        TextView footer=text("CALCULATE SMART  •  LEARN BETTER",9,MUTED,true);
        footer.setGravity(Gravity.CENTER);
        add(screen,footer,4);
        keypadFooter=footer;
        updatePreview();
    }
    private void appendFunction(String f){
        switch(f){
            case "1/x":
                expression=expression.isEmpty()?"1/(":("1/("+expression+")");
                break;
            case "10^x":expression+="10^(";break;
            case "x²":expression+=expression.isEmpty()?"Ans^2":"^2";break;
            case "x³":expression+=expression.isEmpty()?"Ans^3":"^3";break;
            case "nCr":expression+="comb(";break;
            case "nPr":expression+="perm(";break;
            case "mod":expression+="mod";break;
            case ",":expression+=",";break;
            case "π":case "e":case "τ":case "φ":expression+=f;break;
            case "√":expression+="√(";break;
            default:expression+=f+"(";break;
        }
        updatePreview();
    }
    private void onKey(String key){
        switch(key){
            case "AC":expression="";updatePreview();return;
            case "⌫":
                if(!expression.isEmpty())
                    expression=expression.substring(0,
                        expression.offsetByCodePoints(expression.length(),-1));
                updatePreview();return;
            case "=":calculateNow();return;
            case "EXP":
                // EXP inserts scientific E notation: 2 EXP 3 => 2E3.
                // It must follow a number, never create an invalid leading E.
                if(!expression.isEmpty() &&
                    Character.isDigit(expression.charAt(expression.length()-1))){
                    expression+="E";
                }
                updatePreview();return;
            case "()":
                int depth=0;
                for(int i=0;i<expression.length();i++){
                    if(expression.charAt(i)=='(')depth++;
                    else if(expression.charAt(i)==')')depth--;
                }
                char last=expression.isEmpty()?' ':expression.charAt(expression.length()-1);
                boolean close=depth>0&&(Character.isDigit(last)||last==')'||
                    last=='π'||last=='e'||last=='!'||last=='%');
                expression+=close?")":"(";
                break;
            case "sin":case "cos":case "tan":appendFunction(key);return;
            case "√":expression+="√(";break;
            case "x²":appendFunction(key);return;
            case "÷":expression+="/";break;
            case "×":expression+="*";break;
            case "−":expression+="-";break;
            default:expression+=key;
        }
        updatePreview();
    }
    private boolean cacheMatches(){
        return cachedExpression!=null && cachedExpression.equals(expression) &&
            cachedDegrees==degrees &&
            cachedAnsBits==Double.doubleToLongBits(ans) &&
            cachedMemoryBits==Double.doubleToLongBits(memory);
    }
    private double evaluate() throws ExpressionEngine.CalcException{
        if(cacheMatches())return cachedResult;
        double value=ExpressionEngine.calculate(expression,degrees,ans,memory);
        cachedExpression=expression;
        cachedDegrees=degrees;
        cachedAnsBits=Double.doubleToLongBits(ans);
        cachedMemoryBits=Double.doubleToLongBits(memory);
        cachedResult=value;
        cachedFormatted=null;
        return value;
    }
    private String formattedResult() throws ExpressionEngine.CalcException{
        double value=evaluate();
        if(cachedFormatted==null){
            // Exact decimal path for ordinary arithmetic, while scientific
            // functions continue using their established double engine.
            String exact=DecimalArithmetic.tryFormat(expression);
            cachedFormatted=exact!=null?exact:ExpressionEngine.format(value);
        }
        return cachedFormatted;
    }
    private void updatePreview(){
        if(expressionView==null||answerView==null||statusView==null)return;
        expressionView.setText(expression.isEmpty()?"0":displayExpression(expression));
        if(expression.isEmpty()){
            answerView.setText("0");
            statusView.setText("Offline • Scientific • Accurate");
            statusView.setTextColor(tab<=1?0xff325577:MUTED);
            return;
        }
        try{
            answerView.setText(formattedResult());
            statusView.setText("PREVIEW  •  tap = to save");
            statusView.setTextColor(tab<=1?0xff325577:MUTED);
        }catch(ExpressionEngine.CalcException ex){
            answerView.setText("—");
            statusView.setText("Complete expression, then tap =");
            statusView.setTextColor(tab<=1?0xff325577:MUTED);
        }
    }
    private String displayExpression(String value){
        return value.replace("*","×").replace("/","÷").replace("-","−");
    }
    private void calculateNow(){
        if(expression.trim().isEmpty())return;
        try{
            String entered=expression;
            double result=evaluate();
            String readable=formattedResult();
            appendHistory(displayExpression(entered),readable);
            ans=result;
            prefs.edit().putLong("ansBits",Double.doubleToLongBits(ans)).apply();
            expression=readable;
            // Avoid a second immediate evaluation after '='; display the
            // confirmed result directly, leaving next keypress to recalculate.
            expressionView.setText(displayExpression(readable));
            answerView.setText(readable);
            statusView.setText("SAVED TO HISTORY  ✓");
            statusView.setTextColor(tab<=1?0xff135b64:CYAN);
        }catch(ExpressionEngine.CalcException ex){
            statusView.setText(ex.getMessage());
            statusView.setTextColor(tab<=1?0xffa72337:RED);
            answerView.setText("Error");
        }
    }
    private void onMemory(String code){
        try{
            switch(code){
                case "MC":memory=0;saveMemory();toast("Memory cleared");return;
                case "MR":expression+="M";updatePreview();return;
                case "COPY":{
                    String output=expression.isEmpty()?ExpressionEngine.format(ans):
                        formattedResult();
                    copy(output);
                    return;
                }
                case "M+":
                    memory+=expression.isEmpty()?ans:evaluate();break;
                case "M−":
                    memory-=expression.isEmpty()?ans:evaluate();break;
                case "MS":
                    memory=expression.isEmpty()?ans:evaluate();break;
                default:return;
            }
            if(!Double.isFinite(memory)){
                memory=0;toast("Memory overflow: reset");return;
            }
            saveMemory();
            toast("Memory = "+ExpressionEngine.format(memory));
        }catch(ExpressionEngine.CalcException e){toast(e.getMessage());}
    }
    private void saveMemory(){
        prefs.edit().putLong("memoryBits",Double.doubleToLongBits(memory)).apply();
    }
    private void copy(String value){
        ClipboardManager manager=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(manager!=null){
            manager.setPrimaryClip(ClipData.newPlainText("VISION CALC result",value));
            toast("Copied "+value);
        }
    }
    private void toast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
    private void appendHistory(String entered,String result){
        try{
            JSONArray previous=new JSONArray(prefs.getString("history","[]"));
            JSONArray next=new JSONArray();
            JSONObject item=new JSONObject();
            item.put("expression",entered);
            item.put("result",result);
            item.put("time",System.currentTimeMillis());
            next.put(item);
            for(int i=0;i<Math.min(previous.length(),59);i++){
                next.put(previous.getJSONObject(i));
            }
            prefs.edit().putString("history",next.toString()).apply();
        }catch(Exception e){
            // If local history is corrupted, preserve calculation and reset history.
            prefs.edit().remove("history").apply();
        }
    }
    private void buildHistory(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14),dp(14),dp(14),dp(16));
        box.setBackground(shape(PANEL,19,darkMode?0xff245b7b:0xffe0dfeb));
        TextView title=text("RECENT CALCULATIONS",19,CYAN,true);
        add(box,title,0);
        TextView explanation=text("Saved only on this device • tap one to reuse • last 60 results",12,MUTED,false);
        add(box,explanation,5);
        Button clear=button("🗑 Clear history",0xff7c4a6d,0xff573451,0,Color.WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(45));
        lp.topMargin=dp(12);box.addView(clear,lp);
        clear.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Delete calculation history?")
            .setMessage("This clears saved calculations from this device. Memory stays unchanged.")
            .setPositiveButton("Clear",(dialog,which)->{
                prefs.edit().remove("history").apply();showTab();
            })
            .setNegativeButton("Cancel",null).show());
        add(screen,box,0);

        JSONArray items;
        try{items=new JSONArray(prefs.getString("history","[]"));}
        catch(Exception ex){items=new JSONArray();}
        if(items.length()==0){
            TextView empty=text("No calculations yet. Use the Calculator tab to get started.",15,MUTED,false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8),dp(24),dp(8),dp(24));
            add(screen,empty,12);
            return;
        }
        for(int i=0;i<items.length();i++){
            final JSONObject item=items.optJSONObject(i);
            if(item==null)continue;
            String exp=item.optString("expression",""),result=item.optString("result","");
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(15),dp(9),dp(15),dp(9));
            card.setBackground(shape(CARD,17,darkMode?0xff2a668a:0xffd8dae7));
            TextView expr=text(exp,15,WHITE,false);
            expr.setSingleLine(true);expr.setEllipsize(TextUtils.TruncateAt.END);
            add(card,expr,0);
            TextView value=text("= "+result,20,GOLD,true);
            add(card,value,4);
            add(screen,card,8);
            card.setContentDescription("Reuse calculation "+exp+" equals "+result);
            card.setOnClickListener(v->{
                expression=exp.replace("×","*").replace("÷","/").replace("−","-");
                tab=0;buildUi();
            });
            card.setOnLongClickListener(v->{copy(result);return true;});
        }
    }
    private void buildConverter(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15),dp(15),dp(15),dp(18));
        box.setBackground(darkMode?
            gradient(0xff103558,0xff0a2342,20,0xff27678e):
            gradient(0xffffffff,0xfff1f0f8,20,0xffe2e0ec));
        add(box,text("UNIT CONVERTER",19,CYAN,true),0);
        add(box,text("12 offline categories • select units • instant result",12,MUTED,false),4);
        add(box,text("CATEGORY",12,WHITE,true),18);
        categorySpinner=spinner(UnitConverter.names());
        add(box,categorySpinner,6);
        add(box,text("VALUE",12,WHITE,true),15);
        converterValue=new EditText(this);
        converterValue.setSingleLine(true);
        converterValue.setInputType(InputType.TYPE_CLASS_NUMBER|
            InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        converterValue.setText("1");
        converterValue.setTextSize(25);
        converterValue.setTextColor(WHITE);
        converterValue.setHintTextColor(MUTED);
        converterValue.setPadding(dp(14),dp(9),dp(14),dp(9));
        converterValue.setBackground(shape(darkMode?0xff061a32:0xffe6e7ef,
            14,darkMode?0xff34678a:0xffdbdbe8));
        add(box,converterValue,6);
        add(box,text("FROM",12,WHITE,true),15);
        fromSpinner=spinner(new String[]{"m"});
        add(box,fromSpinner,6);
        add(box,text("TO",12,WHITE,true),15);
        toSpinner=spinner(new String[]{"cm"});
        add(box,toSpinner,6);

        converterResult=text("—",29,GOLD,true);
        converterResult.setGravity(Gravity.CENTER);
        converterResult.setBackground(shape(darkMode?0xff061a32:0xffe8e8f1,
            15,darkMode?0xff29547e:0xffd3d1df));
        converterResult.setPadding(dp(10),dp(16),dp(10),dp(16));
        add(box,converterResult,19);
        converterDetails=text("",13,MUTED,false);
        converterDetails.setGravity(Gravity.CENTER);
        add(box,converterDetails,6);
        LinearLayout actions=new LinearLayout(this);
        Button swap=button("⇄ Swap",
            darkMode?0xff19486e:0xff737d97,
            darkMode?0xff102d4d:0xff5e6886,0,Color.WHITE);
        Button clipboard=button("COPY RESULT",
            darkMode?0xff1c83a3:0xff694967,
            darkMode?0xff0c4b7a:0xff51364f,0,Color.WHITE);
        for(Button b:new Button[]{swap,clipboard}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(49),1);
            lp.setMargins(dp(3),0,dp(3),0);
            actions.addView(b,lp);
        }
        swap.setOnClickListener(v->{
            int from=fromSpinner.getSelectedItemPosition(),to=toSpinner.getSelectedItemPosition();
            fromSpinner.setSelection(to);
            toSpinner.setSelection(from);
            updateConversion();
        });
        clipboard.setOnClickListener(v->{
            String out=converterResult.getText().toString();
            if(!out.equals("—")&&!out.startsWith("Error"))copy(out);
        });
        add(box,actions,12);
        add(screen,box,0);
        // Listen only after all dependent widgets are created.
        categorySpinner.setOnItemSelectedListener(new OnSelection(){
            @Override public void selected(int index){
                String[] units=UnitConverter.get(index).units;
                updateSpinner(fromSpinner,units);
                updateSpinner(toSpinner,units);
                toSpinner.setSelection(Math.min(1,units.length-1));
                updateConversion();
            }
        });
        AdapterView.OnItemSelectedListener update=new OnSelection(){
            @Override public void selected(int index){updateConversion();}
        };
        fromSpinner.setOnItemSelectedListener(update);
        toSpinner.setOnItemSelectedListener(update);
        converterValue.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                updateConversion();
            }
            public void afterTextChanged(Editable e){}
        });
        categorySpinner.setSelection(0);
        // Listeners are installed after the widgets exist; populate immediately.
        String[] initial=UnitConverter.get(0).units;
        updateSpinner(fromSpinner,initial);
        updateSpinner(toSpinner,initial);
        toSpinner.setSelection(2);
        updateConversion();
    }
    private Spinner spinner(String[] labels){
        Spinner spinner=new Spinner(this);
        updateSpinner(spinner,labels);
        spinner.setBackground(shape(CARD,13,darkMode?0xff2e8fbb:0xffd7dae8));
        spinner.setPadding(dp(11),dp(7),dp(7),dp(7));
        spinner.setMinimumHeight(dp(49));
        return spinner;
    }
    private void updateSpinner(Spinner spinner,String[] values){
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item,values){
            @Override public View getView(int position,View reuse,ViewGroup parent){
                TextView view=(TextView)super.getView(position,reuse,parent);
                view.setTextColor(WHITE);
                view.setTextSize(16);
                view.setPadding(dp(10),dp(8),dp(10),dp(8));
                return view;
            }
            @Override public View getDropDownView(int position,View reuse,ViewGroup parent){
                TextView view=(TextView)super.getDropDownView(position,reuse,parent);
                view.setTextColor(0xff262b3a);
                view.setBackgroundColor(0xfffaf9fe);
                view.setTextSize(16);
                view.setPadding(dp(16),dp(11),dp(12),dp(11));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    private abstract class OnSelection implements AdapterView.OnItemSelectedListener{
        public abstract void selected(int index);
        public final void onItemSelected(AdapterView<?> parent,View view,int position,long id){
            selected(position);
        }
        public final void onNothingSelected(AdapterView<?> parent){}
    }
    private void updateConversion(){
        if(converterResult==null||converterValue==null||categorySpinner==null||
            fromSpinner==null||toSpinner==null)return;
        if(fromSpinner.getAdapter()==null||toSpinner.getAdapter()==null)return;
        int category=categorySpinner.getSelectedItemPosition();
        if(category<0)return;
        int from=fromSpinner.getSelectedItemPosition(),to=toSpinner.getSelectedItemPosition();
        if(from<0||to<0)return;
        String input=converterValue.getText().toString().trim();
        if(input.isEmpty()||input.equals("-")||input.equals(".")){
            converterResult.setText("—");converterDetails.setText("Enter a value");return;
        }
        try{
            double quantity=Double.parseDouble(input);
            double out=UnitConverter.convert(quantity,category,from,to);
            String formatted=ExpressionEngine.format(out);
            String unitName=UnitConverter.get(category).units[to];
            converterResult.setText(formatted+" "+unitName);
            converterDetails.setText(input+" "+UnitConverter.get(category).units[from]+
                " = "+formatted+" "+unitName);
        }catch(Exception e){
            converterResult.setText("Error");
            converterDetails.setText(e.getMessage()==null?"Invalid value":e.getMessage());
        }
    }
}
