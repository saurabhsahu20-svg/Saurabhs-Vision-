package com.saurabhsvision.keyboard;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Locale;

/** Small offline English keyword index; uses only bundled Unicode emoji. */
public final class EmojiSearchIndex {
    private EmojiSearchIndex(){}
    private static final String[] WORDS={
        "smile happy laugh","love heart kiss","sad cry tears","hands wave clap",
        "person people family","animal cat dog panda bird","nature tree flower leaf",
        "food pizza cake fruit","party celebration gift","symbol check star",
        "science space rocket","cartoon kid hero"
    };
    private static final String[][] EXACT={
        {"cat kitten kitty","🐱 😺 😸 😹 😻 🐈 🐾"},
        {"dog puppy","🐶 🐕 🐩 🐾"},
        {"sun sunny sunshine","☀️ 🌞 🌅 🌻"},
        {"moon night stars","🌙 🌛 🌜 ⭐ 🌟 🌠"},
        {"rain raincloud","🌧️ ☔ 🌦️ 🌈"},
        {"heart love romance","❤️ 🩷 💖 💗 💕 💘 💝"},
        {"fire flame hot","🔥 ❤️‍🔥 ☀️"},
        {"smile happy laugh funny","😀 😄 😁 😊 😂 🤣 🙂 😃"},
        {"cry sad worried","😢 😭 🥺 😔 😞"},
        {"angry mad","😠 😡 🤬 😤"},
        {"school books study","📚 📝 🎓 ✏️ 🧑‍🎓"},
        {"india flag indian","🇮🇳 🇮🇳 🪔"},
        {"rocket space planet galaxy","🚀 🪐 🌍 🛸 🌌"},
        {"flower rose blossom","🌸 🌹 🌺 🌼 🌷 💐"},
        {"car vehicle","🚗 🚕 🚙 🚌 🚓"},
        {"music song dance","🎵 🎶 🎤 💃 🕺"},
        {"football soccer sport","⚽ 🏟️ 🥅 🏆"},
        {"science lab atom","⚛️ 🧪 🔬 🧬 💡"},
        {"birthday cake","🎂 🎉 🥳 🎁 🎈"},
        {"thank thanks grateful","🙏 ❤️ 😊 👏"},
        {"yes okay ok","✅ 👍 👌 💯"},
        {"no wrong","❌ 👎 🚫"}
    };
    public static String[] search(String query,int max){
        int limit=Math.max(1,Math.min(32,max));
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        LinkedHashSet<String> found=new LinkedHashSet<>();
        if(q.isEmpty()){
            for(String x:new String[]{"😀","😊","😂","🥰","❤️","👍","🙏","🎉",
                "🐱","🐶","🌹","✨","🔥","💡","🚀","✅"})found.add(x);
            return found.toArray(new String[0]);
        }
        for(String[] data:EXACT){
            boolean match=false;
            for(String word:data[0].split(" ")){
                if(word.startsWith(q)||q.startsWith(word)){match=true;break;}
            }
            if(match)for(String emoji:data[1].split(" "))found.add(emoji);
        }
        for(int i=0;i<EmojiCatalog.count();i++){
            boolean match=EmojiCatalog.title(i).toLowerCase(Locale.ROOT).contains(q);
            if(!match)for(String word:WORDS[i].split(" "))
                if(word.startsWith(q)||q.startsWith(word)){match=true;break;}
            if(match)for(String e:EmojiCatalog.items(i)){
                found.add(e);
                if(found.size()>=limit)break;
            }
            if(found.size()>=limit)break;
        }
        ArrayList<String> out=new ArrayList<>();
        for(String emoji:found){
            out.add(emoji);
            if(out.size()>=limit)break;
        }
        return out.toArray(new String[0]);
    }
}
