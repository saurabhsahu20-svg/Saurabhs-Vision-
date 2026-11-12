import com.saurabhsvision.keyboard.EmojiCatalog;
import com.saurabhsvision.keyboard.ThemePalette;
import java.util.HashSet;
import java.util.Set;

/** Plain JVM regression test; keeps theme IDs stable and emoji insertions valid. */
public final class ThemeEmojiSmokeTest {
    static void check(boolean ok,String msg){
        if(!ok)throw new AssertionError(msg);
        System.out.println("PASS: "+msg);
    }
    public static void main(String[] args){
        final int count=ThemePalette.count();
        check(count==11,"11 premium themes are available");
        check("Cosmic Blue".equals(ThemePalette.NAMES[0]),"original default neon blue retained");
        check("Pure White".equals(ThemePalette.NAMES[4]),"original white theme index retained");
        check("Maroon Velvet".equals(ThemePalette.NAMES[5]),"maroon theme exists");
        check("Metallic Silver".equals(ThemePalette.NAMES[6]),"silver theme exists");
        check("Emerald Neon".equals(ThemePalette.NAMES[7]),"new neon-green theme exists");
        check("Sapphire Blue".equals(ThemePalette.NAMES[8]),"new premium blue theme exists");
        check(ThemePalette.isLight(4)&&ThemePalette.isLight(6),"white/silver have dark text mode");
        check(!ThemePalette.isLight(0)&&!ThemePalette.isLight(5),"blue/maroon use high contrast light key labels");
        final int[][] palette={
            ThemePalette.ACCENTS, ThemePalette.ON_ACCENT,
            ThemePalette.BASE,ThemePalette.MID,ThemePalette.HIGHLIGHT,
            ThemePalette.KEY_TOP,ThemePalette.KEY_BOTTOM,
            ThemePalette.FUNCTION_TOP,ThemePalette.FUNCTION_BOTTOM,
            ThemePalette.SPACE_TOP,ThemePalette.SPACE_BOTTOM,
            ThemePalette.ENTER_TOP,ThemePalette.ENTER_BOTTOM,
            ThemePalette.STROKE,ThemePalette.TEXT
        };
        for(int i=0;i<palette.length;i++){
            check(palette[i].length==count,"palette array "+i+" covers all themes");
            for(int color:palette[i]) if((color>>>24)!=0xff)
                throw new AssertionError("transparent palette swatch");
        }
        Set<String> names=new HashSet<>();
        for(String name:ThemePalette.NAMES)check(names.add(name),"theme name distinct: "+name);
        check(ThemePalette.norm(-1)==count-1,"saved theme index normalization");
        check(ThemePalette.norm(count)==0,"theme cycling normalization");
        check(EmojiCatalog.count()==12,"12 emoji categories including cartoons");
        int last=EmojiCatalog.count()-1;
        check("Cartoon Friends".equals(EmojiCatalog.title(last)),"Cartoon Friends category available");
        String[] cartoons=EmojiCatalog.items(last);
        check(cartoons.length>=90,"at least 90 cartoon-style emojis");
        check(EmojiCatalog.total()>=1000,"1000+ insertable Unicode emoji selections");
        check(cartoons[0].contains("👦"),"children characters included");
        Set<String> seen=new HashSet<>();
        for(String emoji:cartoons){
            check(!emoji.trim().isEmpty(),"valid nonempty cartoon emoji");
            if(!seen.add(emoji))throw new AssertionError("duplicate cartoon emoji "+emoji);
        }
        System.out.println("TOTAL EMOJI OPTIONS: "+EmojiCatalog.total());
        System.out.println("CARTOON FRIENDS: "+cartoons.length);
        System.out.println("PASS: premium theme arrays and Unicode cartoon keyboard categories");
    }
}
