package com.saurabhsvision.caller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Local-only photo keys, preserving Indian 10-digit / +91 matching. */
public final class CallerNumberKey {
    private CallerNumberKey(){}
    public static String normalized(String phone){
        if(phone==null)return "";
        String clean=phone.replaceAll("[^+0-9]","");
        if(clean.matches("\\+91[0-9]{10}"))return clean.substring(3);
        if(clean.matches("91[0-9]{10}"))return clean.substring(2);
        if(clean.matches("0[0-9]{10}"))return clean.substring(1);
        return clean;
    }
    public static String key(String phone){return hash(normalized(phone));}
    public static String legacyKey(String phone){
        return hash(phone==null?"":phone.replaceAll("[^+0-9]",""));
    }
    private static String hash(String input){
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder("photo_");
            for(int j=0;j<12;j++)out.append(String.format(Locale.ROOT,"%02x",digest[j]));
            return out.toString();
        }catch(Exception ex){return "photo_default";}
    }
}
