package com.saurabhsvision.caller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Only our own explicit notification PendingIntents can invoke this receiver. */
public final class CallActionReceiver extends BroadcastReceiver {
    static final String ACTION_ANSWER="com.saurabhsvision.caller.ANSWER";
    static final String ACTION_DECLINE="com.saurabhsvision.caller.DECLINE";
    static final String ACTION_END="com.saurabhsvision.caller.END";
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||intent.getAction()==null)return;
        switch(intent.getAction()){
            case ACTION_ANSWER:SVInCallService.answer();break;
            case ACTION_DECLINE:SVInCallService.decline();break;
            case ACTION_END:SVInCallService.end();break;
            default:break;
        }
    }
}
