import com.saurabhsvision.visioncalc.ComfortTheme;

/** Pure Java checks: contrast and stable theme selection without Android. */
public final class ComfortThemeTest {
    private static int assertions;
    private static void ok(boolean result,String name){
        assertions++;
        if(!result)throw new AssertionError(name);
    }
    public static void main(String[] args){
        ok(ComfortTheme.NAMES.length==5,"4 soft + 1 neon themes");
        ok(ComfortTheme.clamp(-1)==0,"negative theme corrected");
        ok(ComfortTheme.clamp(99)==0,"invalid theme corrected");
        ok(ComfortTheme.clamp(4)==4,"dark theme preserved");
        ok(!ComfortTheme.choose(0).dark(),"soft blue default");
        ok(ComfortTheme.choose(4).dark(),"old neon dark available");
        for(int i=0;i<4;i++){
            ComfortTheme c=ComfortTheme.choose(i);
            ok(!c.dark(),"pastel "+i+" is light");
            ok(ComfortTheme.contrast(c.ink,c.digit)>=7.0,
                "numeric key readable theme "+i);
            ok(ComfortTheme.contrast(c.ink,c.operator)>=7.0,
                "operator key readable theme "+i);
            ok(ComfortTheme.contrast(c.ink,c.clear)>=7.0,
                "clear key readable theme "+i);
            ok(ComfortTheme.contrast(c.ink,c.equals)>=4.5,
                "equals key readable theme "+i);
            ok(ComfortTheme.contrast(c.ink,c.displayTop)>=7.0,
                "display readable theme "+i);
            ok(ComfortTheme.contrast(c.ink,c.panel)>=7.0,
                "tabs readable theme "+i);
        }
        System.out.println("PASS "+assertions+" comfort theme state/contrast assertions");
    }
}
