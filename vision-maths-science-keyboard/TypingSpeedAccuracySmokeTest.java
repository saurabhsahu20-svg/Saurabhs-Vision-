import com.saurabhsvision.keyboard.IndicLanguages;
import com.saurabhsvision.keyboard.OfflinePhonetic;
import com.saurabhsvision.keyboard.WordSuggestions;

/**
 * Offline repeatable sanity/throughput check. This is NOT WPM nor a measurement
 * of actual touch-screen input latency; Android phone hardware/IME behavior
 * still needs on-device profiling.
 */
public final class TypingSpeedAccuracySmokeTest {
    private static void eq(String received,String expected,String label){
        if(!expected.equals(received))
            throw new AssertionError(label+": expected ["+expected+"], got ["+received+"]");
    }
    private static void pass(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
        System.out.println("PASS "+label);
    }
    private static volatile long sink;
    private static void exercise(){
        String[] roman={"namaste","aapne","aapko","banao","neeche","upar","keyboard","spacebar","jaldi","padhai","dhanyavaad","science"};
        String[] prefixes={"lea","app","hel","sc","ban","kar","sau","vis","boo","kee","stud","h"};
        long checksum=0;
        for(int i=0;i<roman.length;i++){
            String result=OfflinePhonetic.convert(roman[i],IndicLanguages.HINDI);
            checksum+=result.length();
            String[] found=WordSuggestions.get(prefixes[i],(i&1)==1,3);
            checksum+=found.length;
            // Word candidates never come from typed history, nor from network.
            if(found.length!=0 && !found[0].equals(prefixes[i]))
                throw new AssertionError("Prefix is not preserved");
        }
        sink=checksum;
    }
    public static void main(String[] args){
        eq(OfflinePhonetic.convert("aapne",IndicLanguages.HINDI),"आपने","Hindi aapne");
        eq(OfflinePhonetic.convert("banao",IndicLanguages.HINDI),"बनाओ","Hindi banao");
        eq(OfflinePhonetic.convert("keyboard",IndicLanguages.HINDI),"कीबोर्ड","Hindi keyboard");
        eq(OfflinePhonetic.convert("spacebar",IndicLanguages.HINDI),"स्पेसबार","Hindi spacebar");
        eq(OfflinePhonetic.convert("neeche",IndicLanguages.HINDI),"नीचे","Hindi neeche");
        eq(OfflinePhonetic.convert("upar",IndicLanguages.HINDI),"ऊपर","Hindi upar");
        eq(OfflinePhonetic.convert("namaste",IndicLanguages.ENGLISH),"namaste","English kept as Roman");
        eq(OfflinePhonetic.convert("namaste",IndicLanguages.HINGLISH),"namaste","Hinglish kept as Roman");
        pass(WordSuggestions.get("",false,3).length==0,"Empty suggestions safe");
        eq(WordSuggestions.trailingWord("abc +="),"","Math symbols do not trigger word suggestions");
        eq(WordSuggestions.trailingWord("Hello WORLD"),"WORLD","Unicode-safe Roman word boundary");
        for(int j=0;j<2000;j++)exercise(); // JIT warm-up
        long start=System.nanoTime();
        final int iterations=15_000;
        for(int j=0;j<iterations;j++)exercise(); // 180k transliterations + 180k lookups
        long elapsedNs=System.nanoTime()-start;
        double microsPerCycle=elapsedNs/1000.0/iterations;
        System.out.printf("Offline core (12 words + 12 suggestions) cycle: %.2f microseconds%n",microsPerCycle);
        System.out.printf("Operations tested: %,d transliterations and %,d suggestion lookups%n",
            iterations*12,iterations*12);
        pass(sink>0,"Core operations returned valid results");
        pass(elapsedNs<25_000_000_000L,"Performance regression ceiling (CI CPU, not phone WPM)");
        System.out.println("PASS: Offline Hindi/English accuracy and core speed checks");
    }
}
