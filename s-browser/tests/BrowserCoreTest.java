import com.saurabhsvision.sbrowser.UrlTools;
import com.saurabhsvision.sbrowser.DictionaryCore;
import java.util.List;
public final class BrowserCoreTest {
    private static void eq(String a, String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    private static void check(boolean c){if(!c)throw new AssertionError("check failed");}
    public static void main(String[] args){
        eq(UrlTools.resolve("wikipedia.org","Google"),"https://wikipedia.org");
        eq(UrlTools.resolve("http://example.com","Google"),"https://example.com");
        eq(UrlTools.resolve("gravity definition","DuckDuckGo"),"https://duckduckgo.com/?q=gravity+definition");
        eq(UrlTools.resolve("https://example.com/page","Google"),"https://example.com/page");
        check(UrlTools.resolve("javascript:alert(1)","Google")==null);
        check(UrlTools.resolve("file:///sdcard/test","Google")==null);
        check(UrlTools.resolve("https://user:pass@example.com","Google")==null);
        String rows="gravity\tAn attractive force between masses\tगुरुत्वाकर्षण\tScience\n"+
              "geometry\tThe branch of maths studying shapes\tज्यामिति\tMaths";
        List<DictionaryCore.Entry> entries=DictionaryCore.parse(rows);
        check(entries.size()==2);
        eq(DictionaryCore.search(entries,"grav",5).get(0).word,"gravity");
        check(DictionaryCore.search(entries,"कुछ",5).isEmpty());
        System.out.println("PASS: HTTPS search, unsafe URL blocking, and offline dictionary search");
    }
}
