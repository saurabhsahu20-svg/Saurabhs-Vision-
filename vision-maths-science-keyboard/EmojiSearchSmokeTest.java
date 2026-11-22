import com.saurabhsvision.keyboard.EmojiCatalog;
import com.saurabhsvision.keyboard.EmojiSearchIndex;
import java.util.Arrays;

/** Minimal offline search contract for the keyboard's emoji search field. */
public final class EmojiSearchSmokeTest {
    private static void check(boolean value,String why){
        if(!value)throw new AssertionError(why);
    }
    public static void main(String[] args){
        check(Arrays.asList(EmojiSearchIndex.search("cat",16)).contains("🐱"),
            "Cat emoji lookup");
        check(Arrays.asList(EmojiSearchIndex.search("love",16)).contains("❤️"),
            "Heart emoji lookup");
        check(Arrays.asList(EmojiSearchIndex.search("happy",16)).contains("😊"),
            "Happy emoji lookup");
        check(EmojiSearchIndex.search("science",16).length>0,
            "Scientific emoji category");
        check(EmojiSearchIndex.search("nature",16).length>0,
            "Nature emoji category");
        check(EmojiSearchIndex.search("zzzzzz_unmapped",16).length==0,
            "Search does not fake results");
        check(EmojiSearchIndex.search("",16).length<=16,
            "Empty search is bounded");
        check(EmojiSearchIndex.search("animals",16).length<=16,
            "Result count is bounded");
        check(EmojiCatalog.count()>=12,"All original emoji categories retained");
        System.out.println("PASS: offline emoji search keyword, category, empty and limit tests.");
    }
}
