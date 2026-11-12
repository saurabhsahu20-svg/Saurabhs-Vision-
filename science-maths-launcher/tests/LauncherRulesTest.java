import com.saurabhsvision.launcher.LauncherRules;

public final class LauncherRulesTest {
    private static int checks=0;
    private static void assertTrue(boolean value,String label){
        checks++;
        if(!value)throw new AssertionError(label);
    }
    public static void main(String[] args){
        assertTrue(LauncherRules.themeIndex(0,4)==0,"default theme");
        assertTrue(LauncherRules.themeIndex(3,4)==3,"last theme");
        assertTrue(LauncherRules.themeIndex(4,4)==0,"theme cycles");
        assertTrue(LauncherRules.themeIndex(-1,4)==3,"negative theme normalized");
        assertTrue(LauncherRules.matches("Calculator","com.calc.app","calc"),"app label match");
        assertTrue(LauncherRules.matches("My App","com.study.math","MATH"),"package match");
        assertTrue(LauncherRules.matches("Clock","com.clock.app",""),"empty search shows all");
        assertTrue(!LauncherRules.matches("Clock","com.clock.app","Camera"),"not matching");
        assertTrue(LauncherRules.matches("S VISION","com.example.svision","  VISION  "),"trim search");
        try{
            LauncherRules.themeIndex(0,0);
            throw new AssertionError("Expected invalid theme count rejection");
        }catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS: "+checks+" launcher theme and app search assertions");
    }
}
