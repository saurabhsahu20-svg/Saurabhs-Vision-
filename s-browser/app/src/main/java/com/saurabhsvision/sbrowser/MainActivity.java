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
    private static final int PICK_FILE=81, MAX_TABS=8;
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
    private BrowserStore store;
    private DictionaryRepository dictionary;
    private TextToSpeech tts;
    private LinearLayout root,top,bottom,tabLine;
    private HorizontalScrollView tabStrip;
    private FrameLayout contentFrame;
    private EditText address;
    private ProgressBar progress;
    private TextView tabBadge,focusBadge;
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
        tile.setPadding(dp(14),dp(14),dp(12),dp(13));
        GradientDrawable bg=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff1e3b59,0xff122a3f});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),accent);
        tile.setBackground(bg);
        TextView name=text(title,15,WHITE,true);
        name.setSingleLine(true);
        add(tile,name,0);
        add(tile,text(detail,11,MUTED,false),5);
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
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
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
    private void buildShell(){
        root=column();
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{BG,0xff071527,0xff061125}));
        root.setPadding(0,0,0,dp(7));
        setContentView(root);
        top=column();top.setPadding(dp(14),dp(11),dp(14),dp(2));
        top.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xff101e39,0xff0a1931}));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout header=row();
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.s_browser_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView name=text("S Browser",21,WHITE,true);
        LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,-2,1);
        n.leftMargin=dp(9);header.addView(name,n);
        focusBadge=text("STUDY",10,store.focusMode()?MINT:MUTED,true);
        focusBadge.setBackground(card(0xff17354a,0xff326477,13));
        focusBadge.setPadding(dp(9),dp(9),dp(9),dp(9));
        header.addView(focusBadge);
        focusBadge.setOnClickListener(v->toggleFocus());
        TextView menu=text("⋮",27,WHITE,true);
        menu.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(42),dp(42));
        mp.leftMargin=dp(5);header.addView(menu,mp);
        menu.setOnClickListener(v->showTools());
        add(top,header,0);

        LinearLayout search=row();
        search.setBackground(card(PANEL,EDGE,21));
        search.setPadding(dp(10),dp(4),dp(7),dp(4));
        TextView lock=text("⌕",25,CYAN,false);lock.setGravity(Gravity.CENTER);
        search.addView(lock,new LinearLayout.LayoutParams(dp(37),dp(40)));
        address=new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(15);address.setTextColor(WHITE);
        address.setHintTextColor(MUTED);
        address.setHint("Search or enter a website");
        address.setSelectAllOnFocus(true);
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.addView(address,new LinearLayout.LayoutParams(0,dp(44),1));
        address.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_SEARCH){
                openAddress(address.getText().toString());return true;
            }return false;
        });
        TextView go=text("GO",13,CYAN,true);go.setGravity(Gravity.CENTER);
        search.addView(go,new LinearLayout.LayoutParams(dp(48),dp(42)));
        go.setOnClickListener(v->openAddress(address.getText().toString()));
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
        tabLine=row();tabLine.setPadding(dp(1),dp(6),dp(2),dp(7));
        tabStrip.addView(tabLine,new HorizontalScrollView.LayoutParams(-2,-2));
        LinearLayout.LayoutParams tabsP=new LinearLayout.LayoutParams(-1,dp(53));
        tabsP.topMargin=dp(2);top.addView(tabStrip,tabsP);
        contentFrame=new FrameLayout(this);
        root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));

        bottom=row();
        bottom.setPadding(dp(7),dp(5),dp(7),dp(5));
        bottom.setBackground(card(0xff10223d,0xff254970,24));
        String[] icons={"‹","⌂","▣","Aa","⚑","▤"};
        String[] labels={"Back","Home","Tabs","Words","Jobs","News"};
        Runnable[] actions={this::back,this::showHome,this::showTabs,
            ()->showDictionary(""),this::showJobsHub,this::showCityNews};
        for(int i=0;i<icons.length;i++){
            final int index=i;
            LinearLayout cell=column();
            cell.setGravity(Gravity.CENTER);
            TextView icon=text(icons[i],23,i>=4?MINT:CYAN,true);
            icon.setGravity(Gravity.CENTER);cell.addView(icon);
            TextView label=text(labels[i],10,MUTED,false);
            label.setGravity(Gravity.CENTER);cell.addView(label);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(61),1);
            bottom.addView(cell,p);
            cell.setContentDescription(labels[i]);
            cell.setOnClickListener(v->actions[index].run());
            if(i==2)tabBadge=label;
        }
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(-1,dp(72));
        b.setMargins(dp(12),dp(2),dp(12),dp(7));root.addView(bottom,b);
        refreshTabBadge();
    }
    private void refreshTabBadge(){
        if(tabBadge!=null)tabBadge.setText("Tabs "+tabs.size());
        if(focusBadge!=null)focusBadge.setText(store.focusMode()?"FOCUS ON":"STUDY");
        if(focusBadge!=null)focusBadge.setTextColor(store.focusMode()?MINT:MUTED);
        refreshTabStrip();
    }
    private void refreshTabStrip(){
        if(tabLine==null)return;
        tabLine.removeAllViews();
        TextView start=text("⌂  Home",13,home?0xff061527:WHITE,true);
        start.setGravity(Gravity.CENTER);
        start.setPadding(dp(15),0,dp(15),0);
        start.setBackground(card(home?MINT:PANEL,home?MINT:EDGE,14));
        LinearLayout.LayoutParams startP=new LinearLayout.LayoutParams(-2,dp(39));
        startP.rightMargin=dp(7);tabLine.addView(start,startP);
        start.setOnClickListener(v->showHome());
        for(Tab tab:tabs){
            boolean active=!home&&selected>=0&&selected<tabs.size()&&
                tabs.get(selected)==tab;
            LinearLayout chip=row();
            chip.setBackground(card(active?0xff24476a:0xff102239,
                active?CYAN:0xff35516c,13));
            View indicator=new View(this);
            indicator.setBackground(card(active?CYAN:0xff2e4b65,0,3));
            chip.addView(indicator,new LinearLayout.LayoutParams(dp(3),dp(22)));
            String title=tab.title;
            if(title==null||title.isEmpty()||title.equals("New Tab")){
                try{
                    String url=tab.web.getUrl();
                    String host=url==null?"":Uri.parse(url).getHost();
                    title=host==null?"New Tab":host;
                }catch(Exception ignored){title="New Tab";}
            }
            TextView titleView=text(title,13,active?WHITE:MUTED,true);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setMaxWidth(dp(142));
            titleView.setPadding(dp(9),0,dp(5),0);
            chip.addView(titleView,new LinearLayout.LayoutParams(-2,dp(37)));
            TextView close=text("×",21,active?CYAN:MUTED,false);
            close.setGravity(Gravity.CENTER);
            close.setContentDescription("Close "+title);
            chip.addView(close,new LinearLayout.LayoutParams(dp(32),dp(37)));
            close.setOnClickListener(v->closeTab(tab));
            chip.setContentDescription("Open tab: "+title);
            chip.setOnClickListener(v->{
                int index=tabs.indexOf(tab);
                if(index>=0){selected=index;attachCurrent();}
            });
            LinearLayout.LayoutParams chipP=new LinearLayout.LayoutParams(-2,dp(39));
            chipP.rightMargin=dp(7);tabLine.addView(chip,chipP);
        }
        TextView plus=text("＋ New",13,MINT,true);
        plus.setGravity(Gravity.CENTER);
        plus.setPadding(dp(12),0,dp(12),0);
        plus.setBackground(card(0xff133849,0xff3d867e,13));
        tabLine.addView(plus,new LinearLayout.LayoutParams(-2,dp(39)));
        plus.setContentDescription("Create new browser tab");
        plus.setOnClickListener(v->{
            if(createTab()!=null)showHome();
        });
    }
    private void showHome(){
        home=true;
        if(contentFrame==null)return;
        for(Tab t:tabs)t.web.onPause();
        contentFrame.removeAllViews();
        address.setText("");
        progress.setVisibility(View.GONE);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        contentFrame.addView(scroll);
        LinearLayout layout=column();
        layout.setPadding(dp(18),dp(16),dp(18),dp(26));scroll.addView(layout);
        LinearLayout hero=column();hero.setPadding(dp(20),dp(20),dp(20),dp(19));
        hero.setBackground(premiumHero());
        hero.setElevation(dp(3));
        TextView eyebrow=text("✦  SMART BROWSING  •  STUDENT EDITION",12,MINT,true);
        add(hero,eyebrow,0);
        add(hero,text("Explore more. Learn better.",26,WHITE,true),10);
        add(hero,text("●  DAILY STUDENT DASHBOARD  •  "+
            java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.ENGLISH)),
            11,CYAN,true),8);
        add(hero,text("Fast browsing with an offline student dictionary and private local notes.",13,0xffd2e1f5,false),10);
        TextView enter=pill("⌕  Search the web  →",()->{
            address.requestFocus();
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(address,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        enter.setBackground(card(0xff304a7c,0xff7488c9,17));add(hero,enter,16);
        add(layout,hero,2);
        TextView assembly=pill("✦  Daily Assembly & Fun  →",this::showDailyAssembly);
        assembly.setBackground(card(0xff213e57,0xff6389ab,19));
        assembly.setTextColor(WHITE);
        add(layout,assembly,12);
        add(layout,text("Thought • Speech • Sports • General News • Crossword • Puzzles",
            12,MUTED,false),7);
        sectionHeading(layout,"TODAY'S NEWS DESK",CYAN);
        LinearLayout newsQuick=row();
        View newsGeneral=quickCardView("📰 General News",
            "Dated • Source-first",CYAN,()->openAssemblySection(4));
        View newsSports=quickCardView("🏏 Sports News",
            "Recent • Attributed",MINT,()->openAssemblySection(3));
        LinearLayout.LayoutParams leftNews=new LinearLayout.LayoutParams(0,dp(80),1);
        leftNews.rightMargin=dp(10);
        newsQuick.addView(newsGeneral,leftNews);
        newsQuick.addView(newsSports,new LinearLayout.LayoutParams(0,dp(80),1));
        add(layout,newsQuick,7);
        sectionHeading(layout,"STUDY TOOLS",CYAN);
        LinearLayout first=row();
        TextView dictionaryBtn=pill("Aa  Dictionary",()->showDictionary(""));
        TextView notes=pill("✎  Quick Notes",this::editNote);
        first.addView(dictionaryBtn,new LinearLayout.LayoutParams(0,dp(55),1));
        LinearLayout.LayoutParams noteP=new LinearLayout.LayoutParams(0,dp(55),1);
        noteP.leftMargin=dp(10);first.addView(notes,noteP);
        add(layout,first,9);
        LinearLayout second=row();
        TextView study=pill(store.focusMode()?"◉  Focus is ON":"◉  Study Focus",this::toggleFocus);
        TextView books=pill("☆  Bookmarks",this::showBookmarks);
        second.addView(study,new LinearLayout.LayoutParams(0,dp(55),1));
        LinearLayout.LayoutParams booksP=new LinearLayout.LayoutParams(0,dp(55),1);
        booksP.leftMargin=dp(10);second.addView(books,booksP);
        add(layout,second,10);
        LinearLayout jobs=row();
        TextView careers=pill("⚑  CG Jobs & Exam Notices  →",this::showJobsHub);
        careers.setTextColor(MINT);
        careers.setBackground(card(0xff163b4c,0xff3c937e,19));
        jobs.addView(careers,new LinearLayout.LayoutParams(-1,dp(58)));
        add(layout,jobs,11);
        add(layout,text("City-wise official portals • Railway • JEE • Navodaya • Sainik School",12,MUTED,false),7);
        TextView news=pill("▤  Latest Chhattisgarh City News  →",this::showCityNews);
        news.setTextColor(CYAN);
        news.setBackground(card(0xff1b3451,0xff3f7294,19));
        add(layout,news,12);
        add(layout,text("Select your city • See publisher & published date • Read full story",12,MUTED,false),7);
        sectionHeading(layout,"SAURABH VISION LEARNING",MINT);
        TextView learning=pill("✦  Saurabh Vision Learning App  ↗",()->
            openAddress("https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/vision-app/"));
        learning.setTextColor(MINT);
        learning.setBackground(card(0xff163c4b,0xff3b947b,18));
        add(layout,learning,10);
        add(layout,text("Explore your learning app, quizzes and study activities.",
            12,MUTED,false),7);
        List<String> last=store.session();
        if(!last.isEmpty()){
            TextView restore=pill("↶  Restore "+last.size()+" previous tabs",()->restoreTabs(last));
            add(layout,restore,24);
        }
        add(layout,text("S Browser  •  by SAURABH'S VISION™  •  Private local study tools",
            11,MUTED,false),25);
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
        home=false;
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
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code==PICK_FILE&&fileCallback!=null){
            Uri[] uris=WebChromeClient.FileChooserParams.parseResult(result,data);
            fileCallback.onReceiveValue(uris);fileCallback=null;
        }
    }
    private Tab current(){return selected>=0&&selected<tabs.size()?tabs.get(selected):null;}
    private void back(){
        if(fullScreenVideo!=null){hideVideo();return;}
        Tab tab=current();
        if(!home&&tab!=null&&tab.web.canGoBack())tab.web.goBack();
        else showHome();
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
    private void showDictionary(String initial){
        final LinearLayout body=column();body.setPadding(dp(14),dp(5),dp(14),dp(12));
        EditText search=new EditText(this);search.setSingleLine(true);
        search.setHint("Search an English or Hindi word");
        search.setTextSize(16);
        body.addView(search,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView summary=text(dictionary.size()+" words stored offline • English ↔ Hindi",
            12,MUTED,false);add(body,summary,2);
        ScrollView scroll=new ScrollView(this);
        final LinearLayout results=column();scroll.addView(results);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(390));
        sp.topMargin=dp(8);body.addView(scroll,sp);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Aa  Student Dictionary")
            .setView(body).setNegativeButton("Close",null).create();
        Runnable render=()->{
            results.removeAllViews();
            List<DictionaryEntry> matches=dictionary.search(search.getText().toString());
            if(matches.isEmpty())add(results,text("Not in the offline starter dictionary.",14,MUTED,false),10);
            for(DictionaryEntry word:matches){
                LinearLayout item=column();
                item.setPadding(dp(11),dp(11),dp(11),dp(11));
                item.setBackground(card(0xff172943,0xff34567f,12));
                add(item,text(word.word+"   •   "+word.hindi,15,WHITE,true),0);
                add(item,text(word.meaning,12,MUTED,false),5);
                add(results,item,5);
                item.setOnClickListener(v->showWord(word));
            }
        };
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}
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
