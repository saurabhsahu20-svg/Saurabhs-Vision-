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
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
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
    private ImageView photoView, profileImage;
    private TextView nameView,numberView,stateView,timeView,avatarView,profileFallback;
    private FrameLayout profileRing;
    private View wallpaperTint;
    private android.graphics.Bitmap currentBitmap;
    private LinearLayout actionPanel,dtmfPanel;
    private String displayedNumber="",loadedPhoto=null;
    private int previousState=-123;
    private int previousAudioSignature=Integer.MIN_VALUE;
    private int missedTicks=0;
    private boolean keypadVisible=false;
    private BroadcastReceiver receiver;
    private volatile boolean active=true;
    private boolean receiverAttached=false;
    private boolean previewOnly=false;
    private boolean slideActionSent=false;

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
        getWindow().setNavigationBarColor(0xff080e24);
        previewOnly=getIntent()!=null&&getIntent().getBooleanExtra("previewOnly",false);
        build();
    }
    @Override protected void onStart(){
        super.onStart();
        active=true;
        if(loadedPhoto==null&&photoView!=null){
            String number=previewOnly?getIntent().getStringExtra("previewNumber"):
                displayedNumber;
            if(number!=null&&!number.isEmpty())updatePhoto(number);
        }
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
        loadedPhoto=null;
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

        // Dynamic gradient protects legibility in edge-to-edge incoming photos.
        wallpaperTint=new View(this);
        wallpaperTint.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xbb071124,0x2808112b,0x440b0729,0xf9050920}));
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
        TextView logo=text("SV CALLER PRO   •   PRIVATE PHONE",12,
            0xfff0e7ff,true);
        logo.setLetterSpacing(.06f);
        logo.setPadding(dp(8),dp(9),dp(8),dp(9));
        logo.setBackground(fill(0x7d2b225a,0x8c101b3f,25,0x72d1b5fc));
        content.addView(logo,new LinearLayout.LayoutParams(-1,dp(48)));

        // Circular portrait is shown during an active call only.
        profileRing=new FrameLayout(this);
        GradientDrawable ring=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xffffce77,0xffa14cff,0xff5bafff});
        ring.setShape(GradientDrawable.OVAL);
        profileRing.setBackground(ring);
        profileRing.setElevation(dp(7));
        profileRing.setPadding(dp(3),dp(3),dp(3),dp(3));
        profileFallback=text("☎",48,0xffd5c9ff,true);
        profileFallback.setBackground(fill(0xff2a285e,0xff101d44,85,0));
        profileRing.addView(profileFallback,new FrameLayout.LayoutParams(-1,-1));
        profileImage=new ImageView(this);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable mask=new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(0xff1a224b);
        profileImage.setBackground(mask);
        profileImage.setClipToOutline(true);
        profileRing.addView(profileImage,new FrameLayout.LayoutParams(-1,-1));
        profileRing.setContentDescription("Caller contact photo");
        LinearLayout.LayoutParams profileParams=new LinearLayout.LayoutParams(
            dp(136),dp(136));
        profileParams.gravity=Gravity.CENTER_HORIZONTAL;
        profileParams.topMargin=dp(17);
        content.addView(profileRing,profileParams);
        profileRing.setVisibility(View.GONE);

        stateView=text("Connecting…",15,0xffc1dffb,true);
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(-1,dp(39));
        stateLp.topMargin=dp(16);
        content.addView(stateView,stateLp);

        nameView=text("Unknown caller",33,WHITE,true);
        nameView.setShadowLayer(dp(2),0,dp(2),0xbf000000);
        nameView.setMaxLines(2);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(nameView,new LinearLayout.LayoutParams(-1,dp(76)));
        numberView=text("",17,MUTED,false);
        content.addView(numberView,new LinearLayout.LayoutParams(-1,dp(29)));
        timeView=text("",16,0xffd5fbff,true);
        timeView.setBackground(fill(0x99352a75,0x99122247,27,0x77a77aff));
        LinearLayout.LayoutParams timeLp=new LinearLayout.LayoutParams(
            -1,dp(40));
        timeLp.setMargins(dp(43),dp(12),dp(43),0);
        content.addView(timeView,timeLp);

        // Wallpaper is always visible beyond this subtle central monogram.
        View centerSpace=new View(this);
        content.addView(centerSpace,new LinearLayout.LayoutParams(-1,0,1));
        avatarView=text("☎",72,0x50ffffff,true);
        avatarView.setContentDescription("Placeholder when caller has no image");
        content.addView(avatarView,new LinearLayout.LayoutParams(-1,dp(95)));
        View spacer=new View(this);
        content.addView(spacer,new LinearLayout.LayoutParams(-1,0,1));

        dtmfPanel=new LinearLayout(this);
        dtmfPanel.setOrientation(LinearLayout.VERTICAL);
        dtmfPanel.setPadding(dp(6),dp(7),dp(6),dp(6));
        dtmfPanel.setBackground(fill(0xe529225b,0xe5151039,25,0xff9776f2));
        content.addView(dtmfPanel,new LinearLayout.LayoutParams(-1,-2));
        dtmfPanel.setVisibility(View.GONE);
        buildDtmf();

        actionPanel=new LinearLayout(this);
        actionPanel.setOrientation(LinearLayout.VERTICAL);
        actionPanel.setPadding(dp(8),dp(10),dp(8),dp(10));
        actionPanel.setBackground(fill(0x5a22164b,0x96200e40,30,0x708a6bb2));
        LinearLayout.LayoutParams actionP=new LinearLayout.LayoutParams(-1,-2);
        actionP.topMargin=dp(12);
        content.addView(actionPanel,actionP);

        TextView footer=text("SAURABH'S VISION™   •   PREMIUM CALLING",11,
            0xffd7dceb,false);
        LinearLayout.LayoutParams footerP=new LinearLayout.LayoutParams(-1,dp(35));
        footerP.topMargin=dp(8);
        content.addView(footer,footerP);
        previousState=-123;
        previousAudioSignature=Integer.MIN_VALUE;
        displayedNumber="";
        loadedPhoto=null;
        currentBitmap=null;
        if(previewOnly){
            stateView.setText("CALLER SCREEN PREVIEW");
            String number=getIntent().getStringExtra("previewNumber");
            updatePhoto(number==null?"":number);
            timeView.setText("Preview only • no active call");
            actionPanel.removeAllViews();
            buttonRow(new String[]{"✕  CLOSE PREVIEW"},
                new Runnable[]{this::finish},new int[]{0xff2a315d});
        }else refresh();
        applyPhotoPresentation(previewOnly ||
            (!previewOnly&&SVInCallService.activeCall()!=null &&
              SVInCallService.activeCall().getState()==Call.STATE_RINGING));
    }
    private void applyPhotoPresentation(boolean incoming){
        if(photoView==null||profileRing==null)return;
        profileRing.setVisibility(incoming?View.GONE:View.VISIBLE);
        photoView.setVisibility(incoming&&currentBitmap!=null?View.VISIBLE:View.GONE);
        avatarView.setVisibility(incoming&&currentBitmap==null?View.VISIBLE:View.GONE);
        profileImage.setVisibility(!incoming&&currentBitmap!=null?
            View.VISIBLE:View.GONE);
        profileFallback.setVisibility(!incoming&&currentBitmap==null?
            View.VISIBLE:View.GONE);
        wallpaperTint.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            incoming?new int[]{0xe10b1823,0x26030710,0x3009111c,0xfb08111b}:
                new int[]{0x990a0d23,0x80311959,0xcc090d25,0xfc070b1c}));
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
    /** Accessible in-call controls. Only actions backed by Telecom are shown. */
    private void controlRow(String[] icons,String[] labels,Runnable[] actions,
        boolean[] highlighted){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        for(int i=0;i<labels.length;i++){
            final int index=i;
            LinearLayout stack=new LinearLayout(this);
            stack.setOrientation(LinearLayout.VERTICAL);
            stack.setGravity(Gravity.CENTER);
            TextView orb=action(icons[i],
                highlighted[i]?0xff7136e7:0xff2b305e,
                highlighted[i]?0xff4b1aab:0xff111831,
                highlighted[i]?0xffdf9cff:0xff6b83b8);
            orb.setTextSize(26);
            orb.setGravity(Gravity.CENTER);
            orb.setElevation(dp(3));
            stack.addView(orb,new LinearLayout.LayoutParams(dp(67),dp(67)));
            TextView label=text(labels[i],12,WHITE,true);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(29));
            p.topMargin=dp(6);
            stack.addView(label,p);
            LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(0,dp(108),1);
            item.setMargins(dp(2),0,dp(2),0);
            row.addView(stack,item);
            stack.setContentDescription(labels[i]);
            stack.setOnClickListener(v->actions[index].run());
            orb.setOnClickListener(v->actions[index].run());
        }
        actionPanel.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void buildActions(Call call){
        actionPanel.removeAllViews();
        int state=call.getState();
        if(state==Call.STATE_RINGING){
            buildIncomingButtons();
            return;
        }
        SVInCallService service=SVInCallService.activeService();
        CallAudioState audio=service==null?null:service.getCallAudioState();
        boolean muted=audio!=null&&audio.isMuted();
        boolean speaker=audio!=null&&audio.getRoute()==CallAudioState.ROUTE_SPEAKER;
        boolean held=state==Call.STATE_HOLDING;
        controlRow(new String[]{"♩","▦","◖"},
            new String[]{muted?"Unmute":"Mute","Keypad",speaker?"Earpiece":"Speaker"},
            new Runnable[]{
                ()->{
                    SVInCallService svc=SVInCallService.activeService();
                    if(svc!=null)svc.setMuted(!muted);
                    refreshControls();
                },
                ()->{
                    keypadVisible=!keypadVisible;
                    dtmfPanel.setVisibility(keypadVisible?View.VISIBLE:View.GONE);
                },
                ()->{
                    SVInCallService svc=SVInCallService.activeService();
                    CallAudioState route=svc==null?null:svc.getCallAudioState();
                    if(route==null){showAudioRoutes();return;}
                    int target=speaker?CallAudioState.ROUTE_EARPIECE:
                        CallAudioState.ROUTE_SPEAKER;
                    if((route.getSupportedRouteMask()&target)!=0)
                        svc.setAudioRoute(target);
                    else showAudioRoutes();
                    refreshControls();
                }
            },new boolean[]{muted,keypadVisible,speaker});
        boolean canHold=call.getDetails()!=null && call.getDetails().can(
            Details.CAPABILITY_HOLD);
        if(canHold||held){
            controlRow(new String[]{"Ⅱ","◉"},
                new String[]{held?"Resume":"Hold","Audio output"},
                new Runnable[]{
                    ()->{
                        if(call.getState()==Call.STATE_HOLDING)call.unhold();
                        else if(call.getState()==Call.STATE_ACTIVE)call.hold();
                    },
                    this::showAudioRoutes
                },new boolean[]{held,false});
        }else{
            controlRow(new String[]{"◉"},new String[]{"Audio output"},
                new Runnable[]{this::showAudioRoutes},new boolean[]{false});
        }
        LinearLayout endStack=new LinearLayout(this);
        endStack.setOrientation(LinearLayout.VERTICAL);
        endStack.setGravity(Gravity.CENTER);
        TextView end=action("☎",0xffff3b54,0xffbc102f,0xffff9aaa);
        end.setTextSize(32);
        end.setElevation(dp(7));
        end.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams endSize=new LinearLayout.LayoutParams(dp(80),dp(80));
        endSize.topMargin=dp(11);
        endStack.addView(end,endSize);
        TextView endLabel=text("END CALL",13,WHITE,true);
        endStack.addView(endLabel,new LinearLayout.LayoutParams(-1,dp(30)));
        endStack.setContentDescription("End the current phone call");
        endStack.setOnClickListener(v->SVInCallService.end());
        end.setOnClickListener(v->SVInCallService.end());
        actionPanel.addView(endStack,new LinearLayout.LayoutParams(-1,-2));
    }
    /** Large, clearly labelled answer/decline controls over the caller photo. */
    /**
     * Original two-direction slide control: slide RIGHT to answer or LEFT to
     * decline. Telecom is invoked ONLY after the threshold is reached on up.
     * Separate tap buttons below remain accessible to TalkBack users.
     */
    private void buildIncomingButtons(){
        slideActionSent=false;
        TextView guide=text("SLIDE TO ANSWER OR DECLINE",12,0xffedf4ff,true);
        guide.setLetterSpacing(.06f);
        actionPanel.addView(guide,new LinearLayout.LayoutParams(-1,dp(30)));

        final FrameLayout track=new FrameLayout(this);
        track.setBackground(fill(0xb30e213d,0xe91b1c3a,40,0xa06f92c6));
        final TextView left=text("✕  DECLINE",12,0xffff9faa,true);
        final TextView right=text("ANSWER  ☎",12,0xffb4ffda,true);
        FrameLayout.LayoutParams leftP=new FrameLayout.LayoutParams(
            dp(112),-1,Gravity.LEFT|Gravity.CENTER_VERTICAL);
        leftP.leftMargin=dp(12);
        track.addView(left,leftP);
        FrameLayout.LayoutParams rightP=new FrameLayout.LayoutParams(
            dp(112),-1,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        rightP.rightMargin=dp(12);
        track.addView(right,rightP);

        final TextView thumb=action("⇆",0xff8d52dc,0xff32238b,0xffc7acff);
        thumb.setTextSize(28);
        thumb.setGravity(Gravity.CENTER);
        thumb.setElevation(dp(8));
        FrameLayout.LayoutParams thumbP=new FrameLayout.LayoutParams(
            dp(66),dp(66),Gravity.CENTER);
        track.addView(thumb,thumbP);
        track.setContentDescription(
            "Swipe right to answer or left to decline. Tap action buttons below as an alternative.");

        final float[] startX={0f};
        final float[] progress={0f};
        View.OnTouchListener sliderListener=(view,event)->{
            if(slideActionSent)return true;
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                startX[0]=event.getRawX();
                progress[0]=0f;
                thumb.animate().cancel();
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP){
                float maxDistance=Math.max(1f,
                    (track.getWidth()-thumb.getWidth())/2f-dp(9));
                float offset=Math.max(-maxDistance,
                    Math.min(maxDistance,event.getRawX()-startX[0]));
                progress[0]=offset/maxDistance;
                thumb.setTranslationX(offset);
                if(action==MotionEvent.ACTION_UP){
                    boolean ringing=SVInCallService.activeCall()!=null &&
                        SVInCallService.activeCall().getState()==Call.STATE_RINGING;
                    int decision=SlideRule.decision(offset,maxDistance);
                    if(ringing&&decision!=SlideRule.NONE){
                        slideActionSent=true;
                        track.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        if(decision==SlideRule.ANSWER)SVInCallService.answer();
                        else SVInCallService.decline();
                    }else{
                        thumb.animate().translationX(0f).setDuration(170).start();
                    }
                }
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){
                thumb.animate().translationX(0f).setDuration(170).start();
                return true;
            }
            return true;
        };
        track.setOnTouchListener(sliderListener);
        thumb.setOnTouchListener(sliderListener);
        LinearLayout.LayoutParams trackParams=new LinearLayout.LayoutParams(-1,dp(82));
        trackParams.setMargins(dp(3),dp(4),dp(3),dp(12));
        actionPanel.addView(track,trackParams);

        // Alternative accessible actions: these are real call controls,
        // not a simulated swipe animation.
        LinearLayout options=new LinearLayout(this);
        options.setGravity(Gravity.CENTER);
        TextView decline=action("✕  DECLINE",0xff873348,0xff4f1833,0xffdb667c);
        TextView answer=action("☎  ANSWER",0xff167d55,0xff0a533d,0xff63d8a0);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(49),1);
        p.setMargins(dp(4),0,dp(4),0);
        options.addView(decline,p);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(49),1);
        p2.setMargins(dp(4),0,dp(4),0);
        options.addView(answer,p2);
        decline.setOnClickListener(v->{if(!slideActionSent){slideActionSent=true;
            SVInCallService.decline();}});
        answer.setOnClickListener(v->{if(!slideActionSent){slideActionSent=true;
            SVInCallService.answer();}});
        actionPanel.addView(options,new LinearLayout.LayoutParams(-1,-2));
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
    private int currentAudioSignature(){
        SVInCallService svc=SVInCallService.activeService();
        CallAudioState audio=svc==null?null:svc.getCallAudioState();
        return audio==null?0:audio.getRoute()|(audio.isMuted()?65536:0);
    }
    private void refreshControls(){
        Call call=SVInCallService.activeCall();
        if(call!=null){
            buildActions(call);
            previousState=call.getState();
            previousAudioSignature=currentAudioSignature();
        }
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
    /**
     * Read contacts AND decode photos off the UI thread. Resolving a provider
     * synchronously can otherwise freeze the call screen on slower phones.
     */
    /** Resolve and decode once on a worker thread; both views share the result. */
    private void updatePhoto(String number){
        final String target=number==null?"":number;
        if(target.equals(loadedPhoto))return;
        loadedPhoto=target;
        currentBitmap=null;
        photoView.setImageDrawable(null);
        profileImage.setImageDrawable(null);
        nameView.setText(target.isEmpty()?"Unknown caller":target);
        numberView.setText("");
        applyPhotoPresentation(previewOnly ||
            (SVInCallService.activeCall()!=null &&
             SVInCallService.activeCall().getState()==Call.STATE_RINGING));
        final Context application=getApplicationContext();
        new Thread(()->{
            PhotoStore.Match contact=PhotoStore.resolve(application,target);
            android.graphics.Bitmap image=PhotoStore.loadScaled(
                application,contact.photo,1440);
            runOnUiThread(()->{
                if(!active||!target.equals(loadedPhoto)||photoView==null)return;
                nameView.setText(contact.name);
                numberView.setText(contact.name.equals(target)?"":target);
                currentBitmap=image;
                if(image!=null){
                    photoView.setImageBitmap(image);
                    profileImage.setImageBitmap(image);
                }
                applyPhotoPresentation(previewOnly ||
                    (SVInCallService.activeCall()!=null &&
                      SVInCallService.activeCall().getState()==Call.STATE_RINGING));
            });
        },"SVCaller-AsyncPhoto").start();
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
        int audioSignature=currentAudioSignature();
        if(previousState!=state||previousAudioSignature!=audioSignature){
            previousState=state;
            previousAudioSignature=audioSignature;
            buildActions(call);
            applyPhotoPresentation(state==Call.STATE_RINGING);
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
