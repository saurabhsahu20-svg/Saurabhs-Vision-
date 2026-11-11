import com.saurabhsvision.keyboard.KeyboardLayoutPolicy;
public class KeyboardLayoutPolicySmokeTest {
    static void check(boolean condition,String name){
        if(!condition)throw new AssertionError("FAIL "+name);
        System.out.println("PASS "+name);
    }
    private static void test(String name,int[] rows,int budget,int minFooter){
        int[] measured=KeyboardLayoutPolicy.fit(rows,budget,minFooter);
        check(measured.length==rows.length,name+" includes every row");
        check(KeyboardLayoutPolicy.total(measured)<=budget,name+" stays inside screen");
        for(int i=0;i<measured.length;i++)
            check(measured[i]>0,name+" row "+i+" is visible");
        check(measured[measured.length-1]>=Math.min(minFooter,budget-rows.length+1),
            name+" SPACE / ENTER footer is reserved");
    }
    public static void main(String[] args){
        test("Landscape WhatsApp small viewport",new int[]{41,43,43,43,54},154,36);
        test("Tiny OEM viewport",new int[]{41,43,43,43,54},128,36);
        test("Normal portrait",new int[]{41,43,43,43,54},226,36);
        test("Hindi phonetic 6-row layout",new int[]{41,31,43,43,43,54},180,36);
        test("Emoji 8-row layout",new int[]{41,38,42,42,42,42,47,49},186,36);
        test("Language selector",new int[]{41,29,59,59,37,49},190,36);
        // v1.9.2 reference regression: toolbar + THREE letter rows + Space footer.
        // Physical screen expands, rather than relocating Space ahead of Z-X-C.
        for(int budget: new int[]{307,284,255,210,170,142}){
            int[] wanted={49,52,52,52,60};
            int[] measured=KeyboardLayoutPolicy.fit(wanted,budget,42,4);
            check(measured.length==5,"All four key rows present: "+budget);
            check(KeyboardLayoutPolicy.total(measured)<=budget,
                "Keyboard fits within IME budget: "+budget);
            for(int row=1;row<=3;row++)
                check(measured[row]>0,"Alphabet row "+row+" remains visible: "+budget);
            check(measured[4]>=Math.min(42,budget-4),
                "Real last Spacebar row visible: "+budget);
        }
        int[] full=KeyboardLayoutPolicy.fit(new int[]{49,52,52,52,60},300,42,4);
        check(full[1]>=49&&full[2]>=49&&full[3]>=49,
            "Larger keys preserved when four full rows fit");
        check(full[4]>=57,"Full-height Spacebar preserved on large screen");
        System.out.println("All Spacebar layout regression cases passed.");
    }
}
