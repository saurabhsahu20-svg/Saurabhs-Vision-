package com.saurabhsvision.caller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.text.TextUtils;

/**
 * Real Telecom integration. Android binds this ONLY after user selects this
 * app as default Phone/Dialer. Incoming full-screen UI is launched using a
 * high-priority CALL notification with fullScreenIntent; when Android denies
 * full-screen intent access, the user still receives an actionable notification.
 */
public final class SVInCallService extends InCallService {
    static final String ACTION_REFRESH="com.saurabhsvision.caller.REFRESH_CALL";
    static final String CHANNEL_ID="sv_caller_incoming_v1";
    static final int NOTIFICATION_ID=701;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile SVInCallService instance;
    private static volatile Call current;
    private final Call.Callback callback=new Call.Callback(){
        @Override public void onStateChanged(Call call,int state){
            broadcastState();
            if(state==Call.STATE_DISCONNECTED||state==Call.STATE_DISCONNECTING){
                cancelNotification();
            }else showCallNotification(call,state==Call.STATE_RINGING);
        }
        @Override public void onDetailsChanged(Call call,Call.Details details){
            broadcastState();
        }
    };

    static Call activeCall(){return current;}
    static SVInCallService activeService(){return instance;}

    @Override public void onCreate(){
        super.onCreate();
        instance=this;
        createChannel();
    }
    @Override public void onDestroy(){
        cancelNotification();
        if(instance==this){instance=null;current=null;}
        super.onDestroy();
    }
    @Override public void onCallAdded(Call call){
        super.onCallAdded(call);
        // MVP: newest call is the controlled call; full multi-call conference
        // UI is intentionally out of scope.
        if(current!=null&&current!=call)current.unregisterCallback(callback);
        current=call;
        call.registerCallback(callback);
        broadcastState();
        showCallNotification(call,call.getState()==Call.STATE_RINGING);
    }
    @Override public void onCallRemoved(Call call){
        call.unregisterCallback(callback);
        if(current==call)current=null;
        cancelNotification();
        broadcastState();
        super.onCallRemoved(call);
    }
    @Override public void onCallAudioStateChanged(CallAudioState audioState){
        super.onCallAudioStateChanged(audioState);
        broadcastState();
    }
    static String number(Call call){
        try{
            if(call==null||call.getDetails()==null)return "";
            Uri uri=call.getDetails().getHandle();
            return uri==null?"":uri.getSchemeSpecificPart();
        }catch(Exception e){return "";}
    }
    static void answer(){
        Call call=current;
        if(call!=null&&call.getState()==Call.STATE_RINGING)
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
    }
    static void decline(){
        Call call=current;
        if(call!=null){
            if(call.getState()==Call.STATE_RINGING)call.reject(false,null);
            else call.disconnect();
        }
    }
    static void end(){
        Call call=current;
        if(call!=null)call.disconnect();
    }
    static void sendDtmf(char digit){
        Call call=current;
        if(call!=null&&"0123456789*#".indexOf(digit)>=0){
            call.playDtmfTone(digit);
            MAIN.postDelayed(()->{
                if(current==call)call.stopDtmfTone();
            },110);
        }
    }
    private void broadcastState(){
        Intent update=new Intent(ACTION_REFRESH);
        update.setPackage(getPackageName());
        sendBroadcast(update);
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL_ID,
                "Incoming and ongoing calls",NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Calls handled by SV Caller Pro");
            channel.enableVibration(true);
            NotificationManager manager=(NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager!=null)manager.createNotificationChannel(channel);
        }
    }
    private PendingIntent action(String action,int requestCode){
        Intent intent=new Intent(this,CallActionReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(this,requestCode,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void showCallNotification(Call call,boolean incoming){
        String phone=number(call);
        PhotoStore.Match match=PhotoStore.resolve(this,phone);
        Intent display=new Intent(this,InCallActivity.class)
            .setAction("sv_caller_call_ui")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                Intent.FLAG_ACTIVITY_SINGLE_TOP|
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full=PendingIntent.getActivity(this,700,display,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder=new Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_caller)
            .setContentTitle(TextUtils.isEmpty(match.name)?"Phone call":match.name)
            .setContentText(incoming?"Incoming call":"Call in progress")
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(full)
            .setPriority(Notification.PRIORITY_MAX);
        if(incoming){
            builder.setFullScreenIntent(full,true)
                .addAction(R.drawable.ic_caller,"Decline",
                    action(CallActionReceiver.ACTION_DECLINE,702))
                .addAction(R.drawable.ic_caller,"Answer",
                    action(CallActionReceiver.ACTION_ANSWER,703));
        }else{
            builder.addAction(R.drawable.ic_caller,"End call",
                action(CallActionReceiver.ACTION_END,704));
        }
        NotificationManager manager=(NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null){
            try{manager.notify(NOTIFICATION_ID,builder.build());}
            catch(SecurityException ignored){
                // App still retains in-call Service; Android decides UI access.
            }
        }
    }
    private void cancelNotification(){
        NotificationManager manager=(NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null)manager.cancel(NOTIFICATION_ID);
    }
}
