package com.saurabhsvision.launcher;

import java.util.Locale;

/** Small pure-Java launcher rules for repeatable CI tests. */
public final class LauncherRules {
    private LauncherRules(){}
    public static int themeIndex(int raw,int count){
        if(count<=0)throw new IllegalArgumentException("No available themes");
        return Math.floorMod(raw,count);
    }
    public static boolean matches(String title,String pkg,String query){
        if(query==null||query.trim().isEmpty())return true;
        String word=query.trim().toLowerCase(Locale.ROOT);
        return (title!=null&&title.toLowerCase(Locale.ROOT).contains(word))||
            (pkg!=null&&pkg.toLowerCase(Locale.ROOT).contains(word));
    }
}
