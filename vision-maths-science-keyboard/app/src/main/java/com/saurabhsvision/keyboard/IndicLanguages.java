package com.saurabhsvision.keyboard;

import java.util.Locale;

/**
 * 22 Eighth Schedule language choices + English / Roman Hinglish.
 * Input is PHONETIC Roman-to-script transliteration, not a translation service.
 * Some Indic spellings, especially non-Devanagari scripts, are approximate.
 */
public final class IndicLanguages {
    private IndicLanguages(){}
    public static final int ENGLISH=0;
    public static final int HINGLISH=1;
    public static final int HINDI=2;

    // name, native name, script for basic offline input, locale code.
    private static final String[][] LANGUAGES={
        {"English","English","Latin","en"},
        {"Hinglish","Roman हिन्दी","Latin","hi"},
        {"Hindi","हिन्दी","Devanagari","hi"},
        {"Bengali / Bangla","বাংলা","Bengali","bn"},
        {"Marathi","मराठी","Devanagari","mr"},
        {"Tamil","தமிழ்","Tamil","ta"},
        {"Telugu","తెలుగు","Telugu","te"},
        {"Gujarati","ગુજરાતી","Gujarati","gu"},
        {"Kannada","ಕನ್ನಡ","Kannada","kn"},
        {"Malayalam","മലയാളം","Malayalam","ml"},
        {"Punjabi","ਪੰਜਾਬੀ","Gurmukhi","pa"},
        {"Odia","ଓଡ଼ିଆ","Oriya","or"},
        {"Assamese","অসমীয়া","Assamese","as"},
        {"Sanskrit","संस्कृतम्","Devanagari","sa"},
        {"Nepali","नेपाली","Devanagari","ne"},
        {"Konkani","कोंकणी","Devanagari","kok"},
        {"Maithili","मैथिली","Devanagari","mai"},
        {"Dogri","डोगरी","Devanagari","doi"},
        {"Bodo","बड़ो","Devanagari","brx"},
        {"Kashmiri (Devanagari)","कॉशुर","Devanagari","ks"},
        {"Sindhi (Devanagari)","सिंधी","Devanagari","sd"},
        {"Manipuri (Meitei Mayek)","ꯃꯩꯇꯩꯂꯣꯟ","MeiteiMayek","mni"},
        {"Santali (Ol Chiki)","ᱥᱟᱱᱛᱟᱲᱤ","OlChiki","sat"},
        {"Urdu","اردو","Arabic","ur"}
    };
    public static int count(){return LANGUAGES.length;}
    public static int norm(int lang){return Math.max(0,Math.min(count()-1,lang));}
    public static String name(int lang){return LANGUAGES[norm(lang)][0];}
    public static String nativeName(int lang){return LANGUAGES[norm(lang)][1];}
    public static String script(int lang){return LANGUAGES[norm(lang)][2];}
    public static String localeCode(int lang){return LANGUAGES[norm(lang)][3];}
    public static String label(int lang){
        int n=norm(lang);
        return LANGUAGES[n][0]+" · "+LANGUAGES[n][1];
    }
    public static String status(int lang){
        int n=norm(lang);
        if(n==0)return "ABC · English";
        if(n==1)return "Hinglish · Roman letters";
        return LANGUAGES[n][1]+" · Roman → "+LANGUAGES[n][2];
    }
    public static boolean phonetic(int lang){return norm(lang)>=HINDI;}
}
