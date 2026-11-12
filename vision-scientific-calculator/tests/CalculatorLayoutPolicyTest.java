import com.saurabhsvision.visioncalc.CalculatorLayoutPolicy;

public final class CalculatorLayoutPolicyTest{
    private static void eq(int actual,int expected,String title){
        if(actual!=expected)throw new AssertionError(title+" expected "+expected+" but got "+actual);
        System.out.println("PASS: "+title+" = "+actual+"px");
    }
    private static int calculate(int screen,int bottom,int pad,int original,int max){
        return CalculatorLayoutPolicy.extraPerKeyRow(screen,bottom,pad,10,original,max,5);
    }
    public static void main(String[] args){
        eq(calculate(1536,1000,40,100,180),80,"Tall phone expands all five rows");
        eq(calculate(1536,1000,40,100,250),97,"Unused space is distributed equally");
        eq(calculate(870,840,25,70,120),0,"Short phone keeps fully scrollable keys");
        eq(calculate(900,920,30,70,120),0,"Overflow never shrinks key targets");
        eq(calculate(1200,820,20,110,110),0,"Existing max size stays intact");
        eq(calculate(1200,820,20,110,150),40,"Key height cap obeyed");
        eq(CalculatorLayoutPolicy.extraPerKeyRow(1500,1000,20,10,80,180,0),0,
            "Empty row count is safe");
        eq(CalculatorLayoutPolicy.extraPerKeyRow(1500,1000,20,10,80,180,5),94,
            "Five rows are all expanded uniformly");
        System.out.println("PASS: Adaptive scientific calculator layout tests");
    }
}
