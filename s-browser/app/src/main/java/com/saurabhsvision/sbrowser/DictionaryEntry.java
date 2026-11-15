package com.saurabhsvision.sbrowser;

/** Four-column offline student dictionary record. */
public final class DictionaryEntry {
    public final String word, meaning, hindi, example;
    private DictionaryEntry(String w, String m, String h, String e) {
        word=w; meaning=m; hindi=h; example=e;
    }
    public static DictionaryEntry parse(String row) {
        if (row==null || row.trim().isEmpty() || row.startsWith("#")) return null;
        String[] parts=row.split("\\t", -1);
        if(parts.length!=4 || parts[0].trim().isEmpty()) return null;
        return new DictionaryEntry(parts[0].trim(), parts[1].trim(),
            parts[2].trim(), parts[3].trim());
    }
}
