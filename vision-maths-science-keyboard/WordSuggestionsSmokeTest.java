import com.saurabhsvision.keyboard.WordSuggestions;

public final class WordSuggestionsSmokeTest {
    private static void pass(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
        System.out.println("PASS "+label);
    }
    public static void main(String[] args){
        pass(WordSuggestions.trailingWord("please banao").equals("banao"),
            "Hinglish text extraction");
        pass(WordSuggestions.trailingWord("a+b=").equals(""),
            "Non-word symbols produce no predictions");
        pass(WordSuggestions.trailingWord("hello").equals("hello"),
            "English word extraction");
        String[] english=WordSuggestions.get("lea",false,3);
        pass(english.length>=2 && "lea".equals(english[0]) &&
            english[1].startsWith("lea"),"English prefix candidates");
        String[] hindi=WordSuggestions.get("ban",true,3);
        pass(hindi.length>=2 && "ban".equals(hindi[0]) &&
            hindi[1].startsWith("ban"),"Hinglish prefix candidates");
        pass(WordSuggestions.get("",false,3).length==0,
            "Empty input yields no suggestions");
        pass(WordSuggestions.get("password",true,3)[0].equals("password"),
            "No user data saved or searched remotely");
        System.out.println("Offline S Board suggestion strip tests passed.");
    }
}
