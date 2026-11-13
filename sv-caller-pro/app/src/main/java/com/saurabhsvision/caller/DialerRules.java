package com.saurabhsvision.caller;

/** Pure-Java, local-only validation for phone dialpad input. */
public final class DialerRules {
    private DialerRules(){}
    public static boolean validNumber(String raw){
        if(raw==null)return false;
        String value=raw.trim();
        if(value.isEmpty()||value.length()>80)return false;
        if(!value.matches("[+0-9*#(). -]+"))return false;
        int digits=0;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(c>='0'&&c<='9')digits++;
            if(c=='+'&&i!=0)return false;
        }
        return digits>0;
    }
    public static String displayNumber(String raw){
        return raw==null?"":raw.trim();
    }
    public static int theme(int index){
        return Math.max(0,Math.min(3,index));
    }
}
