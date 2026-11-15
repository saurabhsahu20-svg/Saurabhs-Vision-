import com.saurabhsvision.sbrowser.AddressResolver;
public class AddressResolverTest {
    private static void test(boolean ok,String label) {if(!ok)throw new AssertionError(label);}
    public static void main(String[] args) {
        test("https://example.org".equals(AddressResolver.resolve("example.org","google")),"domain");
        test("https://example.org/path".equals(AddressResolver.resolve("example.org/path","google")),"path");
        test("https://example.org".equals(AddressResolver.resolve("https://example.org","google")),"url");
        test(AddressResolver.resolve("javascript:alert(1)","google")==null,"unsafe script blocked");
        test(AddressResolver.resolve("file:///test","google")==null,"file blocked");
        test(AddressResolver.resolve("intent://abc","google")==null,"intent blocked");
        test(AddressResolver.resolve("hello world","duckduckgo").contains("duckduckgo.com/?q=hello+world"),"search");
        test(AddressResolver.resolve("science topic","bing").contains("bing.com/search?q="),"bing");
        test(AddressResolver.resolve(" ","google")==null,"blank");
        System.out.println("PASS address resolution and scheme safety");
    }
}
