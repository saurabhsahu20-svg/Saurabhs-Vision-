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
        // OEM landscape navigation overlays part of the IME surface. The
        // keyboard now explicitly reserves a 58dp strip BELOW the Space row.
        // We simulate the remaining drawable area (305dp cap - 58dp nav inset
        // - 5dp top padding), including constrained screens.
        for(int cap: new int[]{305,275,245}){
            int safeInset=58,topInset=5;
            int budget=cap-safeInset-topInset;
            int[] rows=KeyboardLayoutPolicy.fit(
                new int[]{51,58,58,58,65},budget,42,4);
            check(KeyboardLayoutPolicy.total(rows)+topInset+safeInset<=cap,
                "Navigation-safe four-row keyboard fits in "+cap+"dp");
            check(rows[4]>=42,
                "Space/Enter keeps tappable height above nav bar at "+cap+"dp");
            check(rows[1]>0&&rows[2]>0&&rows[3]>0,
                "Every QWERTY letter row preserved at "+cap+"dp");
        }
        // S Board 1.9.4: verify physical viewport fitting from user's
        // landscape screenshot (1197 x 563 pixels; navigation starts ~491).
        int screenshotScreenHeight=563;
        int viewportCap=KeyboardLayoutPolicy.landscapeHeightCap(screenshotScreenHeight,750);
        check(viewportCap>=355&&viewportCap<=375,
            "Landscape cap is proportional to the 563px screenshot");
        check(100+viewportCap<491,
            "Keyboard bottom stays ABOVE the screen navigation strip");
        for(int screen:new int[]{420,480,563,620,720}){
            int cap=KeyboardLayoutPolicy.landscapeHeightCap(screen,700);
            check(cap<=screen*0.67f,
                "Keyboard frame cannot grow past mobile landscape viewport at "+screen+"px");
            int pad=45; // representative 30dp bottom inset at 1.5x density
            int[] rows=KeyboardLayoutPolicy.fit(
                new int[]{73,83,83,83,93},cap-pad-8,54,4);
            check(KeyboardLayoutPolicy.total(rows)+pad+8<=cap,
                "All real key rows plus nav clearance fit at "+screen+"px");
            check(rows[4]>=54,"Space/Enter remains tappable at "+screen+"px");
            for(int k=1;k<=3;k++)check(rows[k]>0,
                "All letter rows retained at "+screen+"px (row "+k+")");
        }
        int[] full=KeyboardLayoutPolicy.fit(new int[]{49,52,52,52,60},300,42,4);
        check(full[1]>=49&&full[2]>=49&&full[3]>=49,
            "Larger keys preserved when four full rows fit");
        check(full[4]>=57,"Full-height Spacebar preserved on large screen");
        System.out.println("All Spacebar layout regression cases passed.");
    }
}
