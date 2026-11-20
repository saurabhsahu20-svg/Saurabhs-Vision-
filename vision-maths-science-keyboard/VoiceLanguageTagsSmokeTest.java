import com.saurabhsvision.keyboard.IndicLanguages;
import com.saurabhsvision.keyboard.VoiceLanguageTags;

public final class VoiceLanguageTagsSmokeTest {
    private static void eq(String got,String expected){
        if(!expected.equals(got))throw new AssertionError(expected+" != "+got);
    }
    public static void main(String[] args){
        eq(VoiceLanguageTags.forLanguage(IndicLanguages.ENGLISH),"en-IN");
        eq(VoiceLanguageTags.forLanguage(IndicLanguages.HINGLISH),"en-IN");
        eq(VoiceLanguageTags.forLanguage(IndicLanguages.HINDI),"hi-IN");
        eq(VoiceLanguageTags.forLanguage(3),"bn-IN");
        eq(VoiceLanguageTags.forLanguage(5),"ta-IN");
        eq(VoiceLanguageTags.forLanguage(23),"ur-IN");
        for(int i=0;i<IndicLanguages.count();i++){
            String tag=VoiceLanguageTags.forLanguage(i);
            if(!tag.endsWith("-IN")||tag.isEmpty())
                throw new AssertionError("Missing BCP-47 recognition tag for "+i);
        }
        System.out.println("PASS: 24 voice typing locale mappings.");
    }
}
