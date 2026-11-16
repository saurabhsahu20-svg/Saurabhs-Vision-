import com.saurabhsvision.sbrowser.DailyNewsSources;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class DailyNewsSourcesTest {
    private static void check(boolean b,String label){
        if(!b)throw new AssertionError(label);
    }
    public static void main(String[] args){
        for(boolean hindi:new boolean[]{true,false})
            for(boolean cg:new boolean[]{true,false})
                for(int days:new int[]{1,3}){
                    for(String topic:new String[]{
                        DailyNewsSources.SPORTS,DailyNewsSources.BREAKING}){
                        String url=DailyNewsSources.feed(topic,hindi,cg,days);
                        check(url.startsWith("https://news.google.com/rss/search?q="),
                            "HTTPS trusted news RSS");
                        String decoded=URLDecoder.decode(url,StandardCharsets.UTF_8);
                        check(decoded.contains("when:"+days+"d"),"proper freshness window");
                        check(decoded.contains("gl=IN"),"India news region");
                        check(!decoded.contains("javascript:"),"no unsafe schemes");
                    }
                }
        check(URLDecoder.decode(DailyNewsSources.feed("sports",true,true,1),
            StandardCharsets.UTF_8).contains("छत्तीसगढ़"),
            "Chhattisgarh local sports");
        check(URLDecoder.decode(DailyNewsSources.feed("breaking",false,false,3),
            StandardCharsets.UTF_8).contains("India latest news"),
            "India headlines in English");
        boolean rejected=false;
        try{DailyNewsSources.feed("horoscope",true,false,1);}
        catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"unsupported category rejected");
        System.out.println("PASS live sports/breaking headline searches and source constraints");
    }
}
