package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Native Java browser shell built on the system WebView engine.
 *  Privacy: https-only main-frame URLs, no blanket site permissions,
 *  no third-party cookies, and bounded on-device history/bookmarks.
 */
public final class MainActivity extends Activity {
    private static final int BG=0xff081526, CARD=0xff132b43, CARD2=0xff1b3551,
        TEXT=0xfff2fbff, MUTED=0xffabc6d7, CYAN=0xff53e1e9, GREEN=0xff77f0c1;
    private static final int PICK_FILE=4005, MAX_TABS=8;
    private static final String HISTORY="history", BOOKMARKS="bookmarks";
    private final ArrayList<Tab> tabs=new ArrayList<>();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout container,top,nav;
    private FrameLayout pageFrame;
    private EditText address;
    private ProgressBar progress;
    private TextView tabBadge;
    private Tab selected;
    private boolean home=true;
    private ValueCallback<Uri[]> uploadCallback;
    private View videoView;
    private WebChromeClient.CustomViewCallback videoCallback;
    private FrameLayout videoOverlay;
    private String searchEngine="Google";
    private long lastNavigate=0;

    private static final class Tab {
        WebView web;
        String url="", title="New tab";
        Tab(WebView web){this.web=web;}
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private GradientDrawable gradient(int start,int end,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
        d.setCornerRadius(dp(radius));return d;
    }
    private TextView t(String value,int size,int color,boolean bold){
        TextView x=new TextView(this);x.setText(value);x.setTextSize(size);x.setTextColor(color);
        if(bold)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return x;
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void add(LinearLayout layout,View view,int topMargin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(topMargin);layout.addView(view,p);
    }
    private TextView button(String text,int color){
        TextView b=t(text,14,TEXT,true);b.setGravity(Gravity.CENTER);
        b.setBackground(shape(color,16));b.setPadding(dp(11),dp(11),dp(11),dp(11));
        return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        searchEngine=BrowserStore.getSetting(this,"search_engine","Google");
        WebView.setWebContentsDebuggingEnabled(false);
        makeLayout();
        if(getIntent()!=null && Intent.ACTION_VIEW.equals(getIntent().getAction())){
            Uri url=getIntent().getData();
            if(url!=null)navigate(url.toString());
        }
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        if(Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)
            navigate(intent.getData().toString());
    }
    private void makeLayout(){
        container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(BG);setContentView(container);
        buildToolbar();
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);progress.setMax(100);progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(CYAN));
        container.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        progress.setVisibility(View.INVISIBLE);
        pageFrame=new FrameLayout(this);
        container.addView(pageFrame,new LinearLayout.LayoutParams(-1,0,1));
        buildNav();
        container.setOnApplyWindowInsetsListener((v,insets)->{
            container.setPadding(0,0,0,Math.max(dp(5),insets.getSystemWindowInsetBottom()));
            return insets;
        });
        if(selected==null||home)showHome();else showTab(selected);
    }
    private void buildToolbar(){
        top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(14),dp(9),dp(14),dp(12));
        top.setBackgroundColor(BG);container.addView(top);
        LinearLayout branding=new LinearLayout(this);branding.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("S Browser logo");
        branding.addView(logo,new LinearLayout.LayoutParams(dp(43),dp(43)));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(9),0,0,0);
        words.addView(t("S Browser",21,TEXT,true));words.addView(t("by SAURABH'S VISION™",10,CYAN,false));
        branding.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView dictionary=button("A↔अ",CARD2);
        branding.addView(dictionary,new LinearLayout.LayoutParams(dp(61),dp(39)));
        dictionary.setContentDescription("Open offline dictionary");dictionary.setOnClickListener(v->openDictionary(null));
        add(top,branding,0);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8),0,dp(5),0);row.setBackground(shape(CARD,18));
        address=new EditText(this);address.setSingleLine(true);address.setTextColor(TEXT);
        address.setHintTextColor(MUTED);address.setHint("Search or enter website");address.setTextSize(15);
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setSelectAllOnFocus(true);address.setPadding(dp(10),0,dp(4),0);
        row.addView(address,new LinearLayout.LayoutParams(0,dp(49),1));
        TextView submit=t("➜",25,CYAN,true);submit.setGravity(Gravity.CENTER);
        row.addView(submit,new LinearLayout.LayoutParams(dp(43),dp(49)));
        submit.setContentDescription("Search or navigate");submit.setOnClickListener(v->navigate(address.getText().toString()));
        address.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_SEARCH){
                navigate(address.getText().toString());return true;}
            return false;
        });
        add(top,row,12);
    }
    private void buildNav(){
        nav=new LinearLayout(this);nav.setPadding(dp(8),dp(7),dp(8),dp(6));
        nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(0xff0b1c30);
        container.addView(nav,new LinearLayout.LayoutParams(-1,dp(67)));
        navItem("⌂","Home",()->showHome());
        navItem("‹","Back",()->goBack());
        navItem("▣","Tabs",()->showTabs());
        navItem("A↔अ","Dictionary",()->lookupSelection());
        navItem("⋯","More",()->showMore());
    }
    private void navItem(String icon,String title,Runnable action){
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);item.setContentDescription(title);
        TextView glyph=t(icon,23,title.equals("Dictionary")?GREEN:CYAN,true);
        glyph.setGravity(Gravity.CENTER);item.addView(glyph);
        TextView label=t(title,11,MUTED,false);label.setGravity(Gravity.CENTER);
        item.addView(label);
        nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));item.setOnClickListener(v->action.run());
        if(title.equals("Tabs"))tabBadge=glyph;
    }
    private void updateNav(){if(tabBadge!=null)tabBadge.setText("▣"+(tabs.isEmpty()?"":tabs.size()));}
    private void showHome(){
        home=true;pageFrame.removeAllViews();
        if(address!=null)address.setText("");
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        pageFrame.addView(scroll);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(24),dp(18),dp(26));scroll.addView(body);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(21),dp(26),dp(21),dp(24));
        hero.setBackground(gradient(0xff164d79,0xff30245f,28));
        add(hero,t("Explore. Learn. Discover.",25,TEXT,true),0);
        add(hero,t("A simpler way to browse and study — built for everyone.",14,0xffd5e9f8,false),12);
        TextView open=button("Search the web  →",0xff337a9d);
        add(hero,open,20);open.setOnClickListener(v->{address.requestFocus();
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(address,InputMethodManager.SHOW_IMPLICIT);});
        add(body,hero,0);
        TextView quick=t("YOUR STUDY SHORTCUTS",12,CYAN,true);quick.setPadding(0,dp(7),0,dp(4));
        add(body,quick,20);
        String[][] shortcuts={
            {"📚","Wikipedia","https://www.wikipedia.org"},
            {"📖","NCERT","https://ncert.nic.in"},
            {"🎓","Khan Academy","https://www.khanacademy.org"},
            {"🏫","DIKSHA","https://diksha.gov.in"},
            {"🔬","Google Scholar","https://scholar.google.com"},
            {"🌐","Dictionary","dictionary"}
        };
        for(int index=0;index<shortcuts.length;index+=2){
            LinearLayout row=new LinearLayout(this);
            for(int c=0;c<2;c++){
                String[] item=shortcuts[index+c];
                LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);tile.setBackground(shape(CARD,18));
                tile.setPadding(dp(6),dp(13),dp(6),dp(13));
                TextView em=t(item[0],23,TEXT,true);em.setGravity(Gravity.CENTER);tile.addView(em);
                TextView title=t(item[1],14,TEXT,true);title.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(8);tile.addView(title,tp);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(95),1);
                p.setMargins(dp(4),dp(5),dp(4),dp(5));row.addView(tile,p);
                tile.setContentDescription("Open "+item[1]);
                tile.setOnClickListener(v->{if(item[2].equals("dictionary"))openDictionary(null);else navigate(item[2]);});
            }
            add(body,row,0);
        }
        TextView offline=t("OFFLINE LEARNING",12,CYAN,true);add(body,offline,20);
        LinearLayout wordCard=new LinearLayout(this);wordCard.setOrientation(LinearLayout.VERTICAL);
        wordCard.setBackground(shape(CARD2,22));wordCard.setPadding(dp(18),dp(18),dp(18),dp(20));
        wordCard.addView(t("Inbuilt Dictionary",19,TEXT,true));
        add(wordCard,t("260 English words • Hindi meanings • Pronunciation",13,MUTED,false),8);
        TextView dictionary=button("Open dictionary  →",0xff246e78);
        add(wordCard,dictionary,15);dictionary.setOnClickListener(v->openDictionary(null));
        add(body,wordCard,10);
        String last=BrowserStore.getSetting(this,"last_url","");
        if(UrlTools.validated(last)!=null){
            TextView lastTitle=t("RESUME BROWSING",12,CYAN,true);add(body,lastTitle,20);
            TextView resume=button(last.length()>47?last.substring(0,47)+"…":last,CARD);
            add(body,resume,7);resume.setOnClickListener(v->navigate(last));
        }
        TextView footer=t("S Browser  •  Designed by SAURABH'S VISION™",11,MUTED,false);
        footer.setGravity(Gravity.CENTER);add(body,footer,30);
        updateNav();
    }
    private void navigate(String raw){
        String url=UrlTools.resolve(raw,searchEngine);
        if(url==null){toast("Please enter a valid HTTPS address or search term");return;}
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-lastNavigate<180)return;lastNavigate=now;
        if(address!=null){address.clearFocus();
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);}
        if(selected==null)selected=newTab();
        if(selected==null)return;
        home=false;
        showTab(selected);
        selected.web.loadUrl(url);
    }
    private Tab newTab(){
        if(tabs.size()>=MAX_TABS){toast("Maximum 8 tabs. Close one to continue.");return null;}
        WebView web=new WebView(this);WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setBackgroundColor(Color.WHITE);
        final Tab tab=new Tab(web);tabs.add(tab);selected=tab;
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
                if(!req.isForMainFrame())return false;
                Uri uri=req.getUrl();String scheme=uri.getScheme();
                if("https".equalsIgnoreCase(scheme))return false;
                if("http".equalsIgnoreCase(scheme)){
                    String upgraded=UrlTools.resolve(uri.toString(),searchEngine);
                    if(upgraded!=null){view.loadUrl(upgraded);return true;}
                }
                toast("Unsupported or insecure link blocked");return true;
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();toast("Unsafe SSL certificate blocked");
            }
            @Override public void onPageFinished(WebView view,String url){
                String verified=UrlTools.validated(url);
                if(verified==null)return;
                tab.url=verified;
                tab.title=view.getTitle()==null?verified:view.getTitle();
                if(selected==tab&&!home&&address!=null)address.setText(verified);
                BrowserStore.put(MainActivity.this,HISTORY,tab.title,verified,250);
                BrowserStore.setSetting(MainActivity.this,"last_url",verified);
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame())toast("Page could not load. Check your internet connection.");
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int value){
                if(selected!=tab||progress==null)return;
                progress.setVisibility(value>=100?View.INVISIBLE:View.VISIBLE);
                progress.setProgress(value);
            }
            @Override public void onReceivedTitle(WebView view,String title){tab.title=title==null?tab.url:title;}
            @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> result,
                FileChooserParams params){
                if(uploadCallback!=null)uploadCallback.onReceiveValue(null);
                uploadCallback=result;
                try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
                catch(Exception e){uploadCallback=null;toast("File picker unavailable");return false;}
            }
            @Override public void onShowCustomView(View view,CustomViewCallback callback){
                if(videoView!=null){callback.onCustomViewHidden();return;}
                videoView=view;videoCallback=callback;
                videoOverlay=new FrameLayout(MainActivity.this);videoOverlay.setBackgroundColor(Color.BLACK);
                videoOverlay.addView(view,new FrameLayout.LayoutParams(-1,-1));
                container.setVisibility(View.GONE);setContentView(videoOverlay);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            @Override public void onHideCustomView(){leaveVideo();}
        });
        web.setDownloadListener((url,userAgent,contentDisposition,mime,length)->download(url,userAgent,contentDisposition,mime));
        updateNav();return tab;
    }
    private void showTab(Tab tab){
        if(tab==null)return;
        home=false;selected=tab;pageFrame.removeAllViews();
        if(tab.web.getParent()!=null)((ViewGroup)tab.web.getParent()).removeView(tab.web);
        pageFrame.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));
        if(address!=null)address.setText(tab.url);
        updateNav();
    }
    private void showTabs(){
        List<String> choices=new ArrayList<>();choices.add("＋  New tab");
        for(int i=0;i<tabs.size();i++){
            Tab tab=tabs.get(i);
            String title=tab.title==null?"New tab":tab.title;
            choices.add((tab==selected?"● ":"○ ")+(i+1)+". "+
                (title.length()>32?title.substring(0,32)+"…":title));
        }
        choices.add("✕  Close current tab");
        new AlertDialog.Builder(this).setTitle("Tabs ("+tabs.size()+" / "+MAX_TABS+")")
            .setItems(choices.toArray(new String[0]),(dialog,index)->{
                if(index==0){Tab tab=newTab();if(tab!=null){selected=tab;showHome();}}
                else if(index==choices.size()-1)closeTab();
                else {selected=tabs.get(index-1);if(selected.url.isEmpty())showHome();else showTab(selected);}
            }).setNegativeButton("Cancel",null).show();
    }
    private void closeTab(){
        if(selected==null){showHome();return;}
        Tab dead=selected;int index=tabs.indexOf(dead);
        if(dead.web.getParent()!=null)((ViewGroup)dead.web.getParent()).removeView(dead.web);
        dead.web.stopLoading();dead.web.destroy();tabs.remove(dead);
        if(tabs.isEmpty()){selected=null;showHome();}else{
            selected=tabs.get(Math.min(index,tabs.size()-1));showTab(selected);}
        updateNav();
    }
    private void goBack(){
        if(videoView!=null){leaveVideo();return;}
        if(home){if(selected!=null && !selected.url.isEmpty())showTab(selected);else toast("You're on the homepage");}
        else if(selected!=null&&selected.web.canGoBack())selected.web.goBack();
        else showHome();
    }
    @Override public void onBackPressed(){goBack();}
    private void showMore(){
        String[] options={"☆  Bookmark this page","★  Bookmarks","◷  History",
            "↻  Reload","⌕  Find on page","▣  Desktop site","↗  Share page",
            "A↔अ  Look up selected word","⚙  Search engine","✕  Clear browsing data","ⓘ  Privacy & About"};
        new AlertDialog.Builder(this).setTitle("S Browser")
            .setItems(options,(d,which)->{
                switch(which){
                    case 0: bookmark();break;
                    case 1: listSaved(BOOKMARKS);break;
                    case 2: listSaved(HISTORY);break;
                    case 3: if(selected!=null&&!home)selected.web.reload();break;
                    case 4: findInPage();break;
                    case 5: toggleDesktop();break;
                    case 6: sharePage();break;
                    case 7: lookupSelection();break;
                    case 8: changeEngine();break;
                    case 9: clearData();break;
                    case 10: about();break;
                }
            }).setNegativeButton("Close",null).show();
    }
    private void bookmark(){
        if(home||selected==null||selected.url.isEmpty()){toast("Open a website first");return;}
        String url=selected.url;
        if(BrowserStore.has(this,BOOKMARKS,url)){
            new AlertDialog.Builder(this).setMessage("Remove this bookmark?")
                .setPositiveButton("Remove",(d,w)->{BrowserStore.remove(this,BOOKMARKS,url);toast("Bookmark removed");})
                .setNegativeButton("Cancel",null).show();
        }else{
            BrowserStore.put(this,BOOKMARKS,selected.title,url,250);toast("Saved to bookmarks");
        }
    }
    private void listSaved(String key){
        List<BrowserStore.Page> items=BrowserStore.list(this,key);
        if(items.isEmpty()){toast(key.equals(HISTORY)?"No history yet":"No bookmarks yet");return;}
        String[] labels=new String[items.size()];
        for(int i=0;i<items.size();i++){
            BrowserStore.Page p=items.get(i);
            labels[i]=(p.title.length()>36?p.title.substring(0,36)+"…":p.title)+"\n"+p.url;
        }
        new AlertDialog.Builder(this).setTitle(key.equals(HISTORY)?"Browsing history":"Bookmarks")
            .setItems(labels,(d,position)->navigate(items.get(position).url))
            .setNeutralButton("Manage",(d,position)->{
                String[] rowLabels=new String[items.size()];
                for(int i=0;i<items.size();i++)rowLabels[i]=items.get(i).title;
                new AlertDialog.Builder(this).setTitle("Tap to remove an entry")
                    .setItems(rowLabels,(x,i)->{BrowserStore.remove(this,key,items.get(i).url);toast("Entry removed");})
                    .setNegativeButton("Cancel",null).show();
            }).setNegativeButton("Close",null).show();
    }
    private void changeEngine(){
        String[] engines={"Google","DuckDuckGo","Bing"};
        int current=java.util.Arrays.asList(engines).indexOf(searchEngine);
        new AlertDialog.Builder(this).setTitle("Search engine")
            .setSingleChoiceItems(engines,Math.max(0,current),(d,i)->{
                searchEngine=engines[i];BrowserStore.setSetting(this,"search_engine",searchEngine);
                toast("Search engine: "+searchEngine);d.dismiss();
            }).setNegativeButton("Cancel",null).show();
    }
    private void findInPage(){
        if(home||selected==null){toast("Open a page first");return;}
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Find text");
        new AlertDialog.Builder(this).setTitle("Find on page").setView(input)
            .setPositiveButton("Find",(d,w)->selected.web.findAllAsync(input.getText().toString()))
            .setNeutralButton("Next",(d,w)->selected.web.findNext(true))
            .setNegativeButton("Cancel",null).show();
    }
    private void toggleDesktop(){
        if(home||selected==null){toast("Open a page first");return;}
        WebSettings settings=selected.web.getSettings();boolean old=settings.getUseWideViewPort();
        boolean desktop= !selected.web.getSettings().getUserAgentString().contains("X11; Linux x86_64");
        if(desktop)settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        else settings.setUserAgentString(null);
        settings.setUseWideViewPort(true);selected.web.reload();
        toast(desktop?"Desktop view on":"Mobile view on");
    }
    private void sharePage(){
        if(home||selected==null||selected.url.isEmpty()){toast("Open a page first");return;}
        Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,selected.url);
        startActivity(Intent.createChooser(share,"Share website"));
    }
    private void lookupSelection(){
        if(home||selected==null){openDictionary(null);return;}
        selected.web.evaluateJavascript("(function(){try{return String(window.getSelection()).slice(0,80)}catch(e){return ''}})()",raw->{
            String word="";
            try{org.json.JSONArray a=new org.json.JSONArray("["+raw+"]");word=a.optString(0,"");}catch(Exception ignored){}
            String clean=word.trim();
            openDictionary(clean.isEmpty()?null:clean);
        });
    }
    private void openDictionary(String word){
        Intent i=new Intent(this,DictionaryActivity.class);
        if(word!=null)i.putExtra("word",word);
        startActivity(i);
    }
    private void clearData(){
        new AlertDialog.Builder(this).setTitle("Clear browsing data?")
            .setMessage("Remove saved browsing history, cookies and cached website data? Bookmarks are kept.")
            .setPositiveButton("Clear",(d,w)->{
                BrowserStore.clear(this,HISTORY);BrowserStore.setSetting(this,"last_url","");
                CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();
                WebStorage.getInstance().deleteAllData();
                for(Tab tab:tabs){tab.web.clearCache(true);tab.web.clearHistory();}
                toast("Browsing data cleared");
            }).setNegativeButton("Cancel",null).show();
    }
    private void about(){
        new AlertDialog.Builder(this).setTitle("S Browser 1.0 beta")
            .setMessage("by SAURABH'S VISION™\n\n"+
                "A lightweight Android browser with an inbuilt offline 260-word English/Hindi study dictionary.\n\n"+
                "Privacy defaults: HTTPS-only navigation, Android Safe Browsing, third-party cookies disabled, no analytics, and website camera/microphone access denied.\n\n"+
                "Limitations: this first version has no AI assistant, full private mode, online translation, browser extensions, sync, or universal ad blocking. Some sites need permissions we intentionally deny.\n\n"+
                "Website searches and page requests use the internet. Dictionary word lookups work offline.")
            .setPositiveButton("OK",null).show();
    }
    private void download(String url,String userAgent,String contentDisposition,String mime){
        String safe=UrlTools.validated(url);
        if(safe==null){toast("Only HTTPS downloads are supported");return;}
        try{
            String filename=URLUtil.guessFileName(url,contentDisposition,mime);
            filename=filename.replaceAll("[^A-Za-z0-9._()-]","_");
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);request.setDescription("S Browser download");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            if(mime!=null&&!mime.isEmpty())request.setMimeType(mime);
            if(userAgent!=null)request.addRequestHeader("User-Agent",userAgent);
            String cookies=CookieManager.getInstance().getCookie(url);
            if(cookies!=null)request.addRequestHeader("Cookie",cookies);
            request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,filename);
            DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            manager.enqueue(request);toast("Download started. Check notifications.");
        }catch(Exception e){toast("Download failed: unable to save file");}
    }
    private void leaveVideo(){
        if(videoView==null)return;
        if(videoOverlay!=null)videoOverlay.removeAllViews();
        videoView=null;videoOverlay=null;
        setContentView(container);container.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if(videoCallback!=null){videoCallback.onCustomViewHidden();videoCallback=null;}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_FILE && uploadCallback!=null){
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
            uploadCallback=null;
        }
    }
    @Override protected void onPause(){
        super.onPause();CookieManager.getInstance().flush();
        for(Tab tab:tabs)tab.web.onPause();
    }
    @Override protected void onResume(){
        super.onResume();for(Tab tab:tabs)tab.web.onResume();
    }
    @Override protected void onDestroy(){
        if(uploadCallback!=null)uploadCallback.onReceiveValue(null);
        if(videoView!=null)leaveVideo();
        for(Tab tab:tabs){
            if(tab.web.getParent()!=null)((ViewGroup)tab.web.getParent()).removeView(tab.web);
            tab.web.stopLoading();tab.web.destroy();
        }
        tabs.clear();super.onDestroy();
    }
}
