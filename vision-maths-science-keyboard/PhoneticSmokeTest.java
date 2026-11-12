import com.saurabhsvision.keyboard.IndicLanguages;
import com.saurabhsvision.keyboard.OfflinePhonetic;

/** Run using javac/java only; deliberately no Android classes or network. */
public final class PhoneticSmokeTest {
    private static void eq(String actual,String expected,String name){
        if(!expected.equals(actual))throw new AssertionError(
            name+": expected ["+expected+"] but got ["+actual+"]");
        System.out.println("PASS "+name+" : "+actual);
    }
    private static void good(boolean condition,String name){
        if(!condition)throw new AssertionError(name);
        System.out.println("PASS "+name);
    }
    private static boolean inRange(String value,int low,int high){
        for(int i=0;i<value.length();i++){
            int cp=value.codePointAt(i);
            if(cp>=low && cp<=high)return true;
        }
        return false;
    }
    public static void main(String[] args){
        good(IndicLanguages.count()==24,"English + Hinglish + 22 scheduled languages");
        eq(OfflinePhonetic.convert("namaste",IndicLanguages.HINDI),"नमस्ते",
            "Roman namaste -> Hindi");
        eq(OfflinePhonetic.convert("mera",IndicLanguages.HINDI),"मेरा",
            "Roman mera -> Hindi");
        eq(OfflinePhonetic.convert("aap",IndicLanguages.HINDI),"आप",
            "Roman aap -> Hindi");
        eq(OfflinePhonetic.convert("bharat",IndicLanguages.HINDI),"भारत",
            "Roman bharat -> Hindi");
        eq(OfflinePhonetic.convert("shiksha",IndicLanguages.HINDI),"शिक्षा",
            "Roman shiksha -> Hindi");
        eq(OfflinePhonetic.convert("aapne",IndicLanguages.HINDI),"आपने",
            "Common Hindi: aapne");
        eq(OfflinePhonetic.convert("banao",IndicLanguages.HINDI),"बनाओ",
            "Common Hindi: banao");
        eq(OfflinePhonetic.convert("neeche",IndicLanguages.HINDI),"नीचे",
            "Common Hindi: neeche");
        eq(OfflinePhonetic.convert("upar",IndicLanguages.HINDI),"ऊपर",
            "Common Hindi: upar");
        eq(OfflinePhonetic.convert("keyboard",IndicLanguages.HINDI),"कीबोर्ड",
            "Common Hindi: keyboard");
        eq(OfflinePhonetic.convert("spacebar",IndicLanguages.HINDI),"स्पेसबार",
            "Common Hindi: spacebar");
        eq(OfflinePhonetic.convert("namaste",IndicLanguages.ENGLISH),"namaste",
            "English remains in Roman letters");
        eq(OfflinePhonetic.convert("namaste",IndicLanguages.HINGLISH),"namaste",
            "Hinglish remains in Roman letters");
        for(int lang=2;lang<IndicLanguages.count();lang++){
            String result=OfflinePhonetic.convert("namaste",lang);
            good(!result.isEmpty() && !result.equals("namaste"),
                IndicLanguages.name(lang)+" phonetic output");
        }
        good(inRange(OfflinePhonetic.convert("namaste",3),0x0980,0x09ff),
            "Bangla/Bengali glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",5),0x0b80,0x0bff),
            "Tamil glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",6),0x0c00,0x0c7f),
            "Telugu glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",7),0x0a80,0x0aff),
            "Gujarati glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",8),0x0c80,0x0cff),
            "Kannada glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",9),0x0d00,0x0d7f),
            "Malayalam glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",10),0x0a00,0x0a7f),
            "Punjabi/Gurmukhi glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",11),0x0b00,0x0b7f),
            "Odia glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",21),0xabc0,0xabff),
            "Meitei Mayek glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",22),0x1c50,0x1c7f),
            "Santali/Ol Chiki glyph block");
        good(inRange(OfflinePhonetic.convert("namaste",23),0x0600,0x06ff),
            "Urdu/Arabic glyph block");
        System.out.println("All offline language/IME phonetic checks passed.");
    }
}