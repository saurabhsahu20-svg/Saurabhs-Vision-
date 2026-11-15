package com.saurabhsvision.caller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.Call.Details;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Genuine Telecom call UI. Available after SV Caller is chosen as Phone app.
 * Caller photo fills the ENTIRE screen and is never restricted to an avatar.
 */
public final class InCallActivity extends Activity {
    private static final int WHITE=0xfff7faff, MUTED=0xffc6daeb;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable everySecond=new Runnable(){
        @Override public void run(){
            refresh();
            handler.postDelayed(this,1000);
        }
    };
    private FrameLayout frame;
    private ImageView photoView;
    private TextView nameView,numberView,stateView,timeView,avatarView;
    private LinearLayout actionPanel,dtmfPanel;
    private String displayedNumber="",loadedPhoto="";
    private int previousState=-123;
    private int missedTicks=0;
    private boolean keypadVisible=false;
    private BroadcastReceiver receiver;
    private volatile boolean active=true;
    private boolean receiverAttached=false;
    private boolean previewOnly=false;

    private int dp(float value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    private GradientDrawable fill(int top,int bottom,int radius,int stroke){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{top,bottom});
        d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1.2f),stroke);
        return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(value);v.setTextColor(color);v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(android.graphics.Typeface.create(
            "sans-serif-medium",android.graphics.Typeface.BOLD));
        return v;
    }
    @Override public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        if(Build.VERSION.SDK_INT>=27){
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xff071225);
        previewOnly=getIntent()!=null&&getIntent().getBooleanExtra("previewOnly",false);
        build();
    }
    @Override protected void onStart(){
        super.onStart();
        active=true;
        receiver=new BroadcastReceiver(){
            @Override public void onReceive(Context ctx,Intent intent){refresh();}
        };
        IntentFilter filter=new IntentFilter(SVInCallService.ACTION_REFRESH);
        if(Build.VERSION.SDK_INT>=33){
            registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
        }else registerReceiver(receiver,filter);
        receiverAttached=true;
        handler.removeCallbacks(everySecond);
        handler.post(everySecond);
    }
    @Override protected void onStop(){
        active=false;
        handler.removeCallbacks(everySecond);
        if(receiverAttached&&receiver!=null){
            unregisterReceiver(receiver);
            receiverAttached=false;
        }
        super.onStop();
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        boolean wasPreview=previewOnly;
        previewOnly=intent!=null&&intent.getBooleanExtra("previewOnly",false);
        if(wasPreview!=previewOnly)build();
        else refresh();
    }
    @Override public void onConfigurationChanged(Configuration config){
        super.onConfigurationChanged(config);
        build();
    }
    @Override public void onBackPressed(){
        if(previewOnly){finish();return;}
        // Back leaves the call running, not disconnected.
        moveTaskToBack(true);
    }
    private void build(){
        frame=new FrameLayout(this);
        int[][] palettes={
            {0xff11265e,0xff181144,0xff050818},
            {0xff632a91,0xff271457,0xff09091c},
            {0xffa8586e,0xff3d204c,0xff070a21},
            {0xff066e6b,0xff0c3451,0xff070f24},
            {0xff98753b,0xff42365e,0xff070b20},
            {0xff1b5bba,0xff272184,0xff070b25}
        };
        int choice=DialerRules.theme(
            getSharedPreferences("caller_settings",MODE_PRIVATE).getInt("theme",0));
        frame.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,palettes[choice]));
        photoView=new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setContentDescription("Full-screen caller photo wallpaper");
        frame.addView(photoView,new FrameLayout.LayoutParams(-1,-1));

        // Fallback gradient and dark scrim keep both photos and text legible.
        View wallpaperTint=new View(this);
        wallpaperTint.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0x663c217f,0x7720103f,0xb0081029,0xf205091a}));
        frame.addView(wallpaperTint,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        setContentView(frame);

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(21),dp(21),dp(21),dp(26));
        scroll.addView(content);
        TextView logo=text("✦  SV CALLER PRO   •   PRIVATE & PREMIUM",12,
            0xffebd4ff,true);
        logo.setLetterSpacing(.06f);
        logo.setPadding(dp(8),dp(9),dp(8),dp(9));
        logo.setBackground(fill(0x9933215e,0x88201940,25,0x99aa89e9));
        content.addView(logo,new LinearLayout.LayoutParams(-1,dp(49)));
        stateView=text("Connecting…",15,0xffb8f8fc,true);
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(-1,dp(39));
        stateLp.topMargin=dp(21);
        content.addView(stateView,stateLp);

        nameView=text("Unknown caller",35,WHITE,true);
        nameView.setShadowLayer(dp(3),0,dp(2),Color.BLACK);
        nameView.setMaxLines(2);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(nameView,new LinearLayout.LayoutParams(-1,dp(81)));
        numberView=text("",15,MUTED,false);
        content.addView(numberView,new LinearLayout.LayoutParams(-1,dp(29)));
        timeView=text("",15,0xff9cf8da,true);
        content.addView(timeView,new LinearLayout.LayoutParams(-1,dp(31)));

        // Wallpaper is always visible beyond this subtle central monogram.
        View centerSpace=new View(this);
        content.addView(centerSpace,new LinearLayout.LayoutParams(-1,0,1));
        avatarView=text("✦",80,0x55ffffff,true);
        avatarView.setContentDescription("Placeholder when caller has no image");
        content.addView(avatarView,new LinearLayout.LayoutParams(-1,dp(95)));
        View spacer=new View(this);
        content.addView(spacer,new LinearLayout.LayoutParams(-1,0,1));

        dtmfPanel=new LinearLayout(this);
        dtmfPanel.setOrientation(LinearLayout.VERTICAL);
        dtmfPanel.setPadding(dp(6),dp(7),dp(6),dp(6));
        dtmfPanel.setBackground(fill(0xdd252653,0xdd0d1230,25,0xff6b61ae));
        content.addView(dtmfPanel,new LinearLayout.LayoutParams(-1,-2));
        dtmfPanel.setVisibility(View.GONE);
        buildDtmf();

        actionPanel=new LinearLayout(this);
        actionPanel.setOrientation(LinearLayout.VERTICAL);
        actionPanel.setPadding(dp(8),dp(10),dp(8),dp(10));
        actionPanel.setBackground(fill(0x992a2756,0xcc101733,31,0x996a64a9));
        LinearLayout.LayoutParams actionP=new LinearLayout.LayoutParams(-1,-2);
        actionP.topMargin=dp(12);
        content.addView(actionPanel,actionP);

        TextView footer=text("YOUR CALL   •   YOUR PHOTO   •   YOUR PRIVACY",11,
            0xffd7dceb,false);
        LinearLayout.LayoutParams footerP=new LinearLayout.LayoutParams(-1,dp(35));
        footerP.topMargin=dp(8);
        content.addView(footer,footerP);
        previousState=-123;
        displayedNumber="";
        loadedPhoto="";
        if(previewOnly){
            stateView.setText("CALLER SCREEN PREVIEW");
            String number=getIntent().getStringExtra("previewNumber");
            updatePhoto(number==null?"":number);
            timeView.setText("Preview only • no active call");
            actionPanel.removeAllViews();
            buttonRow(new String[]{"✕  CLOSE PREVIEW"},
                new Runnable[]{this::finish},new int[]{0xff2a315d});
        }else refresh();
    }
    private TextView action(String title,int top,int bottom,int outline){
        TextView t=text(title,14,WHITE,true);
        t.setPadding(dp(3),dp(11),dp(3),dp(11));
        t.setBackground(fill(top,bottom,32,outline));
        t.setElevation(dp(2));
        t.setContentDescription(title);
        return t;
    }
    private void buttonRow(String[] labels,Runnable[] actions,int[] colors){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        for(int i=0;i<labels.length;i++){
            int c=colors[i];
            int bot=c==0xffe32943?0xffa31534:
                c==0xff16bd71?0xff0b824f:0xff141c41;
            TextView b=action(labels[i],c,bot,0x88d5efff);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),1);
            p.setMargins(dp(3),dp(3),dp(3),dp(3));
            row.addView(b,p);
            Runnable run=actions[i];
            b.setOnClickListener(v->run.run());
        }
        actionPanel.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void buildActions(Call call){
        actionPanel.removeAllViews();
        int state=call.getState();
        if(state==Call.STATE_RINGING){
            buttonRow(new String[]{"✕  DECLINE","✓  ANSWER"},
                new Runnable[]{SVInCallService::decline,SVInCallService::answer},
                new int[]{0xffe32943,0xff16bd71});
            return;
        }
        SVInCallService service=SVInCallService.activeService();
        CallAudioState audio=service==null?null:service.getCallAudioState();
        boolean muted=audio!=null&&audio.isMuted();
        boolean speaker=audio!=null&&audio.getRoute()==CallAudioState.ROUTE_SPEAKER;
        boolean held=state==Call.STATE_HOLDING;
        buttonRow(new String[]{muted?"MIC ON":"MUTE",speaker?"EARPIECE":"SPEAKER","KEYPAD"},
            new Runnable[]{
                ()->{SVInCallService svc=SVInCallService.activeService();
                    if(svc!=null)svc.setMuted(!muted);refreshControls();},
                ()->{SVInCallService svc=SVInCallService.activeService();
                    if(svc!=null)svc.setAudioRoute(speaker?
                        CallAudioState.ROUTE_EARPIECE:CallAudioState.ROUTE_SPEAKER);
                    refreshControls();},
                ()->{keypadVisible=!keypadVisible;
                    dtmfPanel.setVisibility(keypadVisible?View.VISIBLE:View.GONE);}
            },new int[]{0xff2a315d,0xff2a315d,0xff2a315d});
        boolean canHold=call.getDetails()!=null && call.getDetails().can(
            Details.CAPABILITY_HOLD);
        buttonRow(new String[]{"◉  CHOOSE AUDIO OUTPUT"},
            new Runnable[]{this::showAudioRoutes},new int[]{0xff2a315d});
        if(canHold||held){
            buttonRow(new String[]{held?"RESUME":"HOLD","✕  END CALL"},
                new Runnable[]{
                    ()->{if(call.getState()==Call.STATE_HOLDING)call.unhold();else call.hold();},
                    SVInCallService::end
                },new int[]{0xff2a315d,0xffe32943});
        }else{
            buttonRow(new String[]{"✕  END CALL"},
                new Runnable[]{SVInCallService::end},new int[]{0xffe32943});
        }
    }
    /** Show only audio routes that Android Telecom reports as available. */
    private void showAudioRoutes(){
        SVInCallService service=SVInCallService.activeService();
        CallAudioState state=service==null?null:service.getCallAudioState();
        if(state==null){
            Toast.makeText(this,"Audio routes not available",Toast.LENGTH_SHORT).show();
            return;
        }
        int mask=state.getSupportedRouteMask();
        java.util.ArrayList<String> labels=new java.util.ArrayList<>();
        java.util.ArrayList<Integer> routes=new java.util.ArrayList<>();
        int[] ids={CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_SPEAKER,CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioState.ROUTE_BLUETOOTH};
        String[] names={"Phone earpiece","Speaker","Wired headset","Bluetooth"};
        for(int j=0;j<ids.length;j++){
            if((mask&ids[j])==0)continue;
            labels.add((state.getRoute()==ids[j]?"✓  ":"     ")+names[j]);
            routes.add(ids[j]);
        }
        if(routes.isEmpty()){
            Toast.makeText(this,"No selectable audio routes",Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Choose call audio output")
            .setItems(labels.toArray(new String[0]),(dialog,which)->{
                SVInCallService svc=SVInCallService.activeService();
                if(svc!=null){svc.setAudioRoute(routes.get(which));refreshControls();}
            }).setNegativeButton("Cancel",null).show();
    }
    private void refreshControls(){
        Call call=SVInCallService.activeCall();
        if(call!=null){buildActions(call);previousState=call.getState();}
    }
    private void buildDtmf(){
        TextView caption=text("In-call keypad • Sends real DTMF tones",12,MUTED,true);
        dtmfPanel.addView(caption,new LinearLayout.LayoutParams(-1,dp(24)));
        String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","#"}};
        for(String[] group:keys){
            LinearLayout row=new LinearLayout(this);
            for(String key:group){
                TextView b=action(key,0xff16436b,0xff0b294b,0x553ee8ff);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);
                p.setMargins(dp(4),dp(3),dp(4),dp(3));
                row.addView(b,p);
                b.setOnClickListener(v->SVInCallService.sendDtmf(key.charAt(0)));
            }
            dtmfPanel.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
    }
    private String callLabel(int state){
        switch(state){
            case Call.STATE_RINGING:return "INCOMING CALL";
            case Call.STATE_DIALING:return "DIALING…";
            case Call.STATE_CONNECTING:return "CONNECTING…";
            case Call.STATE_ACTIVE:return "CONNECTED";
            case Call.STATE_HOLDING:return "ON HOLD";
            case Call.STATE_DISCONNECTING:return "ENDING…";
            case Call.STATE_DISCONNECTED:return "CALL ENDED";
            default:return "PHONE CALL";
        }
    }
    private void updatePhoto(String number){
        PhotoStore.Match contact=PhotoStore.resolve(this,number);
        nameView.setText(contact.name);
        numberView.setText(contact.name.equals(number)?"":number);
        Uri photo=contact.photo;
        String key=photo==null?"":photo.toString();
        if(loadedPhoto.equals(key))return;
        loadedPhoto=key;
        photoView.setImageDrawable(null);
        avatarView.setVisibility(View.VISIBLE);
        if(photo==null)return;
        new Thread(()->{
            android.graphics.Bitmap bitmap=PhotoStore.loadScaled(
                getApplicationContext(),photo,1600);
            runOnUiThread(()->{
                if(!active||!key.equals(loadedPhoto)||photoView==null)return;
                if(bitmap!=null){
                    photoView.setImageBitmap(bitmap);
                    avatarView.setVisibility(View.GONE);
                }
            });
        },"SVCaller-ContactPhoto").start();
    }
    private void refresh(){
        if(previewOnly||actionPanel==null)return;
        Call call=SVInCallService.activeCall();
        if(call==null){
            missedTicks++;
            if(missedTicks>=5){
                stateView.setText("CALL FINISHED");
                finish();
            }
            return;
        }
        missedTicks=0;
        int state=call.getState();
        if(state==Call.STATE_DISCONNECTED){
            stateView.setText("CALL ENDED");
            handler.postDelayed(this::finish,1000);
            return;
        }
        if(previousState!=state){
            previousState=state;
            buildActions(call);
        }
        stateView.setText(callLabel(state));
        String number=SVInCallService.number(call);
        if(!number.equals(displayedNumber)){
            displayedNumber=number;
            updatePhoto(number);
        }
        long start=0;
        try{if(call.getDetails()!=null)start=call.getDetails().getConnectTimeMillis();}
        catch(Exception ignored){}
        if(start>0&&state==Call.STATE_ACTIVE){
            long sec=Math.max(0,(System.currentTimeMillis()-start)/1000);
            timeView.setText(String.format(java.util.Locale.ROOT,
                "%02d:%02d",sec/60,sec%60));
        }else timeView.setText(state==Call.STATE_RINGING?"FULL SCREEN CALLER PHOTO":"");
    }
}
