package com.saurabhsvision.keyboard;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Offline Latin-key phonetic transliteration for Indian keyboard modes.
 * Not machine translation and not a dictionary-powered Indic IME.
 * Multiple roman spellings can produce approximate script spellings.
 * Source and user keystrokes never leave the device.
 */
public final class OfflinePhonetic {
    private OfflinePhonetic(){}

    private static final Map<String,String> CONS=new HashMap<>();
    private static final Map<String,String[]> VOWELS=new HashMap<>();
    private static final Map<String,String> WORDS=new HashMap<>();
    private static final ArrayList<String> LETTER_KEYS=new ArrayList<>();
    private static final String VIRAMA="्";

    private static void addCon(String key,String letter){
        CONS.put(key,letter);
        LETTER_KEYS.add(key);
    }
    private static void addVowel(String key,String standalone,String mark){
        VOWELS.put(key,new String[]{standalone,mark});
        LETTER_KEYS.add(key);
    }
    static{
        // More specific digraphs are preferred over 1-letter keys.
        addCon("ksh","क्ष"); addCon("chh","छ");
        addCon("tth","ठ"); addCon("ddh","ढ");
        addCon("kh","ख"); addCon("gh","घ");
        addCon("ch","च"); addCon("jh","झ"); addCon("th","थ");
        addCon("dh","ध"); addCon("ph","फ"); addCon("bh","भ");
        addCon("sh","श"); addCon("ss","ष"); addCon("ng","ङ");
        addCon("ny","ञ"); addCon("gy","ज्ञ"); addCon("jn","ज्ञ");
        addCon("tr","त्र"); addCon("tt","ट"); addCon("dd","ड");
        addCon("rh","ढ़"); addCon("zh","ळ");
        addCon("k","क");addCon("g","ग");addCon("c","क");
        addCon("j","ज");addCon("t","त");addCon("d","द");
        addCon("n","न");addCon("p","प");addCon("b","ब");
        addCon("m","म");addCon("y","य");addCon("r","र");
        addCon("l","ल");addCon("v","व");addCon("w","व");
        addCon("s","स");addCon("h","ह");addCon("f","फ़");
        addCon("z","ज़");addCon("q","क");addCon("x","क्स");
        addVowel("aa","आ","ा");addVowel("ee","ई","ी");
        addVowel("ii","ई","ी");addVowel("oo","ऊ","ू");
        addVowel("uu","ऊ","ू");addVowel("ai","ऐ","ै");
        addVowel("au","औ","ौ");addVowel("ou","औ","ौ");
        addVowel("ri","ऋ","ृ");
        addVowel("a","अ","");addVowel("i","इ","ि");
        addVowel("u","उ","ु");addVowel("e","ए","े");
        addVowel("o","ओ","ो");
        Collections.sort(LETTER_KEYS,new Comparator<String>(){
            @Override public int compare(String a,String b){
                return Integer.compare(b.length(),a.length());
            }
        });

        addWord("namaste","नमस्ते");addWord("namaskar","नमस्कार");
        addWord("namaskaar","नमस्कार");addWord("shukriya","शुक्रिया");
        addWord("dhanyavad","धन्यवाद");addWord("dhanyavaad","धन्यवाद");
        addWord("mera","मेरा");addWord("meri","मेरी");addWord("mere","मेरे");
        addWord("main","मैं");addWord("mein","में");
        addWord("aap","आप");addWord("aapka","आपका");addWord("aapki","आपकी");
        addWord("aapke","आपके");addWord("aapse","आपसे");
        addWord("kya","क्या");addWord("kyun","क्यों");addWord("kyu","क्यों");
        addWord("kaise","कैसे");addWord("kaisa","कैसा");addWord("kahan","कहाँ");
        addWord("hai","है");addWord("hain","हैं");addWord("ho","हो");
        addWord("nahi","नहीं");addWord("nahin","नहीं");addWord("nhi","नहीं");
        addWord("tum","तुम");addWord("tumhara","तुम्हारा");addWord("hum","हम");
        addWord("bharat","भारत");addWord("india","इंडिया");
        addWord("shiksha","शिक्षा");addWord("vidya","विद्या");
        addWord("dost","दोस्त");addWord("doston","दोस्तों");
        addWord("school","स्कूल");addWord("padhai","पढ़ाई");
        addWord("achha","अच्छा");addWord("accha","अच्छा");
        addWord("theek","ठीक");addWord("thik","ठीक");
        addWord("sab","सब");addWord("sahi","सही");
        addWord("hindi","हिन्दी");addWord("english","इंग्लिश");
        addWord("marathi","मराठी");addWord("telugu","तेलुगु");
        addWord("bangla","बांग्ला");addWord("tamil","तमिल");
        addWord("gujarati","गुजराती");addWord("aaj","आज");
        addWord("kal","कल");addWord("shubh","शुभ");
        addWord("subah","सुबह");addWord("raat","रात");
        addWord("bahut","बहुत");addWord("kripya","कृपया");
        addWord("pyaar","प्यार");addWord("pyar","प्यार");
        addWord("prem","प्रेम");addWord("saurabh","सौरभ");
        addWord("vision","विज़न");addWord("maths","मैथ्स");
        addWord("science","साइंस");addWord("khabar","खबर");
        addWord("khana","खाना");addWord("bhai","भाई");
        addWord("behen","बहन");addWord("paani","पानी");
        addWord("pani","पानी");addWord("tumhe","तुम्हें");
        addWord("mujhe","मुझे");addWord("mujhse","मुझसे");
        addWord("kar","कर");addWord("karo","करो");
        addWord("karna","करना");addWord("karte","करते");
        addWord("achhe","अच्छे");addWord("bohot","बहुत");
        addWord("acche","अच्छे");
    }
    private static void addWord(String a,String b){WORDS.put(a,b);}

    private static String keyAt(String roman,int i){
        for(String k:LETTER_KEYS){
            if(i+k.length()<=roman.length() && roman.regionMatches(i,k,0,k.length()))
                return k;
        }
        return null;
    }
    private static String devanagari(String roman){
        if(roman.isEmpty())return "";
        String fixed=WORDS.get(roman);
        if(fixed!=null)return fixed;
        StringBuilder out=new StringBuilder();
        int i=0;
        while(i<roman.length()){
            String key=keyAt(roman,i);
            if(key==null){out.append(roman.charAt(i));i++;continue;}
            String con=CONS.get(key);
            if(con!=null){
                int after=i+key.length();
                String next=after<roman.length()?keyAt(roman,after):null;
                if(next!=null && VOWELS.containsKey(next)){
                    String[] vowel=VOWELS.get(next);
                    out.append(con).append(vowel[1]); // implicit 'a' has no mark
                    i=after+next.length();
                }else{
                    out.append(con);
                    if(next!=null && CONS.containsKey(next))out.append(VIRAMA);
                    i=after;
                }
                continue;
            }
            String[] vowel=VOWELS.get(key);
            if(vowel!=null){out.append(vowel[0]);i+=key.length();}
            else {out.append(key);i+=key.length();}
        }
        return out.toString();
    }

    /** Roman letters converted to the selected script. Approximate, not translation. */
    public static String convert(String roman,int language){
        if(roman==null||roman.isEmpty())return "";
        language=IndicLanguages.norm(language);
        if(language<IndicLanguages.HINDI)return roman;
        String normalized=roman.toLowerCase(Locale.ROOT);
        String script=IndicLanguages.script(language);
        if("MeiteiMayek".equals(script))return meitei(normalized);
        if("OlChiki".equals(script))return olChiki(normalized);
        if("Arabic".equals(script))return urdu(normalized);
        String devanagari=devanagari(normalized);
        if("Devanagari".equals(script))return devanagari;
        return convertBrahmic(devanagari,script);
    }

    private static String convertBrahmic(String devanagari,String script){
        int offset;
        switch(script){
            case "Bengali":case "Assamese":offset=0x80;break;
            case "Gurmukhi":offset=0x100;break;
            case "Gujarati":offset=0x180;break;
            case "Oriya":offset=0x200;break;
            case "Tamil":offset=0x280;break;
            case "Telugu":offset=0x300;break;
            case "Kannada":offset=0x380;break;
            case "Malayalam":offset=0x400;break;
            default:return devanagari;
        }
        StringBuilder result=new StringBuilder();
        for(int i=0;i<devanagari.length();i++){
            char c=devanagari.charAt(i);
            if(c>=0x0900 && c<=0x097F){
                int cp=c+offset;
                // Tamil script has fewer consonant series; choose closest letter.
                if("Tamil".equals(script))cp=tamilFallback(c,cp);
                // Punjabi has fewer aspirated/retroflex characters.
                if("Gurmukhi".equals(script))cp=gurmukhiFallback(c,cp);
                if("Assamese".equals(script)){
                    if(c=='र')cp=0x09F0;
                    if(c=='व')cp=0x09F1;
                }
                if(Character.getType(cp)==Character.UNASSIGNED){
                    // Keep the rendered text readable instead of tofu boxes.
                    cp=nearestAvailable(c,offset);
                }
                result.appendCodePoint(cp);
            }else result.append(c);
        }
        return result.toString();
    }
    private static int nearestAvailable(char c,int offset){
        if(c=='क'||c=='ख'||c=='ग'||c=='घ')return 0x0915+offset;
        if(c=='च'||c=='छ'||c=='ज'||c=='झ')return 0x091A+offset;
        if(c=='ट'||c=='ठ'||c=='ड'||c=='ढ')return 0x091F+offset;
        if(c=='त'||c=='थ'||c=='द'||c=='ध')return 0x0924+offset;
        if(c=='प'||c=='फ'||c=='ब'||c=='भ')return 0x092A+offset;
        return c; // uncommon letters: retain source rather than silently delete
    }
    private static int tamilFallback(char devanagari,int mapped){
        switch(devanagari){
            case 'ख':case 'ग':case 'घ':return 0x0B95; // க
            case 'छ':case 'झ':return 0x0B9A; // ச
            case 'ज':return 0x0B9C; // ஜ
            case 'ठ':case 'ड':case 'ढ':return 0x0B9F; // ட
            case 'थ':case 'द':case 'ध':return 0x0BA4; // த
            case 'फ':case 'ब':case 'भ':return 0x0BAA; // ப
            case 'श':case 'ष':return 0x0BB7; // ஷ
            case 'स':return 0x0BB8; // ஸ
            case 'ह':return 0x0BB9; // ஹ
            case 'र':return 0x0BB0;
            default:return mapped;
        }
    }
    private static int gurmukhiFallback(char dev,int mapped){
        if(dev=='श'||dev=='ष')return 0x0A36;
        if(dev=='ज़')return 0x0A5B;
        if(dev=='फ़')return 0x0A5E;
        return mapped;
    }

    private static String romanAsLetters(String roman,Map<String,String> alphabet){
        StringBuilder out=new StringBuilder();
        String[] digraphs={"chh","ksh","ng","kh","gh","ch","jh","th","dh","ph","bh","sh","aa","ee","oo","ai","au"};
        int i=0;
        while(i<roman.length()){
            boolean used=false;
            for(String key:digraphs){
                String v=alphabet.get(key);
                if(v!=null && roman.startsWith(key,i)){
                    out.append(v);i+=key.length();used=true;break;
                }
            }
            if(!used){
                String k=roman.substring(i,i+1);
                out.append(alphabet.containsKey(k)?alphabet.get(k):k);
                i++;
            }
        }
        return out.toString();
    }
    private static final Map<String,String> MEITEI=letterMap(
        "a:ꯑ aa:ꯑꯥ i:ꯏ u:ꯎ e:ꯑꯦ o:ꯑꯣ k:ꯀ kh:ꯈ g:ꯒ gh:ꯘ ch:ꯆ j:ꯖ jh:ꯓ t:ꯇ th:ꯊ d:ꯗ dh:ꯙ n:ꯅ p:ꯄ ph:ꯐ b:ꯕ bh:ꯚ m:ꯃ y:ꯌ r:ꯔ l:ꯂ v:ꯋ w:ꯋ s:ꯁ sh:ꯁ h:ꯍ ng:ꯉ");
    private static final Map<String,String> OLCHIKI=letterMap(
        "a:ᱟ aa:ᱟ i:ᱤ u:ᱩ e:ᱮ o:ᱳ k:ᱠ kh:ᱠ g:ᱜ gh:ᱜ ch:ᱪ j:ᱡ jh:ᱡ t:ᱛ th:ᱛ d:ᱫ dh:ᱫ n:ᱱ p:ᱯ ph:ᱯ b:ᱵ bh:ᱵ m:ᱢ y:ᱭ r:ᱨ l:ᱞ v:ᱣ w:ᱣ s:ᱥ sh:ᱥ h:ᱦ ng:ᱝ");
    private static final Map<String,String> URDU=letterMap(
        "a:ا aa:آ i:ی u:و e:ے o:و k:ک kh:خ g:گ gh:غ ch:چ j:ج jh:جھ t:ت th:تھ d:د dh:دھ n:ن p:پ ph:ف b:ب bh:بھ m:م y:ی r:ر l:ل v:و w:و s:س sh:ش h:ہ ng:نگ q:ق z:ز f:ف");
    private static Map<String,String> letterMap(String defs){
        Map<String,String> m=new HashMap<>();
        for(String pair:defs.split(" ")){
            int sep=pair.indexOf(':');
            if(sep>0)m.put(pair.substring(0,sep),pair.substring(sep+1));
        }
        return m;
    }
    private static String meitei(String text){return romanAsLetters(text,MEITEI);}
    private static String olChiki(String text){return romanAsLetters(text,OLCHIKI);}
    private static String urdu(String text){return romanAsLetters(text,URDU);}
}
