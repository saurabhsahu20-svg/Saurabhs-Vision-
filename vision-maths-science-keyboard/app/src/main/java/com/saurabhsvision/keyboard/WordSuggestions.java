package com.saurabhsvision.keyboard;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Small, private, on-device completion strip. Unlike a full Gboard dictionary,
 * it offers a fixed set of common English / Hinglish completions and never
 * learns, logs, syncs, or saves any words typed by the user.
 */
public final class WordSuggestions {
    private WordSuggestions(){}
    private static final String[] ENGLISH={
        "about","above","after","again","all","also","always","am","and","answer","are","around",
        "as","ask","at","back","because","before","best","better","book","board","but","by",
        "can","chapter","class","classes","come","computer","concept","correct","could","create",
        "day","dear","different","do","does","done","don't","download","education","email","english",
        "every","example","exam","excellent","experience","explain","feel","find","fine","first",
        "for","free","from","fun","future","get","give","good","great","happy","has","have","hello",
        "help","here","hi","home","how","idea","image","in","india","information","install",
        "into","is","it","just","keyboard","know","language","learn","learning","like","link",
        "little","look","make","many","math","mathematics","maths","may","me","message",
        "more","most","much","my","name","need","new","next","no","not","notes","now",
        "of","okay","on","one","only","open","or","our","out","page","paper","please",
        "practice","problem","project","question","questions","read","real","right","saurabh",
        "say","science","screen","send","should","simple","small","smart","so","solution",
        "some","space","start","study","student","students","success","support","sure",
        "teacher","test","thank","thanks","that","the","their","them","then","there","these",
        "they","this","time","to","today","tomorrow","type","typing","update","use","very",
        "vision","want","was","we","website","well","were","what","when","where","which",
        "white","who","why","will","with","work","working","world","would","yes","you","your"
    };
    private static final String[] HINGLISH={
        "aaj","aap","aapka","aapke","aapki","aapse","ab","accha","acche","achha",
        "achhe","acha","aur","bahut","bana","banao","banai","banaya","bas","batao","bharat",
        "bhai","board","chaahiye","chahiye","chalo","class","dhanyavaad","didi","dost",
        "ek","hai","hain","ham","hame","hamara","hindi","ho","hoga","honi","hum","india",
        "isse","iska","isme","jaana","jaisa","jaldi","ka","kaam","kab","kaise","kal","kar",
        "karna","karo","karte","kare","karke","karna","ki","kisi","ko","koi","kuch",
        "kyun","kya","main","mera","mere","meri","mil","mujhe","naam","namaste","naya",
        "naye","nahi","nahin","nhi","padhai","pani","par","please","raha","rahe",
        "rahi","sahi","sath","saath","sab","saurabh","school","shiksha","shubh","sir",
        "space","theek","thik","tum","tumhara","tumhe","uska","usme","wala","wale",
        "wali","ya","ye","yeh","yahan"
    };
    private static String normalize(String word){
        return word==null?"":word.toLowerCase(Locale.ROOT).trim();
    }
    /** Suggestions are generated from fixed vocabularies, not a typed-word log. */
    public static String[] get(String typed,boolean hinglish,int max){
        String prefix=normalize(typed);
        if(prefix.length()<1||prefix.length()>24||max<1)return new String[0];
        ArrayList<String> out=new ArrayList<>();
        out.add(typed);
        if(prefix.length()<2)return out.toArray(new String[0]);
        String[] dictionary=hinglish?HINGLISH:ENGLISH;
        for(String candidate:dictionary){
            if(candidate.startsWith(prefix)&&!candidate.equals(prefix)){
                out.add(candidate);
                if(out.size()>=max)break;
            }
        }
        return out.toArray(new String[0]);
    }
    /** Identify the trailing Roman-letter token without saving or transmitting it. */
    public static String trailingWord(CharSequence text){
        if(text==null||text.length()==0)return "";
        int from=text.length();
        while(from>0&&isLatin(text.charAt(from-1)))from--;
        int len=text.length()-from;
        if(len<1||len>24)return "";
        return text.subSequence(from,text.length()).toString();
    }
    private static boolean isLatin(char c){
        return c>='a'&&c<='z'||c>='A'&&c<='Z';
    }
}
