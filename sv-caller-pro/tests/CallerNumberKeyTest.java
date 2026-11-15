import com.saurabhsvision.caller.CallerNumberKey;

public final class CallerNumberKeyTest {
    private static void eq(String actual,String expected,String msg){
        if(!actual.equals(expected))throw new AssertionError(msg);
    }
    public static void main(String[] args){
        eq(CallerNumberKey.normalized("+91 98765 43210"),"9876543210","India international");
        eq(CallerNumberKey.normalized("09876543210"),"9876543210","India trunk");
        eq(CallerNumberKey.normalized("0091 9876543210"),"9876543210","India IDD");
        eq(CallerNumberKey.normalized("98765-43210"),"9876543210","India local");
        eq(CallerNumberKey.key("9876543210"),CallerNumberKey.key("+91 9876543210"),"same photo");
        eq(CallerNumberKey.legacyKey("9876543210"),CallerNumberKey.key("9876543210"),"v1 compatible");
        eq(CallerNumberKey.normalized("+1 (202) 555-1234"),"+12025551234","other country");
        eq(CallerNumberKey.normalized(null),"","null");
        System.out.println("PASS caller number and photo compatibility rules");
    }
}
