package com.saurabhsvision.caller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SV CALLER PRO — premium local-only dialer and phone app.
 * Actual in-call controls work through SVInCallService once selected by user
 * as Android's default Dialer. Never sends contacts/call data over Internet.
 */
public final class MainActivity extends Activity {
    private static final int NAVY=0xff070b1d,CYAN=0xff52e5fd,WHITE=0xfff7f8ff,
        GOLD=0xffefce82,MUTED=0xffa5b5d4;
    private static final int VIOLET=0xffa57aff,GLASS=0xff172646;
    private static final int REQUEST_ROLE=301,REQUEST_PHONE=302,
        REQUEST_CONTACTS=303,REQUEST_LOG=304,REQUEST_IMAGE=305,
        REQUEST_NOTIFICATIONS=306;
    private static final String[] TABS={"DIALPAD","CONTACTS","RECENTS","FAVORITES"};
    private SharedPreferences prefs;
    private int tab=0,theme=0;
    private boolean homeOpen=true;
    private boolean settingsOpen=false;
    private boolean contactDetailOpen=false;
    private String detailName="",detailNumber="";
    private final Handler searchHandler=new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private long lastHistoryLoadAt=0;
    private int recentFilter=0;
    private String dialText="", photoTarget="",pendingDial="";
    private LinearLayout root,panel;
    private ScrollView scroll;
    private EditText numberInput,searchInput;
    private TextView roleIndicator;
    private ArrayList<Person> contacts=new ArrayList<>();
    private ArrayList<Recent> recentCalls=new ArrayList<>();
    private boolean contactsLoading=false,recentsLoading=false;
    private boolean contactDataLoaded=false,recentDataLoaded=false;
    private String permissionSnapshot="";

    private static final class Person {
        final String name,number,searchName,searchDigits;
        Person(String name,String number){
            this.name=name;
            this.number=number;
            this.searchName=name==null?"":name.toLowerCase(Locale.ROOT);
            this.searchDigits=CallerNumberKey.normalized(number).replaceAll("[^0-9]","");
        }
    }
    private static final class Recent {
        final String number,name;
        final int type,duration;
        final long date;
        Recent(String number,String name,int type,int duration,long date){
            this.number=number;this.name=name;this.type=type;
            this.duration=duration;this.date=date;
        }
    }

    private int dp(float v){
        return (int)(v*getResources().getDisplayMetrics().density+.5f);
    }
    private GradientDrawable gradient(int top,int bottom,int radius,int border){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{top,bottom});
        d.setCornerRadius(dp(radius));
        if(border!=0)d.setStroke(dp(1),border);
        return d;
    }
    private TextView text(String value,float size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);t.setTextColor(color);t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return t;
    }
    private void add(LinearLayout col,View view,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(top);
        col.addView(view,p);
    }
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_SHORT).show();}
    private boolean granted(String permission){
        return checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean defaultDialer(){
        TelecomManager manager=(TelecomManager)getSystemService(Context.TELECOM_SERVICE);
        return manager!=null&&getPackageName().equals(manager.getDefaultDialerPackage());
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("caller_settings",MODE_PRIVATE);
        theme=prefs.getInt("theme",0);
        if(state!=null){
            dialText=state.getString("dialText","");
            tab=state.getInt("tab",0);
            homeOpen=state.getBoolean("homeOpen",true);
            settingsOpen=state.getBoolean("settingsOpen",false);
            contactDetailOpen=state.getBoolean("contactDetailOpen",false);
            detailName=state.getString("detailName","");
            detailNumber=state.getString("detailNumber","");
            recentFilter=state.getInt("recentFilter",0);
        }
        readDialIntent(getIntent());
        buildUi();
    }
    @Override protected void onResume(){
        super.onResume();
        // Refresh role/permission badges after Android system approval or settings.
        String snapshot=permissionsSnapshot();
        if(!snapshot.equals(permissionSnapshot)){
            permissionSnapshot=snapshot;
            if(root!=null)buildUi();
        }else if(tab==2&&!homeOpen&&recentDataLoaded
            &&System.currentTimeMillis()-lastHistoryLoadAt>15000){
            recentDataLoaded=false;
            if(root!=null)buildUi();
        }
    }
    private String permissionsSnapshot(){
        return defaultDialer()+"|"+
            granted(Manifest.permission.CALL_PHONE)+"|"+
            granted(Manifest.permission.READ_CONTACTS)+"|"+
            granted(Manifest.permission.READ_CALL_LOG)+"|"+
            (Build.VERSION.SDK_INT<33 || granted(Manifest.permission.POST_NOTIFICATIONS));
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        readDialIntent(intent);
        tab=0;homeOpen=false;buildUi();
    }
    private void readDialIntent(Intent intent){
        if(intent==null)return;
        Uri uri=intent.getData();
        if(Intent.ACTION_DIAL.equals(intent.getAction())&&uri!=null){
            String number=uri.getSchemeSpecificPart();
            if(number!=null&&number.length()<=80){
                dialText=number;
                homeOpen=false;
            }
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){
        if(numberInput!=null)dialText=numberInput.getText().toString();
        state.putString("dialText",dialText);
        state.putInt("tab",tab);
        state.putBoolean("homeOpen",homeOpen);
        state.putBoolean("settingsOpen",settingsOpen);
        state.putBoolean("contactDetailOpen",contactDetailOpen);
        state.putString("detailName",detailName);
        state.putString("detailNumber",detailNumber);
        state.putInt("recentFilter",recentFilter);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed(){
        if(settingsOpen||contactDetailOpen){
            settingsOpen=false;contactDetailOpen=false;buildUi();
        }else if(!homeOpen){homeOpen=true;buildUi();}
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(pendingSearch!=null)searchHandler.removeCallbacks(pendingSearch);
        super.onDestroy();
    }
    private void buildUi(){
        if(numberInput!=null)dialText=numberInput.getText().toString();
        numberInput=null;searchInput=null;
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);
        LinearLayout screen=new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff05091b,0xff141038,0xff050d23}));
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(11),dp(16),dp(25));
        scroll.addView(root);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(screen);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            root.setPadding(dp(18),Math.max(dp(10),insets.getSystemWindowInsetTop()+dp(7)),
                dp(18),dp(24));
            return insets;
        });
        buildHeader();
        panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        add(root,panel,13);
        if(settingsOpen){
            buildSettingsScreen();
        }else if(contactDetailOpen){
            buildContactDetails();
        }else if(homeOpen){
            buildHomeDashboard();
        }else{
            switch(tab){
                case 0:buildDialpad();break;
                case 1:buildContacts();break;
                case 2:buildRecents();break;
                case 3:buildFavorites();break;
            }
            buildBottomNav(screen);
        }
        permissionSnapshot=permissionsSnapshot();
    }
    private void buildHeader(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView emblem=new ImageView(this);
        emblem.setImageResource(R.drawable.ic_sv_caller_pro);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        emblem.setContentDescription("SV Caller Pro blue-gold icon. Open home.");
        emblem.setOnClickListener(v->{homeOpen=true;buildUi();});
        row.addView(emblem,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout names=new LinearLayout(this);
        names.setPadding(dp(10),0,0,0);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text("SV Caller Pro 3.2",21,WHITE,true));
        names.addView(text("SAURABH'S VISION™   •   PREMIUM PHONE",10,CYAN,true));
        row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=text("⚙",24,WHITE,true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(gradient(0xff7344bc,0xff152953,19,0xffb997f7));
        settings.setElevation(dp(5));
        settings.setContentDescription("Call and theme settings");
        row.addView(settings,new LinearLayout.LayoutParams(dp(45),dp(45)));
        settings.setOnClickListener(v->showSettings());
        add(root,row,3);

        roleIndicator=text(defaultDialer()?
            "✓  DEFAULT PHONE APP  •  CALL WALLPAPER ENABLED":
            "SET AS DEFAULT PHONE APP FOR FULL-SCREEN CALLS ›",
            11,defaultDialer()?0xffc1f9dd:0xffffdf89,true);
        roleIndicator.setGravity(Gravity.CENTER);
        roleIndicator.setPadding(dp(5),dp(9),dp(5),dp(9));
        roleIndicator.setBackground(gradient(
            defaultDialer()?0xff145650:0xff392779,
            defaultDialer()?0xff142c42:0xff1b2c60,23,
            defaultDialer()?0xff5deab7:0xff9776ed));
        add(root,roleIndicator,13);
        roleIndicator.setOnClickListener(v->showPermissionSetup());
    }

    /** A real, user-controlled permission guide. No automatic grants, no
     * misleading check marks, and every request has a Settings fallback. */
    private void showPermissionSetup(){
        String[] names={
            (defaultDialer()?"✓ ":"○ ")+"Default Phone App",
            (granted(Manifest.permission.CALL_PHONE)?"✓ ":"○ ")+"Phone Calls",
            (granted(Manifest.permission.READ_CONTACTS)?"✓ ":"○ ")+"Contacts",
            ((defaultDialer()&&granted(Manifest.permission.READ_CALL_LOG))?
                "✓ ":"○ ")+"Call History",
            ((Build.VERSION.SDK_INT<33||granted(
                Manifest.permission.POST_NOTIFICATIONS))?"✓ ":"○ ")+"Call Notifications",
            "⚙ Open Default Apps Settings",
            "⚙ Open SV Caller App Permissions",
            "◉ Full-Screen Call Alerts"
        };
        new AlertDialog.Builder(this).setTitle("SV CALLER • PERMISSION SETUP")
            .setMessage("Tap any item to open its real Android permission screen. "+
                "For a full-screen incoming call, this app must first be selected "+
                "as the Default Phone App. Your existing phone app is not changed "+
                "without your approval.")
            .setItems(names,(dialog,which)->{
                switch(which){
                    case 0:
                        if(!defaultDialer())requestDialerRole();
                        else toast("Already the default Phone app");
                        break;
                    case 1:requestPermissionOrExplain(
                        Manifest.permission.CALL_PHONE,REQUEST_PHONE,
                        "Phone Calls");break;
                    case 2:requestPermissionOrExplain(
                        Manifest.permission.READ_CONTACTS,REQUEST_CONTACTS,
                        "Contacts");break;
                    case 3:
                        if(!defaultDialer()){
                            new AlertDialog.Builder(this).setTitle("Call History")
                                .setMessage("Choose SV Caller Pro as your default "+
                                    "Phone app first, then allow local Call History.")
                                .setPositiveButton("Choose Phone App",(d,w)->requestDialerRole())
                                .setNegativeButton("Cancel",null).show();
                        }else requestPermissionOrExplain(
                            Manifest.permission.READ_CALL_LOG,REQUEST_LOG,
                            "Call History");
                        break;
                    case 4:requestNotificationPermission();break;
                    case 5:openDefaultPhoneSettings();break;
                    case 6:openAppPermissionSettings();break;
                    case 7:openFullScreenSettings();break;
                    default:break;
                }
            }).setPositiveButton("DONE",null).show();
    }
    private void requestPermissionOrExplain(String permission,int request,String label){
        if(granted(permission)){toast(label+" permission already allowed");return;}
        if(shouldShowRequestPermissionRationale(permission)){
            new AlertDialog.Builder(this).setTitle("Allow "+label)
                .setMessage("SV Caller uses this permission only for its "+
                    "phone features. You can change it in Android Settings.")
                .setPositiveButton("Continue",(d,w)->requestPermissions(
                    new String[]{permission},request))
                .setNeutralButton("Settings",(d,w)->openAppPermissionSettings())
                .setNegativeButton("Cancel",null).show();
        }else requestPermissions(new String[]{permission},request);
    }
    private void openAppPermissionSettings(){
        try{
            Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:"+getPackageName()));
            startActivity(intent);
        }catch(Exception ex){
            toast("Go to Android Settings → Apps → SV Caller Pro → Permissions");
        }
    }
    private void openDefaultPhoneSettings(){
        try{
            startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_SETTINGS));}
            catch(Exception ignored){toast("Open Settings → Default Apps → Phone");}
        }
    }
    /** Opens a real app tab. Home tiles never simulate unsupported features. */
    private void openTab(int target){homeOpen=false;settingsOpen=false;contactDetailOpen=false;tab=target;buildUi();}
    private void dashboardTile(LinearLayout row,String icon,String label,
        String subtitle,Runnable action,boolean purple){
        LinearLayout tile=new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6),dp(10),dp(6),dp(10));
        tile.setBackground(gradient(purple?0xff47207d:0xff0b4777,
            purple?0xff15163a:0xff0c1937,24,purple?0xffbb66f3:0xff43b9fa));
        tile.setElevation(dp(4));
        TextView symbol=text(icon,27,purple?0xffe2acff:0xff9cecff,true);
        symbol.setGravity(Gravity.CENTER);
        tile.addView(symbol);
        TextView title=text(label,15,WHITE,true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);
        tp.topMargin=dp(5);tile.addView(title,tp);
        TextView detail=text(subtitle,10,MUTED,false);
        detail.setGravity(Gravity.CENTER);tile.addView(detail);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(108),1);
        p.setMargins(dp(4),dp(5),dp(4),dp(5));
        row.addView(tile,p);
        tile.setContentDescription(label+" - "+subtitle);
        tile.setOnClickListener(v->action.run());
    }
    /** Premium home screen; features link only to functions actually implemented. */
    private void buildHomeDashboard(){
        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20),dp(23),dp(20),dp(21));
        hero.setBackground(gradient(0xff005d9c,0xff7a169c,28,0xff96a4ff));
        hero.setElevation(dp(6));
        add(hero,text("✦  PREMIUM PHONE EXPERIENCE",11,0xffe6f7ff,true),0);
        add(hero,text("Your calls. Your style.",25,WHITE,true),9);
        add(hero,text("Full-screen caller photos  •  Fast dialpad  •  Offline privacy",
            13,0xffecedff,false),8);
        TextView action=text("☎   OPEN DIALPAD    ›",15,WHITE,true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(gradient(0xff264fcb,0xffa424c5,24,0xffaba2ff));
        LinearLayout.LayoutParams actionLp=new LinearLayout.LayoutParams(-1,dp(51));
        actionLp.topMargin=dp(18);hero.addView(action,actionLp);
        action.setOnClickListener(v->openTab(0));
        add(panel,hero,9);
        add(panel,text("✦  QUICK ACCESS",13,0xffd7bfff,true),19);
        String[][] labels={{"▦","Dialpad","Make a call"},
            {"♙","Contacts","Find a person"},
            {"◷","Recents","Local call history"},
            {"★","Favorites","Saved numbers"},
            {"▧","Caller Photos","HD wallpapers"},
            {"◈","Themes","Six color styles"},
            {"⚙","Settings","Customize app"},
            {"✓","Permissions","Phone setup"}};
        Runnable[] actions={
            ()->openTab(0),()->openTab(1),()->openTab(2),()->openTab(3),
            ()->{if(dialText.trim().isEmpty()){
                openTab(0);toast("Enter a number then tap HD Photo");
            }else showPhotoOptions(dialText);},
            this::showThemes,this::showSettings,this::showPermissionSetup
        };
        for(int i=0;i<labels.length;i+=2){
            LinearLayout row=new LinearLayout(this);
            for(int col=0;col<2;col++){
                int index=i+col;
                dashboardTile(row,labels[index][0],labels[index][1],
                    labels[index][2],actions[index],(index%2)==1);
            }
            add(panel,row,3);
        }
        TextView footer=text(
            "PRIVATE PHONE  •  LOCAL CONTACTS  •  NO ADS",
            10,CYAN,true);
        footer.setGravity(Gravity.CENTER);
        add(panel,footer,18);
    }

    /** Thumb-friendly fixed dock; the contacts and dialpad remain scrollable. */
    private void buildBottomNav(LinearLayout screen){
        LinearLayout dock=new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(6),dp(6),dp(6),dp(6));
        dock.setBackground(gradient(0xff242451,0xff07132d,29,0xff5e68ae));
        dock.setElevation(dp(10));
        String[] icons={"▦","♙","◷","★"};
        String[] names={"Keypad","Contacts","Recents","Favorites"};
        for(int i=0;i<4;i++){
            final int index=i;
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            if(i==tab)item.setBackground(gradient(0xffaa28eb,0xff353ad8,24,0xffc698ff));
            TextView icon=text(icons[i],22,i==tab?WHITE:MUTED,true);
            icon.setGravity(Gravity.CENTER);
            item.addView(icon,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView label=text(names[i],11,i==tab?WHITE:0xffb3bed5,true);
            label.setGravity(Gravity.CENTER);
            item.addView(label,new LinearLayout.LayoutParams(-1,dp(22)));
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,dp(62),1);
            size.setMargins(dp(2),0,dp(2),0);
            dock.addView(item,size);
            item.setContentDescription("Open "+names[i]);
            item.setOnClickListener(v->{if(tab!=index){tab=index;homeOpen=false;buildUi();}});
        }
        LinearLayout.LayoutParams dockP=new LinearLayout.LayoutParams(-1,dp(74));
        dockP.setMargins(dp(8),dp(2),dp(8),dp(8));
        screen.addView(dock,dockP);
    }
    private void buildDialpad(){
        LinearLayout face=new LinearLayout(this);
        face.setOrientation(LinearLayout.VERTICAL);
        face.setPadding(dp(18),dp(15),dp(18),dp(16));
        face.setBackground(gradient(0xff20366d,0xff111b40,29,0xff5983d9));
        face.setElevation(dp(3));
        TextView label=text("✦  HD CALLER ID   •   SMART KEYPAD",11,CYAN,true);
        add(face,label,0);
        numberInput=new EditText(this);
        numberInput.setSingleLine(true);
        numberInput.setText(dialText);
        numberInput.setSelection(numberInput.length());
        numberInput.setTextColor(WHITE);
        numberInput.setHintTextColor(MUTED);
        numberInput.setTextSize(32);
        numberInput.setHint("Enter a number");
        numberInput.setGravity(Gravity.CENTER);
        numberInput.setInputType(InputType.TYPE_CLASS_PHONE);
        numberInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        numberInput.setShowSoftInputOnFocus(false);
        numberInput.setBackgroundColor(Color.TRANSPARENT);
        add(face,numberInput,6);
        numberInput.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){dialText=s.toString();}
            public void afterTextChanged(Editable e){}
        });
        LinearLayout small=new LinearLayout(this);
        TextView clear=smallButton("⌫  Delete");
        TextView photo=smallButton("▧  FULL PHOTO");
        small.addView(clear,new LinearLayout.LayoutParams(0,dp(40),1));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(40),1);
        pp.leftMargin=dp(8);
        small.addView(photo,pp);
        clear.setOnClickListener(v->{
            if(numberInput.length()>0){
                int end=numberInput.length();
                numberInput.getText().delete(end-1,end);
            }
        });
        clear.setOnLongClickListener(v->{numberInput.setText("");return true;});
        photo.setOnClickListener(v->showPhotoOptions(numberInput.getText().toString()));
        add(face,small,8);
        add(panel,face,0);

        // Premium editorial banner: explanatory design only, not a fake service.
        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18),dp(15),dp(18),dp(15));
        hero.setBackground(gradient(0xff072f83,0xff7e159a,26,0xff8d77fa));
        hero.setElevation(dp(5));
        TextView heroTitle=text("✦  FULL SCREEN. FULL PERSONALITY.",15,WHITE,true);
        add(hero,heroTitle,0);
        TextView heroDesc=text("Personal caller wallpapers • Six themes • Private on your phone",
            12,0xffe0e9ff,false);
        add(hero,heroDesc,5);
        TextView heroAction=text("CUSTOMIZE CALLER PHOTO  ›",11,0xffb2ffeb,true);
        heroAction.setPadding(0,dp(10),0,0);
        add(hero,heroAction,0);
        hero.setOnClickListener(v->showPhotoOptions(numberInput.getText().toString()));
        add(panel,hero,13);

        LinearLayout guide=new LinearLayout(this);
        guide.setGravity(Gravity.CENTER);
        TextView help=text("LONG-PRESS 0 FOR +   •   SECURE ON-DEVICE CALLING",11,MUTED,false);
        help.setGravity(Gravity.CENTER);
        guide.addView(help);
        add(panel,guide,11);

        String[][] keypad={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","#"}};
        String[][] captions={{"","ABC","DEF"},{"GHI","JKL","MNO"},
            {"PQRS","TUV","WXYZ"},{"","+",""}};
        for(int r=0;r<4;r++){
            LinearLayout row=new LinearLayout(this);
            for(int c=0;c<3;c++){
                String key=keypad[r][c],sub=captions[r][c];
                FrameLayout holder=new FrameLayout(this);
                LinearLayout circle=new LinearLayout(this);
                circle.setGravity(Gravity.CENTER);
                circle.setOrientation(LinearLayout.VERTICAL);
                circle.setBackground(gradient(0xff313064,0xff071637,50,
                    (r+c)%2==0?0xff9e61e9:0xff498fec));
                circle.setElevation(dp(4));
                TextView digit=text(key,30,WHITE,true);
                digit.setGravity(Gravity.CENTER);
                circle.addView(digit);
                TextView detail=text(sub,10,0xff87dffa,true);
                detail.setGravity(Gravity.CENTER);
                circle.addView(detail);
                holder.addView(circle,new FrameLayout.LayoutParams(dp(78),dp(78),Gravity.CENTER));
                LinearLayout.LayoutParams tileSize=new LinearLayout.LayoutParams(0,dp(84),1);
                tileSize.setMargins(dp(2),dp(1),dp(2),dp(1));
                row.addView(holder,tileSize);
                circle.setContentDescription("Dial "+key);
                circle.setOnClickListener(v->{
                    numberInput.append(key);
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    circle.animate().scaleX(.92f).scaleY(.92f).setDuration(65)
                        .withEndAction(()->circle.animate().scaleX(1f).scaleY(1f)
                        .setDuration(110).start()).start();
                });
                if(key.equals("0")){
                    circle.setOnLongClickListener(v->{numberInput.append("+");return true;});
                }
            }
            add(panel,row,2);
        }
        LinearLayout callStack=new LinearLayout(this);
        callStack.setOrientation(LinearLayout.VERTICAL);
        callStack.setGravity(Gravity.CENTER);
        TextView call=text("☎",32,Color.WHITE,true);
        call.setGravity(Gravity.CENTER);
        call.setBackground(gradient(0xff18f55a,0xff0ba94c,48,0xffa3ffbb));
        call.setElevation(dp(7));
        callStack.addView(call,new LinearLayout.LayoutParams(dp(83),dp(83)));
        TextView callLabel=text("CALL NOW",13,0xff95ffd4,true);
        callLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelP=new LinearLayout.LayoutParams(-1,dp(27));
        labelP.topMargin=dp(5);
        callStack.addView(callLabel,labelP);
        LinearLayout.LayoutParams callP=new LinearLayout.LayoutParams(-1,dp(124));
        callP.topMargin=dp(7);
        panel.addView(callStack,callP);
        call.setContentDescription("Call typed phone number");
        call.setOnClickListener(v->callNumber(numberInput.getText().toString()));
        TextView foot=text("✧  BEAUTIFUL CALLS. PRIVATE BY DESIGN.  ✧",11,GOLD,true);
        foot.setGravity(Gravity.CENTER);
        add(panel,foot,0);
        buildDialpadExtras();
    }

    /** Fills the formerly empty lower screen with ACTUAL working features.
     * The user may scroll on compact displays instead of cutting off keys.
     * No mock video calling, recording or spam-blocking icons.
     */
    private void buildDialpadExtras(){
        int allowed=0;
        if(defaultDialer())allowed++;
        if(granted(Manifest.permission.CALL_PHONE))allowed++;
        if(granted(Manifest.permission.READ_CONTACTS))allowed++;
        if(Build.VERSION.SDK_INT<33||
            granted(Manifest.permission.POST_NOTIFICATIONS))allowed++;
        if(allowed<4){
            LinearLayout box=new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(16),dp(14),dp(16),dp(14));
            box.setBackground(gradient(0xff363071,0xff182144,23,0xff9764e0));
            add(box,text("◈  FINISH PHONE SETUP   •   "+allowed+"/4 READY",
                12,GOLD,true),0);
            TextView setup=smallButton("Allow permissions and default Phone app  ›");
            setup.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(46));
            sp.topMargin=dp(10);
            box.addView(setup,sp);
            setup.setOnClickListener(v->showPermissionSetup());
            add(panel,box,10);
        }
        TextView head=text("✦   CUSTOMIZE YOUR CALLS   ✦",12,0xffd6c4ff,true);
        head.setGravity(Gravity.CENTER);
        add(panel,head,18);
        String[] names={"▧  Caller photos","◈  Themes",
            "⚙  Permissions","☎  Phone app"};
        int[] tools={3,4,5,6};
        for(int row=0;row<2;row++){
            LinearLayout strip=new LinearLayout(this);
            for(int col=0;col<2;col++){
                final int index=row*2+col;
                TextView tile=text(names[index],13,WHITE,true);
                tile.setGravity(Gravity.CENTER);
                tile.setBackground(gradient(index%2==0?0xff153f67:0xff423578,
                    0xff121b41,23,index%2==0?0xff4489c8:0xffa27ee5));
                tile.setElevation(dp(3));
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(61),1);
                tp.setMargins(dp(4),dp(4),dp(4),dp(4));
                strip.addView(tile,tp);
                tile.setOnClickListener(v->runQuickTool(tools[index]));
            }
            add(panel,strip,4);
        }
        TextView footer=text("SV CALLER PRO 3.2   •   SAURABH'S VISION™",11,CYAN,true);
        footer.setGravity(Gravity.CENTER);
        add(panel,footer,14);
    }
    private void runQuickTool(int action){
        switch(action){
            case 0:tab=1;buildUi();break;
            case 1:tab=2;buildUi();break;
            case 2:tab=3;buildUi();break;
            case 3:showPhotoOptions(dialText);break;
            case 4:showThemes();break;
            case 5:showPermissionSetup();break;
            case 6:openDefaultPhoneSettings();break;
            case 7:new AlertDialog.Builder(this)
                .setTitle("SV CALLER PRO • HELP")
                .setMessage("Tap FIX PERMISSIONS to select SV Caller Pro "+
                    "as your Default Phone app and allow needed permissions. "+
                    "You can set a separate caller photo with PHOTO. "+
                    "Incoming full-screen alerts may require permission "+
                    "from your Android device. Other Phone apps can be "+
                    "restored in Android Default Apps settings.")
                .setPositiveButton("OK",null).show();break;
            default:break;
        }
    }
    private TextView smallButton(String title){
        TextView t=text(title,12,WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(gradient(0xff353a7e,0xff0c1e46,22,0xff8587d5));
        return t;
    }
    private EditText addSearch(LinearLayout target,String hint,Runnable rerender){
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setHint(hint);
        search.setTextSize(17);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setPadding(dp(17),dp(6),dp(15),dp(6));
        search.setBackground(gradient(0xff293463,0xff0c1738,25,0xff8775cb));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));
        p.bottomMargin=dp(14);
        target.addView(search,p);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                if(pendingSearch!=null)searchHandler.removeCallbacks(pendingSearch);
                pendingSearch=()->{if(!isFinishing()&&!homeOpen)rerender.run();};
                searchHandler.postDelayed(pendingSearch,125);
            }
            public void afterTextChanged(Editable s){}
        });
        searchInput=search;
        return search;
    }
    private boolean requestContactsPermission(){
        if(granted(Manifest.permission.READ_CONTACTS))return true;
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},REQUEST_CONTACTS);
        return false;
    }
    private void buildContacts(){
        if(!granted(Manifest.permission.READ_CONTACTS)){
            permissionCard("CONTACTS ACCESS","To show contact names and full-screen photos, "+
                "allow access to your phone contacts. Your contacts stay on this device.",
                ()->requestContactsPermission());
            return;
        }
        addSearch(panel,"Search name or number",this::renderContacts);
        if(!contactDataLoaded){loadContacts();return;}
        renderContacts();
    }
    private void buildRecents(){
        if(!defaultDialer()){
            permissionCard("DEFAULT PHONE ROLE",
                "Set SV Caller Pro as your default Phone app to see calls and handle "+
                    "full-screen incoming phone calls.",
                this::requestDialerRole);
            return;
        }
        if(!granted(Manifest.permission.READ_CALL_LOG)){
            permissionCard("CALL HISTORY ACCESS",
                "Allow call log permission to show local recent phone calls. "+
                    "No history is uploaded or shared.",
                ()->requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},REQUEST_LOG));
            return;
        }
        if(!recentDataLoaded){loadRecents();return;}
        renderRecents();
    }
    private void buildFavorites(){
        if(!granted(Manifest.permission.READ_CONTACTS)){
            permissionCard("FAVORITES & CONTACTS",
                "Allow contacts access to see names and add contact wallpaper photos.",
                ()->requestContactsPermission());
            return;
        }
        addSearch(panel,"Search favorites",this::renderFavorites);
        if(!contactDataLoaded){loadContacts();return;}
        renderFavorites();
    }
    private void permissionCard(String title,String message,Runnable action){
        LinearLayout box=new LinearLayout(this);
        box.setPadding(dp(19),dp(19),dp(19),dp(19));
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(gradient(0xff173f64,0xff0a2446,20,0xff52bed8));
        add(box,text(title,19,GOLD,true),0);
        TextView help=text(message,15,WHITE,false);
        help.setPadding(0,dp(10),0,dp(16));
        add(box,help,0);
        TextView button=smallButton("GRANT / OPEN SETTINGS");
        button.setGravity(Gravity.CENTER);
        box.addView(button,new LinearLayout.LayoutParams(-1,dp(48)));
        button.setOnClickListener(v->action.run());
        add(panel,box,8);
    }
    private void loadContacts(){
        if(contactsLoading)return;
        contactsLoading=true;
        add(panel,text("Loading contacts from your device…",14,MUTED,false),8);
        new Thread(()->{
            ArrayList<Person> items=new ArrayList<>();
            Set<String> seen=new HashSet<>();
            String[] cols={
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            try(Cursor c=getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,cols,
                null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")){
                if(c!=null){
                    while(c.moveToNext()&&items.size()<750){
                        String name=c.getString(0),number=c.getString(1);
                        if(TextUtils.isEmpty(number))continue;
                        String key=CallerNumberKey.normalized(number);
                        if(key.length()<2||!seen.add(key))continue;
                        items.add(new Person(TextUtils.isEmpty(name)?number:name,number));
                    }
                }
            }catch(Exception ignored){}
            runOnUiThread(()->{
                contactsLoading=false;
                contacts.clear();contacts.addAll(items);
                contactDataLoaded=true;
                if(tab==1||tab==3)buildUi();
            });
        },"SVCaller-Contacts").start();
    }
    private void loadRecents(){
        if(recentsLoading)return;
        recentsLoading=true;
        add(panel,text("Loading call history from this phone…",14,MUTED,false),8);
        new Thread(()->{
            ArrayList<Recent> items=new ArrayList<>();
            String[] columns={CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,CallLog.Calls.DURATION,CallLog.Calls.DATE};
            try(Cursor cursor=getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    columns,null,null,CallLog.Calls.DATE+" DESC")){
                if(cursor!=null){
                    while(cursor.moveToNext()&&items.size()<75){
                        String number=cursor.getString(0),name=cursor.getString(1);
                        if(number==null)number="";
                        items.add(new Recent(number,TextUtils.isEmpty(name)?number:name,
                            cursor.getInt(2),cursor.getInt(3),cursor.getLong(4)));
                    }
                }
            }catch(Exception ignored){}
            runOnUiThread(()->{
                recentsLoading=false;recentCalls.clear();recentCalls.addAll(items);
                recentDataLoaded=true;
                lastHistoryLoadAt=System.currentTimeMillis();
                if(tab==2&&!homeOpen)buildUi();
            });
        },"SVCaller-CallHistory").start();
    }
    private boolean matchesPerson(Person p,String q,String digits){
        return q.isEmpty() || p.searchName.contains(q) || p.number.contains(q)
            || (!digits.isEmpty() && p.searchDigits.contains(digits));
    }
    private void renderContacts(){
        if(panel==null||tab!=1||searchInput==null||!contactDataLoaded)return;
        String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String digits=q.replaceAll("[^0-9]","");
        // Keep search field in place, rerender only result cards.
        while(panel.getChildCount()>1)panel.removeViewAt(1);
        int count=0;
        for(Person p:contacts){
            if(!matchesPerson(p,q,digits))continue;
            showPerson(p,false);
            if(++count>=140)break;
        }
        if(count==0)add(panel,text("No matching contacts found.",14,MUTED,false),5);
    }
    private void renderFavorites(){
        if(panel==null||tab!=3||searchInput==null||!contactDataLoaded)return;
        String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String digits=q.replaceAll("[^0-9]","");
        while(panel.getChildCount()>1)panel.removeViewAt(1);
        Set<String> favorites=prefs.getStringSet("favorite_numbers",new HashSet<String>());
        int count=0;
        for(Person p:contacts){
            if(!favorites.contains(p.number))continue;
            if(!matchesPerson(p,q,digits))continue;
            showPerson(p,true);
            if(++count>=140)break;
        }
        if(count==0)add(panel,text("No favorites yet. Long-press a contact to pin it.",14,MUTED,false),5);
    }
    private void showPerson(Person person,boolean favorite){
        LinearLayout card=new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12),dp(10),dp(10),dp(10));
        card.setBackground(gradient(0xff242c60,0xff0c1537,24,0xff705db0));
        TextView badge=text(person.name.isEmpty()?"?":
            person.name.substring(0,1).toUpperCase(Locale.ROOT),
            23,GOLD,true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(gradient(0xff9c2de8,0xff174bad,44,0xffca93ff));
        card.addView(badge,new LinearLayout.LayoutParams(dp(45),dp(45)));
        LinearLayout info=new LinearLayout(this);
        info.setPadding(dp(11),0,dp(5),0);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(person.name,16,WHITE,true));
        info.addView(text(person.number,12,MUTED,false));
        card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView call=smallButton("☎");
        call.setGravity(Gravity.CENTER);
        card.addView(call,new LinearLayout.LayoutParams(dp(50),dp(44)));
        call.setOnClickListener(v->callNumber(person.number));
        card.setContentDescription("Contact "+person.name+" "+person.number);
        card.setOnClickListener(v->{
            detailName=person.name;detailNumber=person.number;
            contactDetailOpen=true;settingsOpen=false;homeOpen=false;
            buildUi();
        });
        card.setOnLongClickListener(v->{
            String[] options={
                "Choose full-screen call photo",
                "Preview full-screen photo",
                "Remove custom photo",
                favorite?"Unpin from favorites":"Add to favorites",
                "Call "+person.name
            };
            new AlertDialog.Builder(this).setTitle(person.name)
                .setItems(options,(dialog,choice)->{
                    switch(choice){
                        case 0:choosePhoto(person.number);break;
                        case 1:previewCallScreen(person.number);break;
                        case 2:removeCustomPhoto(person.number);break;
                        case 3:toggleFavorite(person.number);break;
                        case 4:callNumber(person.number);break;
                        default:break;
                    }
                }).show();
            return true;
        });
        add(panel,card,7);
    }
    private void renderRecents(){
        if(panel==null||tab!=2||!recentDataLoaded)return;
        panel.removeAllViews();
        LinearLayout filters=new LinearLayout(this);
        filters.setGravity(Gravity.CENTER);
        String[] categories={"ALL","MISSED","INCOMING","OUTGOING"};
        for(int i=0;i<categories.length;i++){
            final int selected=i;
            TextView pill=text(categories[i],11,recentFilter==i?WHITE:MUTED,true);
            pill.setGravity(Gravity.CENTER);
            pill.setBackground(gradient(
                recentFilter==i?0xff9326d8:0xff1b2a4e,
                recentFilter==i?0xff3a4ab9:0xff101831,22,
                recentFilter==i?0xffbf87ff:0xff4c5687));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
            p.setMargins(dp(2),dp(2),dp(2),dp(2));
            filters.addView(pill,p);
            pill.setOnClickListener(v->{recentFilter=selected;renderRecents();});
        }
        add(panel,filters,2);
        if(recentCalls.isEmpty()){
            add(panel,text("No call history available on this device.",15,MUTED,false),12);
            return;
        }
        SimpleDateFormat format=new SimpleDateFormat("d MMM, h:mm a",Locale.getDefault());
        int visible=0;
        for(Recent item:recentCalls){
            if(recentFilter==1&&item.type!=CallLog.Calls.MISSED_TYPE)continue;
            if(recentFilter==2&&item.type!=CallLog.Calls.INCOMING_TYPE)continue;
            if(recentFilter==3&&item.type!=CallLog.Calls.OUTGOING_TYPE)continue;
            String type=item.type==CallLog.Calls.MISSED_TYPE?"MISSED":
                item.type==CallLog.Calls.OUTGOING_TYPE?"OUTGOING":
                item.type==CallLog.Calls.INCOMING_TYPE?"INCOMING":
                item.type==CallLog.Calls.REJECTED_TYPE?"REJECTED":
                item.type==CallLog.Calls.BLOCKED_TYPE?"BLOCKED":"OTHER";
            int callColor=item.type==CallLog.Calls.MISSED_TYPE?0xffff9bb0:
                item.type==CallLog.Calls.OUTGOING_TYPE?0xff65eab3:0xffabcbff;
            LinearLayout line=new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(14),dp(10),dp(11),dp(10));
            line.setBackground(gradient(0xff292e64,0xff0e1839,25,0xff686bb6));
            TextView glyph=text(item.type==CallLog.Calls.MISSED_TYPE?"↙":
                item.type==CallLog.Calls.OUTGOING_TYPE?"↗":"↘",25,callColor,true);
            glyph.setGravity(Gravity.CENTER);
            glyph.setBackground(gradient(0xff1b305c,0xff151b3d,22,0xff5b79b3));
            line.addView(glyph,new LinearLayout.LayoutParams(dp(46),dp(46)));
            LinearLayout names=new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.setPadding(dp(11),0,0,0);
            names.addView(text(item.name,16,WHITE,true));
            names.addView(text(type+"  •  "+format.format(new Date(item.date))+
                "  •  "+item.duration+"s",11,callColor,false));
            line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            TextView dial=smallButton("☎");
            dial.setGravity(Gravity.CENTER);
            line.addView(dial,new LinearLayout.LayoutParams(dp(49),dp(43)));
            dial.setOnClickListener(v->callNumber(item.number));
            line.setOnClickListener(v->{dialText=item.number;tab=0;buildUi();});
            add(panel,line,7);
            visible++;
        }
        if(visible==0)add(panel,text("No calls in this category.",14,MUTED,false),14);
    }
    private void toggleFavorite(String number){
        Set<String> items=new HashSet<>(prefs.getStringSet(
            "favorite_numbers",new HashSet<String>()));
        if(items.contains(number)){items.remove(number);toast("Removed from favorites");}
        else {items.add(number);toast("Added to favorites");}
        prefs.edit().putStringSet("favorite_numbers",items).apply();
        if(tab==3)buildUi();
    }

    /** Preview the actual caller wallpaper UI without placing a phone call. */
    private void previewCallScreen(String number){
        Intent preview=new Intent(this,InCallActivity.class);
        preview.putExtra("previewOnly",true);
        preview.putExtra("previewNumber",number==null?"":number.trim());
        startActivity(preview);
    }
    private void removeCustomPhoto(String number){
        if(PhotoStore.chosenPhoto(this,number)==null){
            toast("No custom photo to remove");return;
        }
        new AlertDialog.Builder(this).setTitle("Remove custom wallpaper?")
            .setMessage("The original contact photo will still be available. " +
                "Only the wallpaper chosen in SV Caller will be removed.")
            .setPositiveButton("REMOVE",(d,w)->{
                PhotoStore.removePhoto(this,number);
                toast("Custom caller wallpaper removed");
            }).setNegativeButton("Cancel",null).show();
    }
    private void showPhotoOptions(String number){
        if(number==null||number.trim().isEmpty()){
            toast("Enter or select a number first");return;
        }
        String target=number.trim();
        new AlertDialog.Builder(this).setTitle("Full-screen caller photo")
            .setItems(new String[]{"Choose / replace wallpaper",
                "Preview full-screen caller screen","Remove custom wallpaper"},
                (dialog,which)->{
                    switch(which){
                        case 0:choosePhoto(target);break;
                        case 1:previewCallScreen(target);break;
                        case 2:removeCustomPhoto(target);break;
                        default:break;
                    }
                }).setNegativeButton("Cancel",null).show();
    }
    private void choosePhoto(String number){
        if(TextUtils.isEmpty(number.trim())){toast("Enter a number first");return;}
        photoTarget=number.trim();
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.setType("image/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(picker,REQUEST_IMAGE);}
        catch(Exception e){toast("No image picker available");}
    }
    private void callNumber(String raw){
        String number=raw==null?"":raw.trim();
        if(number.isEmpty()){toast("Enter a phone number");return;}
        if(!DialerRules.validNumber(number)){
            toast("Use a valid phone number");return;
        }
        pendingDial=number;
        if(!defaultDialer()){
            toast("Choose SV Caller as your default Phone app to manage calls");
            requestDialerRole();return;
        }
        if(!granted(Manifest.permission.CALL_PHONE)){
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE},REQUEST_PHONE);
            return;
        }
        TelecomManager manager=(TelecomManager)getSystemService(Context.TELECOM_SERVICE);
        if(manager==null){toast("Phone calling is unavailable on this device");return;}
        try{
            // Telecom correctly routes emergency calls to the preloaded dialer.
            manager.placeCall(Uri.fromParts("tel",number,null),new Bundle());
            recentDataLoaded=false;
            Intent call=new Intent(this,InCallActivity.class);
            call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(call);
        }catch(SecurityException error){
            toast("Phone permission is required to place calls");
        }catch(Exception error){
            toast("Call could not be started. Check SIM and default Phone settings.");
        }
    }
    private void requestDialerRole(){
        if(defaultDialer()){
            toast("SV Caller Pro is already your default Phone app");
            buildUi();return;
        }
        try{
            if(Build.VERSION.SDK_INT>=29){
                RoleManager role=(RoleManager)getSystemService(Context.ROLE_SERVICE);
                if(role!=null && role.isRoleAvailable(RoleManager.ROLE_DIALER)){
                    startActivityForResult(
                        role.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                        REQUEST_ROLE);
                    return;
                }
            }
            if(Build.VERSION.SDK_INT<29){
                Intent legacy=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                legacy.putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    getPackageName());
                startActivityForResult(legacy,REQUEST_ROLE);
                return;
            }
        }catch(Exception ignored){}
        showManualDialerChoice();
    }
    private void showManualDialerChoice(){
        new AlertDialog.Builder(this).setTitle("Select your Phone app")
            .setMessage("Android did not open the Phone-role chooser. "+
                "Open Default Apps, select Phone App, and choose SV Caller Pro "+
                "if your device lists it. If it does not appear, your "+
                "Android version or phone manufacturer may not support this "+
                "prototype as a default calling app.")
            .setPositiveButton("OPEN DEFAULT APPS",
                (d,w)->openDefaultPhoneSettings())
            .setNeutralButton("APP PERMISSIONS",
                (d,w)->openAppPermissionSettings())
            .setNegativeButton("Cancel",null).show();
    }
    private void showSettings(){
        String[] choices={"Set as Default Phone App",
            "Choose Call Screen Theme","Manage Caller Photo for Typed Number",
            "Preview Full-Screen Caller Screen",
            "Allow Contact Photos","Allow Call History",
            "Allow Call Notifications",
            "Full-Screen Call Permission","Privacy & About"};
        new AlertDialog.Builder(this).setTitle("SV CALLER PRO • SETTINGS")
            .setItems(choices,(dialog,which)->{
                switch(which){
                    case 0:requestDialerRole();break;
                    case 1:showThemes();break;
                    case 2:showPhotoOptions(dialText);break;
                    case 3:previewCallScreen(dialText);break;
                    case 4:requestContactsPermission();break;
                    case 5:
                        if(!defaultDialer())requestDialerRole();
                        else if(!granted(Manifest.permission.READ_CALL_LOG))
                            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},REQUEST_LOG);
                        else toast("Call history access enabled");
                        break;
                    case 6:requestNotificationPermission();break;
                    case 7:openFullScreenSettings();break;
                    case 8:
                        new AlertDialog.Builder(this).setTitle("Privacy & Features")
                            .setMessage("SV Caller Pro is an offline, user-selected default Phone app.\n\n"+
                                "Full-screen caller photos use your local contact image or "+
                                "a photo you select with Android's system picker. "+
                                "All custom-photo links, favorites, and settings stay on this phone.\n\n"+
                                "No contacts, call logs, or images are uploaded. "+
                                "No ads or analytics.\n\n"+
                                "Call recording, video calling, cloud spam detection, "+
                                "and automatic SIM selection are NOT included in this release.")
                            .setPositiveButton("OK",null).show();
                        break;
                    default:break;
                }
            }).show();
    }
    private void showThemes(){
        String[] choices={"Cosmic Navy & Cyan","Purple Galaxy",
            "Rose Gold","Emerald Science","Midnight Gold","Aurora Blue"};
        int initial=Math.max(0,Math.min(prefs.getInt("theme",0),choices.length-1));
        new AlertDialog.Builder(this).setTitle("Call Screen Wallpaper Theme")
            .setSingleChoiceItems(choices,initial,(dialog,which)->{
                theme=which;
                prefs.edit().putInt("theme",which).apply();
                toast(choices[which]+" selected for no-photo calls");
                dialog.dismiss();
            }).setNegativeButton("Close",null).show();
    }
    private void requestNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&!granted(Manifest.permission.POST_NOTIFICATIONS)){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_NOTIFICATIONS);
        }else toast("Call notifications ready");
    }
    private void openFullScreenSettings(){
        if(Build.VERSION.SDK_INT>=34){
            NotificationManager manager=(NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager!=null&&!manager.canUseFullScreenIntent()){
                try{
                    Intent grant=new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                    grant.setData(Uri.parse("package:"+getPackageName()));
                    startActivity(grant);return;
                }catch(Exception ignored){}
            }
            toast("Full-screen call permission is enabled or managed by Android");
        }else toast("Incoming calls use Android's high priority call notifications");
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==REQUEST_ROLE){
            if(defaultDialer()){
                toast("SV Caller Pro is now your default Phone app");
                requestNotificationPermission();
            }else showManualDialerChoice();
            buildUi();
        }else if(request==REQUEST_IMAGE&&result==RESULT_OK&&data!=null){
            Uri uri=data.getData();
            if(uri!=null&&!photoTarget.isEmpty()){
                try{getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);}
                catch(SecurityException ignored){}
                PhotoStore.savePhoto(this,photoTarget,uri);
                toast("Full-screen caller photo saved for "+photoTarget);
            }
        }
    }
    @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(req,permissions,grants);
        boolean approved=grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED;
        if(req==REQUEST_PHONE){
            if(approved&&!pendingDial.isEmpty())callNumber(pendingDial);
            else toast("Phone permission was not granted");
        }else if(req==REQUEST_CONTACTS){
            if(approved){contactDataLoaded=false;buildUi();}
            else toast("Contact names/photos require permission");
        }else if(req==REQUEST_LOG){
            if(approved){recentDataLoaded=false;buildUi();}
            else toast("Call history permission was not granted");
        }else if(req==REQUEST_NOTIFICATIONS){
            toast(approved?"Call notifications enabled":
                "Incoming call alerts may be limited without permission");
        }
        if(!approved&&(req==REQUEST_PHONE||req==REQUEST_CONTACTS||
                req==REQUEST_LOG||req==REQUEST_NOTIFICATIONS)){
            new AlertDialog.Builder(this).setTitle("Permission not granted")
                .setMessage("Android has not allowed this permission. "+
                    "You can try again from Permission Setup or review "+
                    "the permission in Android App Settings.")
                .setPositiveButton("OPEN APP SETTINGS",
                    (d,w)->openAppPermissionSettings())
                .setNegativeButton("Cancel",null).show();
        }
        buildUi();
    }
}
