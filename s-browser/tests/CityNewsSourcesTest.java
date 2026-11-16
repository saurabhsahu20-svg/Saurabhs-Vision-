import com.saurabhsvision.sbrowser.CityNewsSources;
import com.saurabhsvision.sbrowser.GovtSources;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CityNewsSourcesTest {
    private static void check(boolean works,String label) {
        if(!works)throw new AssertionError(label);
    }
    public static void main(String[] args) {
        List<String> cities=CityNewsSources.cities();
        check(cities.contains("Raipur"),"Raipur covered");
        check(cities.contains("Bhilai"),"Bhilai covered");
        check(cities.contains("Saraipali"),"Saraipali covered");
        check(cities.contains("Durg"),"Durg covered");
        check(cities.contains("Ambikapur"),"Ambikapur covered");
        check(cities.contains("Raigarh"),"Raigarh covered");
        check(cities.contains("Sukma"),"Sukma covered");
        check(cities.size()>45,"Districts and additional city choices");
        for(String d:GovtSources.DISTRICTS){
            check(cities.contains(d),"District included: "+d);
        }
        check("Pithora".equals(CityNewsSources.normalize("  Pithora ")),
            "Custom towns allowed");
        check("सारंगढ़".equals(CityNewsSources.normalize("सारंगढ़")),
            "Hindi city names allowed");
        check(CityNewsSources.normalize("javascript:alert(1)").isEmpty(),
            "Unsafe input rejected");
        check(CityNewsSources.normalize("https://example.org").isEmpty(),
            "Do not allow arbitrary URL in query");
        String feed=CityNewsSources.rssUrl("Raipur",true,7);
        check(feed.startsWith("https://news.google.com/rss/search?q="),
            "Google News HTTPS RSS search");
        String decoded=URLDecoder.decode(feed,StandardCharsets.UTF_8);
        check(decoded.contains("\"Raipur\""),"City exact qualifier");
        check(decoded.contains("छत्तीसगढ़"),"Region qualifier");
        check(decoded.contains("when:7d"),"Seven day search window");
        String english=URLDecoder.decode(CityNewsSources.rssUrl("Bhilai",false,1),
            StandardCharsets.UTF_8);
        check(english.contains("Chhattisgarh"),"English state name");
        check(english.contains("when:1d"),"24 hour search");
        check(CityNewsSources.rssUrl("test.com/evil",true,7)==null,
            "Invalid search city cannot build URL");
        long now=System.currentTimeMillis();
        long hour=60L*60L*1000L;
        check(CityNewsSources.inWindow(now-hour,now,1),"Recent dated item");
        check(!CityNewsSources.inWindow(now-48*hour,now,1),
            "Old item is not 'latest' within 24h");
        check(!CityNewsSources.inWindow(0L,now,7),
            "Missing publication date excluded");
        check(!CityNewsSources.inWindow(now+hour,now,7),
            "Future news is not shown");
        check(CityNewsSources.liveNewsSearch("Raigarh").startsWith(
            "https://news.google.com/search?q="),"Live news search link");
        System.out.println("PASS 33 districts, custom city validation, news source and freshness filters");
    }
}
