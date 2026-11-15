import com.saurabhsvision.sbrowser.DictionaryEntry;
public class DictionaryEntryTest {
    private static void test(boolean ok,String label) {if(!ok)throw new AssertionError(label);}
    public static void main(String[] args) {
        DictionaryEntry e=DictionaryEntry.parse("atom\tSmallest unit of an element\tपरमाणु\tAn atom contains a nucleus.");
        test(e!=null && "atom".equals(e.word),"word");
        test(e!=null && "परमाणु".equals(e.hindi),"Hindi text");
        test(DictionaryEntry.parse("#comment")==null,"comments");
        test(DictionaryEntry.parse("bad row")==null,"malformed row");
        System.out.println("PASS offline dictionary parser");
    }
}
