package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** Public social-profile links. Does not access a user's Instagram account. */
public final class SocialLinks {
    private SocialLinks() {}

    public static final String INSTAGRAM_USERNAME = "saurabhs_vision";
    public static final String INSTAGRAM_PROFILE = "https://www.instagram.com/saurabhs_vision/";

    /** Opens the supplied Instagram profile in the Instagram app or browser. */
    public static void openInstagram(Activity activity) {
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_PROFILE));
        view.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            activity.startActivity(view);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, "No app can open this Instagram link.", Toast.LENGTH_LONG).show();
        }
    }
}
