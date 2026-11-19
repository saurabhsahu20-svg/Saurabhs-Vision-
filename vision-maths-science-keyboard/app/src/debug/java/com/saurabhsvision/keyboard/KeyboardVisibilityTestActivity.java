package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

/** Debug-only on-device IME visibility probe; not included in release builds. */
public final class KeyboardVisibilityTestActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,16,16,16);
        root.setBackgroundColor(Color.WHITE);
        TextView help=new TextView(this);
        help.setText("S BOARD KEYBOARD BOTTOM ROW VISIBILITY TEST");
        help.setTextSize(16);
        help.setTextColor(0xff13245b);
        root.addView(help,new LinearLayout.LayoutParams(-1,-2));
        TextView typingStatus=new TextView(this);
        typingStatus.setText("Characters entered: 0");
        typingStatus.setTextSize(16);
        typingStatus.setTextColor(0xff13245b);
        typingStatus.setContentDescription("IME_TAP_CHARACTER_COUNT");
        root.addView(typingStatus,new LinearLayout.LayoutParams(-1,56));
        EditText test=new EditText(this);
        test.setHint("TEST_FIELD");
        test.setContentDescription("IME_TYPING_TEST_FIELD");
        test.setSingleLine(true);
        test.setTextSize(18);
        test.setBackgroundColor(0xffeff6ff);
        root.addView(test,new LinearLayout.LayoutParams(-1,90));
        test.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence a,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence a,int start,int before,int count){
                typingStatus.setText("Characters entered: "+a.length());
            }
            @Override public void afterTextChanged(Editable a){}
        });
        setContentView(root);
        test.requestFocus();
        test.postDelayed(()->{
            InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm!=null) imm.showSoftInput(test,InputMethodManager.SHOW_IMPLICIT);
        },800);
    }
}
