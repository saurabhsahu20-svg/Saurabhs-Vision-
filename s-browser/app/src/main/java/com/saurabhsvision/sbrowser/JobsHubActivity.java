package com.saurabhsvision.sbrowser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

/** Official Jobs & Exams portal, with honest source labels and no invented vacancies. */
public final class JobsHubActivity extends Activity {
    private static final int BG=0xff071327, PANEL=0xff122642, EDGE=0xff315479;
    private static final int WHITE=0xfff4f8ff,MUTED=0xffa9bed6,
        CYAN=0xff60def3, GREEN=0xff77e5b6, GOLD=0xffffcf8b;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Runnable pending;
    private LinearLayout list,root,filters;
    private TextView areaBtn,typeBtn,alertBtn,subtitle;
    private EditText searchBox;
    private String selectedArea="All Chhattisgarh",selectedType="All";
    private int currentCount=0;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int stroke,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));
        if(stroke!=0)g.setStroke(dp(1),stroke);
        return g;
    }
    private TextView label(String s,int font,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(font);v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        return v;
    }
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private void add(LinearLayout parent,View view,int gap){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(gap);parent.addView(view,p);
    }
    private TextView button(String text,Runnable onTap){
        TextView t=label(text,13,WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12),dp(12),dp(12),dp(12));
        t.setBackground(shape(PANEL,EDGE,17));
        t.setOnClickListener(v->onTap.run());return t;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        android.content.SharedPreferences prefs=getSharedPreferences(
            GovtAlerts.PREFS,MODE_PRIVATE);
        selectedArea=prefs.getString("job_area","All Chhattisgarh");
        selectedType=prefs.getString("job_type","All");
        if(GovtAlerts.enabled(this))GovtAlerts.schedule(this);
        build();
    }
    private void build(){
        root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(15),dp(12),dp(15),dp(12));
        setContentView(root);
        LinearLayout top=row();
        TextView back=button("‹",this::finish);
        top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(46)));
        LinearLayout title=column();title.setPadding(dp(11),0,0,0);
        add(title,label("Jobs & Exams",24,WHITE,true),0);
        add(title,label("Chhattisgarh  •  Official sources",12,CYAN,false),2);
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView news=button("📰",()->startActivity(
            new Intent(this,CityNewsActivity.class)));
        top.addView(news,new LinearLayout.LayoutParams(dp(50),dp(46)));
        TextView bell=button("🔔",this::showAlertSettings);
        top.addView(bell,new LinearLayout.LayoutParams(dp(50),dp(46)));
        add(root,top,0);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=column();body.setPadding(0,dp(12),0,dp(20));
        scroll.addView(body);

        LinearLayout intro=column();intro.setPadding(dp(16),dp(16),dp(16),dp(15));
        intro.setBackground(shape(0xff183554,0xff5075a9,22));
        add(intro,label("Government jobs • Entrance exams",17,WHITE,true),0);
        add(intro,label("Source-first information. Check eligibility, last date and PDF on the official website before applying.",12,MUTED,false),7);
        add(body,intro,2);

        searchBox=new EditText(this);
        searchBox.setSingleLine(true);searchBox.setTextSize(15);searchBox.setTextColor(WHITE);
        searchBox.setHintTextColor(MUTED);
        searchBox.setHint("Post, department, exam, or keyword");
        searchBox.setBackground(shape(PANEL,EDGE,19));
        searchBox.setPadding(dp(16),0,dp(16),0);
        LinearLayout.LayoutParams searchP=new LinearLayout.LayoutParams(-1,dp(53));
        searchP.topMargin=dp(15);body.addView(searchBox,searchP);

        filters=row();
        areaBtn=button("📍 "+selectedArea+"  ▾",this::chooseArea);
        typeBtn=button(selectedType+"  ▾",this::chooseType);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(50),1);
        a.rightMargin=dp(8);filters.addView(areaBtn,a);
        filters.addView(typeBtn,new LinearLayout.LayoutParams(0,dp(50),1));
        add(body,filters,9);

        TextView searchOfficial=button("⌕  Search government websites  →",this::runOfficialSearch);
        searchOfficial.setTextColor(CYAN);
        add(body,searchOfficial,10);

        LinearLayout tools=row();
        alertBtn=button(GovtAlerts.enabled(this)?"🔔 Alerts ON":"🔔 Get update alerts",
            this::showAlertSettings);
        TextView tips=button("ⓘ How results work",this::showAccuracyInfo);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1);
        p.rightMargin=dp(7);tools.addView(alertBtn,p);
        tools.addView(tips,new LinearLayout.LayoutParams(0,dp(47),1));
        add(body,tools,10);

        subtitle=label("OFFICIAL PORTALS",13,GOLD,true);
        add(body,subtitle,22);

        list=column();add(body,list,6);
        TextView note=label("Only verified government/exam portals are listed as official. Search-engine results are NOT independently verified vacancies. This directory does not display guessed dates or opening counts.",12,MUTED,false);
        add(body,note,17);

        searchBox.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                if(pending!=null)handler.removeCallbacks(pending);
                pending=()->render();handler.postDelayed(pending,130);
            }
            public void afterTextChanged(Editable e){}
        });
        render();
    }
    private String query(){return searchBox==null?"":searchBox.getText().toString().trim();}
    private void chooseArea(){
        new AlertDialog.Builder(this).setTitle("City or region")
            .setItems(GovtSources.CITIES,(d,i)->{
                if(i==GovtSources.CITIES.length-1)chooseOtherDistrict();
                else changeArea(GovtSources.CITIES[i]);
            }).setNegativeButton("Cancel",null).show();
    }
    private void chooseOtherDistrict(){
        new AlertDialog.Builder(this).setTitle("All 33 CG districts")
            .setItems(GovtSources.DISTRICTS,(d,i)->changeArea(GovtSources.DISTRICTS[i]))
            .setNegativeButton("Cancel",null).show();
    }
    private void changeArea(String area){
        selectedArea=area;
        getSharedPreferences(GovtAlerts.PREFS,MODE_PRIVATE).edit()
            .putString("job_area",area).apply();
        areaBtn.setText("📍 "+selectedArea+"  ▾");
        render();
        if(GovtAlerts.enabled(this))GovtAlerts.schedule(this);
    }
    private void chooseType(){
        new AlertDialog.Builder(this).setTitle("Select vacancy/exam category")
            .setItems(GovtSources.TYPES,(d,i)->{
                selectedType=GovtSources.TYPES[i];
                getSharedPreferences(GovtAlerts.PREFS,MODE_PRIVATE).edit()
                    .putString("job_type",selectedType).apply();
                typeBtn.setText(selectedType+"  ▾");
                render();
                if(GovtAlerts.enabled(this))GovtAlerts.schedule(this);
            }).setNegativeButton("Cancel",null).show();
    }
    private void render(){
        if(list==null)return;
        list.removeAllViews();
        List<GovtSources.Source> sources=GovtSources.filter(
            selectedArea,selectedType,query());
        currentCount=sources.size();
        subtitle.setText(currentCount+" OFFICIAL PORTAL"+(currentCount==1?"":"S")+
            " FOR YOUR SELECTION");
        if(sources.isEmpty()){
            TextView none=label("No matching portal in this curated directory. Try Government Site Search above, or change the city/category.",14,MUTED,false);
            add(list,none,12);
        }
        for(GovtSources.Source src:sources)sourceCard(src);
        String district=GovtSources.normalizedArea(selectedArea);
        if(!selectedArea.equals("All Chhattisgarh")&&!hasDirectDistrict(district)){
            TextView hint=label(
                "This district does not yet have a verified direct portal in the app. Use the Government Site Search button; confirm every result's .gov.in/.nic.in domain and notice PDF.",13,GOLD,false);
            add(list,hint,16);
        }
    }
    private boolean hasDirectDistrict(String district){
        for(GovtSources.Source s:GovtSources.OFFICIAL)
            if(s.category.equals("District jobs")&&s.area.equalsIgnoreCase(district))return true;
        return false;
    }
    private void sourceCard(GovtSources.Source src){
        LinearLayout card=column();card.setPadding(dp(14),dp(14),dp(14),dp(12));
        card.setBackground(shape(PANEL,EDGE,19));
        TextView headline=label(src.title,17,WHITE,true);add(card,headline,0);
        TextView details=label(src.authority+"  •  "+src.category+
            "\n"+src.note,12,MUTED,false);
        add(card,details,5);
        TextView host=label("✓ OFFICIAL: "+src.host(),12,GREEN,false);
        add(card,host,8);
        LinearLayout actions=row();
        TextView open=button("Official notices  ↗",()->openWebsite(src.url));
        open.setTextColor(GREEN);
        TextView find=button("Find by keyword  ⌕",
            ()->openWebsite(GovtSources.officialSearch(src,query())));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);
        p.rightMargin=dp(7);actions.addView(open,p);
        actions.addView(find,new LinearLayout.LayoutParams(0,dp(48),1));
        add(card,actions,11);
        add(list,card,8);
    }
    private void openWebsite(String url){
        if(url==null||!url.startsWith("https://")){toast("Secure official link required");return;}
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url),this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    private void runOfficialSearch(){
        String place=GovtSources.normalizedArea(selectedArea);
        String query=query();
        if(!selectedArea.equals("All Chhattisgarh")&&!place.isEmpty()){
            openWebsite(GovtSources.districtSearch(place,
                (selectedType.equals("All")?"":selectedType)+" "+query));
            return;
        }
        if(selectedType.equals("Railway")){
            openWebsite(GovtSources.officialSearch(find("rrb"),query));
        }else if(selectedType.equals("Engineering exams")){
            openWebsite(GovtSources.officialSearch(find("jee_main"),query));
        }else if(selectedType.equals("School admissions")){
            openWebsite(GovtSources.officialSearch(find("navodaya"),query));
        }else if(selectedType.equals("CG Govt")){
            openWebsite(GovtSources.officialSearch(find("cgvyapam"),query));
        }else{
            openWebsite(GovtSources.districtSearch("Chhattisgarh",query));
        }
    }
    private GovtSources.Source find(String id){
        for(GovtSources.Source src:GovtSources.OFFICIAL)if(src.id.equals(id))return src;
        return GovtSources.OFFICIAL.get(0);
    }
    private void showAccuracyInfo(){
        new AlertDialog.Builder(this).setTitle("How accuracy works")
            .setMessage("1. Open official portal / notification PDF.\n"+
                "2. Check notification number, publish date and last application date.\n"+
                "3. Read age, education, category, fee and location requirements.\n"+
                "4. Apply only through the publisher's official portal.\n\n"+
                "Search results are links, NOT confirmed open vacancies. A portal update can be a result, correction, merit list or old archive.\n\n"+
                "NTSE: the app links to NCERT; an active exam cycle is not assumed.")
            .setPositiveButton("Understood",null).show();
    }
    private void showAlertSettings(){
        boolean enabled=GovtAlerts.enabled(this);
        long last=GovtAlerts.prefs(this).getLong("last_check",0L);
        String lastChecked=last==0L?"Not checked yet":
            java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT).format(new java.util.Date(last));
        String details="Your saved region: "+selectedArea+"\n"+
            "Category: "+selectedType+"\n"+
            "Last background check: "+lastChecked+"\n\n"+
            "Optional Android background check (approximately daily when the system allows)."+
            " If a watched official page changes, the app can alert you to review it."+
            " Changes are NOT guaranteed to be a new vacancy or an open application."+
            " No account, SMS or personal data is required.";
        new AlertDialog.Builder(this).setTitle("Official portal update alerts")
            .setMessage(details)
            .setPositiveButton(enabled?"Turn alerts OFF":"Turn alerts ON",(d,w)->{
                if(enabled){
                    GovtAlerts.setEnabled(this,false);
                    alertBtn.setText("🔔 Get update alerts");
                    toast("Update checks disabled");
                }else{
                    if(Build.VERSION.SDK_INT>=33&&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            !=PackageManager.PERMISSION_GRANTED){
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},92);
                    }else startAlerts();
                }
            })
            .setNeutralButton("Check now",(d,w)->{
                GovtAlerts.checkNow(this);
                toast("Checking official portals. Updates will be shown if a watched page changed.");
            })
            .setNegativeButton("Close",null).show();
    }
    private void startAlerts(){
        GovtAlerts.setEnabled(this,true);
        if(alertBtn!=null)alertBtn.setText("🔔 Alerts ON");
        toast("Alerts enabled. First scan creates a baseline; Android controls check timing.");
        GovtAlerts.checkNow(this);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==92&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)
            startAlerts();
        else if(request==92)toast("Notification permission required for update alerts");
    }
    @Override protected void onDestroy(){
        if(pending!=null)handler.removeCallbacks(pending);
        super.onDestroy();
    }
}
