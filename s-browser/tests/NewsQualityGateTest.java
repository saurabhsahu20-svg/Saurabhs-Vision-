import com.saurabhsvision.sbrowser.NewsQualityGate;

public final class NewsQualityGateTest {
    private static void ok(boolean x,String label){
        if(!x)throw new AssertionError(label);
    }
    public static void main(String[] args){
        String[] trusted={
            "https://www.thehindu.com/news/",
            "https://indianexpress.com/",
            "https://www.newsonair.gov.in/",
            "https://pib.gov.in/",
            "https://www.bbc.com/news",
            "https://www.reuters.com/",
            "https://www.espncricinfo.com/",
            "https://sportstar.thehindu.com/",
            "https://olympics.com/"
        };
        for(String s:trusted)ok(NewsQualityGate.trustedPublisherUrl(s),"approved publisher "+s);
        String[] unsafe={
            "http://thehindu.com/",
            "https://thehindu.com.evil.example/",
            "https://not-thehindu.com/",
            "https://thehindu.com@evil.example/",
            "https://evil.example/thehindu.com",
            "javascript:alert(1)",
            "https://example.com/",
            "https://user:pass@thehindu.com/",
            "https://www.thehindu.com.attacker.invalid/"
        };
        for(String s:unsafe)ok(!NewsQualityGate.trustedPublisherUrl(s),
            "rejected untrusted publisher "+s);
        ok(NewsQualityGate.suitableHeadline("Official election results published by commission"),
            "ordinary dated reporting accepted");
        ok(!NewsQualityGate.suitableHeadline("Click here"),"very short title blocked");
        ok(!NewsQualityGate.suitableHeadline("Sponsored update about sports matches"),
            "sponsored title blocked");
        ok(!NewsQualityGate.suitableHeadline("Horoscope today predicts tomorrow's exam"),
            "horoscope forecast excluded");
        String cross=NewsQualityGate.searchForCorroboration(
            "School students learn about science");
        ok(cross.startsWith("https://news.google.com/search?q="),
            "cross-reference search remains HTTPS");
        ok(cross.contains("Science")||cross.contains("science"),
            "cross-reference includes headline");
        System.out.println("PASS strict trusted publisher HTTPS host validation and headline checks");
    }
}
