import com.saurabhsvision.sbrowser.DailyAssemblyContent;
import java.time.LocalDate;
import java.util.Arrays;

public final class DailyAssemblyContentTest {
    private static void check(boolean b,String label){
        if(!b)throw new AssertionError(label);
    }
    private static String letters(String s){
        char[] c=s.toUpperCase().toCharArray();
        Arrays.sort(c);return new String(c);
    }
    public static void main(String[] args){
        LocalDate start=LocalDate.of(2026,11,16);
        check(DailyAssemblyContent.SIGNS.length==12,"12 zodiac signs");
        int thoughts=0,speeches=0,crosswords=0;
        java.util.HashSet<String> originalThoughts=new java.util.HashSet<>();
        java.util.HashSet<String> originalSpeeches=new java.util.HashSet<>();
        java.util.HashSet<String> crosswordLayouts=new java.util.HashSet<>();
        for(int i=0;i<84;i++){
            LocalDate day=start.plusDays(i);
            DailyAssemblyContent.Thought t=DailyAssemblyContent.thought(day);
            DailyAssemblyContent.Speech s=DailyAssemblyContent.speech(day);
            java.util.HashSet<String> fiveThoughts=new java.util.HashSet<>();
            java.util.HashSet<String> fiveTopics=new java.util.HashSet<>();
            for(int choice=0;choice<5;choice++){
                DailyAssemblyContent.Thought option=DailyAssemblyContent.thoughtOption(day,choice);
                DailyAssemblyContent.Speech speechOption=DailyAssemblyContent.speechOption(day,choice);
                check(option.english.length()>15&&option.hindi.length()>12,
                    "daily bilingual thought choice "+choice);
                check(speechOption.english.length()>150&&speechOption.hindi.length()>80,
                    "daily bilingual speech choice "+choice);
                fiveThoughts.add(option.english);
                fiveTopics.add(speechOption.title);
            }
            check(fiveThoughts.size()==5,"five distinct daily thoughts");
            check(fiveTopics.size()==5,"five distinct daily speech topics");
            check(DailyAssemblyContent.thoughtOption(day,0).english.equals(t.english),
                "first thought retains existing daily script");
            check(DailyAssemblyContent.speechOption(day,0).title.equals(s.title),
                "first speech retains existing daily script");
            if(i>0){
                check(!fiveThoughts.contains(DailyAssemblyContent.thoughtOption(
                    day.minusDays(1),0).english),
                    "daily thought selection changes");
            }
            DailyAssemblyContent.Puzzle p=DailyAssemblyContent.puzzle(day);
            DailyAssemblyContent.Crossword c=DailyAssemblyContent.crossword(day);
            check(t.english.length()>15&&t.hindi.length()>12,"bilingual thought "+i);
            check(s.english.length()>150&&s.hindi.length()>80,"full short speech "+i);
            check(!p.scrambled.equals(p.answer),"scramble is actually scrambled "+i);
            check(letters(p.scrambled).equals(letters(p.answer)),
                "scrambled letters match answer "+i);
            check(p.riddle.length()>8&&p.riddleAnswer.length()>3,
                "riddle and answer "+i);
            check(c.size()==4&&c.across.length==4&&c.down.length==4,
                "4x4 crossword with clues "+i);
            for(int x=0;x<4;x++){
                check(c.rows[x].length()==4,"full row "+i);
                for(int y=0;y<4;y++){
                    check(c.cell(x,y)==c.cell(y,x),
                        "each row/column forms same word "+i+" "+x+","+y);
                }
            }
            String[] solved=new String[16];
            for(int x=0;x<4;x++)
                for(int y=0;y<4;y++)
                    solved[x*4+y]=String.valueOf(c.cell(x,y));
            check(c.correct(solved),"correct crossword accepted "+i);
            solved[3]="!";
            check(!c.correct(solved),"invalid crossword rejected "+i);
            check(DailyAssemblyContent.answer(p.answer.toLowerCase(),p.answer),
                "case insensitive checking");
            check(!DailyAssemblyContent.answer("WRONG",p.answer),
                "wrong answer blocked");
            check(DailyAssemblyContent.assemblyPlan(day,true).contains(t.hindi),
                "Hindi script includes current thought");
            check(DailyAssemblyContent.assemblyPlan(day,false).contains(s.english),
                "English script includes current speech");
            for(int sign=0;sign<12;sign++)
                check(!DailyAssemblyContent.reflection(day,sign).isEmpty(),
                    "fun reflection for every sign");
            originalThoughts.add(t.english);
            originalSpeeches.add(s.title);
            crosswordLayouts.add(String.join(",",c.rows));
        }
        check(originalThoughts.size()==31,"31 unique daily thoughts");
        check(originalSpeeches.size()==31,"31 unique short speeches");
        check(crosswordLayouts.size()>=3,"3 crossword layouts");
        System.out.println("PASS daily assembly thoughts, speeches, horoscope "+
            "entertainment and playable crossword/puzzles");
    }
}
