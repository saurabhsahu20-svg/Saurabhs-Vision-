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
        // Screenshot regression: the Space/Enter key row is at index 3,
        // directly ABOVE the shifted Z-X-C row (index 4).
        for(int budget: new int[]{192,174,153,135,118}){
            int[] wanted={39,38,38,44,38};
            int[] lifted=KeyboardLayoutPolicy.fit(wanted,budget,34,3);
            check(lifted.length==5,"Lifted Space action row present: "+budget);
            check(KeyboardLayoutPolicy.total(lifted)<=budget,
                "Lifted Space and bottom alphabet fit into "+budget);
            check(lifted[3]>=Math.min(34,budget-4),
                "Lifted Space row receives reserved height: "+budget);
            check(lifted[4]>0,"Last alphabet/Backspace row also visible: "+budget);
        }
        System.out.println("All Spacebar layout regression cases passed.");
    }
}
