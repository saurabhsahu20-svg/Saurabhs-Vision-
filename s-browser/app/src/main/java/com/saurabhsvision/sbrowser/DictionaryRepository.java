package com.saurabhsvision.sbrowser;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** An offline, compact starter dictionary. No network or accounts required. */
public final class DictionaryRepository {
    private final ArrayList<DictionaryEntry> entries=new ArrayList<>();
    public DictionaryRepository(Context context){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
            context.getAssets().open("dictionary.tsv"),StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                DictionaryEntry entry=DictionaryEntry.parse(line);
                if(entry!=null)entries.add(entry);
            }
        }catch(Exception ignored){}
        entries.sort(Comparator.comparing(e->e.word.toLowerCase(Locale.ROOT)));
    }
    public int size(){return entries.size();}
    public List<DictionaryEntry> search(String query){
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        ArrayList<DictionaryEntry> results=new ArrayList<>();
        for(DictionaryEntry item:entries){
            if(q.isEmpty() || item.word.toLowerCase(Locale.ROOT).startsWith(q) ||
               item.hindi.contains(q)){
                results.add(item);
                if(results.size()>=55)break;
            }
        }
        if(results.isEmpty()&&!q.isEmpty()){
            for(DictionaryEntry item:entries){
                if(item.meaning.toLowerCase(Locale.ROOT).contains(q) ||
                   item.word.toLowerCase(Locale.ROOT).contains(q)){
                    results.add(item);
                    if(results.size()>=55)break;
                }
            }
        }
        return Collections.unmodifiableList(results);
    }
}
