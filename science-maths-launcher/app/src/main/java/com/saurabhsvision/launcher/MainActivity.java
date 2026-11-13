package com.saurabhsvision.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * S VISION Launcher — a real, offline Android HOME application.
 *
 * Loads launchable apps with package visibility scoped to ACTION_MAIN +
 * CATEGORY_LAUNCHER; opens their actual exported launch activities.
 * Favorites and theme are saved only on the user's device.
 */
public final class MainActivity extends Activity {
    // The default Pearl Light palette is deliberately high contrast.
    // Original four neon-dark themes remain available as presets 0–3.
    private int BACKGROUND=0xfff5fbff,WHITE=0xff112f63;
    private int CYAN=0xff076dbb,GOLD=0xffc88a12,MUTED=0xff537394;
    private boolean lightTheme(){return prefs!=null&&
        LauncherRules.themeIndex(prefs.getInt(PREF_THEME,4),
            ScienceWallpaperView.THEMES.length)==4;}
    private void applyPalette(){
        if(lightTheme()){
            BACKGROUND=0xfff5fbff;WHITE=0xff112f63;
            CYAN=0xff086db6;GOLD=0xffad7005;MUTED=0xff537394;
        }else{
            BACKGROUND=0xff061432;WHITE=0xfff2faff;
            CYAN=0xff57ecff;GOLD=0xffffd673;MUTED=0xffafd3e9;
        }
    }
    private static final String PREFS="sv_launcher_settings",PREF_THEME="theme";
    private static final String PREF_FAVORITES="favorite_components";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private FrameLayout frame;
    private ScienceWallpaperView wallpaper;
    private boolean drawerOpen=false;
    private TextView clock,date,battery,drawerTitle;
    private GridLayout drawerGrid;
    private final ArrayList<AppItem> apps=new ArrayList<>();
    private boolean appsLoaded=false;
    private boolean refreshRunning=false;
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            updateClock();
            handler.postDelayed(this,30_000);
        }
    };
    private static final class AppItem{
        final String label,packageName,activityName,id;
        final android.graphics.drawable.Drawable icon;
        AppItem(String label,ResolveInfo info,PackageManager pm){
            this.label=label;
            this.packageName=info.activityInfo.packageName;
            this.activityName=info.activityInfo.name;
            this.id=new ComponentName(packageName,activityName).flattenToString();
            this.icon=info.loadIcon(pm);
        }
    }

    private int dp(float value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    private GradientDrawable panel(int top,int bottom,float round,int border){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{top,bottom});
        d.setCornerRadius(dp(round));
        if(border!=0)d.setStroke(dp(1),border);
        return d;
    }
    private TextView text(String label,float size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(label);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(true);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return t;
    }
    private void add(LinearLayout column,View child,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(top);
        column.addView(child,p);
    }
    private TextView control(String label){
        TextView t=text(label,13,lightTheme()?0xff0b4276:WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(11),dp(6),dp(11),dp(6));
        t.setBackground(lightTheme()?
            panel(0xffffffff,0xffdfedff,13,0xff93bbdd):
            panel(0xff1c477c,0xff0b2a58,13,0xff367fba));
        return t;
    }
    private void toast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        applyPalette();
        applySystemBars();
        buildUi();
        refreshApps();
    }
    @Override protected void onResume(){
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
        if(appsLoaded && !refreshRunning && apps.size()==0)refreshApps();
    }
    @Override protected void onPause(){
        super.onPause();handler.removeCallbacks(ticker);
    }
    @Override public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        buildUi();
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        drawerOpen=false;buildUi();
    }
    @Override public void onBackPressed(){
        if(drawerOpen){drawerOpen=false;buildUi();}
        // A default home screen stays open on the system Back key.
    }

    private void applySystemBars(){
        getWindow().setStatusBarColor(lightTheme()?0xffd9edff:BACKGROUND);
        getWindow().setNavigationBarColor(lightTheme()?0xffe8f5ff:BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
            lightTheme()?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);
    }
    private void buildUi(){
        applyPalette();
        applySystemBars();
        frame=new FrameLayout(this);
        frame.setBackgroundColor(BACKGROUND);
        wallpaper=new ScienceWallpaperView(this);
        wallpaper.setThemeIndex(prefs.getInt(PREF_THEME,4));
        frame.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        final ScrollView content=new ScrollView(this);
        content.setFillViewport(true);
        content.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams sc=new FrameLayout.LayoutParams(-1,-1);
        frame.addView(content,sc);
        setContentView(frame);
        // Keep all controls above OEM status/navigation bars on Android 15.
        content.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(0,insets.getSystemWindowInsetTop(),
                0,insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(14),dp(10),dp(14),dp(15));
        content.addView(column);
        if(drawerOpen)buildDrawer(column);
        else if(lightTheme())buildLightHome(column);
        else buildHome(column);
        updateClock();
    }

    /** Screenshot-inspired light-mode Home.
     * Functional app shortcuts are separate from decorative Canvas graphics.
     */
    private void buildLightHome(LinearLayout column){
        column.setPadding(dp(14),dp(4),dp(14),dp(13));

        LinearLayout brand=new LinearLayout(this);
        brand.setGravity(Gravity.CENTER);
        brand.setOrientation(LinearLayout.VERTICAL);
        // The user's genuine red 3D S logo includes the brand name;
        // display the uploaded artwork itself, not a new text approximation.
        ImageView brandImage=new ImageView(this);
        brandImage.setImageResource(R.drawable.svision_red_brand);
        brandImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandImage.setContentDescription("SAURABH'S VISION red 3D S brand logo");
        brand.addView(brandImage,new LinearLayout.LayoutParams(dp(112),dp(112)));
        TextView caption=text("SMART HOME • SCIENTIFIC VISION",10,0xff225ca0,true);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams=new LinearLayout.LayoutParams(-1,-2);
        captionParams.topMargin=dp(1);
        brand.addView(caption,captionParams);
        add(column,brand,5);

        // The search pill searches ACTUAL installed apps, not fake Google data.
        LinearLayout search=new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(18),0,dp(12),0);
        search.setBackground(panel(0xffffffff,0xffe8f7ff,28,0xff80c4f5));
        search.setElevation(dp(3));
        TextView g=text("G",23,0xff2186df,true);
        g.setGravity(Gravity.CENTER);
        search.addView(g,new LinearLayout.LayoutParams(dp(28),dp(54)));
        TextView label=text("  Search apps & learning",15,0xff54748f,false);
        search.addView(label,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView arrow=text("⌕",25,0xff116cb2,true);
        arrow.setGravity(Gravity.CENTER);
        search.addView(arrow,new LinearLayout.LayoutParams(dp(34),dp(54)));
        search.setContentDescription("Search installed apps");
        search.setOnClickListener(v->openDrawer(""));
        add(column,search,11);

        LinearLayout timeRow=new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout times=new LinearLayout(this);
        times.setOrientation(LinearLayout.VERTICAL);
        clock=text("10:24",48,0xff0a326f,true);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        times.addView(clock);
        date=text("",14,0xff204a7b,false);
        times.addView(date);
        timeRow.addView(times,new LinearLayout.LayoutParams(0,-2,1f));
        View vertical=new View(this);
        vertical.setBackgroundColor(0xffa2c8e7);
        LinearLayout.LayoutParams separator=new LinearLayout.LayoutParams(dp(1),dp(58));
        separator.setMargins(dp(11),0,dp(15),0);
        timeRow.addView(vertical,separator);
        LinearLayout info=new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView symbol=text("☀  SCIENCE TODAY",14,0xffb9810a,true);
        info.addView(symbol);
        battery=text("⚡ Battery",13,0xff204d7d,true);
        info.addView(battery);
        TextView line=text("Maths • Science • Discovery",10,0xff6483a6,false);
        info.addView(line);
        timeRow.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
        add(column,timeRow,15);

        // Main science identity matches the supplied composition. Decorative
        // atom, DNA, molecular model, flask and book are drawn by the wallpaper.
        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(5),dp(3),dp(5),dp(3));
        TextView title=text("Maths &",30,0xff0e3b88,true);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(1.5f,0,dp(1),0x33ffffff);
        hero.addView(title);
        TextView science=text("Science",34,0xff1053a0,true);
        science.setGravity(Gravity.CENTER);
        hero.addView(science);
        TextView ribbon=text("LAUNCHER",14,0xffffffff,true);
        ribbon.setGravity(Gravity.CENTER);
        ribbon.setLetterSpacing(.14f);
        ribbon.setBackground(panel(0xff157edc,0xff093c8e,14,0xfff9c342));
        LinearLayout.LayoutParams ribbonP=new LinearLayout.LayoutParams(dp(174),dp(31));
        ribbonP.topMargin=dp(4);
        hero.addView(ribbon,ribbonP);
        add(column,hero,13);

        // Freely use the area occupied by artistic book/helix, but avoid
        // buttons in that region. On short phones the content can scroll.
        View artSpace=new View(this);
        int heightDp=(int)(getResources().getDisplayMetrics().heightPixels/
            getResources().getDisplayMetrics().density);
        int artHeight=Math.max(84,Math.min(185,(heightDp-650)/2+85));
        column.addView(artSpace,new LinearLayout.LayoutParams(-1,dp(artHeight)));

        LinearLayout tools=new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER);
        String[] symbols={"＋","✎","⚛","π"};
        String[] labels={"Calculator","Notes","Science","Maths"};
        int[] top={0xff237eec,0xffffc64b,0xff8c55ed,0xff1176db};
        int[] bottom={0xff063c9f,0xffe9870e,0xff5428aa,0xff173db0};
        for(int i=0;i<4;i++){
            final int idx=i;
            LinearLayout tile=new LinearLayout(this);
            tile.setGravity(Gravity.CENTER);
            tile.setOrientation(LinearLayout.VERTICAL);
            TextView graphic=text(symbols[i],29,Color.WHITE,true);
            graphic.setGravity(Gravity.CENTER);
            graphic.setBackground(panel(top[i],bottom[i],18,0xfff4ce6a));
            graphic.setElevation(dp(3));
            tile.addView(graphic,new LinearLayout.LayoutParams(dp(60),dp(60)));
            TextView caption=text(labels[i],12,0xff10336d,true);
            caption.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(27));
            cp.topMargin=dp(5);
            tile.addView(caption,cp);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(101),1);
            tools.addView(tile,lp);
            tile.setContentDescription("Open "+labels[i]);
            tile.setOnClickListener(v->{
                if(idx==0){
                    if(!tryOpenInstalled("s calc","calculator","calc")){
                        toast("Calculator not installed. Choose an app from All Apps.");
                        openDrawer("");
                    }
                }else if(idx==1){
                    if(!tryOpenInstalled("notes","keep","notepad"))showQuickNotes();
                }else showLearningLab(idx==2?1:0);
            });
        }
        add(column,tools,7);

        // Accessible All Apps and favorites. On-device clock and buttons
        // remain real, unlike the static reference illustration.
        LinearLayout lower=new LinearLayout(this);
        lower.setGravity(Gravity.CENTER);
        TextView dots=text("●  ○  ○",13,0xff337fd0,true);
        dots.setGravity(Gravity.CENTER);
        lower.addView(dots,new LinearLayout.LayoutParams(0,dp(27),1f));
        TextView all=control("ALL APPS  ›");
        lower.addView(all,new LinearLayout.LayoutParams(dp(111),dp(33)));
        all.setOnClickListener(v->openDrawer(""));
        add(column,lower,0);

        View spacer=new View(this);
        column.addView(spacer,new LinearLayout.LayoutParams(-1,0,1f));
        buildLightDock(column);
    }

    /** Local, no-cloud notepad fallback when a note app is not installed. */
    private void showQuickNotes(){
        final EditText entry=new EditText(this);
        entry.setText(prefs.getString("quick_notes",""));
        entry.setHint("Write a short note, formula or reminder…");
        entry.setTextSize(16);
        entry.setMinLines(5);
        entry.setGravity(Gravity.TOP);
        new AlertDialog.Builder(this)
            .setTitle("✎ S VISION Quick Notes")
            .setView(entry)
            .setPositiveButton("Save on device",(dialog,which)->{
                prefs.edit().putString("quick_notes",entry.getText().toString()).apply();
                toast("Your note is saved locally");
            })
            .setNegativeButton("Cancel",null).show();
    }
    private boolean tryOpenInstalled(String... names){
        if(!appsLoaded)return false;
        for(String target:names){
            for(AppItem item:apps){
                if(item.label.toLowerCase(Locale.ROOT).contains(target)){
                    openApp(item);return true;
                }
            }
        }
        return false;
    }

    private void buildLightDock(LinearLayout column){
        LinearLayout dock=new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(5),dp(10),dp(5),dp(8));
        dock.setBackground(panel(0xf9ffffff,0xdde5f5ff,25,0xffc6e5ff));
        dock.setElevation(dp(6));
        String[] symbols={"☎","●●●","🌐","▧","◉"};
        String[] labels={"Phone","Messages","Browser","Gallery","Camera"};
        int[] tops={0xff40e86a,0xff36adff,0xff31bbff,0xffba80fa,0xff4d86bf};
        int[] bottoms={0xff0ba730,0xff0755ca,0xff0e69d7,0xff5b28bf,0xff133b76};
        for(int i=0;i<5;i++){
            final int idx=i;
            LinearLayout item=new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            TextView icon=text(symbols[i],i==1?17:26,0xffffffff,true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(panel(tops[i],bottoms[i],17,0xffffffff));
            icon.setElevation(dp(2));
            item.addView(icon,new LinearLayout.LayoutParams(dp(55),dp(55)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(66),1f);
            dock.addView(item,lp);
            item.setContentDescription("Open "+labels[i]);
            item.setOnClickListener(v->{
                switch(idx){
                    case 0:external(new Intent(Intent.ACTION_DIAL));break;
                    case 1:external(new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:")));break;
                    case 2:external(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com")));break;
                    case 3:
                        if(!tryOpenInstalled("gallery","photos","files")){
                            Intent picker=new Intent(Intent.ACTION_GET_CONTENT);
                            picker.setType("image/*");
                            external(picker);
                        }
                        break;
                    case 4:external(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));break;
                    default:break;
                }
            });
        }
        add(column,dock,9);
    }

    private void buildHome(LinearLayout column){
        LinearLayout heading=new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView emblem=new ImageView(this);
        emblem.setImageResource(R.drawable.svision_red_brand);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        emblem.setContentDescription("SAURABH'S VISION genuine red S logo");
        heading.addView(emblem,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout labels=new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(11),0,0,0);
        labels.addView(text("SAURABH'S VISION™",18,WHITE,true));
        labels.addView(text("MATHS & SCIENCE LAUNCHER",11,CYAN,true));
        heading.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=control("⚙");
        settings.setContentDescription("Open launcher settings");
        heading.addView(settings,new LinearLayout.LayoutParams(dp(44),dp(44)));
        settings.setOnClickListener(v->showSettings());
        add(column,heading,1);

        LinearLayout clockCard=new LinearLayout(this);
        clockCard.setOrientation(LinearLayout.VERTICAL);
        clockCard.setPadding(dp(21),dp(10),dp(21),dp(12));
        clockCard.setBackground(panel(0xaa112f62,0xba091b41,23,0xff315fa0));
        TextView title=text("⚛  ATOMIC TIME  •  π 3.14159265",12,CYAN,true);
        add(clockCard,title,0);
        clock=text("12:00",getResources().getConfiguration().orientation==
            Configuration.ORIENTATION_LANDSCAPE?42:59,WHITE,true);
        add(clockCard,clock,1);
        date=text("",16,GOLD,true);
        add(clockCard,date,0);
        battery=text("",12,MUTED,false);
        add(clockCard,battery,6);
        add(column,clockCard,25);

        LinearLayout search=new LinearLayout(this);
        search.setPadding(dp(17),dp(2),dp(12),dp(2));
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setBackground(panel(0xffe4f8ff,0xffa6d8ed,24,0xff5be7ff));
        TextView prompt=text("⌕   Search your apps and tools",16,0xff10324f,true);
        search.addView(prompt,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView open=text("⊞",25,0xff10324f,true);
        open.setGravity(Gravity.CENTER);
        search.addView(open,new LinearLayout.LayoutParams(dp(35),dp(48)));
        search.setContentDescription("Open app drawer and search apps");
        search.setOnClickListener(v->openDrawer(""));
        add(column,search,18);

        LinearLayout scienceRow=new LinearLayout(this);
        for(int i=0;i<2;i++){
            final int index=i;
            LinearLayout card=new LinearLayout(this);
            card.setGravity(Gravity.CENTER);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(6),dp(13),dp(6),dp(13));
            card.setBackground(panel(i==0?0xbb182b75:0xbb075463,
                0xc3071e3c,18,i==0?0xff8bb3ff:0xff66f5da));
            TextView symbol=text(i==0?"∑ π √":"⚛ 🧬",29,
                i==0?GOLD:CYAN,true);
            symbol.setGravity(Gravity.CENTER);
            card.addView(symbol);
            TextView label=text(i==0?"MATHS LAB":"SCIENCE LAB",13,WHITE,true);
            label.setGravity(Gravity.CENTER);
            card.addView(label);
            TextView hint=text(i==0?"Equations & formulas":"Atoms & discoveries",
                11,MUTED,false);
            hint.setGravity(Gravity.CENTER);
            card.addView(hint);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(116),1);
            p.setMargins(dp(2),0,dp(2),0);
            scienceRow.addView(card,p);
            card.setOnClickListener(v->showLearningLab(index));
        }
        add(column,scienceRow,20);

        LinearLayout section=new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView favoritesTitle=text("★  FAVORITES & YOUR APPS",13,WHITE,true);
        section.addView(favoritesTitle,new LinearLayout.LayoutParams(0,dp(39),1));
        TextView all=control("ALL APPS  ›");
        all.setTextSize(11);
        section.addView(all,new LinearLayout.LayoutParams(dp(106),dp(37)));
        all.setOnClickListener(v->openDrawer(""));
        add(column,section,18);

        GridLayout favoritesGrid=new GridLayout(this);
        favoritesGrid.setColumnCount(4);
        if(!appsLoaded){
            TextView loading=text("Finding installed apps…",13,MUTED,false);
            favoritesGrid.addView(loading);
        }else{
            List<AppItem> favorites=homeApps();
            int n=Math.min(8,favorites.size());
            for(int i=0;i<n;i++)favoritesGrid.addView(
                appTile(favorites.get(i),getTileWidth()));
            if(n==0){
                TextView empty=text("Open All Apps to find and pin favorites.",13,MUTED,false);
                favoritesGrid.addView(empty);
            }
        }
        add(column,favoritesGrid,4);

        // Grow an elastic gap on taller screens to position dock at the base.
        View stretch=new View(this);
        column.addView(stretch,new LinearLayout.LayoutParams(-1,0,1));
        TextView tip=text("Long-press an app to pin or unpin it  •  Four science themes",11,MUTED,false);
        tip.setGravity(Gravity.CENTER);
        add(column,tip,12);
        buildDock(column);
    }

    private int getTileWidth(){
        return Math.max(dp(64),(getResources().getDisplayMetrics().widthPixels-dp(40))/4);
    }
    private void buildDock(LinearLayout column){
        LinearLayout dock=new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setPadding(dp(3),dp(6),dp(3),dp(6));
        dock.setBackground(panel(0xdd18396d,0xdd06172e,22,0xff318aca));
        String[] labels={"☎","✉","🌐","▦","⚙"};
        String[] captions={"Phone","Messages","Browser","Apps","Theme"};
        for(int i=0;i<labels.length;i++){
            final int idx=i;
            LinearLayout item=new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView glyph=text(labels[i],24,i==4?GOLD:CYAN,true);
            glyph.setGravity(Gravity.CENTER);
            item.addView(glyph);
            TextView cap=text(captions[i],10,WHITE,false);
            cap.setGravity(Gravity.CENTER);
            item.addView(cap);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(66),1);
            dock.addView(item,lp);
            item.setContentDescription("Open "+captions[i]);
            item.setOnClickListener(v->{
                switch(idx){
                    case 0:external(new Intent(Intent.ACTION_DIAL));break;
                    case 1:external(new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:")));break;
                    case 2:external(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com")));break;
                    case 3:openDrawer("");break;
                    case 4:showThemes();break;
                    default:break;
                }
            });
        }
        add(column,dock,13);
    }

    private void buildDrawer(LinearLayout column){
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=control("‹  HOME");
        top.addView(back,new LinearLayout.LayoutParams(dp(93),dp(44)));
        back.setOnClickListener(v->{drawerOpen=false;buildUi();});
        drawerTitle=text("ALL APPS",21,WHITE,true);
        drawerTitle.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        top.addView(drawerTitle,new LinearLayout.LayoutParams(0,dp(44),1));
        add(column,top,4);

        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(17);
        input.setTextColor(0xff0a3049);
        input.setHintTextColor(0xff607c94);
        input.setHint("Search installed apps…");
        input.setPadding(dp(18),dp(7),dp(15),dp(7));
        input.setBackground(panel(0xffe7f9ff,0xffb9ddea,17,0xff4adaf5));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        add(column,input,18);
        TextView caption=text("Tap to open  •  Long-press to add/remove Favorites",
            12,MUTED,false);
        add(column,caption,11);

        drawerGrid=new GridLayout(this);
        drawerGrid.setColumnCount(4);
        drawerGrid.setPadding(dp(3),dp(8),dp(3),dp(8));
        if(lightTheme())drawerGrid.setBackground(
            panel(0xf9ffffff,0xebf6fffe,18,0xffbedcf3));
        add(column,drawerGrid,15);
        if(appsLoaded)renderAppGrid("");
        else drawerTitle.setText("LOADING APPS…");

        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence x,int st,int count,int after){}
            public void onTextChanged(CharSequence value,int st,int before,int count){
                renderAppGrid(value.toString());
            }
            public void afterTextChanged(Editable x){}
        });
    }

    private void openDrawer(String initialQuery){
        drawerOpen=true;buildUi();
        // Users can tap search and type; don't force-open the IME each time.
    }
    private void renderAppGrid(String filter){
        if(!drawerOpen||drawerGrid==null||!appsLoaded)return;
        drawerGrid.removeAllViews();
        String query=filter.toLowerCase(Locale.ROOT).trim();
        int found=0;
        int width=getTileWidth();
        for(AppItem app:apps){
            if(!query.isEmpty()&&!app.label.toLowerCase(Locale.ROOT).contains(query)
                    &&!app.packageName.toLowerCase(Locale.ROOT).contains(query))continue;
            drawerGrid.addView(appTile(app,width));
            found++;
        }
        drawerTitle.setText("ALL APPS  •  "+found);
        if(found==0){
            TextView no=text("No matching applications.",14,MUTED,false);
            drawerGrid.addView(no);
        }
    }

    private View appTile(AppItem item,int tileWidth){
        LinearLayout tile=new LinearLayout(this);
        tile.setGravity(Gravity.CENTER);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(3),dp(9),dp(3),dp(9));
        if(lightTheme())tile.setBackground(
            panel(0xfff4faff,0xffe5f1fb,14,0xffd7e7f5));
        ImageView icon=new ImageView(this);
        icon.setImageDrawable(item.icon);
        icon.setContentDescription(item.label+" app icon");
        tile.addView(icon,new LinearLayout.LayoutParams(dp(45),dp(45)));
        TextView title=text(item.label,11,WHITE,false);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams textParams=new LinearLayout.LayoutParams(-1,dp(35));
        textParams.topMargin=dp(6);
        tile.addView(title,textParams);
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
        lp.width=Math.max(dp(63),tileWidth-dp(3));
        lp.height=dp(103);
        lp.setMargins(0,dp(3),0,dp(3));
        tile.setLayoutParams(lp);
        tile.setContentDescription("Open "+item.label+"; long press to pin favorite");
        tile.setOnClickListener(v->openApp(item));
        tile.setOnLongClickListener(v->{toggleFavorite(item);return true;});
        return tile;
    }

    private List<AppItem> homeApps(){
        ArrayList<AppItem> selected=new ArrayList<>();
        Set<String> pinned=prefs.getStringSet(PREF_FAVORITES,new HashSet<String>());
        for(AppItem item:apps)if(pinned.contains(item.id))selected.add(item);
        // On first launch show real apps instead of a blank grid.
        String[] preferred={"calculator","camera","chrome","browser","youtube",
            "settings","gallery","photos","calendar","clock","messages","files"};
        for(String name:preferred){
            for(AppItem item:apps){
                if(selected.size()>=8)break;
                if(item.label.toLowerCase(Locale.ROOT).contains(name)&&!selected.contains(item))
                    selected.add(item);
            }
        }
        for(AppItem item:apps){
            if(selected.size()>=8)break;
            if(!selected.contains(item))selected.add(item);
        }
        return selected;
    }
    private void toggleFavorite(AppItem item){
        Set<String> current=new HashSet<>(prefs.getStringSet(
            PREF_FAVORITES,new HashSet<String>()));
        boolean added;
        if(current.contains(item.id)){current.remove(item.id);added=false;}
        else {current.add(item.id);added=true;}
        prefs.edit().putStringSet(PREF_FAVORITES,current).apply();
        toast(item.label+(added?" added to Favorites":" removed from Favorites"));
    }
    private void openApp(AppItem app){
        try{
            Intent launch=new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(app.packageName,app.activityName));
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);
        }catch(Exception e){
            toast("Could not open "+app.label+". Try another app.");
            refreshApps();
        }
    }
    private void refreshApps(){
        if(refreshRunning)return;
        refreshRunning=true;
        new Thread(()->{
            ArrayList<AppItem> fresh=new ArrayList<>();
            try{
                PackageManager pm=getPackageManager();
                Intent query=new Intent(Intent.ACTION_MAIN);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> found=pm.queryIntentActivities(query,0);
                Set<String> seen=new HashSet<>();
                for(ResolveInfo info:found){
                    if(info.activityInfo==null)continue;
                    if(getPackageName().equals(info.activityInfo.packageName))continue;
                    String id=new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name).flattenToString();
                    if(!seen.add(id))continue;
                    String label=String.valueOf(info.loadLabel(pm));
                    fresh.add(new AppItem(label,info,pm));
                }
                fresh.sort(Comparator.comparing(x->x.label.toLowerCase(Locale.ROOT)));
            }catch(Exception ignored){}
            runOnUiThread(()->{
                apps.clear();apps.addAll(fresh);
                appsLoaded=true;refreshRunning=false;
                // Preserve drawer context if apps finish loading during search.
                buildUi();
            });
        },"SVision-AppDiscovery").start();
    }

    private void updateClock(){
        Date now=new Date();
        if(clock!=null){
            clock.setText(new SimpleDateFormat("h:mm",Locale.getDefault()).format(now));
        }
        if(date!=null){
            date.setText(new SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(now));
        }
        if(battery!=null){
            Intent status=registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(status!=null){
                int charge=status.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale=status.getIntExtra(BatteryManager.EXTRA_SCALE,100);
                if(charge>=0&&scale>0)
                    battery.setText("⚡  BATTERY "+charge*100/scale+"%    •    OFFLINE LAUNCHER");
            }else battery.setText("SCIENCE AT YOUR FINGERTIPS");
        }
    }

    private void showThemes(){
        int selected=LauncherRules.themeIndex(prefs.getInt(PREF_THEME,4),
            ScienceWallpaperView.THEMES.length);
        new AlertDialog.Builder(this)
            .setTitle("🎨 Science & Maths Themes")
            .setSingleChoiceItems(ScienceWallpaperView.THEMES,selected,(dialog,which)->{
                prefs.edit().putInt(PREF_THEME,which).apply();
                buildUi();
                toast(ScienceWallpaperView.THEMES[which]+" selected");
                dialog.dismiss();
            }).setNegativeButton("Close",null).show();
    }
    private void showLearningLab(int index){
        final String message=index==0?
            "π = 3.141592653589793\n\n"+
            "Pythagoras: a² + b² = c²\n\n"+
            "Quadratic formula: x = (−b ± √(b²−4ac)) / 2a\n\n"+
            "Circle area: A = πr²\n\n"+
            "Trigonometry: sin²θ + cos²θ = 1":
            "Newton's second law: F = ma\n\n"+
            "Energy: E = mc²\n\n"+
            "Ohm's law: V = IR\n\n"+
            "Water molecule: H₂O\n\n"+
            "DNA stores genetic information.";
        new AlertDialog.Builder(this)
            .setTitle(index==0?"∑ Maths Lab":"⚛ Science Lab")
            .setMessage(message)
            .setPositiveButton("Explore installed apps",(dialog,which)->openDrawer(""))
            .setNegativeButton("Close",null).show();
    }
    private void showSettings(){
        String[] options={
            "Choose Maths & Science Theme",
            "Set as Default Home Launcher",
            "Refresh Installed Apps",
            "Open Android Settings",
            "Privacy and About"
        };
        new AlertDialog.Builder(this)
            .setTitle("S VISION Launcher Settings")
            .setItems(options,(dialog,which)->{
                switch(which){
                    case 0:showThemes();break;
                    case 1:requestDefaultHome();break;
                    case 2:refreshApps();toast("Refreshing apps");break;
                    case 3:external(new Intent(Settings.ACTION_SETTINGS));break;
                    case 4:new AlertDialog.Builder(this)
                        .setTitle("About S VISION Launcher")
                        .setMessage("Maths & Science Launcher by SAURABH'S VISION™.\n"+
                            "Functional Home and App Drawer, app search, pinned favorites, "+
                            "five offline science themes including Pearl Light, clock, and Maths/Science cards.\n\n"+
                            "No Internet permission, no ads, no login or data collection. "+
                            "Favorite shortcuts and the selected theme remain only on your phone.\n\n"+
                            "This is an initial launcher release, not a promise of "+
                            "lock-screen controls, live wallpapers or third-party widgets.")
                        .setPositiveButton("OK",null).show();break;
                    default:break;
                }
            }).show();
    }
    private void requestDefaultHome(){
        try{
            if(Build.VERSION.SDK_INT>=29){
                RoleManager role=(RoleManager)getSystemService(Context.ROLE_SERVICE);
                if(role!=null && role.isRoleAvailable(RoleManager.ROLE_HOME)){
                    if(role.isRoleHeld(RoleManager.ROLE_HOME)){
                        toast("S VISION Launcher is already your default Home");
                        return;
                    }
                    startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME),42);
                    return;
                }
            }
            external(new Intent(Settings.ACTION_HOME_SETTINGS));
        }catch(Exception e){
            toast("Open Settings → Default apps → Home app to choose S VISION.");
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==42){
            toast(result==RESULT_OK?
                "S VISION is now your default Home launcher":
                "You can choose S VISION from Android Default apps.");
        }
    }
    private void external(Intent intent){
        try{startActivity(intent);}
        catch(Exception e){toast("No compatible app found on this device.");}
    }
}
