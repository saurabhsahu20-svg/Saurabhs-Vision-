package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Android launcher widget-configuration screen. Each widget can choose an
 * independent 3D artwork (100 skins) or any of the 60 classic digital styles.
 */
public final class VisionWidgetSetupActivity extends Activity {
    private static final int NAVY=0xff081b40,GOLD=0xffffd05b;
    private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private int style=0,art=-1;
    private TextView choice;
    private ImageView preview;
    private Bitmap shown;
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));return g;
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(NAVY);b.setBackground(bg(Color.WHITE,12));
        b.setMinHeight(dp(44));return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        style=getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).getInt("pending",0);
        art=getSharedPreferences(VisionWidgetProvider.PREFS,MODE_PRIVATE).getInt("pending_art",-1);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xffeef3fd);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(18),dp(14),dp(28));
        scroll.addView(root);setContentView(scroll);
        TextView head=new TextView(this);
        head.setText("🎓 VISION EDU WALLS 10.0\n100 3D Digital Widgets + 60 Classic");
        head.setTextColor(NAVY);head.setTextSize(22);
        root.addView(head);
        choice=new TextView(this);
        choice.setTextColor(0xff1645a8);choice.setTextSize(15);
        choice.setPadding(dp(4),dp(12),dp(4),dp(10));
        root.addView(choice);
        preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(bg(NAVY,15));
        root.addView(preview,new LinearLayout.LayoutParams(-1,dp(180)));

        Button done=button("✅ Add Digital Widget with Selected 3D Background");
        done.setBackground(bg(GOLD,14));root.addView(done);
        done.setOnClickListener(v->{
            if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
            VisionWidgetProvider.setWidget(this,widgetId,style,art);
            Intent result=new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
            setResult(RESULT_OK,result);finish();
        });

        Button useClassic=button("🎨 Classic Digital Widget (no 3D picture)");
        root.addView(useClassic);useClassic.setOnClickListener(v->setArt(-1));
        TextView artTitle=new TextView(this);
        artTitle.setText("🌌 100 BUILT-IN 3D BACKGROUNDS");
        artTitle.setTextColor(NAVY);artTitle.setTextSize(17);
        artTitle.setPadding(0,dp(22),0,dp(8));root.addView(artTitle);
        for(int i=0;i<100;i++){
            final int id=i;
            Button b=button(String.format(java.util.Locale.US,"%03d  %s",i+1,BuiltIn3DWidgetArt.title(this,i)));
            root.addView(b);
            b.setOnClickListener(v->setArt(id));
        }
        TextView classicTitle=new TextView(this);
        classicTitle.setText("⏰ 60 CLASSIC DIGITAL WIDGET STYLES");
        classicTitle.setTextColor(NAVY);classicTitle.setTextSize(17);
        classicTitle.setPadding(0,dp(21),0,dp(8));root.addView(classicTitle);
        for(int n=0;n<WidgetCatalog.COUNT;n++){
            final int selected=n;
            Button b=button((n+1)+". "+WidgetCatalog.name(n)+" · "+WidgetCatalog.category(n));
            root.addView(b);
            b.setOnClickListener(v->{style=selected;show();});
        }
        show();
    }
    private void setArt(int id){art=id;show();}
    private void show(){
        if(choice==null)return;
        String label=BuiltIn3DWidgetArt.valid(art)?
            "🌌 3D Art "+(art+1)+"/100 — "+BuiltIn3DWidgetArt.title(this,art):
            "🎨 Classic widget "+(style+1)+"/60 — "+WidgetCatalog.name(style);
        choice.setText(label+"\nShows REAL phone clock, date, battery %, and optional weather.");
        if(shown!=null){preview.setImageDrawable(null);shown.recycle();shown=null;}
        shown=BuiltIn3DWidgetArt.background(this,art,500,220);
        if(shown==null)shown=WidgetCatalog.background(style,500,220);
        preview.setImageBitmap(shown);
    }
    @Override protected void onDestroy(){
        // View references are cleared before optional bitmap cleanup.
        if(preview!=null)preview.setImageDrawable(null);
        if(shown!=null){shown.recycle();shown=null;}
        super.onDestroy();
    }
}