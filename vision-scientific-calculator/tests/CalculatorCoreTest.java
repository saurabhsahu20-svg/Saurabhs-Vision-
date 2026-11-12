import com.saurabhsvision.visioncalc.ExpressionEngine;
import com.saurabhsvision.visioncalc.UnitConverter;

public final class CalculatorCoreTest {
    private static int checks=0;
    private static void near(double actual,double expected,double tolerance,String label){
        checks++;
        if(!Double.isFinite(actual)||Math.abs(actual-expected)>tolerance*Math.max(1.0,Math.abs(expected)))
            throw new AssertionError(label+" expected "+expected+" got "+actual);
    }
    private static double calc(String expression,boolean deg) throws Exception{
        return ExpressionEngine.calculate(expression,deg,5,10);
    }
    private static void error(String expression,String expected) throws Exception{
        checks++;
        try{calc(expression,true);}
        catch(ExpressionEngine.CalcException ex){
            if(!ex.getMessage().contains(expected))
                throw new AssertionError(expression+" wrong error: "+ex.getMessage());
            return;
        }
        throw new AssertionError("Expected error: "+expression);
    }
    public static void main(String[] args) throws Exception{
        near(calc("2+3*4",true),14,1e-13,"operators");
        near(calc("(2+3)*4",true),20,1e-13,"parentheses");
        near(calc("2^3^2",true),512,1e-13,"right-associative exponent");
        near(calc("-2^2",true),-4,1e-13,"unary exponent precedence");
        near(calc("(-2)^2",true),4,1e-13,"negative power base");
        near(calc("2^-3",true),.125,1e-13,"negative exponent");
        near(calc("5!",true),120,1e-13,"factorial");
        near(calc("0!",true),1,1e-13,"factorial zero");
        near(calc("25% * 200",true),50,1e-13,"percentage");
        near(calc("2π",true),2*Math.PI,1e-13,"implicit multiply pi");
        near(calc("2(3+4)",true),14,1e-13,"implicit multiply bracket");
        near(calc("3sin(30)",true),1.5,1e-13,"implicit multiply function");
        near(calc("sin(30)",true),.5,1e-13,"sine degrees");
        near(calc("cos(60)",true),.5,1e-13,"cosine degrees");
        near(calc("tan(45)",true),1,1e-13,"tangent degrees");
        near(calc("sin(π/2)",false),1,1e-13,"sine radians");
        near(calc("asin(1)",true),90,1e-13,"inverse sine");
        near(calc("ln(e)",true),1,1e-13,"natural log");
        near(calc("log(1000)",true),3,1e-13,"base ten log");
        near(calc("√(81)",true),9,1e-13,"square root symbol");
        near(calc("sqrt(2)^2",true),2,1e-12,"square root function");
        near(calc("cbrt(27)",true),3,1e-13,"cube root");
        near(calc("comb(5,2)",true),10,1e-13,"combinations");
        near(calc("perm(5,2)",true),20,1e-13,"permutations");
        near(calc("gcd(24,18)",true),6,1e-13,"gcd");
        near(calc("lcm(12,18)",true),36,1e-13,"lcm");
        near(calc("root(3,27)",true),3,1e-13,"nth root");
        near(calc("root(3,-8)",true),-2,1e-13,"odd root of negative value");
        near(calc("pow(2,10)",true),1024,1e-13,"power function");
        near(calc("max(8,13)-min(5,3)",true),10,1e-13,"min max");
        near(calc("10mod3",true),1,1e-13,"modulus");
        near(calc("Ans+M",true),15,1e-13,"answer and memory");
        near(calc("1.25E-3",true),.00125,1e-13,"scientific notation");
        near(calc("floor(3.9)+ceil(2.1)",true),6,1e-13,"floor ceil");
        near(calc("0.1+0.2",true),.3,1e-13,"fractional addition");
        if(!"0.3".equals(ExpressionEngine.format(calc("0.1+0.2",true))))
            throw new AssertionError("Floating point display must be human-readable");
        checks++;
        error("1/0","divide by zero");
        error("sqrt(-1)","Math domain");
        error("ln(0)","Math domain");
        error("171!","Factorial");
        error("tan(90)","Tangent is undefined");
        error("root(2,-9)","Even root");
        error("3+","Incomplete expression");
        error("(2+3","Missing closing bracket");
        error("comb(3,7)","nCr");
        error("mystery(7)","Unknown function");
        near(UnitConverter.convert(1000,0,0,2),1,1e-13,"mm to m");
        near(UnitConverter.convert(1,0,3,2),1000,1e-13,"km to m");
        near(UnitConverter.convert(32,2,1,0),0,1e-13,"Fahrenheit to Celsius");
        near(UnitConverter.convert(0,2,0,1),32,1e-13,"Celsius to Fahrenheit");
        near(UnitConverter.convert(273.15,2,2,0),0,1e-13,"Kelvin to Celsius");
        near(UnitConverter.convert(1,6,2,1),1.609344,1e-12,"mph to km/h");
        near(UnitConverter.convert(1,8,5,0),3600000,1e-12,"kWh to joule");
        if("0".equals(ExpressionEngine.format(UnitConverter.convert(1,8,6,0))))
            throw new AssertionError("Small eV values must NOT be formatted as zero");
        checks++;
        near(UnitConverter.convert(1,10,5,1),1024,1e-12,"KiB to byte");
        near(UnitConverter.convert(180,9,0,1),Math.PI,1e-12,"degree to rad");
        near(UnitConverter.convert(60,11,4,0),1,1e-12,"rpm to Hz");
        if(UnitConverter.count()!=12)throw new AssertionError("Missing conversion categories");
        checks++;
        try{
            UnitConverter.convert(-274,2,0,2);
            throw new AssertionError("Below absolute zero must fail");
        }catch(IllegalArgumentException expected){checks++;}
        // Regression load: pure core should remain responsive with many calculations.
        double checksum=0;
        long start=System.nanoTime();
        for(int i=0;i<100_000;i++){
            checksum+=calc("sin(30)^2+cos(30)^2+log(100)",true);
        }
        near(checksum,300_000,1e-10,"repeat calculation accuracy");
        long elapsedMs=(System.nanoTime()-start)/1000000;
        System.out.println("PASS "+checks+" assertions; 100,000 scientific expressions in "+elapsedMs+"ms");
        System.out.println("NOTE: JVM core benchmark only; not real Android UI latency.");
    }
}
