import com.saurabhsvision.caller.SlideRule;

public final class SlideRuleTest {
    private static void check(int actual,int expected,String label){
        if(actual!=expected)throw new AssertionError(label);
    }
    public static void main(String[] args){
        check(SlideRule.decision(0f,100f),0,"stationary");
        check(SlideRule.decision(74f,100f),0,"short right swipe");
        check(SlideRule.decision(-74f,100f),0,"short left swipe");
        check(SlideRule.decision(75f,100f),1,"confirmed answer");
        check(SlideRule.decision(-75f,100f),-1,"confirmed decline");
        check(SlideRule.decision(120f,100f),1,"right overshoot");
        check(SlideRule.decision(-120f,100f),-1,"left overshoot");
        check(SlideRule.decision(10f,0f),0,"unlaid out view");
        check(SlideRule.decision(Float.NaN,100f),0,"invalid input");
        System.out.println("PASS sliding Answer / Decline gesture thresholds");
    }
}
