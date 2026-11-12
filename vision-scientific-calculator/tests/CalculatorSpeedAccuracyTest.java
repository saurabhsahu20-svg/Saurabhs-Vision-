import com.saurabhsvision.visioncalc.DecimalArithmetic;
import com.saurabhsvision.visioncalc.ExpressionEngine;

/** JVM-only regression for display precision, guardrails and parser throughput. */
public final class CalculatorSpeedAccuracyTest {
    private static int assertions=0;
    private static volatile int sink=0;
    private static void ok(boolean condition,String message){
        assertions++;
        if(!condition)throw new AssertionError(message);
    }
    private static void exact(String expression,String result) {
        String actual=DecimalArithmetic.tryFormat(expression);
        ok(result.equals(actual),
            expression+" → expected '"+result+"', got '"+actual+"'");
    }
    private static void unsupported(String expression) {
        ok(DecimalArithmetic.tryFormat(expression)==null,
            "Must decline expression requiring the scientific engine: "+expression);
    }
    public static void main(String[] args) throws Exception {
        exact("0.1+0.2","0.3");
        exact("0.3-0.2","0.1");
        exact("0.1*0.2","0.02");
        exact("3.25*4","13");
        exact("0.01*(0.2+0.3)","0.005");
        exact("(.5+.05)*2","1.1");
        exact("1234567890123+4","1234567890127");
        exact("9007199254740993+1","9007199254740994");
        exact("12 / 3 + 1.2","5.2");
        exact("3--2","5");
        exact("-(8+2)*3","-30");
        exact("1/8","0.125");
        exact("1.2e-3".replace("e-3","/1000"),"0.0012");
        exact("9÷3","3");
        exact("3×(-2)","-6");
        exact("4−2","2");
        exact("0.00000001*0.00000001","0.0000000000000001");
        unsupported("1/0");
        unsupported("sin(30)");
        unsupported("2π");
        unsupported("25%");
        unsupported("3^2");
        unsupported("Ans+2");
        unsupported("1.2E-3");
        unsupported("1.2.3");
        unsupported("(2+3");
        unsupported("7+");
        unsupported("6(2+3)");
        unsupported("1 mod 2");
        ok("1234567890123".equals(ExpressionEngine.format(1234567890123d)),
            "15-digit safe integer must never be rounded down");
        ok("9007199254740991".equals(ExpressionEngine.format(9007199254740991d)),
            "Largest safely-representable exact integer stays intact");
        ok("0.3".equals(ExpressionEngine.format(0.1+0.2)),
            "Display hides binary representation noise");
        ok(!"0".equals(ExpressionEngine.format(1e-21)),
            "Tiny nonzero scientific results remain visible");
        ok(ExpressionEngine.format(Math.PI).startsWith("3.14159265358979"),
            "15 significant digits for scientific doubles");
        double result=ExpressionEngine.calculate("2^3^2",true,0,0);
        ok(result==512,"Right-associative exponents unchanged");
        double angle=ExpressionEngine.calculate("sin(30)+cos(60)",true,0,0);
        ok(Math.abs(angle-1)<1e-12,"Trigonometry unaffected by decimal display path");

        // There is no claim of 3x speed or measured phone latency.
        // JVM microbenchmark tests pure computation on CI runners only.
        for(int i=0;i<2500;i++){
            sink+=DecimalArithmetic.tryFormat("0.1+0.2").length();
            sink+=ExpressionEngine.format(1234567890123d).length();
        }
        int count=80_000;
        long start=System.nanoTime();
        for(int i=0;i<count;i++){
            String display=DecimalArithmetic.tryFormat("(0.1+0.2)*100");
            if(!"30".equals(display))throw new AssertionError("Inconsistent decimal result");
            sink+=display.length();
        }
        long ms=(System.nanoTime()-start)/1_000_000L;
        ok(ms<25_000,"Offline decimal core benchmark must remain bounded on CI");
        System.out.println("PASS "+assertions+" precision/compatibility assertions");
        System.out.println("PASS "+count+" decimal operations in "+ms+"ms; NOT device WPM/UI latency");
        System.out.println("PASS no changes to supported scientific syntax");
    }
}
