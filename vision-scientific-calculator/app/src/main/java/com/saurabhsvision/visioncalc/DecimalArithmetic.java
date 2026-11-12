package com.saurabhsvision.visioncalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Exact-decimal display path for ordinary +, -, ×, ÷ and parentheses.
 *
 * Decimal inputs like 0.1+0.2 are not exactly representable as IEEE-754
 * doubles. This parser preserves the entered decimal values for display,
 * with DECIMAL128 precision for non-terminating division. It intentionally
 * declines trig, pi, powers, modulo, percent, scientific notation, Ans and
 * memory; those stay with the existing scientific expression engine.
 *
 * This is a display helper. Scientific functions and stored Ans/M retain
 * the existing IEEE-754 double arithmetic model.
 */
public final class DecimalArithmetic {
    private static final MathContext DIV_CONTEXT=MathContext.DECIMAL128;
    private DecimalArithmetic(){}

    public static String tryFormat(String input) {
        if(input==null || input.length()>160)return null;
        String s=input.replace('×','*').replace('÷','/').replace('−','-').trim();
        if(s.isEmpty())return null;
        // Explicit whitelist prevents unknown functions from being treated
        // as decimal operations and makes backtracking unnecessary.
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(!(c>='0'&&c<='9')&&c!='.'&&c!='+'&&c!='-'&&
                    c!='*'&&c!='/'&&c!='('&&c!=')'&&!Character.isWhitespace(c))
                return null;
        }
        try{
            Parser p=new Parser(s);
            BigDecimal result=p.expression(0);
            p.skip();
            if(p.pos!=s.length())return null;
            if(result.precision()>100 || Math.abs(result.scale())>100)return null;
            BigDecimal display=result.stripTrailingZeros();
            String text=display.toPlainString();
            if(text.length()>55){
                // Keep even very long results readable without lying about
                // precision; significant digits remain in scientific notation.
                return display.round(new MathContext(24,RoundingMode.HALF_UP))
                        .stripTrailingZeros().toString().replace("E+","E");
            }
            return text.equals("-0")?"0":text;
        }catch(ArithmeticException|NumberFormatException|IllegalArgumentException ex){
            return null; // Real parser still handles errors and unsupported input.
        }
    }

    private static final class Parser {
        final String text;
        int pos,operations;
        Parser(String text){this.text=text;}
        void skip(){
            while(pos<text.length()&&Character.isWhitespace(text.charAt(pos)))pos++;
        }
        boolean eat(char c){
            skip();
            if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}
            return false;
        }
        void consume(){
            if(++operations>100)throw new IllegalArgumentException("too many operations");
        }
        BigDecimal expression(int depth){
            BigDecimal value=term(depth);
            while(true){
                if(eat('+')){consume();value=value.add(term(depth));}
                else if(eat('-')){consume();value=value.subtract(term(depth));}
                else return value;
            }
        }
        BigDecimal term(int depth){
            BigDecimal value=unary(depth);
            while(true){
                if(eat('*')){consume();value=value.multiply(unary(depth));}
                else if(eat('/')){
                    consume();
                    BigDecimal divisor=unary(depth);
                    if(divisor.signum()==0)throw new ArithmeticException("Divide by zero");
                    value=value.divide(divisor,DIV_CONTEXT);
                }else return value;
            }
        }
        BigDecimal unary(int depth){
            if(eat('+')){consume();return unary(depth);}
            if(eat('-')){consume();return unary(depth).negate();}
            return primary(depth);
        }
        BigDecimal primary(int depth){
            if(depth>40)throw new IllegalArgumentException("deep parentheses");
            if(eat('(')){
                BigDecimal value=expression(depth+1);
                if(!eat(')'))throw new IllegalArgumentException("Missing bracket");
                return value;
            }
            skip();
            int start=pos,digits=0;
            while(pos<text.length()&&Character.isDigit(text.charAt(pos))){
                pos++;digits++;
            }
            if(pos<text.length()&&text.charAt(pos)=='.'){
                pos++;
                while(pos<text.length()&&Character.isDigit(text.charAt(pos))){
                    pos++;digits++;
                }
            }
            if(digits==0||digits>45)throw new NumberFormatException("Invalid digits");
            return new BigDecimal(text.substring(start,pos));
        }
    }
}
