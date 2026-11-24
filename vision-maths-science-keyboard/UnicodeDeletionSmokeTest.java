import com.saurabhsvision.keyboard.UnicodeDeletion;
public final class UnicodeDeletionSmokeTest {
    private static void eq(int expected,int actual,String caseName){
        if(actual!=expected)throw new AssertionError(caseName+": "+actual+" expected "+expected);
    }
    public static void main(String[] args){
        eq(1,UnicodeDeletion.backspaceUnits(null),"null editor");
        eq(1,UnicodeDeletion.backspaceUnits(""),"empty editor");
        eq(1,UnicodeDeletion.backspaceUnits("a"),"ASCII");
        eq(1,UnicodeDeletion.backspaceUnits("न"),"Devanagari");
        eq(1,UnicodeDeletion.backspaceUnits("aह"),"mixed BMP");
        eq(2,UnicodeDeletion.backspaceUnits("😀"),"emoji surrogate pair");
        eq(2,UnicodeDeletion.backspaceUnits("hi😀"),"emoji after Latin");
        eq(2,UnicodeDeletion.backspaceUnits("a🤖"),"robot emoji after Latin");
        eq(1,UnicodeDeletion.backspaceUnits("😀a"),"Latin after emoji");
        eq(1,UnicodeDeletion.backspaceUnits("a\ud83d"),"orphan high surrogate");
        eq(1,UnicodeDeletion.backspaceUnits("\ude00"),"orphan low surrogate");
        System.out.println("PASS: UTF-16 fallback never splits supported emoji pairs; 11 cases.");
    }
}
