package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * S Browser beta: native Android WebView, no ads or analytics.
 * Offline dictionary and local browsing data. No JavaScript bridges.
 */
public final class MainActivity extends Activity {
    private static final int BG=0xff071327, PANEL=0xff11233f, EDGE=0xff244d74,
        WHITE=0xfff4f8ff, MUTED=0xff9cb2ce, CYAN=0xff55e4f4,
        MINT=0xff72eabb, PURPLE=0xffa993ff;
    private static final int PICK_FILE=81, VOICE_SEARCH=82, QR_SCAN=83, MAX_TABS=8;
    private static final String[] FOCUS_DOMAINS={
        "instagram.com","tiktok.com","facebook.com","x.com","reddit.com"
    };
    private static final class Tab {
        WebView web;
        String title="New Tab";
        String mobileUserAgent="";
        boolean desktop=false;
    }
    private final ArrayList<Tab> tabs=new ArrayList<>();
    private int selected=-1;
    private boolean home=true;
    private boolean dashboardOpen=false;
    private BrowserStore store;
    private DictionaryRepository dictionary;
    private TextToSpeech tts;
    private LinearLayout root,top,bottom,tabLine;
    private HorizontalScrollView tabStrip;
    private FrameLayout contentFrame;
    private AutoCompleteTextView address;
    private ProgressBar progress;
    private TextView tabBadge,focusBadge;
    private LinearLayout[] navigationCells;
    private TextView[] navigationIcons,navigationLabels;
    private int selectedNavigation=2;
    private ValueCallback<Uri[]> fileCallback;
    private View fullScreenVideo;
    private WebChromeClient.CustomViewCallback videoCallback;

    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable card(int fill,int stroke,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
    }
    private GradientDrawable premiumHero(){
        GradientDrawable d=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff314874,0xff20345b,0xff173c4c});
        d.setCornerRadius(dp(24));
        d.setStroke(dp(1),0xff6381ae);
        return d;
    }
    private View quickCardView(String title,String detail,int accent,Runnable action){
        LinearLayout tile=column();
        tile.setPadding(dp(11),dp(13),dp(10),dp(12));
        GradientDrawable bg=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff1e3b59,0xff122a3f});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),accent);
        tile.setBackground(bg);
        TextView name=text(title,13,WHITE,true);
        name.setSingleLine(true);
        add(tile,name,0);
        TextView subtitle=text(detail,11,MUTED,false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        add(tile,subtitle,5);
        tile.setOnClickListener(v->action.run());
        tile.setContentDescription(title+". "+detail);
        return tile;
    }
    private void sectionHeading(LinearLayout host,String title,int accent){
        LinearLayout header=row();
        View mark=new View(this);
        mark.setBackground(card(accent,0,5));
        header.addView(mark,new LinearLayout.LayoutParams(dp(4),dp(21)));
        TextView label=text(title,12,accent,true);
        label.setPadding(dp(10),0,dp(7),0);
        header.addView(label,new LinearLayout.LayoutParams(-2,dp(30)));
        View rule=new View(this);rule.setBackgroundColor(0xff284565);
        LinearLayout.LayoutParams ruleP=new LinearLayout.LayoutParams(0,dp(1),1);
        ruleP.leftMargin=dp(8);header.addView(rule,ruleP);
        add(host,header,21);
    }
    private TextView text(String label,int size,int color,boolean medium){
        TextView v=new TextView(this);
        v.setText(label);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    private void add(LinearLayout container,View view,int topMargin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(topMargin);container.addView(view,p);
    }
    private TextView pill(String title,Runnable onClick){
        TextView t=text(title,14,WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(card(PANEL,EDGE,17));
        t.setPadding(dp(12),dp(13),dp(12),dp(13));
        t.setOnClickListener(v->onClick.run());
        return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff061c47);
        getWindow().setNavigationBarColor(0xff061c47);
        store=new BrowserStore(this);
        dictionary=new DictionaryRepository(this);
        if(GovtAlerts.enabled(this))GovtAlerts.schedule(this);
        tts=new TextToSpeech(this,result->{
            if(result==TextToSpeech.SUCCESS&&tts!=null)tts.setLanguage(Locale.US);
        });
        buildShell();
        Intent intent=getIntent();
        if(intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)
            openAddress(intent.getData().toString());
        else showHome();
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        if(intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)
            openAddress(intent.getData().toString());
    }
    private static final int HOME_INK=0xff0e2450, HOME_MUTED=0xff4d6486,
        HOME_BLUE=0xff1269e9, HOME_BORDER=0xffa7d1ff;
    private GradientDrawable gradient(int first,int second,int border,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{first,second});
        d.setCornerRadius(dp(radius));
        if(border!=0)d.setStroke(dp(1),border);
        return d;
    }
    private TextView homeButton(String title,int background,int border,int fg,Runnable click){
        TextView t=text(title,14,fg,true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10),dp(12),dp(10),dp(12));
        t.setBackground(card(background,border,17));
        t.setOnClickListener(v->click.run());
        t.setContentDescription(title);
        return t;
    }
    private void homeSection(LinearLayout host,String icon,String title){
        LinearLayout bar=row();
        TextView label=text(icon+"  "+title,19,HOME_INK,true);
        bar.addView(label,new LinearLayout.LayoutParams(-2,dp(43)));
        View line=new View(this);line.setBackgroundColor(0xff8bbaff);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(1),1);
        p.leftMargin=dp(10);bar.addView(line,p);
        add(host,bar,12);
    }
    private View homeFeature(String icon,String title,int start,int finish,int border,
        Runnable action){
        LinearLayout tile=column();tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5),dp(9),dp(5),dp(9));
        tile.setBackground(gradient(start,finish,border,19));
        TextView image=text(icon,29,HOME_BLUE,true);image.setGravity(Gravity.CENTER);
        tile.addView(image,new LinearLayout.LayoutParams(-1,dp(39)));
        TextView titleView=text(title,12,HOME_INK,true);
        titleView.setGravity(Gravity.CENTER);
        titleView.setMaxLines(2);
        tile.addView(titleView,new LinearLayout.LayoutParams(-1,dp(37)));
        tile.setOnClickListener(v->action.run());
        tile.setContentDescription(title);
        return tile;
    }
    private void homeBanner(LinearLayout host,String icon,String heading,String subtitle,
        int left,int right,int border,Runnable action){
        LinearLayout banner=row();
        banner.setPadding(dp(15),dp(13),dp(13),dp(13));
        banner.setBackground(gradient(left,right,border,20));
        TextView symbol=text(icon,29,HOME_BLUE,true);
        symbol.setGravity(Gravity.CENTER);
        banner.addView(symbol,new LinearLayout.LayoutParams(dp(47),dp(52)));
        LinearLayout textBox=column();textBox.setPadding(dp(9),0,0,0);
        add(textBox,text(heading,17,HOME_INK,true),0);
        add(textBox,text(subtitle,11,HOME_MUTED,false),5);
        banner.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text("❯",23,HOME_BLUE,true);arrow.setGravity(Gravity.CENTER);
        banner.addView(arrow,new LinearLayout.LayoutParams(dp(31),dp(48)));
        banner.setOnClickListener(v->action.run());
        add(host,banner,11);
    }
    private void requestVoiceSearch(){
        try{
            Intent request=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            request.putExtra(RecognizerIntent.EXTRA_PROMPT,"Search the web");
            startActivityForResult(request,VOICE_SEARCH);
        }catch(ActivityNotFoundException error){
            toast("Voice recognition is unavailable on this device");
        }
    }
    private void openQrScanner(){
        // Opens an installed compatible scanner only; no fake built-in scanner.
        Intent scan=new Intent("com.google.zxing.client.android.SCAN");
        scan.putExtra("SCAN_MODE","QR_CODE_MODE");
        try{startActivityForResult(scan,QR_SCAN);}
        catch(ActivityNotFoundException e){
            toast("A compatible QR scanner app is required");
        }
    }
    private void showAccountInfo(){
        new AlertDialog.Builder(this).setTitle("S Browser • Local profile")
            .setMessage("No account required. Bookmarks, notes and history are stored on this device.")
            .setPositiveButton("OK",null).show();
    }
    private void buildShell(){
        root=column();
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xffeaf6ff,0xfffafdff,0xffd9ecff}));
        root.setPadding(0,0,0,dp(7));
        setContentView(root);
        top=column();top.setPadding(dp(12),dp(9),dp(12),dp(2));
        top.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xfff7fbff,0xffd5edff}));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout header=row();
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.rainbow_s_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout branding=column();
        TextView name=text("S Browser  ✓",19,HOME_INK,true);
        name.setSingleLine(true);
        branding.addView(name);
        TextView byline=text("A Saurabh Vision Product",11,HOME_MUTED,false);
        branding.addView(byline);
        LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,-2,1);
        n.leftMargin=dp(11);header.addView(branding,n);
        focusBadge=text("🎓 Student",10,HOME_INK,true);
        focusBadge.setBackground(gradient(0xffffe9a3,0xffffd16f,0xffaa851c,13));
        focusBadge.setPadding(dp(9),dp(9),dp(9),dp(9));
        header.addView(focusBadge);
        focusBadge.setOnClickListener(v->toggleFocus());
        TextView menu=text("⋮",27,HOME_INK,true);
        menu.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(42),dp(42));
        mp.leftMargin=dp(5);header.addView(menu,mp);
        menu.setOnClickListener(v->showTools());
        add(top,header,0);
        // Header actions need real minimum height: prior 36dp clips text.
        LinearLayout headerActions=row();
        TextView gift=homeButton("🎁 Daily",0xffedf7ff,HOME_BORDER,HOME_INK,
            this::showDailyAssembly);
        TextView notices=homeButton("◉ News",0xffedf7ff,HOME_BORDER,HOME_INK,
            this::showCityNews);
        TextView profile=homeButton("♙ Profile",0xffedf7ff,HOME_BORDER,HOME_INK,
            this::showAccountInfo);
        TextView[] headerButtons={gift,notices,profile};
        for(int i=0;i<headerButtons.length;i++){
            headerButtons[i].setTextSize(13);
            headerButtons[i].setSingleLine(true);
            headerButtons[i].setPadding(dp(5),0,dp(5),0);
            headerButtons[i].setMinHeight(dp(49));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(49),1);
            if(i<2)p.rightMargin=dp(7);
            headerActions.addView(headerButtons[i],p);
        }
        add(top,headerActions,5);

        LinearLayout search=row();
        search.setBackground(card(0xffffffff,0xff4f93ec,24));
        search.setElevation(dp(3));
        search.setPadding(dp(10),dp(4),dp(7),dp(4));
        TextView lock=text("G",25,0xff2384e9,true);lock.setGravity(Gravity.CENTER);
        search.addView(lock,new LinearLayout.LayoutParams(dp(37),dp(40)));
        address=new AutoCompleteTextView(this);
        address.setThreshold(1);
        ArrayList<String> suggestions=new ArrayList<>();
        for(BrowserStore.Site site:store.list("history")){
            if(site.url!=null&&suggestions.size()<50)suggestions.add(site.url);
        }
        address.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_dropdown_item_1line,suggestions));
        address.setDropDownBackgroundDrawable(card(0xffffffff,HOME_BORDER,12));
        address.setOnItemClickListener((parent,view,position,id)->
            openAddress(String.valueOf(parent.getItemAtPosition(position))));
        address.setSingleLine(true);
        address.setTextSize(15);address.setTextColor(HOME_INK);
        address.setHintTextColor(HOME_MUTED);
        address.setHint("Search or enter a website");
        address.setSelectAllOnFocus(true);
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.addView(address,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView voice=text("🎙",19,HOME_INK,true);
        voice.setGravity(Gravity.CENTER);
        voice.setContentDescription("Voice search");
        voice.setOnClickListener(v->requestVoiceSearch());
        search.addView(voice,new LinearLayout.LayoutParams(dp(38),dp(43)));
        TextView qr=text("▦",23,HOME_BLUE,true);
        qr.setGravity(Gravity.CENTER);
        qr.setContentDescription("QR scanner (requires installed scanner)");
        qr.setOnClickListener(v->openQrScanner());
        search.addView(qr,new LinearLayout.LayoutParams(dp(34),dp(43)));
        address.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_SEARCH){
                openAddress(address.getText().toString());return true;
            }return false;
        });
        TextView go=text("⌕",26,0xffffffff,true);go.setGravity(Gravity.CENTER);
        search.addView(go,new LinearLayout.LayoutParams(dp(48),dp(42)));
        go.setOnClickListener(v->openAddress(address.getText().toString()));
        go.setBackground(card(0xff174eab,0xff397df0,15));
        add(top,search,7);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgress(0);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressP=new LinearLayout.LayoutParams(-1,dp(3));
        progressP.topMargin=dp(3);top.addView(progress,progressP);
        tabStrip=new HorizontalScrollView(this);
        tabStrip.setHorizontalScrollBarEnabled(false);
        tabStrip.setFillViewport(false);
        tabStrip.setSmoothScrollingEnabled(true);
        tabStrip.setOverScrollMode(View.OVER_SCROLL_NEVER);
        tabStrip.setHorizontalFadingEdgeEnabled(false);
        tabLine=row();tabLine.setPadding(dp(1),dp(5),dp(2),dp(6));
        tabStrip.addView(tabLine,new HorizontalScrollView.LayoutParams(-2,-2));
        LinearLayout.LayoutParams tabsP=new LinearLayout.LayoutParams(-1,dp(59));
        tabsP.topMargin=dp(2);top.addView(tabStrip,tabsP);
        contentFrame=new FrameLayout(this);
        root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));

        // Adaptive, thumb-friendly navigation with a single highlighted action.
        HorizontalScrollView bottomScroll=new HorizontalScrollView(this);
        bottomScroll.setHorizontalScrollBarEnabled(false);
        bottomScroll.setFillViewport(true);
        bottomScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        bottomScroll.setBackground(gradient(0xff0a2f60,0xff061c47,0xff3971ba,24));
        bottomScroll.setElevation(dp(6));
        bottom=row();
        bottom.setPadding(dp(7),dp(7),dp(7),dp(7));
        String[] icons={"‹","›","⌂","▣","Aa","⚑","▤","☷"};
        String[] labels={"Back","Forward","Home","Tabs","Words","Jobs","News","More"};
        Runnable[] bottomActions={this::back,this::forward,this::showHome,
            this::showTabs,()->showDictionary(""),this::showJobsHub,
            this::showCityNews,this::showTools};
        navigationCells=new LinearLayout[labels.length];
        navigationIcons=new TextView[labels.length];
        navigationLabels=new TextView[labels.length];
        int widthDp=(int)(getResources().getDisplayMetrics().widthPixels/
            getResources().getDisplayMetrics().density);
        int itemWidth=Math.max(62,Math.min(92,(widthDp-22)/8));
        for(int i=0;i<labels.length;i++){
            final int index=i;
            LinearLayout cell=column();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2),dp(4),dp(2),dp(4));
            cell.setMinimumHeight(dp(58));
            TextView icon=text(icons[i],25,0xffeaf4ff,true);
            icon.setGravity(Gravity.CENTER);
            cell.addView(icon,new LinearLayout.LayoutParams(-1,dp(30)));
            TextView label=text(labels[i],12,0xffe1edff,true);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            cell.addView(label,new LinearLayout.LayoutParams(-1,dp(20)));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(itemWidth),dp(62));
            p.rightMargin=dp(2);
            bottom.addView(cell,p);
            cell.setContentDescription(labels[i]);
            cell.setClickable(true);
            cell.setForeground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x44ffffff),null,
                card(0xffffffff,0,17)));
            cell.setOnClickListener(v->{
                selectedNavigation=index;
                updateBottomNavigation();
                bottomActions[index].run();
            });
            navigationCells[i]=cell;
            navigationIcons[i]=icon;
            navigationLabels[i]=label;
            if(i==3)tabBadge=label;
        }
        bottomScroll.addView(bottom,new HorizontalScrollView.LayoutParams(-2,-2));
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(-1,dp(86));
        b.setMargins(dp(8),dp(3),dp(8),dp(8));
        root.addView(bottomScroll,b);
        updateBottomNavigation();
        refreshTabBadge();
    }
    private void updateBottomNavigation(){
        if(navigationCells==null)return;
        for(int i=0;i<navigationCells.length;i++){
            boolean active=i==selectedNavigation;
            navigationCells[i].setBackground(active?
                gradient(0xff178fff,0xff0057cb,0xff69cfff,17):
                card(0x00000000,0,17));
            navigationIcons[i].setTextColor(active?0xffffffff:0xffd3e7ff);
            navigationLabels[i].setTextColor(active?0xffffffff:0xffd3e7ff);
            navigationCells[i].setElevation(active?dp(2):0);
            navigationCells[i].setSelected(active);
        }
    }
    private void refreshTabBadge(){
        if(tabBadge!=null)tabBadge.setText("Tabs "+tabs.size());
        if(focusBadge!=null)focusBadge.setText(store.focusMode()?"★ FOCUS ON":"🎓 Student");
        if(focusBadge!=null)focusBadge.setTextColor(HOME_INK);
        refreshTabStrip();
    }
    private void addQuickTab(String label,boolean active,Runnable action){
        TextView chip=text(label,13,active?0xffffffff:HOME_INK,true);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(dp(14),0,dp(14),0);
        chip.setBackground(gradient(active?0xff167eff:0xfff9fcff,
            active?0xff0055d4:0xffe7f4ff,active?HOME_BLUE:HOME_BORDER,16));
        chip.setContentDescription(label);
        chip.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(46));
        p.rightMargin=dp(8);tabLine.addView(chip,p);
    }
    private void refreshTabStrip(){
        if(tabLine==null)return;
        tabLine.removeAllViews();
        addQuickTab("⌂ Home",home&&!dashboardOpen,this::showHome);
        addQuickTab("◉ Akashvani News",false,
            ()->openAddress("https://www.newsonair.gov.in/"));
        addQuickTab("▤ Study Dashboard",home&&dashboardOpen,
            this::showStudentDashboard);
        addQuickTab("▤ Study Portal",false,
            ()->openAddress("https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/vision-app/"));
        addQuickTab("＋ New Tab",false,()->{
            if(createTab()!=null)showHome();
        });
        // Open web tabs stay accessible after the four pinned shortcuts.
        for(Tab tab:tabs){
            boolean active=!home&&selected>=0&&selected<tabs.size()&&
                tabs.get(selected)==tab;
            LinearLayout chip=row();
            chip.setBackground(card(active?0xffc4e5ff:0xfff9fcff,
                active?HOME_BLUE:HOME_BORDER,14));
            String title=tab.title;
            if(title==null||title.isEmpty()||"New Tab".equals(title)){
                try{
                    String url=tab.web.getUrl();
                    String host=url==null?null:Uri.parse(url).getHost();
                    title=host==null?"New Tab":host;
                }catch(Exception ignored){title="New Tab";}
            }
            TextView titleView=text(title,13,HOME_INK,true);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setMaxWidth(dp(148));
            titleView.setPadding(dp(11),0,dp(5),0);
            chip.addView(titleView,new LinearLayout.LayoutParams(-2,dp(46)));
            TextView close=text("×",21,HOME_BLUE,true);
            close.setGravity(Gravity.CENTER);
            close.setContentDescription("Close "+title);
            chip.addView(close,new LinearLayout.LayoutParams(dp(36),dp(46)));
            close.setOnClickListener(v->closeTab(tab));
            chip.setOnClickListener(v->{
                int index=tabs.indexOf(tab);
                if(index>=0){selected=index;attachCurrent();}
            });
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(46));
            p.rightMargin=dp(8);tabLine.addView(chip,p);
            if(active){
                // Keep the selected tab visible without forcing repeated scroll animations.
                tabStrip.post(()->{
                    int left=chip.getLeft(),right=chip.getRight();
                    int scroll=tabStrip.getScrollX(),width=tabStrip.getWidth();
                    if(left<scroll)tabStrip.smoothScrollTo(Math.max(0,left-dp(12)),0);
                    else if(right>scroll+width)
                        tabStrip.smoothScrollTo(right-width+dp(12),0);
                });
            }
        }
    }
    private void showHome(){
        selectedNavigation=2;
        updateBottomNavigation();
        dashboardOpen=false;
        home=true;
        if(contentFrame==null)return;
        if(top!=null)top.setVisibility(View.GONE);
        for(Tab tab:tabs)tab.web.onPause();
        contentFrame.removeAllViews();
        address.setText("");
        address.dismissDropDown();
        progress.setVisibility(View.GONE);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xffffffff);
        contentFrame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout layout=column();
        layout.setPadding(dp(20),dp(12),dp(20),dp(30));
        scroll.addView(layout);
        // Inspired by a clean search engine landing page, with S Browser identity.
        LinearLayout links=row();
        TextView all=homeButton("ALL",0xffffffff,0,HOME_BLUE,this::showHome);
        TextView images=homeButton("IMAGES",0xffffffff,0,HOME_MUTED,
            ()->openAddress("https://www.google.com/imghp?hl=en"));
        links.addView(all,new LinearLayout.LayoutParams(dp(68),dp(48)));
        links.addView(images,new LinearLayout.LayoutParams(dp(94),dp(48)));
        View spacer=new View(this);
        links.addView(spacer,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView daily=homeButton("♧",0xffffffff,0,HOME_INK,this::showDailyAssembly);
        daily.setContentDescription("Daily assembly");
        links.addView(daily,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView features=homeButton("▦",0xffffffff,0,HOME_INK,
            this::showStudentDashboard);
        features.setContentDescription("Study Dashboard");
        links.addView(features,new LinearLayout.LayoutParams(dp(48),dp(48)));
        add(layout,links,0);
        View underline=new View(this);
        underline.setBackgroundColor(HOME_BLUE);
        LinearLayout.LayoutParams ul=new LinearLayout.LayoutParams(dp(49),dp(2));
        layout.addView(underline,ul);

        // Top margin scales gently by screen height. Do not push input off
        // small phones or leave the keyboard obscuring important controls.
        int screenDp=(int)(getResources().getDisplayMetrics().heightPixels/
            getResources().getDisplayMetrics().density);
        int heroGap=Math.max(46,Math.min(145,screenDp/8));
        View gap=new View(this);
        layout.addView(gap,new LinearLayout.LayoutParams(-1,dp(heroGap)));
        ImageView rainbowLogo=new ImageView(this);
        rainbowLogo.setImageResource(R.drawable.rainbow_s_icon);
        rainbowLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        rainbowLogo.setContentDescription("Rainbow S Browser logo");
        LinearLayout.LayoutParams rainbowParams=new LinearLayout.LayoutParams(
            dp(98),dp(98));
        rainbowParams.gravity=Gravity.CENTER_HORIZONTAL;
        layout.addView(rainbowLogo,rainbowParams);
        TextView brand=text("Browser",43,HOME_INK,true);
        brand.setGravity(Gravity.CENTER);
        brand.setContentDescription("S Browser search home");
        add(layout,brand,0);
        TextView byline=text("Explore • Study • Learn • Grow",12,HOME_MUTED,false);
        byline.setGravity(Gravity.CENTER);
        add(layout,byline,2);

        LinearLayout box=row();
        box.setBackground(card(0xffffffff,0xffdce2ea,30));
        box.setElevation(dp(2));
        box.setPadding(dp(12),0,dp(8),0);
        TextView searchIcon=text("⌕",27,0xff84909f,false);
        searchIcon.setGravity(Gravity.CENTER);
        box.addView(searchIcon,new LinearLayout.LayoutParams(dp(35),dp(54)));
        EditText query=new EditText(this);
        query.setSingleLine(true);
        query.setTextSize(16);
        query.setTextColor(HOME_INK);
        query.setHintTextColor(0xff82909f);
        query.setHint("Search or enter a website");
        query.setBackgroundColor(Color.TRANSPARENT);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setInputType(InputType.TYPE_CLASS_TEXT);
        box.addView(query,new LinearLayout.LayoutParams(0,dp(54),1));
        query.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_SEARCH||action==EditorInfo.IME_ACTION_GO){
                String value=query.getText().toString().trim();
                if(!value.isEmpty())openAddress(value);
                return true;
            }
            return false;
        });
        TextView mic=text("🎙",20,HOME_MUTED,true);
        mic.setGravity(Gravity.CENTER);
        mic.setContentDescription("Voice search");
        mic.setOnClickListener(v->requestVoiceSearch());
        box.addView(mic,new LinearLayout.LayoutParams(dp(40),dp(54)));
        TextView scan=text("▦",23,HOME_MUTED,true);
        scan.setGravity(Gravity.CENTER);
        scan.setContentDescription("Scan QR code with installed scanner");
        scan.setOnClickListener(v->openQrScanner());
        box.addView(scan,new LinearLayout.LayoutParams(dp(39),dp(54)));
        add(layout,box,25);

        TextView study=text("▦  Open Student Dashboard",14,HOME_BLUE,true);
        study.setGravity(Gravity.CENTER);
        study.setPadding(dp(8),dp(13),dp(8),dp(13));
        study.setOnClickListener(v->showStudentDashboard());
        add(layout,study,17);

        List<BrowserStore.Site> history=store.list("history");
        TextView heading=text(history.isEmpty()?"START EXPLORING":"RECENTLY VISITED",
            18,HOME_INK,true);
        add(layout,heading,28);
        if(history.isEmpty()){
            addSearchSuggestion(layout,"News and current affairs","latest India news");
            addSearchSuggestion(layout,"Study resources","educational resources");
            addSearchSuggestion(layout,"Explore Saurabh Vision","https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/vision-app/");
            addSearchSuggestion(layout,"Student dashboard",null);
        }else{
            int count=0;
            for(BrowserStore.Site item:history){
                if(count++>=7)break;
                String title=item.title==null||item.title.trim().isEmpty()?
                    item.url:item.title;
                addSearchSuggestion(layout,title,item.url);
            }
        }
        refreshTabBadge();
    }
    private void addSearchSuggestion(LinearLayout host,String title,String target){
        LinearLayout line=row();
        line.setPadding(dp(10),dp(13),dp(10),dp(13));
        TextView icon=text("⌕",23,HOME_MUTED,false);
        line.addView(icon,new LinearLayout.LayoutParams(dp(37),dp(37)));
        TextView label=text(title,15,HOME_INK,false);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        line.addView(label,new LinearLayout.LayoutParams(0,dp(37),1));
        line.setOnClickListener(v->{
            if(target==null)showStudentDashboard();
            else openAddress(target);
        });
        add(host,line,1);
        View divider=new View(this);
        divider.setBackgroundColor(0xffe7e9ef);
        host.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void showStudentDashboard(){
        selectedNavigation=2;
        updateBottomNavigation();
        dashboardOpen=true;
        home=true;
        if(top!=null)top.setVisibility(View.VISIBLE);
        if(contentFrame==null)return;
        for(Tab tab:tabs)tab.web.onPause();
        contentFrame.removeAllViews();
        address.setText("");
        address.dismissDropDown();
        progress.setVisibility(View.GONE);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff6fbff);
        contentFrame.addView(scroll);
        LinearLayout layout=column();
        layout.setPadding(dp(14),dp(13),dp(14),dp(25));
        scroll.addView(layout);

        LinearLayout hero=column();
        hero.setPadding(dp(18),dp(17),dp(18),dp(18));
        hero.setBackground(gradient(0xffd7efff,0xff71b7f9,0xff5a9dfe,23));
        hero.setElevation(dp(3));
        add(hero,text("🎓  SMART BROWSING  •  STUDENT EDITION",
            12,HOME_INK,true),0);
        add(hero,text("Explore • Study • Learn • Grow",25,HOME_INK,true),9);
        add(hero,text("A fast, student-friendly browser with study tools, " +
            "daily updates and learning resources — all in one place.",
            13,HOME_INK,false),9);
        LinearLayout heroActions=row();
        TextView start=homeButton("🎓 Start Studying  →",
            0xff065de6,0xff3a8fff,0xffffffff,()->
                openAddress("https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/vision-app/"));
        heroActions.addView(start,new LinearLayout.LayoutParams(0,dp(51),1));
        TextView features=homeButton("▦ Explore",0xffffffff,0xff3989ec,HOME_BLUE,
            this::showDailyAssembly);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(51),1);
        ep.leftMargin=dp(9);heroActions.addView(features,ep);
        add(hero,heroActions,17);
        add(layout,hero,0);

        // TODAY'S NEWS DESK; no fabricated live alerts.
        // Scrollable collection: never squeeze nine shortcuts into one phone row.
        HorizontalScrollView rail=new HorizontalScrollView(this);
        rail.setHorizontalScrollBarEnabled(false);
        rail.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout newsRail=row();
        newsRail.setPadding(dp(1),dp(6),dp(1),dp(5));
        String[] quickIcons={"▤","🏆","◉","💡","🎙","✧","▦","🎮","＋"};
        String[] quickLabels={"General News","Sports News","Breaking News",
            "Daily Thought","Speech","Astrology","Crossword","Puzzle Games","More"};
        Runnable[] quickActions={
            ()->openAssemblySection(4),()->openAssemblySection(3),
            ()->openAssemblySection(4),()->openAssemblySection(1),
            ()->openAssemblySection(2),()->openAssemblySection(5),
            ()->openAssemblySection(6),()->openAssemblySection(7),
            this::showDailyAssembly
        };
        for(int i=0;i<quickIcons.length;i++){
            final int index=i;
            int tint=i%3==0?0xffe1efff:i%3==1?0xffe7f9f5:0xffffeff3;
            View tile=homeFeature(quickIcons[i],quickLabels[i],0xffffffff,tint,
                HOME_BORDER,quickActions[index]);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(100),dp(94));
            p.rightMargin=dp(7);newsRail.addView(tile,p);
        }
        rail.addView(newsRail,new HorizontalScrollView.LayoutParams(-2,-2));
        add(layout,rail,14);

        homeSection(layout,"🎓","Study Tools");
        HorizontalScrollView toolsScroll=new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools=row();
        View[] cards={
            homeFeature("📖","Dictionary",0xfff8fcff,0xffcfebff,0xff9fcbff,
                ()->showDictionary("")),
            homeFeature("📝","Quick Notes",0xfff8ffff,0xffc9f4f0,0xff99ded7,
                this::editNote),
            homeFeature("🎯","Study Focus",0xfffff9f1,0xffffdfc8,0xffffbe94,
                this::toggleFocus),
            homeFeature("⭐","Bookmarks",0xfffaf5ff,0xffe4d6ff,0xffc7adfa,
                this::showBookmarks),
            homeFeature("⬇","Downloads",0xfffff4fa,0xffffdbef,0xffeeacd2,
                this::openDownloads)
        };
        for(int i=0;i<cards.length;i++){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(106),dp(104));
            if(i!=cards.length-1)p.rightMargin=dp(7);
            tools.addView(cards[i],p);
        }
        toolsScroll.addView(tools,new HorizontalScrollView.LayoutParams(-2,-2));
        add(layout,toolsScroll,6);

        homeBanner(layout,"▣","CG Jobs & Exam Notices",
            "Official jobs • Admit cards • Results",
            0xffecfff6,0xffc9f2e8,0xff81dbc7,this::showJobsHub);
        homeBanner(layout,"◉","Latest Chhattisgarh City News",
            "City-wise updates • Sources • Published dates",
            0xffe9f5ff,0xffc7e5ff,0xff89baf9,this::showCityNews);

        homeSection(layout,"📖","Saurabh Vision Learning");
        LinearLayout learning=column();
        learning.setPadding(dp(17),dp(15),dp(17),dp(16));
        learning.setBackground(gradient(0xff5622c7,0xffac43e4,0xffe7a5ff,22));
        TextView learningTitle=text("✦  Saurabh Vision Learning App  ✓",
            19,0xffffffff,true);
        add(learning,learningTitle,0);
        add(learning,text("Study • Quizzes • Practice • PDFs • Daily Updates",
            12,0xfff5eaff,false),7);
        TextView open=homeButton("Open Learning App  ↗",
            0xffffffff,0xffd0b2fb,0xff5e22b9,()->
                openAddress("https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/vision-app/"));
        add(learning,open,12);
        add(layout,learning,6);
        LinearLayout utility=row();
        TextView news=homeButton("▤ Latest News",0xffffffff,HOME_BORDER,HOME_BLUE,
            ()->openAssemblySection(4));
        utility.addView(news,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView games=homeButton("🎮 Games",0xffffffff,HOME_BORDER,HOME_BLUE,
            ()->openAssemblySection(7));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(48),1);
        gp.leftMargin=dp(8);utility.addView(games,gp);
        add(layout,utility,13);
        LinearLayout benefits=row();
        String[] benefitLabels={"⚡ Fast","🛡 Private","📚 All-in-One","💎 Student"};
        for(int i=0;i<benefitLabels.length;i++){
            TextView v=homeButton(benefitLabels[i],0xffedf7ff,HOME_BORDER,
                HOME_INK,this::showAccountInfo);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(47),1);
            if(i>0)bp.leftMargin=dp(4);
            benefits.addView(v,bp);
        }
        add(layout,benefits,12);
        List<String> last=store.session();
        if(!last.isEmpty()){
            TextView restore=homeButton("↶ Restore "+last.size()+" previous tabs",
                0xffe3f2ff,HOME_BORDER,HOME_BLUE,()->restoreTabs(last));
            add(layout,restore,13);
        }
        add(layout,text("S Browser  •  SAURABH'S VISION™  •  Student Edition",
            11,HOME_MUTED,false),22);
        refreshTabBadge();
    }
    private void restoreTabs(List<String> urls){
        store.saveSession(new ArrayList<>());
        for(String url:urls){
            if(tabs.size()>=MAX_TABS)break;
            Tab tab=createTab();
            tab.web.loadUrl(url);
        }
        if(!tabs.isEmpty()){selected=tabs.size()-1;attachCurrent();}
    }
    private boolean focusBlocked(Uri uri){
        if(!store.focusMode())return false;
        String host=uri.getHost();
        if(host==null)return false;
        String h=host.toLowerCase(Locale.ROOT);
        for(String domain:FOCUS_DOMAINS)
            if(h.equals(domain)||h.endsWith("."+domain))return true;
        return false;
    }
    private void openAddress(String input){
        String url=AddressResolver.resolve(input,store.searchEngine());
        if(url==null){toast("Enter a valid website or search. Unsafe links are blocked.");return;}
        Uri uri=Uri.parse(url);
        if(focusBlocked(uri)){toast("Study Focus is on. Turn it off to open this site.");return;}
        if(selected<0||selected>=tabs.size()){
            if(createTab()==null)return;
        }
        Tab current=tabs.get(selected);
        // Reuse the selected WebView instead of recreating it for each search.
        attachCurrent();
        current.web.loadUrl(url);
        address.clearFocus();
        ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(),0);
    }
    private Tab createTab(){
        if(tabs.size()>=MAX_TABS){toast("Maximum 8 tabs. Close one to add another.");return null;}
        Tab tab=new Tab();
        tab.web=new WebView(this);
        tab.mobileUserAgent=tab.web.getSettings().getUserAgentString();
        configureWebView(tab);
        tabs.add(tab);selected=tabs.size()-1;
        refreshTabBadge();
        return tab;
    }
    private void attachCurrent(){
        if(selected<0||selected>=tabs.size()){showHome();return;}
        // Web tabs use a neutral toolbar state; Home is highlighted only on Home.
        selectedNavigation=-1;
        updateBottomNavigation();
        dashboardOpen=false;
        home=false;
        if(top!=null)top.setVisibility(View.VISIBLE);
        WebView web=tabs.get(selected).web;
        for(Tab tab:tabs)if(tab.web!=web)tab.web.onPause();
        if(web.getParent()!=contentFrame){
            contentFrame.removeAllViews();
            if(web.getParent() instanceof ViewGroup)
                ((ViewGroup)web.getParent()).removeView(web);
            contentFrame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        }
        web.onResume();
        String url=web.getUrl();
        address.setText(url!=null&&url.startsWith("http")?url:"");
        refreshTabBadge();
    }
    private void configureWebView(Tab tab){
        WebView web=tab.web;
        web.setBackgroundColor(WHITE);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
                Uri uri=req.getUrl();
                String scheme=uri.getScheme();
                if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme)){
                    toast("Only web pages can open in S Browser");return true;
                }
                if(focusBlocked(uri)){
                    toast("Site blocked while Study Focus is enabled");return true;
                }
                return false;
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();toast("Unsafe certificate: connection blocked");
            }
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){
                if(selected>=0&&selected<tabs.size()&&tabs.get(selected)==tab&&!home){
                    address.setText(url);progress.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPageFinished(WebView view,String url){
                tab.title=view.getTitle()==null||view.getTitle().isEmpty()?url:view.getTitle();
                if(url!=null)store.history(url,tab.title);
                refreshTabStrip();
                if(selected>=0&&selected<tabs.size()&&tabs.get(selected)==tab&&!home){
                    address.setText(url);progress.setVisibility(View.GONE);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){
                if(!home&&selected>=0&&selected<tabs.size()&&tabs.get(selected)==tab){
                    progress.setProgress(p);
                    progress.setVisibility(p>=100?View.GONE:View.VISIBLE);
                }
            }
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,
                FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=callback;
                try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
                catch(Exception e){fileCallback=null;toast("File picker unavailable");return false;}
            }
            @Override public void onShowCustomView(View view,CustomViewCallback callback){
                if(fullScreenVideo!=null){callback.onCustomViewHidden();return;}
                fullScreenVideo=view;videoCallback=callback;
                ((ViewGroup)root.getParent()).addView(view,
                    new ViewGroup.LayoutParams(-1,-1));
                top.setVisibility(View.GONE);bottom.setVisibility(View.GONE);
                root.setVisibility(View.INVISIBLE);
            }
            @Override public void onHideCustomView(){hideVideo();}
        });
        web.setDownloadListener((url,userAgent,disposition,mimeType,length)->
            downloadFile(url,mimeType,disposition));
    }
    private void hideVideo(){
        if(fullScreenVideo==null)return;
        if(fullScreenVideo.getParent() instanceof ViewGroup)
            ((ViewGroup)fullScreenVideo.getParent()).removeView(fullScreenVideo);
        fullScreenVideo=null;
        root.setVisibility(View.VISIBLE);top.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
        if(videoCallback!=null){videoCallback.onCustomViewHidden();videoCallback=null;}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        // Always complete a pending WebView file chooser, even on cancellation.
        if(request==PICK_FILE){
            if(fileCallback!=null){
                Uri[] files=WebChromeClient.FileChooserParams.parseResult(result,data);
                fileCallback.onReceiveValue(files);
                fileCallback=null;
            }
            return;
        }
        if(result!=RESULT_OK||data==null)return;
        if(request==VOICE_SEARCH){
            ArrayList<String> matches=data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
            if(matches!=null&&!matches.isEmpty()){
                address.setText(matches.get(0));
                openAddress(matches.get(0));
            }
        }else if(request==QR_SCAN){
            String scanned=data.getStringExtra("SCAN_RESULT");
            if(scanned!=null&&!scanned.trim().isEmpty())openAddress(scanned);
        }
    }
    private Tab current(){return selected>=0&&selected<tabs.size()?tabs.get(selected):null;}
    private void back(){
        if(fullScreenVideo!=null){hideVideo();return;}
        // On the start page Back must leave the app, not redraw Home forever.
        if(home){if(dashboardOpen)showHome();else finish();return;}
        Tab tab=current();
        if(tab!=null&&tab.web.canGoBack()){tab.web.goBack();return;}
        showHome();
    }
    private void forward(){
        Tab tab=current();
        if(!home&&tab!=null&&tab.web.canGoForward())tab.web.goForward();
        else if(home&&tab!=null)attachCurrent();
    }
    private void showTabs(){
        LinearLayout list=column();list.setPadding(dp(13),dp(8),dp(13),dp(8));
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Open tabs ("+tabs.size()+")")
            .setView(scroll).setPositiveButton("+ New Tab",(d,w)->{
                if(tabs.size()<MAX_TABS){createTab();showHome();}
                else toast("Close a tab first.");
            }).setNegativeButton("Done",null).create();
        if(tabs.isEmpty())add(list,text("No tabs open yet.",15,MUTED,false),10);
        for(int i=0;i<tabs.size();i++){
            final Tab tab=tabs.get(i);
            LinearLayout entry=row();
            TextView title=text((i+1)+". "+(tab.title==null?"New Tab":tab.title),
                14,WHITE,false);
            title.setSingleLine(true);
            entry.addView(title,new LinearLayout.LayoutParams(0,dp(59),1));
            title.setOnClickListener(v->{
                selected=tabs.indexOf(tab);dialog.dismiss();attachCurrent();
            });
            TextView close=pill("✕",()->{
                closeTab(tab);dialog.dismiss();showTabs();
            });
            entry.addView(close,new LinearLayout.LayoutParams(dp(49),dp(48)));
            add(list,entry,4);
        }
        dialog.show();
    }
    private void closeTab(Tab tab){
        int index=tabs.indexOf(tab);
        if(index<0)return;
        boolean current=!home&&index==selected;
        tabs.remove(index);
        if(tab.web.getParent() instanceof ViewGroup)
            ((ViewGroup)tab.web.getParent()).removeView(tab.web);
        tab.web.stopLoading();tab.web.destroy();
        if(tabs.isEmpty()){selected=-1;showHome();return;}
        if(index<selected)selected--;
        else if(index==selected)selected=Math.min(index,tabs.size()-1);
        if(current)attachCurrent();
        else refreshTabBadge();
    }
    private void downloadFile(String url,String mime,String disposition){
        if(url==null||!(url.startsWith("https://")||url.startsWith("http://"))){
            toast("Only web downloads are supported");return;
        }
        try{
            String filename=URLUtil.guessFileName(url,disposition,mime);
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);
            request.setMimeType(mime==null?"application/octet-stream":mime);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            if(Build.VERSION.SDK_INT>=29)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename);
            else request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,filename);
            ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            toast("Downloading: "+filename);
        }catch(Exception e){toast("Download could not start");}
    }
    private void showList(String title,List<BrowserStore.Site> items){
        if(items.isEmpty()){toast("Nothing saved yet");return;}
        String[] options=new String[items.size()];
        for(int i=0;i<items.size();i++)options[i]=items.get(i).title;
        new AlertDialog.Builder(this).setTitle(title).setItems(options,
            (d,i)->openAddress(items.get(i).url)).setNegativeButton("Close",null).show();
    }
    private void showJobsHub(){startActivity(new Intent(this,JobsHubActivity.class));}
    private void showCityNews(){startActivity(new Intent(this,CityNewsActivity.class));}
    private void showDailyAssembly(){
        openAssemblySection(0);
    }
    private void openAssemblySection(int section){
        Intent intent=new Intent(this,DailyAssemblyActivity.class);
        intent.putExtra("open_scene",section);
        startActivity(intent);
    }
    private void showBookmarks(){showList("Bookmarks",store.list("bookmarks"));}
    private void showHistory(){showList("Browsing history",store.list("history"));}
    private void editNote(){
        Tab tab=current();
        String url=tab!=null&&tab.web.getUrl()!=null?tab.web.getUrl():"s-browser-home";
        EditText editor=new EditText(this);
        editor.setText(store.note(url));editor.setTextSize(16);
        editor.setHint("Write your study notes here…");
        editor.setMinLines(5);
        editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        new AlertDialog.Builder(this).setTitle("Study note • Saved on device")
            .setView(editor).setNegativeButton("Cancel",null)
            .setPositiveButton("Save",(d,w)->{store.saveNote(url,editor.getText().toString());
                toast("Note saved locally");}).show();
    }
    private void openOnlineDictionary(String query){
        String word=query==null?"":query.trim();
        if(word.isEmpty()){toast("Enter a word to search online");return;}
        // Use a HTTPS reference dictionary rather than manufacturing definitions.
        String target="https://en.wiktionary.org/w/index.php?search="+
            Uri.encode(word)+"&title=Special%3ASearch";
        openAddress(target);
    }
    private void showDictionary(String initial){
        final LinearLayout body=column();
        body.setPadding(dp(14),dp(8),dp(14),dp(12));
        TextView caption=text("A–Z English / Hindi dictionary",17,WHITE,true);
        add(body,caption,0);
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("Type any English or Hindi word");
        search.setTextSize(16);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        add(body,search,5);

        HorizontalScrollView letters=new HorizontalScrollView(this);
        letters.setHorizontalScrollBarEnabled(false);
        LinearLayout alphabet=row();
        alphabet.setPadding(dp(1),dp(6),dp(1),dp(6));
        for(char c='A';c<='Z';c++){
            final String letter=String.valueOf(c);
            TextView chip=text(letter,14,WHITE,true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(card(0xff193e67,0xff4a83bd,12));
            chip.setOnClickListener(v->search.setText(letter));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(44),dp(44));
            lp.rightMargin=dp(5);
            alphabet.addView(chip,lp);
        }
        letters.addView(alphabet,new HorizontalScrollView.LayoutParams(-2,-2));
        add(body,letters,5);

        TextView summary=text(dictionary.size()+
            " curated offline words across A–Z • Online lookup available",
            12,MUTED,false);
        add(body,summary,3);
        ScrollView scroll=new ScrollView(this);
        final LinearLayout results=column();scroll.addView(results);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(310));
        sp.topMargin=dp(8);body.addView(scroll,sp);
        TextView online=text("🌐  Search word online  ↗",15,MINT,true);
        online.setGravity(Gravity.CENTER);
        online.setBackground(card(0xff153e51,0xff388978,15));
        online.setPadding(dp(10),dp(13),dp(10),dp(13));
        add(body,online,10);

        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Aa  S Browser Dictionary")
            .setView(body).setNegativeButton("Close",null).create();
        online.setOnClickListener(v->{
            String query=search.getText().toString().trim();
            if(query.isEmpty()){toast("Enter a word first");return;}
            dialog.dismiss();
            openOnlineDictionary(query);
        });
        Runnable render=()->{
            results.removeAllViews();
            String query=search.getText().toString();
            List<DictionaryEntry> matches=dictionary.search(query);
            if(matches.isEmpty()){
                add(results,text("Not in offline words. Use Search word online below.",
                    14,MUTED,false),10);
            }
            for(DictionaryEntry word:matches){
                LinearLayout item=column();
                item.setPadding(dp(11),dp(10),dp(11),dp(10));
                item.setBackground(card(0xff172943,0xff34567f,12));
                add(item,text(word.word+"   •   "+word.hindi,15,WHITE,true),0);
                add(item,text(word.meaning,12,MUTED,false),5);
                add(results,item,5);
                item.setOnClickListener(v->showWord(word));
            }
        };
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                render.run();
            }
            public void afterTextChanged(Editable e){}
        });
        search.setText(initial==null?"":initial);
        render.run();
        dialog.show();
    }
    private void showWord(DictionaryEntry entry){
        String body="Hindi: "+entry.hindi+"\n\nMeaning: "+entry.meaning+
            "\n\n"+entry.example;
        new AlertDialog.Builder(this).setTitle(entry.word).setMessage(body)
            .setPositiveButton("🔊 Pronounce",(d,w)->{
                if(tts!=null)tts.speak(entry.word,TextToSpeech.QUEUE_FLUSH,null,"dictionary");
            }).setNegativeButton("Done",null).show();
    }
    private void defineSelected(){
        Tab tab=current();
        if(tab==null||home){showDictionary("");return;}
        tab.web.evaluateJavascript("(function(){return String(window.getSelection()).slice(0,100)})()",value->{
            try{
                String chosen=new JSONArray("["+value+"]").getString(0).trim();
                showDictionary(chosen.split(" ")[0]);
            }catch(Exception ignored){showDictionary("");}
        });
    }
    private void showReadingMode(){
        Tab tab=current();
        if(tab==null||home){toast("Open an article first");return;}
        tab.web.evaluateJavascript(
            "(function(){return document.body ? document.body.innerText.slice(0,16000) : ''})()",value->{
            try{
                String article=new JSONArray("["+value+"]").getString(0);
                if(article.isEmpty()){toast("No readable text found");return;}
                ScrollView scroll=new ScrollView(this);
                TextView reading=text(article,17,WHITE,false);
                reading.setTextIsSelectable(true);reading.setPadding(dp(17),dp(17),dp(17),dp(18));
                reading.setBackgroundColor(0xff13203c);scroll.addView(reading);
                new AlertDialog.Builder(this).setTitle("Reading mode")
                    .setView(scroll).setPositiveButton("Close",null).show();
            }catch(Exception ignored){toast("Reading mode unavailable on this page");}
        });
    }
    private void toggleFocus(){
        boolean enabled=!store.focusMode();store.setFocusMode(enabled);
        refreshTabBadge();
        if(home)showHome();
        toast(enabled?"Study Focus on: distracting sites paused":"Study Focus off");
    }
    private void changeEngine(){
        String[] engines={"Google","DuckDuckGo","Bing"};
        String[] values={"google","duckduckgo","bing"};
        int current=0;
        for(int i=0;i<values.length;i++)if(values[i].equals(store.searchEngine()))current=i;
        new AlertDialog.Builder(this).setTitle("Default search engine")
            .setSingleChoiceItems(engines,current,(d,which)->{
                store.setSearchEngine(values[which]);d.dismiss();
                toast("Search engine: "+engines[which]);
            }).setNegativeButton("Cancel",null).show();
    }
    private void clearData(){
        new AlertDialog.Builder(this).setTitle("Clear browsing data?")
            .setMessage("This removes local history, website cookies and stored site data. Saved bookmarks and dictionary remain.")
            .setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{
                store.clearHistory();
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                WebStorage.getInstance().deleteAllData();
                for(Tab t:tabs)t.web.clearCache(true);
                toast("Local browsing data cleared");
            }).show();
    }
    private void openDownloads(){
        try{startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));}
        catch(Exception e){toast("Open your phone's Downloads app");}
    }
    private void showTools(){
        String[] choices={"☆  Bookmark this page","⌕  Find in page",
            "Aa  Define selected word","☷  Reading mode","✎  Study notes",
            "◉  Study Focus","▣  Bookmarks","◷  History","↓  Downloads",
            "⌕  Search engine","▤  Desktop / mobile site","↗  Share page",
            "⊘  Clear browsing data","ⓘ  About S Browser",
            "⚑  CG Jobs, Railway & Exams","▤  Latest CG City News",
            "→  Forward page","✦  Daily Assembly & Fun"};
        new AlertDialog.Builder(this).setTitle("S Browser tools").setItems(choices,(d,i)->{
            Tab tab=current();
            switch(i){
                case 0:
                    if(tab==null||home){toast("Open a website first");return;}
                    boolean on=store.toggleBookmark(tab.web.getUrl(),tab.title);
                    toast(on?"Bookmarked":"Bookmark removed");break;
                case 1:
                    if(tab==null||home){toast("Open a page first");return;}
                    EditText q=new EditText(this);q.setSingleLine(true);
                    new AlertDialog.Builder(this).setTitle("Find in page").setView(q)
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Find",(x,k)->tab.web.findAllAsync(q.getText().toString()))
                        .show();break;
                case 2:defineSelected();break;
                case 3:showReadingMode();break;
                case 4:editNote();break;
                case 5:toggleFocus();break;
                case 6:showBookmarks();break;
                case 7:showHistory();break;
                case 8:openDownloads();break;
                case 9:changeEngine();break;
                case 10:
                    if(tab==null||home){toast("Open a page first");return;}
                    tab.desktop=!tab.desktop;
                    tab.web.getSettings().setUserAgentString(tab.desktop?
                        tab.mobileUserAgent.replace("Mobile",""):
                        tab.mobileUserAgent);
                    tab.web.reload();toast(tab.desktop?"Desktop mode":"Mobile mode");break;
                case 11:
                    if(tab==null||home){toast("Open a page first");return;}
                    Intent share=new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,tab.web.getUrl());
                    startActivity(Intent.createChooser(share,"Share page"));break;
                case 12:clearData();break;
                case 13:new AlertDialog.Builder(this).setTitle("About S Browser")
                    .setMessage("S Browser 1.4 Beta • SAURABH'S VISION™\n\n"+
                    "Android WebView-powered browsing; a curated offline English–Hindi student dictionary, local bookmarks, notes and Study Focus. No ads or analytics.\n\n"+
                    "Study Focus blocks selected top-level websites, not every possible distracting resource. This browser is an early beta and does not yet provide full incognito isolation, automatic spam protection or universal ad blocking.")
                    .setPositiveButton("OK",null).show();break;
                case 14:showJobsHub();break;
                case 15:showCityNews();break;
                case 16:forward();break;
                case 17:showDailyAssembly();break;
            }
        }).show();
    }

    @Override public void onBackPressed(){
        if(fullScreenVideo!=null){hideVideo();return;}
        back();
    }
    @Override protected void onPause(){
        for(Tab tab:tabs)tab.web.onPause();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(!home&&selected>=0&&selected<tabs.size())
            tabs.get(selected).web.onResume();
    }
    @Override protected void onStop(){
        super.onStop();
        ArrayList<String> session=new ArrayList<>();
        for(Tab tab:tabs){
            String url=tab.web.getUrl();
            if(url!=null&&url.startsWith("http"))session.add(url);
        }
        store.saveSession(session);
    }
    @Override protected void onDestroy(){
        if(tts!=null){tts.stop();tts.shutdown();tts=null;}
        if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}
        for(Tab tab:tabs){
            if(tab.web.getParent() instanceof ViewGroup)
                ((ViewGroup)tab.web.getParent()).removeView(tab.web);
            tab.web.stopLoading();tab.web.destroy();
        }
        tabs.clear();
        super.onDestroy();
    }
}
