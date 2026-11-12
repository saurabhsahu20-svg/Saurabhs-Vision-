package com.saurabhsvision.visioncalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Offline scientific expression evaluator, independent of Android.
 *
 * Grammar:
 * expression = term (('+'|'-') term)*
 * term       = signed (('*'|'/'|'mod'|implicit multiplication) signed)*
 * signed     = ('+'|'-') signed | power
 * power      = postfix ('^' signed)?
 * postfix    = primary ('!'|'%')*
 * primary    = number | constant | identifier '(' arg (',' arg)* ')'
 *            | '(' expression ')'
 *
 * Power associates to the right: 2^3^2=512, -2^2=-4.
 * Percent means /100 (e.g. 25% of 200 = 25% × 200 = 50).
 */
public final class ExpressionEngine {
    private ExpressionEngine(){}
    public static final class CalcException extends Exception {
        public CalcException(String message){super(message);}
    }
    public static double calculate(String input, boolean degrees,double ans,double memory)
            throws CalcException {
        if(input==null||input.trim().isEmpty())throw new CalcException("Enter an expression");
        if(input.length()>500)throw new CalcException("Expression is too long");
        Parser p=new Parser(input,degrees,ans,memory);
        double value=p.expression();
        p.skipSpaces();
        if(p.pos!=p.text.length())throw new CalcException("Unexpected character at "+(p.pos+1));
        return finite(value);
    }
    public static String format(double value){
        if(!Double.isFinite(value))return "Math error";
        // Never hide scientifically meaningful tiny results (e.g. 1 electron-volt).
        if(value==0)return "0";
        // Preserve EVERY digit of a safely representable whole number.
        // The old 12-significant-digit rounding changed 1234567890123
        // into 1234567890120 even though the double contained it exactly.
        if(value==Math.rint(value) && Math.abs(value)<=9007199254740991d)
            return BigDecimal.valueOf(value).toBigIntegerExact().toString();
        BigDecimal decimal=BigDecimal.valueOf(value)
            .round(new MathContext(15,RoundingMode.HALF_UP)).stripTrailingZeros();
        String plain=decimal.toPlainString();
        if(plain.length()<=20 && !plain.contains("E"))return plain;
        return decimal.toString().replace("E+","E");
    }
    private static double finite(double n) throws CalcException {
        if(!Double.isFinite(n))throw new CalcException("Math domain or overflow error");
        return n;
    }
    private static final class Parser {
        final String text;
        final boolean degrees;
        final double ans,memory;
        int pos,depth;
        Parser(String s,boolean d,double a,double m){
            text=s.replace('×','*').replace('÷','/').replace('−','-').replace('√','√')
                  .replace('π','π').replace('％','%').replace('^','^');
            degrees=d;ans=a;memory=m;
        }
        void skipSpaces(){while(pos<text.length()&&Character.isWhitespace(text.charAt(pos)))pos++;}
        boolean eat(char c){skipSpaces();if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}return false;}
        boolean matchWord(String word){
            skipSpaces();
            if(text.regionMatches(true,pos,word,0,word.length())&&
                (pos+word.length()==text.length()||
                    !Character.isLetter(text.charAt(pos+word.length())))){
                pos+=word.length();return true;
            }
            return false;
        }
        double expression() throws CalcException {
            double value=term();
            while(true){
                if(eat('+'))value=finite(value+term());
                else if(eat('-'))value=finite(value-term());
                else return value;
            }
        }
        double term() throws CalcException {
            double value=signed();
            while(true){
                if(eat('*'))value=finite(value*signed());
                else if(eat('/')){double divisor=signed();
                    if(divisor==0)throw new CalcException("Cannot divide by zero");
                    value=finite(value/divisor);
                }
                else if(matchWord("mod")){
                    double divisor=signed();
                    if(divisor==0)throw new CalcException("Cannot divide by zero");
                    value=finite(value%divisor);
                }
                else if(startsPrimary())value=finite(value*signed());
                else return value;
            }
        }
        boolean startsPrimary(){
            skipSpaces();
            if(pos>=text.length())return false;
            char c=text.charAt(pos);
            return c=='('||c=='π'||c=='τ'||c=='φ'||c=='√'||
                Character.isDigit(c)||Character.isLetter(c);
        }
        double signed() throws CalcException {
            if(++depth>80)throw new CalcException("Too many nested operations");
            try{
                if(eat('+'))return signed();
                if(eat('-'))return finite(-signed());
                return power();
            }finally{depth--;}
        }
        double power() throws CalcException {
            double base=postfix();
            if(eat('^'))return finite(Math.pow(base,signed()));
            return base;
        }
        double postfix() throws CalcException {
            double value=primary();
            while(true){
                if(eat('!'))value=factorial(value);
                else if(eat('%'))value=finite(value/100.0);
                else return value;
            }
        }
        double primary() throws CalcException {
            skipSpaces();
            if(eat('(')){
                double result=expression();
                if(!eat(')'))throw new CalcException("Missing closing bracket");
                return result;
            }
            if(eat('π'))return Math.PI;
            if(eat('τ'))return 2*Math.PI;
            if(eat('φ'))return (1+Math.sqrt(5))/2;
            if(eat('√')){
                // Prefix square root works with √(9) or √9.
                if(eat('(')){
                    double argument=expression();
                    if(!eat(')'))throw new CalcException("Missing closing bracket");
                    return function("sqrt",new double[]{argument});
                }
                return function("sqrt",new double[]{postfix()});
            }
            skipSpaces();
            if(pos<text.length()&&(Character.isDigit(text.charAt(pos))||text.charAt(pos)=='.')){
                return number();
            }
            if(pos<text.length()&&Character.isLetter(text.charAt(pos))){
                int start=pos;
                while(pos<text.length()&&Character.isLetter(text.charAt(pos)))pos++;
                String name=text.substring(start,pos).toLowerCase(Locale.ROOT);
                switch(name){
                    case "pi":return Math.PI;
                    case "tau":return 2*Math.PI;
                    case "e":return Math.E;
                    case "ans":return ans;
                    case "m":return memory;
                    case "phi":return (1+Math.sqrt(5))/2;
                }
                if(!eat('('))throw new CalcException("Unknown function or constant: "+name);
                double first=expression();
                if(eat(',')){
                    double second=expression();
                    if(!eat(')'))throw new CalcException("Missing closing bracket");
                    return function(name,new double[]{first,second});
                }
                if(!eat(')'))throw new CalcException("Missing closing bracket");
                return function(name,new double[]{first});
            }
            throw new CalcException(pos>=text.length()?
                "Incomplete expression":"Unexpected character at "+(pos+1));
        }
        double number() throws CalcException {
            int start=pos,integerDigits=0;
            while(pos<text.length()&&Character.isDigit(text.charAt(pos))){
                pos++;integerDigits++;
            }
            if(pos<text.length()&&text.charAt(pos)=='.'){
                pos++;
                while(pos<text.length()&&Character.isDigit(text.charAt(pos))){
                    pos++;integerDigits++;
                }
            }
            if(integerDigits==0)throw new CalcException("Invalid number");
            // Exponent can be explicit 1.2E-3; plain e is Euler's constant.
            if(pos<text.length()&&(text.charAt(pos)=='E'||text.charAt(pos)=='e')){
                int exponentStart=pos;pos++;
                if(pos<text.length()&&(text.charAt(pos)=='+'||text.charAt(pos)=='-'))pos++;
                int digitsStart=pos;
                while(pos<text.length()&&Character.isDigit(text.charAt(pos)))pos++;
                if(digitsStart==pos)pos=exponentStart;
            }
            try{return finite(Double.parseDouble(text.substring(start,pos)));}
            catch(NumberFormatException ex){throw new CalcException("Invalid number");}
        }
        double factorial(double value) throws CalcException {
            if(value<0||value!=Math.rint(value)||value>170)
                throw new CalcException("Factorial requires a whole number from 0 to 170");
            double out=1;
            for(int i=2;i<=value;i++)out*=i;
            return finite(out);
        }
        double degreesToRadians(double n){return degrees?Math.toRadians(n):n;}
        double radiansToDegrees(double n){return degrees?Math.toDegrees(n):n;}
        double trigRounded(double n) throws CalcException{
            // Trigonometric functions have machine-error residuals at 0, 90, 180°.
            if(Math.abs(n)<1e-14)return 0;
            return finite(n);
        }
        void oneArg(double[] args,String name) throws CalcException {
            if(args.length!=1)throw new CalcException(name+" needs one argument");
        }
        void twoArgs(double[] args,String name) throws CalcException {
            if(args.length!=2)throw new CalcException(name+" needs two arguments");
        }
        long whole(double value,String label) throws CalcException{
            if(value<0||value!=Math.rint(value)||value>1000000)
                throw new CalcException(label+" needs a nonnegative whole number");
            return (long)value;
        }
        double function(String name,double[] args) throws CalcException{
            double n=args[0];
            switch(name){
                case "sin":oneArg(args,name);return trigRounded(Math.sin(degreesToRadians(n)));
                case "cos":oneArg(args,name);return trigRounded(Math.cos(degreesToRadians(n)));
                case "tan":{
                    oneArg(args,name);
                    double rad=degreesToRadians(n);
                    if(Math.abs(Math.cos(rad))<1e-13)
                        throw new CalcException("Tangent is undefined at this angle");
                    return finite(Math.tan(rad));
                }
                case "cot":{
                    oneArg(args,name);
                    double angle=degreesToRadians(n);
                    double sin=Math.sin(angle);
                    if(Math.abs(sin)<1e-13)
                        throw new CalcException("Cotangent is undefined at this angle");
                    return trigRounded(finite(Math.cos(angle)/sin));
                }
                case "sec":{
                    oneArg(args,name);
                    double cosine=Math.cos(degreesToRadians(n));
                    if(Math.abs(cosine)<1e-13)
                        throw new CalcException("Secant is undefined at this angle");
                    return finite(1/cosine);
                }
                case "csc":{
                    oneArg(args,name);
                    double sine=Math.sin(degreesToRadians(n));
                    if(Math.abs(sine)<1e-13)
                        throw new CalcException("Cosecant is undefined at this angle");
                    return finite(1/sine);
                }
                case "asin":oneArg(args,name);return finite(radiansToDegrees(Math.asin(n)));
                case "acos":oneArg(args,name);return finite(radiansToDegrees(Math.acos(n)));
                case "atan":oneArg(args,name);return finite(radiansToDegrees(Math.atan(n)));
                case "sinh":oneArg(args,name);return finite(Math.sinh(n));
                case "cosh":oneArg(args,name);return finite(Math.cosh(n));
                case "tanh":oneArg(args,name);return finite(Math.tanh(n));
                case "sqrt":oneArg(args,name);return finite(Math.sqrt(n));
                case "cbrt":oneArg(args,name);return finite(Math.cbrt(n));
                case "abs":oneArg(args,name);return Math.abs(n);
                case "ln":oneArg(args,name);return finite(Math.log(n));
                case "log":oneArg(args,name);return finite(Math.log10(n));
                case "exp":oneArg(args,name);return finite(Math.exp(n));
                case "floor":oneArg(args,name);return Math.floor(n);
                case "ceil":oneArg(args,name);return Math.ceil(n);
                case "round":oneArg(args,name);return Math.floor(n+0.5);
                case "fact":oneArg(args,name);return factorial(n);
                case "inv":
                case "recip":
                    oneArg(args,name);
                    if(n==0)throw new CalcException("Cannot divide by zero");
                    return finite(1/n);
                case "deg":oneArg(args,name);return Math.toDegrees(n);
                case "rad":oneArg(args,name);return Math.toRadians(n);
                case "root":
                    twoArgs(args,name);
                    if(n==0)throw new CalcException("Zeroth root is undefined");
                    if(args[1]<0){
                        if(n!=Math.rint(n)||Math.abs(n%2)!=1)
                            throw new CalcException("Even root of a negative number is undefined");
                        return finite(-Math.pow(-args[1],1/n));
                    }
                    return finite(Math.pow(args[1],1/n));
                case "pow":twoArgs(args,name);return finite(Math.pow(n,args[1]));
                case "min":twoArgs(args,name);return Math.min(n,args[1]);
                case "max":twoArgs(args,name);return Math.max(n,args[1]);
                case "ncr":
                case "comb":{
                    twoArgs(args,name);
                    long k=whole(args[1],"nCr");
                    long m=whole(n,"nCr");
                    if(m>10000||k>m)throw new CalcException("nCr requires n≥r, n≤10,000");
                    k=Math.min(k,m-k);
                    double out=1;
                    for(int i=1;i<=k;i++)out=finite(out*(m-k+i)/i);
                    return out;
                }
                case "npr":
                case "perm":{
                    twoArgs(args,name);
                    long m=whole(n,"nPr"),k=whole(args[1],"nPr");
                    if(m>10000||k>m)throw new CalcException("nPr requires n≥r, n≤10,000");
                    double out=1;
                    for(long i=0;i<k;i++)out=finite(out*(m-i));
                    return out;
                }
                case "gcd":
                case "lcm":{
                    twoArgs(args,name);
                    long a=whole(n,name),b=whole(args[1],name);
                    long x=a,y=b;
                    while(y!=0){long t=x%y;x=y;y=t;}
                    if(name.equals("gcd"))return x;
                    return finite(x==0?0:(double)(a/x)*b);
                }
                default:throw new CalcException("Unknown function: "+name);
            }
        }
    }
}
