package com.saurabhsvision.keyboard;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

/** One-time, explicitly user-initiated microphone permission request.
 * Android resumes the previous messaging editor when this finishes.
 */
public final class VoicePermissionActivity extends Activity {
    private static final int REQUEST_AUDIO=1217;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
            finish();
        }else{
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_AUDIO);
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] perms,int[] results){
        super.onRequestPermissionsResult(request,perms,results);
        if(request==REQUEST_AUDIO){
            boolean granted=results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this,granted?
                "Microphone enabled. Return to your message and tap 🎙 to dictate.":
                "Microphone access denied. Voice typing stays off.",
                Toast.LENGTH_LONG).show();
            finish();
        }
    }
}