package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Launcher opens this screen when a user adds the widget. */
public final class VisionWidgetSetupActivity extends Activity {
    private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private int style=0;
    private TextView choice;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        style=getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).getInt("pending",0);
        ScrollView sc=new ScrollView(this);
        sc.setBackgroundColor(0xfff0f5ff);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,30);sc.addView(root);setContentView(sc);
        TextView title=new TextView(this);
        title.setText("🎓 VISION EDU WALLS — 60 WIDGETS");
        title.setTextSize(23);title.setTextColor(0xff071b49);root.addView(title);
        choice=new TextView(this);choice.setTextSize(17);choice.setTextColor(0xff1746d2);
        root.addView(choice);select(style);
        Button save=new Button(this);save.setText("✅ Add Selected Widget");root.addView(save);
        save.setOnClickListener(v->{
            if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
            VisionWidgetProvider.setStyle(this,widgetId,style);
            Intent result=new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
            setResult(RESULT_OK,result);finish();
        });
        for(int n=0;n<WidgetCatalog.COUNT;n++){
            final int index=n;
            Button b=new Button(this);b.setAllCaps(false);
            b.setText((n+1)+". "+WidgetCatalog.name(n)+" — "+WidgetCatalog.category(n));
            b.setTextColor(0xff071b49);
            root.addView(b);
            b.setOnClickListener(v->select(index));
        }
    }
    private void select(int index){style=WidgetCatalog.norm(index);
        if(choice!=null)choice.setText("Selected "+(style+1)+"/60: "+WidgetCatalog.name(style)+"\n"+WidgetCatalog.subtitle(style));}
}
