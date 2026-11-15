package com.saurabhsvision.sbrowser;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import java.util.List;

/**
 * Opt-in best-effort portal update checks. NEVER claims a changed page means
 * a newly open vacancy. Notifications always direct users to official sources.
 */
public final class GovtAlerts {
    public static final String PREFS="s_browser_jobs_alerts";
    public static final String CHANNEL="official_page_updates";
    private static final int PERIODIC=64121;
    private static final int MANUAL=64122;
    private GovtAlerts(){}
    static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    public static boolean enabled(Context c){return prefs(c).getBoolean("alerts_enabled",false);}
    public static void setEnabled(Context c,boolean enabled){
        prefs(c).edit().putBoolean("alerts_enabled",enabled).apply();
        if(enabled)schedule(c);
        else{
            JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobs!=null){jobs.cancel(PERIODIC);jobs.cancel(MANUAL);}
        }
    }
    public static void schedule(Context c){
        if(!enabled(c))return;
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(jobs==null)return;
        JobInfo info=new JobInfo.Builder(PERIODIC,
            new ComponentName(c,GovtAlertJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(24L*60L*60L*1000L)
            .setPersisted(false)
            .build();
        jobs.schedule(info);
        createChannel(c);
    }
    public static void checkNow(Context c){
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(jobs==null)return;
        JobInfo task=new JobInfo.Builder(MANUAL,
            new ComponentName(c,GovtAlertJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setOverrideDeadline(0L)
            .build();
        jobs.schedule(task);
        createChannel(c);
    }
    public static void createChannel(Context c){
        NotificationManager nm=(NotificationManager)
            c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        NotificationChannel channel=new NotificationChannel(
            CHANNEL,"Official Jobs & Exam Portal Updates",
            NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Changes to official sites, not verified new vacancies.");
        nm.createNotificationChannel(channel);
    }
    static void showUpdateNotification(Context c,List<String> changed){
        if(changed==null||changed.isEmpty()||!enabled(c))return;
        if(Build.VERSION.SDK_INT>=33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED)return;
        createChannel(c);
        NotificationManager nm=(NotificationManager)
            c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        String detail=changed.size()==1?changed.get(0):
            changed.size()+" official portals have changed";
        Intent intent=new Intent(c,JobsHubActivity.class);
        PendingIntent pending=PendingIntent.getActivity(c,441,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification msg=new Notification.Builder(c,CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("S Browser • Check official portal")
            .setContentText(detail+" — verify notices and application deadlines")
            .setStyle(new Notification.BigTextStyle().bigText(
                detail+". The official page changed. It may be a result, correction, "+
                "archive entry or vacancy. Please verify the notice PDF, current "+
                "application status and dates before taking action."))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build();
        nm.notify(6901,msg);
    }
}
