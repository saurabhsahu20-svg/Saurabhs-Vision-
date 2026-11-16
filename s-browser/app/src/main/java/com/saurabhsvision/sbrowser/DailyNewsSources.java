package com.saurabhsvision.sbrowser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Category-specific news searches; result time & publisher are verified by RSS parser. */
public final class DailyNewsSources {
    private DailyNewsSources(){}
    public static final String SPORTS="sports";
    public static final String BREAKING="breaking";

    public static String searchPage(String category,boolean hindi,boolean cgOnly){
        if(!SPORTS.equals(category)&&!BREAKING.equals(category))
            throw new IllegalArgumentException("Unknown category");
        String phrase;
        if(SPORTS.equals(category)){
            phrase=hindi?(cgOnly?"छत्तीसगढ़ खेल समाचार":"भारत खेल समाचार"):
                (cgOnly?"Chhattisgarh sports news":"India sports news");
        }else{
            phrase=hindi?(cgOnly?"छत्तीसगढ़ ताजा खबर":"भारत समाचार"):
                (cgOnly?"Chhattisgarh latest news":"India latest news");
        }
        return "https://news.google.com/search?q="+
            URLEncoder.encode(phrase,StandardCharsets.UTF_8)+
            "&hl="+(hindi?"hi":"en-IN")+"&gl=IN&ceid=IN:"+(hindi?"hi":"en");
    }
    public static String feed(String category,boolean hindi,boolean cgOnly,int days){
        if(!SPORTS.equals(category)&&!BREAKING.equals(category))
            throw new IllegalArgumentException("Unknown category");
        int window=days==1?1:3;
        String words;
        if(SPORTS.equals(category)){
            words=hindi?
                (cgOnly?"छत्तीसगढ़ खेल समाचार":"भारत खेल समाचार"):
                (cgOnly?"Chhattisgarh sports news":"India sports news");
        }else{
            words=hindi?
                (cgOnly?"छत्तीसगढ़ ताजा समाचार":"भारत प्रमुख समाचार"):
                (cgOnly?"Chhattisgarh latest news":"India latest news");
        }
        String search=words+" when:"+window+"d";
        return "https://news.google.com/rss/search?q="+
            URLEncoder.encode(search,StandardCharsets.UTF_8)+
            "&hl="+(hindi?"hi":"en-IN")+"&gl=IN&ceid=IN:"+(hindi?"hi":"en");
    }
}
