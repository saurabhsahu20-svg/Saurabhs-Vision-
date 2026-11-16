package com.saurabhsvision.sbrowser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Search helpers. A matching RSS headline is NOT fact-checking. */
public final class CityNewsSources {
    private CityNewsSources(){}

    public static final String STATE="All Chhattisgarh";
    public static final String GOVT_NEWS="https://jansampark.cg.gov.in/dprnewsportal/";
    public static final String DISTRICTS="https://chhattisgarh.s3waas.gov.in/";

    // Includes the official 33 districts and additional major cities/towns.
    // User may enter any other Chhattisgarh locality in the search field.
    public static final String[] POPULAR={"All Chhattisgarh",
        "Raipur","Bhilai","Durg","Nava Raipur","Bilaspur","Rajnandgaon",
        "Dhamtari","Mahasamund","Saraipali","Raigarh","Korba","Ambikapur",
        "Jagdalpur","Kawardha","Champa","Janjgir","Balod","Bemetara",
        "Kanker","Sakti","Jashpur Nagar","Kondagaon","Dantewada","Bijapur",
        "Narayanpur","Surajpur","Manendragarh","Chirmiri","Pendra","Mungeli",
        "Sukma","Dongargarh","Tilda Newra","Arang","Abhanpur","Bhatapara",
        "Baloda Bazar","Dalli Rajhara","Kumhari","Katghora","Kharsia",
        "Pathalgaon","Saraipali","Pithora","Bagbahara","Kurud","Charama"};

    public static String normalize(String city) {
        String name=city==null?"":city.trim().replaceAll("\\s+"," ");
        if(name.isEmpty()||name.equalsIgnoreCase(STATE))return STATE;
        if(name.length()>55)return "";
        if(!name.matches("[\\p{L}\\p{M} .'-]+"))return "";
        return name;
    }

    public static List<String> cities(){
        ArrayList<String> all=new ArrayList<>();
        for(String name:POPULAR)if(!all.contains(name))all.add(name);
        for(String name:GovtSources.DISTRICTS)if(!all.contains(name))all.add(name);
        return Collections.unmodifiableList(all);
    }

    public static String rssUrl(String city,boolean hindi,int days){
        String chosen=normalize(city);
        if(chosen.isEmpty())return null;
        int window=days==1?1:days==3?3:7;
        String query;
        String region=hindi?"छत्तीसगढ़":"Chhattisgarh";
        if(STATE.equals(chosen)) {
            query=region+" news when:"+window+"d";
        } else {
            // State qualifier reduces same-named places from other regions.
            query="\""+chosen+"\" \""+region+"\" when:"+window+"d";
        }
        String code=hindi?"hi":"en-IN";
        return "https://news.google.com/rss/search?q="+
            URLEncoder.encode(query,StandardCharsets.UTF_8)+
            "&hl="+code+"&gl=IN&ceid=IN:"+(hindi?"hi":"en");
    }

    public static String liveNewsSearch(String city){
        String name=normalize(city);
        if(name.isEmpty())return null;
        String phrase=(name.equals(STATE)?"छत्तीसगढ़":name+" छत्तीसगढ़")+
            " latest news when:7d";
        return "https://news.google.com/search?q="+
            URLEncoder.encode(phrase,StandardCharsets.UTF_8)+
            "&hl=hi&gl=IN&ceid=IN:hi";
    }

    public static boolean inWindow(long time,long now,int days){
        // Do not label a future-dated or stale article "latest".
        return time>0L&&time<=now+5L*60L*1000L&&
            time>=now-days*24L*60L*60L*1000L;
    }
}
