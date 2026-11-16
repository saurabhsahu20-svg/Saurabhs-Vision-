package com.saurabhsvision.sbrowser;

import java.net.URI;
import java.util.Locale;

/**
 * Conservative public-source selection for school assembly. Publisher trust
 * is not the same as independent fact-checking of a headline.
 */
public final class NewsQualityGate {
    private NewsQualityGate(){}

    private static final String[] PUBLISHERS={
        "newsonair.gov.in","pib.gov.in","ddnews.gov.in",
        "thehindu.com","indianexpress.com","hindustantimes.com",
        "indiatoday.in","ndtv.com","reuters.com","apnews.com",
        "bbc.com","bbc.co.uk","timesofindia.indiatimes.com",
        "news18.com","business-standard.com","livemint.com",
        "economictimes.indiatimes.com","theprint.in",
        "espncricinfo.com","olympics.com","icc-cricket.com",
        "bcci.tv","sportstar.thehindu.com"
    };

    /** Only HTTPS matching publisher domains and their genuine subdomains. */
    public static boolean trustedPublisherUrl(String url){
        if(url==null||url.trim().isEmpty())return false;
        try{
            URI parsed=new URI(url.trim());
            if(!"https".equalsIgnoreCase(parsed.getScheme()))return false;
            if(parsed.getRawUserInfo()!=null)return false;
            String host=parsed.getHost();
            if(host==null)return false;
            host=host.toLowerCase(Locale.ROOT);
            for(String site:PUBLISHERS){
                if(host.equals(site)||host.endsWith("."+site))return true;
            }
        }catch(Exception ignored){}
        return false;
    }

    public static boolean suitableHeadline(String title){
        if(title==null)return false;
        String t=title.trim().toLowerCase(Locale.ROOT);
        if(t.length()<14||t.length()>240)return false;
        // Ads, forecasts and rolling "live" pages are not concise facts.
        return !t.startsWith("sponsored")&&!t.contains("advertisement")&&
            !t.startsWith("horoscope")&&!t.contains("astrology prediction");
    }

    public static String searchForCorroboration(String headline){
        String q=headline==null?"":headline.trim();
        if(q.length()>120)q=q.substring(0,120);
        return "https://news.google.com/search?q="+
            java.net.URLEncoder.encode(q,java.nio.charset.StandardCharsets.UTF_8)+
            "&hl=en-IN&gl=IN&ceid=IN:en";
    }
}
