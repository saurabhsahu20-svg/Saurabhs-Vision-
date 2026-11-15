package com.saurabhsvision.sbrowser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lightweight offline dictionary model; no Android dependency or network calls. */
public final class DictionaryCore {
    public static final class Entry {
        public final String word;
        public final String meaning;
        public final String hindi;
        public final String category;
        public Entry(String word,String meaning,String hindi,String category) {
            this.word=word;this.meaning=meaning;this.hindi=hindi;this.category=category;
        }
    }
    private DictionaryCore() { }
    public static List<Entry> parse(String raw) {
        List<Entry> data = new ArrayList<>();
        if(raw==null) return data;
        for(String line:raw.split("\\r?\\n")) {
            String[] parts=line.split("\\t",-1);
            if(parts.length==4 && !parts[0].trim().isEmpty() && !parts[0].startsWith("#"))
                data.add(new Entry(parts[0].trim(),parts[1].trim(),parts[2].trim(),parts[3].trim()));
        }
        Collections.sort(data,Comparator.comparing(e->e.word.toLowerCase(Locale.ROOT)));
        return data;
    }
    public static List<Entry> search(List<Entry> source,String query,int max) {
        if(source==null || max<=0) return Collections.emptyList();
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Entry> out=new ArrayList<>();
        for(Entry e:source) {
            if(q.isEmpty() || e.word.toLowerCase(Locale.ROOT).startsWith(q)) {
                out.add(e);if(out.size()==max)return out;
            }
        }
        if(!q.isEmpty() && out.size()<max) {
            for(Entry e:source) {
                if(!e.word.toLowerCase(Locale.ROOT).startsWith(q) &&
                    (e.word.toLowerCase(Locale.ROOT).contains(q)||
                     e.hindi.toLowerCase(Locale.ROOT).contains(q))) {
                    out.add(e);if(out.size()==max)break;
                }
            }
        }
        return out;
    }
}
