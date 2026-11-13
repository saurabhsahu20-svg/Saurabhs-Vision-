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
    private static final int NAVY=0xff07152e,CYAN=0xff3ce6ed,WHITE=0xfff7fbff,
        GOLD=0xffffd772,MUTED=0xffa3bfd6;
    private static final int REQUEST_ROLE=301,REQUEST_PHONE=302,
        REQUEST_CONTACTS=303,REQUEST_LOG=304,REQUEST_IMAGE=305,
        REQUEST_NOTIFICATIONS=306;
    private static final String[] TABS={"DIALPAD","CONTACTS","RECENTS","FAVORITES"};
    private SharedPreferences prefs;
    private int tab=0,theme=0;
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
        final String name,number;
        Person(String name,String number){this.name=name;this.number=number;}
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
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
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
        tab=0;buildUi();
    }
    private void readDialIntent(Intent intent){
        if(intent==null)return;
        Uri uri=intent.getData();
        if(Intent.ACTION_DIAL.equals(intent.getAction())&&uri!=null){
            String number=uri.getSchemeSpecificPart();
            if(number!=null&&number.length()<=80)dialText=number;
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){
        if(numberInput!=null)dialText=numberInput.getText().toString();
        state.putString("dialText",dialText);
        state.putInt("tab",tab);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed(){
        if(tab!=0){tab=0;buildUi();}
        else super.onBackPressed();
    }
    private void buildUi(){
        if(numberInput!=null)dialText=numberInput.getText().toString();
        numberInput=null;searchInput=null;
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(NAVY);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(10),dp(14),dp(24));
        scroll.addView(root);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            root.setPadding(dp(14),Math.max(dp(10),insets.getSystemWindowInsetTop()+dp(7)),
                dp(14),Math.max(dp(24),insets.getSystemWindowInsetBottom()+dp(13)));
            return insets;
        });
        buildHeader();
        buildTabs();
        panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        add(root,panel,13);
        switch(tab){
            case 0:buildDialpad();break;
            case 1:buildContacts();break;
            case 2:buildRecents();break;
            case 3:buildFavorites();break;
        }
        permissionSnapshot=permissionsSnapshot();
    }
    private void buildHeader(){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView emblem=new ImageView(this);
        emblem.setImageResource(R.drawable.ic_sv_brand);
        emblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        emblem.setContentDescription("SAURABH'S VISION original red 3D S logo");
        row.addView(emblem,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout names=new LinearLayout(this);
        names.setPadding(dp(10),0,0,0);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text("SV CALLER PRO",22,WHITE,true));
        names.addView(text("by SAURABH'S VISION™",11,CYAN,true));
        row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=text("⚙",25,WHITE,true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(gradient(0xff214471,0xff112a4c,13,0xff4da2d6));
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
            defaultDialer()?0xff0a4b46:0xff553b17,
            defaultDialer()?0xff0b292e:0xff332711,13,
            defaultDialer()?0xff3acbac:0xff9c7746));
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
    private void buildTabs(){
        LinearLayout row=new LinearLayout(this);
        row.setPadding(dp(3),dp(4),dp(3),dp(4));
        row.setBackground(gradient(0xff173a6b,0xff0c2548,17,0xff346a92));
        for(int i=0;i<TABS.length;i++){
            int index=i;
            TextView t=text(TABS[i],i==3?10:11,i==tab?NAVY:MUTED,true);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setBackground(gradient(i==tab?CYAN:0xff173658,
                i==tab?0xff75f4ef:0xff102a4a,12,0));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);
            lp.setMargins(dp(2),0,dp(2),0);
            row.addView(t,lp);
            t.setContentDescription("Open "+TABS[i]);
            t.setOnClickListener(v->{tab=index;buildUi();});
        }
        add(root,row,15);
    }
    private void buildDialpad(){
        LinearLayout face=new LinearLayout(this);
        face.setOrientation(LinearLayout.VERTICAL);
        face.setPadding(dp(12),dp(12),dp(12),dp(11));
        face.setBackground(gradient(0xff173964,0xff0b2447,19,0xff3586b0));
        TextView label=text("PHONE NUMBER  •  HD CALLER PHOTO",12,CYAN,true);
        add(face,label,0);
        numberInput=new EditText(this);
        numberInput.setSingleLine(true);
        numberInput.setText(dialText);
        numberInput.setSelection(numberInput.length());
        numberInput.setTextColor(WHITE);
        numberInput.setHintTextColor(MUTED);
        numberInput.setTextSize(29);
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
        TextView clear=smallButton("⌫  DELETE");
        TextView photo=smallButton("▧  CALLER PHOTO");
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
        photo.setOnClickListener(v->choosePhoto(numberInput.getText().toString()));
        add(face,small,8);
        add(panel,face,0);

        LinearLayout guide=new LinearLayout(this);
        guide.setGravity(Gravity.CENTER);
        TextView help=text("Enter a number  •  Long-press 0 for +",12,MUTED,false);
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
                LinearLayout tile=new LinearLayout(this);
                tile.setGravity(Gravity.CENTER);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setBackground(gradient(0xff204572,0xff0d2b52,25,0xff3075a6));
                TextView digit=text(key,30,WHITE,true);
                digit.setGravity(Gravity.CENTER);
                tile.addView(digit);
                TextView detail=text(sub,11,CYAN,false);
                detail.setGravity(Gravity.CENTER);
                tile.addView(detail);
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,dp(72),1);
                size.setMargins(dp(4),dp(4),dp(4),dp(4));
                row.addView(tile,size);
                tile.setContentDescription("Dial "+key);
                tile.setOnClickListener(v->{
                    numberInput.append(key);
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                });
                if(key.equals("0")){
                    tile.setOnLongClickListener(v->{numberInput.append("+");return true;});
                }
            }
            add(panel,row,1);
        }

        TextView call=text("☎   CALL NOW",25,Color.WHITE,true);
        call.setGravity(Gravity.CENTER);
        call.setBackground(gradient(0xff28d78b,0xff0a965c,28,0xff73ffb8));
        call.setElevation(dp(4));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(69));
        cp.setMargins(dp(19),dp(12),dp(19),0);
        panel.addView(call,cp);
        call.setContentDescription("Call typed phone number");
        call.setOnClickListener(v->callNumber(numberInput.getText().toString()));

        TextView foot=text("✦  CONTACT PHOTO WILL FILL THE ENTIRE SCREEN  ✦",
            11,GOLD,true);
        foot.setGravity(Gravity.CENTER);
        add(panel,foot,17);
    }
    private TextView smallButton(String title){
        TextView t=text(title,12,WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(gradient(0xff234f80,0xff122b4b,12,0xff3986b8));
        return t;
    }
    private EditText addSearch(LinearLayout target,String hint,Runnable rerender){
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setHint(hint);
        search.setTextSize(17);
        search.setTextColor(0xff15324d);
        search.setHintTextColor(0xff69849d);
        search.setPadding(dp(17),dp(6),dp(15),dp(6));
        search.setBackground(gradient(0xfff9fcff,0xffdcf2ff,18,0xff78c8e2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));
        p.bottomMargin=dp(14);
        target.addView(search,p);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){rerender.run();}
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
                        String key=number.replaceAll("[^+0-9]","");
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
                if(tab==2)buildUi();
            });
        },"SVCaller-CallHistory").start();
    }
    private void renderContacts(){
        if(panel==null||tab!=1||searchInput==null||!contactDataLoaded)return;
        String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        // Keep search field in place, rerender only result cards.
        while(panel.getChildCount()>1)panel.removeViewAt(1);
        int count=0;
        for(Person p:contacts){
            if(!q.isEmpty()&&!p.name.toLowerCase(Locale.ROOT).contains(q)&&
                !p.number.toLowerCase(Locale.ROOT).contains(q))continue;
            showPerson(p,false);
            if(++count>=140)break;
        }
        if(count==0)add(panel,text("No matching contacts found.",14,MUTED,false),5);
    }
    private void renderFavorites(){
        if(panel==null||tab!=3||searchInput==null||!contactDataLoaded)return;
        String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        while(panel.getChildCount()>1)panel.removeViewAt(1);
        Set<String> favorites=prefs.getStringSet("favorite_numbers",new HashSet<String>());
        int count=0;
        for(Person p:contacts){
            if(!favorites.contains(p.number))continue;
            if(!q.isEmpty()&&!p.name.toLowerCase(Locale.ROOT).contains(q)&&
                !p.number.toLowerCase(Locale.ROOT).contains(q))continue;
            showPerson(p,true);
            if(++count>=140)break;
        }
        if(count==0)add(panel,text("No favorites yet. Long-press a contact to pin it.",14,MUTED,false),5);
    }
    private void showPerson(Person person,boolean favorite){
        LinearLayout card=new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12),dp(10),dp(10),dp(10));
        card.setBackground(gradient(0xff1b426d,0xff0c294b,17,0xff2d6689));
        TextView badge=text(person.name.isEmpty()?"?":
            person.name.substring(0,1).toUpperCase(Locale.ROOT),
            23,GOLD,true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(gradient(0xff244d85,0xff18385e,20,0xff6da9c7));
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
            dialText=person.number;tab=0;buildUi();
        });
        card.setOnLongClickListener(v->{
            String[] options={
                "Choose full-screen call photo",
                "Remove custom photo",
                favorite?"Unpin from favorites":"Add to favorites",
                "Call "+person.name
            };
            new AlertDialog.Builder(this).setTitle(person.name)
                .setItems(options,(dialog,choice)->{
                    switch(choice){
                        case 0:choosePhoto(person.number);break;
                        case 1:PhotoStore.removePhoto(this,person.number);
                            toast("Custom photo removed");break;
                        case 2:toggleFavorite(person.number);break;
                        case 3:callNumber(person.number);break;
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
        if(recentCalls.isEmpty()){
            add(panel,text("No call history available on this device.",15,MUTED,false),10);
            return;
        }
        for(Recent item:recentCalls){
            String type=item.type==CallLog.Calls.MISSED_TYPE?"MISSED":
                item.type==CallLog.Calls.OUTGOING_TYPE?"OUTGOING":"INCOMING";
            LinearLayout line=new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(16),dp(10),dp(11),dp(10));
            line.setBackground(gradient(0xff1b3f65,0xff0b2545,16,0xff335f85));
            LinearLayout names=new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(text(item.name,16,WHITE,true));
            names.addView(text(type+"  •  "+new SimpleDateFormat(
                "d MMM, h:mm a",Locale.getDefault()).format(new Date(item.date))+
                "  •  "+item.duration+"s",11,
                item.type==CallLog.Calls.MISSED_TYPE?0xffffa7aa:MUTED,false));
            line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            TextView dial=smallButton("☎");
            dial.setGravity(Gravity.CENTER);
            line.addView(dial,new LinearLayout.LayoutParams(dp(51),dp(45)));
            dial.setOnClickListener(v->callNumber(item.number));
            line.setOnClickListener(v->{dialText=item.number;tab=0;buildUi();});
            add(panel,line,7);
        }
    }
    private void toggleFavorite(String number){
        Set<String> items=new HashSet<>(prefs.getStringSet(
            "favorite_numbers",new HashSet<String>()));
        if(items.contains(number)){items.remove(number);toast("Removed from favorites");}
        else {items.add(number);toast("Added to favorites");}
        prefs.edit().putStringSet("favorite_numbers",items).apply();
        if(tab==3)buildUi();
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
            "Choose Call Screen Theme","Choose Photo for Typed Number",
            "Allow Contact Photos","Allow Call History",
            "Allow Call Notifications",
            "Full-Screen Call Permission","Privacy & About"};
        new AlertDialog.Builder(this).setTitle("SV CALLER PRO • SETTINGS")
            .setItems(choices,(dialog,which)->{
                switch(which){
                    case 0:requestDialerRole();break;
                    case 1:showThemes();break;
                    case 2:choosePhoto(dialText);break;
                    case 3:requestContactsPermission();break;
                    case 4:
                        if(!defaultDialer())requestDialerRole();
                        else if(!granted(Manifest.permission.READ_CALL_LOG))
                            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},REQUEST_LOG);
                        else toast("Call history access enabled");
                        break;
                    case 5:requestNotificationPermission();break;
                    case 6:openFullScreenSettings();break;
                    case 7:
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
            "Rose Gold","Emerald Science"};
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
