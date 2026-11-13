import com.saurabhsvision.caller.DialerRules;

public final class DialerRulesTest {
    private static int checks;
    private static void valid(String raw,boolean expected,String title){
        checks++;
        boolean actual=DialerRules.validNumber(raw);
        if(actual!=expected)
            throw new AssertionError(title+": expected "+expected+" got "+actual);
    }
    private static void eq(int actual,int expected,String title){
        checks++;
        if(actual!=expected)throw new AssertionError(title);
    }
    public static void main(String[] args){
        valid("9876543210",true,"local mobile");
        valid("+919876543210",true,"international mobile");
        valid("011-2345 6789",true,"landline with hyphen");
        valid("112",true,"local emergency number");
        valid("911",true,"alternate emergency number");
        valid("123 456 (789)",true,"formatted digits");
        valid("*123#",true,"SIM service format");
        valid("0",true,"single digit");
        valid("",false,"empty");
        valid("   ",false,"blank");
        valid(null,false,"null");
        valid("abc+123",false,"letters");
        valid("123;456",false,"semicolon");
        valid("12@34",false,"at symbol");
        valid("12/34",false,"path separators");
        valid("+12+34",false,"double plus");
        valid("123+45",false,"plus not first");
        valid("+",false,"plus only");
        valid("() - .",false,"punctuation only");
        valid("1".repeat(81),false,"overlength");
        valid("1".repeat(80),true,"max length");
        valid("\n123",true,"trim whitespace");
        valid("12\n34",false,"embedded newline");
        eq(DialerRules.theme(-1),0,"lower theme bound");
        eq(DialerRules.theme(0),0,"cosmic theme");
        eq(DialerRules.theme(3),3,"emerald theme");
        eq(DialerRules.theme(4),3,"upper theme bound");
        System.out.println("PASS "+checks+" dialer input validation and theme assertions");
    }
}
