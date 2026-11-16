package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** City-level news with published time and credited original publisher. */
public final class CityNewsActivity extends Activity {
    private static final int BG=0xff071327,PANEL=0xff142841,EDGE=0xff2c5276;
    private static final int WHITE=0xfff2f7ff,MUTED=0xffa8bad3,
        CYAN=0xff65dcfa,MINT=0xff80edc0,GOLD=0xffffd190;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout root,results;
    private TextView cityLabel,periodLabel,langLabel,status;
    private ProgressBar loading;
    private String city=CityNewsSources.STATE;
    private int days=7;
    private boolean hindi=true,closed=false;
    private int sequence=0;
    private long lastFetch=0L;

    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int fill,int border,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);g.setCornerRadius(dp(radius));
        if(border!=0)g.setStroke(dp(1),border);
        return g;
    }
    private TextView text(String s,int size,int color,boolean medium){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(size);v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        return v;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout l,View view,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(margin);l.addView(view,p);
    }
    private TextView chip(String title,Runnable click){
        TextView t=text(title,13,WHITE,true);
        t.setBackground(shape(PANEL,EDGE,17));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9),dp(11),dp(9),dp(11));
        t.setOnClickListener(v->click.run());
        return t;
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        android.content.SharedPreferences p=getSharedPreferences("s_browser_news",MODE_PRIVATE);
        city=p.getString("news_city",CityNewsSources.STATE);
        days=p.getInt("news_days",7);
        hindi=p.getBoolean("news_hindi",true);
        if(CityNewsSources.normalize(city).isEmpty())city=CityNewsSources.STATE;
        build();
        reload();
    }
    private void build(){
        root=column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(14),dp(9),dp(14),dp(12));
        setContentView(root);

        LinearLayout bar=row();
        TextView back=chip("‹",this::finish);
        bar.addView(back,new LinearLayout.LayoutParams(dp(43),dp(47)));
        LinearLayout heading=column();heading.setPadding(dp(11),0,0,0);
        add(heading,text("CG City News",23,WHITE,true),0);
        add(heading,text("Latest available headlines • credited sources",
            12,CYAN,false),2);
        bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView update=chip("↻",this::reload);
        bar.addView(update,new LinearLayout.LayoutParams(dp(48),dp(47)));
        add(root,bar,0);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=column();body.setPadding(dp(3),dp(8),dp(3),dp(20));
        scroller.addView(body);

        LinearLayout information=column();
        information.setPadding(dp(15),dp(14),dp(15),dp(14));
        information.setBackground(shape(0xff183451,0xff3f779a,21));
        add(information,text("खबरें अपने शहर की",20,WHITE,true),0);
        add(information,text(
            "Headlines are provided by news publishers. Check the full original article before sharing or relying on the information.",
            12,MUTED,false),7);
        add(body,information,2);

        cityLabel=chip("📍 "+city+"  ▾",this::chooseCity);
        cityLabel.setTextColor(MINT);
        LinearLayout.LayoutParams cityP=new LinearLayout.LayoutParams(-1,dp(51));
        cityP.topMargin=dp(13);body.addView(cityLabel,cityP);

        LinearLayout toggles=row();
        periodLabel=chip(periodName()+"  ▾",this::choosePeriod);
        langLabel=chip(hindi?"हिन्दी  ▾":"English  ▾",this::chooseLanguage);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(48),1);
        a.rightMargin=dp(8);toggles.addView(periodLabel,a);
        toggles.addView(langLabel,new LinearLayout.LayoutParams(0,dp(48),1));
        add(body,toggles,8);

        LinearLayout links=row();
        TextView govt=chip("🏛 Govt. updates",()->
            openUrl(CityNewsSources.GOVT_NEWS));
        TextView search=chip("⌕ News search",()->
            openUrl(CityNewsSources.liveNewsSearch(city)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);
        p.rightMargin=dp(7);links.addView(govt,p);
        links.addView(search,new LinearLayout.LayoutParams(0,dp(48),1));
        add(body,links,9);

        status=text("Checking latest headlines…",12,GOLD,false);
        add(body,status,17);
        loading=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        loading.setIndeterminate(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(3));
        lp.topMargin=dp(6);body.addView(loading,lp);

        results=column();add(body,results,6);
        add(body,text("Transparency: S Browser does not write, fact-check, or endorse headlines. "+
            "News aggregation may omit articles. The displayed date comes from the feed, "+
            "and articles can be corrected or updated after publishing.",12,MUTED,false),18);
    }

    private String periodName(){
        return days==1?"Last 24 hours":days==3?"Last 3 days":"Last 7 days";
    }
    private void prefs(){
        getSharedPreferences("s_browser_news",MODE_PRIVATE).edit()
            .putString("news_city",city).putInt("news_days",days)
            .putBoolean("news_hindi",hindi).apply();
    }
    private void chooseCity(){
        final List<String> all=CityNewsSources.cities();
        String[] options=new String[all.size()+1];
        options[0]="⌕ Enter any other CG town or city";
        for(int i=0;i<all.size();i++)options[i+1]=all.get(i);
        new AlertDialog.Builder(this).setTitle("Choose CG city or district")
            .setItems(options,(dialog,which)->{
                if(which==0)customCity();
                else selectCity(all.get(which-1));
            }).setNegativeButton("Cancel",null).show();
    }
    private void customCity(){
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setText(city.equals(CityNewsSources.STATE)?"":city);
        input.setHint("e.g. Baghbahara, Pithora, Charoda");
        input.setInputType(InputType.TYPE_CLASS_TEXT|
            InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Search any Chhattisgarh city")
            .setView(input).setNegativeButton("Cancel",null)
            .setPositiveButton("Show news",(d,w)->{
                String chosen=CityNewsSources.normalize(input.getText().toString());
                if(chosen.isEmpty()||chosen.equals(CityNewsSources.STATE)){
                    toast("Please enter a valid city or town");return;
                }
                selectCity(chosen);
            }).show();
    }
    private void selectCity(String chosen){
        city=chosen;cityLabel.setText("📍 "+city+"  ▾");
        prefs();reload();
    }
    private void choosePeriod(){
        String[] names={"Last 24 hours","Last 3 days","Last 7 days"};
        new AlertDialog.Builder(this).setTitle("Publication period")
            .setItems(names,(d,index)->{
                days=index==0?1:index==1?3:7;
                periodLabel.setText(periodName()+"  ▾");
                prefs();reload();
            }).show();
    }
    private void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle("News feed language")
            .setItems(new String[]{"हिन्दी (India)","English (India)"},(d,index)->{
                hindi=index==0;
                langLabel.setText(hindi?"हिन्दी  ▾":"English  ▾");
                prefs();reload();
            }).show();
    }
    private void reload(){
        int request=++sequence;
        final String area=city;
        final int window=days;
        final boolean language=hindi;
        status.setText("Loading publisher headlines for "+area+"…");
        results.removeAllViews();
        loading.setVisibility(View.VISIBLE);
        new Thread(()->{
            CityNewsFeed.Result output=null;
            String error=null;
            try{output=CityNewsFeed.fetch(area,language,window);}
            catch(Exception e){error=e.getMessage();}
            final CityNewsFeed.Result delivered=output;
            final String failure=error;
            runOnUiThread(()->{
                if(closed||request!=sequence)return;
                loading.setVisibility(View.GONE);
                if(delivered==null){
                    status.setText("Could not get live news. Check internet or try News search.");
                    add(results,text("Network/feed unavailable"+
                        (failure==null?"":": "+failure)+
                        ". No outdated headlines have been substituted.",
                        13,MUTED,false),12);
                }else{
                    lastFetch=delivered.checkedAt;
                    render(delivered);
                }
            });
        },"SBrowser-CityNews").start();
    }
    private void render(CityNewsFeed.Result feed){
        results.removeAllViews();
        DateFormat checked=DateFormat.getTimeInstance(DateFormat.SHORT);
        status.setText(feed.stories.size()+" published headline(s)  •  Checked "+
            checked.format(new Date(feed.checkedAt))+
            "  •  "+periodName());
        if(feed.stories.isEmpty()){
            add(results,text("No dated articles found for this city and time range. "+
                "Try 7 days, the other language, or News search. "+
                "This does not mean nothing happened in your city.",
                14,MUTED,false),12);
            return;
        }
        for(CityNewsFeed.Story article:feed.stories){
            LinearLayout item=column();
            item.setPadding(dp(15),dp(14),dp(15),dp(13));
            item.setBackground(shape(PANEL,EDGE,18));
            TextView title=text(article.title,16,WHITE,true);
            title.setMaxLines(4);
            add(item,title,0);
            String when=DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(article.published));
            add(item,text("📰 "+article.source+"  •  "+when,12,CYAN,false),9);
            TextView open=text("Read original news  ↗",13,MINT,true);
            add(item,open,10);
            item.setContentDescription(article.title+
                ". Publisher: "+article.source+". Published "+when);
            item.setOnClickListener(v->openUrl(article.url));
            add(results,item,9);
        }
    }
    private void openUrl(String url){
        if(!CityNewsFeed.isWebUrl(url)){toast("News link is invalid");return;}
        Intent view=new Intent(Intent.ACTION_VIEW,Uri.parse(url),this,MainActivity.class);
        view.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(view);
    }
    @Override protected void onDestroy(){
        closed=true;sequence++;handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
