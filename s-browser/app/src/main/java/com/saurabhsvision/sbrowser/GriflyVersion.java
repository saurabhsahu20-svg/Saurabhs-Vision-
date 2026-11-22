package com.saurabhsvision.sbrowser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compares owner-published Grifly versions without lexicographic mistakes. */
public final class GriflyVersion {
    private static final Pattern TAG=Pattern.compile(
        "^grifly-v([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})(?:[-+].*)?$");
    private static final Pattern INSTALLED=Pattern.compile(
        "^([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})(?:[-+].*)?$");
    private GriflyVersion(){}
    public static int[] fromTag(String tag){return extract(tag,TAG);}
    public static int[] fromInstalled(String version){return extract(version,INSTALLED);}
    private static int[] extract(String s,Pattern pattern){
        if(s==null)return null;
        Matcher m=pattern.matcher(s);
        if(!m.matches())return null;
        try{return new int[]{Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3))};}
        catch(NumberFormatException e){return null;}
    }
    public static int compare(int[] a,int[] b){
        if(a==null||b==null)throw new IllegalArgumentException("Invalid Grifly version");
        for(int i=0;i<3;i++)if(a[i]!=b[i])return Integer.compare(a[i],b[i]);
        return 0;
    }
}
