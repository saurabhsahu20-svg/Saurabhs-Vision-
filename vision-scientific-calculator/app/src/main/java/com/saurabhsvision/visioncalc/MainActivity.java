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
    private TextView themeToggle;
    private static final String[] TAB_NAMES={"CALCULATOR","UNIT CONVERTER","HISTORY"};
    private SharedPreferences prefs;
    private LinearLayout root, screen;
    private TextView expressionView,answerView,statusView,degreeButton;
    private ScrollView scroll;
    private String expression="";
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
        darkMode=prefs.getBoolean("darkMode",false);
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
        if(darkMode){
            NAVY=0xff06162d; PANEL=0xff0d2545; CARD=0xff14335a;
            CYAN=0xff23dbf0; GOLD=0xffffd16b; WHITE=0xfff7fbff;
            MUTED=0xffa8c7dd; SOFT=0xff173b65; RED=0xffff9b9b;
        }else{
            NAVY=0xfff8f7fc; PANEL=0xfff1f0f8; CARD=0xffe4e5ee;
            CYAN=0xff4a6da8; GOLD=0xff634366; WHITE=0xff1c2330;
            MUTED=0xff697187; SOFT=0xffdee0ea; RED=0xffa23a54;
        }
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
        root.setPadding(dp(13),dp(9),dp(13),dp(16));
        scroll.addView(root);
        setContentView(scroll);
        buildHeader();
        buildTabs();
        screen=new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fill=new LinearLayout.LayoutParams(-1,0,1f);
        fill.topMargin=dp(10);
        root.addView(screen,fill);
        showTab();
    }
    private void buildHeader(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);
        icon.setImageResource(R.drawable.ic_vision_calc);
        icon.setContentDescription("VISION CALC calculator logo");
        row.addView(icon,new LinearLayout.LayoutParams(dp(45),dp(45)));

        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(9),0,0,0);
        TextView brand=text("VISION CALC",21,WHITE,true);
        titles.addView(brand);
        TextView tag=text("SAURABH'S VISION™  •  Learn & Calculate",10,MUTED,false);
        titles.addView(tag);
        row.addView(titles,new LinearLayout.LayoutParams(0,-2,1f));

        TextView historyIcon=text("◷",26,WHITE,true);
        historyIcon.setGravity(Gravity.CENTER);
        historyIcon.setBackground(shape(CARD,14,0));
        historyIcon.setContentDescription("Open calculation history");
        LinearLayout.LayoutParams hLp=new LinearLayout.LayoutParams(dp(42),dp(42));
        hLp.rightMargin=dp(7);row.addView(historyIcon,hLp);
        historyIcon.setOnClickListener(v->{tab=2;buildUi();});

        themeToggle=text(darkMode?"☀":"☾",25,WHITE,true);
        themeToggle.setGravity(Gravity.CENTER);
        themeToggle.setBackground(shape(CARD,14,0));
        themeToggle.setContentDescription(darkMode?"Switch to light theme":"Switch to dark theme");
        row.addView(themeToggle,new LinearLayout.LayoutParams(dp(42),dp(42)));
        themeToggle.setOnClickListener(v->{
            darkMode=!darkMode;
            prefs.edit().putBoolean("darkMode",darkMode).apply();
            applyPalette();
            buildUi();
        });
        add(root,row,4);
    }
    private void buildTabs(){
        LinearLayout tabs=new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackground(shape(PANEL,18,0));
        tabs.setPadding(dp(4),dp(4),dp(4),dp(4));
        for(int i=0;i<TAB_NAMES.length;i++){
            final int index=i;
            TextView chip=text(TAB_NAMES[i],11,
                i==tab?(darkMode?NAVY:0xffffffff):MUTED,true);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setTextScaleX(.94f);
            chip.setBackground(shape(i==tab?CYAN:CARD,13,0));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            tabs.addView(chip,lp);
            chip.setContentDescription("Open "+TAB_NAMES[i]+" tab");
            chip.setOnClickListener(v->{tab=index;buildUi();});
        }
        add(root,tabs,12);
    }
    private void showTab(){
        screen.removeAllViews();
        if(tab==0)buildCalculator();
        else if(tab==1)buildConverter();
        else buildHistory();
    }
    private void buildCalculator(){
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16),dp(10),dp(16),dp(14));
        panel.setBackground(gradient(0xff0e2c51,0xff071d3a,17,0xff1b567b));
        TextView title=text("SCIENTIFIC EXPRESSION",11,CYAN,true);
        add(panel,title,0);
        expressionView=text(expression.isEmpty()?"0":expression,25,WHITE,true);
        expressionView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);
        expressionView.setContentDescription("Expression "+(expression.isEmpty()?"empty":expression));
        add(panel,expressionView,5);
        answerView=text("0",28,GOLD,true);
        answerView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        answerView.setSingleLine(true);
        answerView.setEllipsize(TextUtils.TruncateAt.START);
        add(panel,answerView,7);
        statusView=text("Offline • DEG/RAD • 12 converters",11,MUTED,false);
        statusView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        add(panel,statusView,7);
        add(screen,panel,0);

        String[] memoryKeys={"MC","MR","M+","M−","MS","COPY"};
        LinearLayout memoryRow=new LinearLayout(this);
        for(String token:memoryKeys){
            Button b=button(token,0xff1b4068,0xff112b4a,0xff2d5a7e,CYAN);
            b.setTextSize(12);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(41),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            memoryRow.addView(b,lp);
            b.setOnClickListener(v->onMemory(token));
        }
        add(screen,memoryRow,12);
        add(screen,text("FUNCTIONS  •  swipe sideways for more",11,MUTED,true),13);
        HorizontalScrollView functionsScroll=new HorizontalScrollView(this);
        functionsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout functions=new LinearLayout(this);
        functions.setGravity(Gravity.CENTER_VERTICAL);
        String[] funcTokens={"sin","cos","tan","asin","acos","atan",
            "ln","log","sqrt","cbrt","abs","exp","sinh","cosh","tanh",
            "nCr","nPr","root","gcd","lcm","floor","ceil",
            "mod","1/x","10^x","x²","e","τ","φ","rad","deg"};
        for(String token:funcTokens){
            Button chip=button(token,0xff1e4673,0xff0b2b4c,0xff2c89a0,WHITE);
            chip.setTextSize(13);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(68),dp(42));
            lp.setMargins(dp(2),dp(4),dp(3),dp(4));
            functions.addView(chip,lp);
            chip.setOnClickListener(v->appendFunction(token));
        }
        functionsScroll.addView(functions);
        add(screen,functionsScroll,1);

        String[][] rows={
            {"AC","(",")","%","⌫"},
            {"sin","cos","tan","√","x²"},
            {"7","8","9","÷","^"},
            {"4","5","6","×","!"},
            {"1","2","3","−","π"},
            {"0",".","Ans","+","="}
        };
        int h=dp(landscape()?45:55);
        for(String[] keys:rows){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            for(String key:keys){
                boolean equal=key.equals("=");
                boolean control=key.equals("AC")||key.equals("⌫");
                boolean isDigit=key.matches("[0-9.]");
                boolean action=!isDigit;
                int top=equal?0xff69f6fc:(control?0xff9b4151:(action?0xff245f8a:0xff214a78));
                int bottom=equal?0xff0bbddc:(control?0xff6a273d:(action?0xff0d325b:0xff0b2b51));
                int border=equal?0xffbfffff:(control?0xffd58d9b:(action?0xff439bca:0xff3678a6));
                Button button=button(key,top,bottom,border,equal?NAVY:WHITE);
                button.setTextSize(key.length()>2?14:20);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,h,1);
                lp.setMargins(dp(2),dp(3),dp(2),dp(3));
                row.addView(button,lp);
                button.setOnClickListener(v->onKey(key));
            }
            add(screen,row,0);
        }
        TextView examples=text(
            "Try: sin(30) × 200  •  comb(5,2)  •  (2+3)^2  •  9!  •  log(100)",
            11,MUTED,false);
        examples.setGravity(Gravity.CENTER);
        add(screen,examples,10);
        updatePreview();
    }
    private void appendFunction(String f){
        switch(f){
            case "1/x":
                expression=expression.isEmpty()?"1/(":("1/("+expression+")");
                break;
            case "10^x":expression+="10^(";break;
            case "x²":expression+=expression.isEmpty()?"Ans^2":"^2";break;
            case "nCr":expression+="comb(";break;
            case "nPr":expression+="perm(";break;
            case "mod":expression+="mod";break;
            case "e":case "τ":case "φ":expression+=f;break;
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
    private double evaluate() throws ExpressionEngine.CalcException{
        return ExpressionEngine.calculate(expression,degrees,ans,memory);
    }
    private void updatePreview(){
        if(expressionView==null||answerView==null||statusView==null)return;
        expressionView.setText(expression.isEmpty()?"0":displayExpression(expression));
        if(expression.isEmpty()){
            answerView.setText("0");
            statusView.setText("Offline • Scientific • Accurate");
            statusView.setTextColor(MUTED);
            return;
        }
        try{
            double value=evaluate();
            answerView.setText(ExpressionEngine.format(value));
            statusView.setText("PREVIEW  •  tap = to save");
            statusView.setTextColor(MUTED);
        }catch(ExpressionEngine.CalcException ex){
            answerView.setText("—");
            statusView.setText("Complete expression, then tap =");
            statusView.setTextColor(MUTED);
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
            String readable=ExpressionEngine.format(result);
            appendHistory(displayExpression(entered),readable);
            ans=result;
            prefs.edit().putLong("ansBits",Double.doubleToLongBits(ans)).apply();
            expression=readable;
            updatePreview();
            statusView.setText("SAVED TO HISTORY  ✓");
            statusView.setTextColor(CYAN);
        }catch(ExpressionEngine.CalcException ex){
            statusView.setText(ex.getMessage());
            statusView.setTextColor(RED);
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
                        ExpressionEngine.format(evaluate());
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
        box.setBackground(shape(PANEL,17,0xff245b7b));
        TextView title=text("RECENT CALCULATIONS",19,CYAN,true);
        add(box,title,0);
        TextView explanation=text("Saved only on this device • tap one to reuse • last 60 results",12,MUTED,false);
        add(box,explanation,5);
        Button clear=button("🗑 Clear history",0xff5b2e46,0xff361c32,0xff99677b,WHITE);
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
            card.setBackground(shape(CARD,13,0xff2a668a));
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
        box.setBackground(gradient(0xff103558,0xff0a2342,17,0xff27678e));
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
        converterValue.setBackground(shape(0xff061a32,12,0xff34678a));
        add(box,converterValue,6);
        add(box,text("FROM",12,WHITE,true),15);
        fromSpinner=spinner(new String[]{"m"});
        add(box,fromSpinner,6);
        add(box,text("TO",12,WHITE,true),15);
        toSpinner=spinner(new String[]{"cm"});
        add(box,toSpinner,6);

        converterResult=text("—",29,GOLD,true);
        converterResult.setGravity(Gravity.CENTER);
        converterResult.setBackground(shape(0xff061a32,13,0xff29547e));
        converterResult.setPadding(dp(10),dp(16),dp(10),dp(16));
        add(box,converterResult,19);
        converterDetails=text("",13,MUTED,false);
        converterDetails.setGravity(Gravity.CENTER);
        add(box,converterDetails,6);
        LinearLayout actions=new LinearLayout(this);
        Button swap=button("⇄ Swap",0xff19486e,0xff102d4d,CYAN,WHITE);
        Button clipboard=button("COPY RESULT",0xff1c83a3,0xff0c4b7a,CYAN,WHITE);
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
        spinner.setBackground(shape(0xff163b63,11,0xff2e8fbb));
        spinner.setPadding(dp(11),dp(7),dp(7),dp(7));
        spinner.setMinimumHeight(dp(49));
        return spinner;
    }
    private void updateSpinner(Spinner spinner,String[] values){
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,values);
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
