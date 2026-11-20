package com.saurabhsvision.keyboard;

/** BCP-47 tags for the Android SpeechRecognizer. Engine support varies. */
public final class VoiceLanguageTags {
    private VoiceLanguageTags(){}
    public static String forLanguage(int selectedLanguage) {
        if(selectedLanguage==IndicLanguages.ENGLISH ||
           selectedLanguage==IndicLanguages.HINGLISH) return "en-IN";
        String code=IndicLanguages.localeCode(selectedLanguage);
        if(code==null||code.trim().isEmpty())return "en-IN";
        return code+"-IN";
    }
}