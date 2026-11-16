package com.saurabhsvision.sbrowser;

import java.time.LocalDate;
import java.util.Locale;

/** Original offline, age-appropriate daily school assembly material. */
public final class DailyAssemblyContent {
    private DailyAssemblyContent(){}

    public static final class Thought {
        public final String english,hindi;
        Thought(String en,String hi){english=en;hindi=hi;}
    }
    public static final class Speech {
        public final String title,english,hindiTitle,hindi;
        Speech(String title,String english,String hiTitle,String hindi){
            this.title=title;this.english=english;this.hindiTitle=hiTitle;this.hindi=hindi;
        }
    }
    public static final class Puzzle {
        public final String scrambled,answer,hint,riddle,riddleAnswer;
        Puzzle(String scrambled,String answer,String hint,String riddle,String riddleAnswer){
            this.scrambled=scrambled;this.answer=answer;this.hint=hint;
            this.riddle=riddle;this.riddleAnswer=riddleAnswer;
        }
    }
    public static final class Crossword {
        public final String[] rows, across, down;
        Crossword(String[] rows,String[] across,String[] down){
            this.rows=rows;this.across=across;this.down=down;
        }
        public int size(){return rows.length;}
        public char cell(int row,int col){return rows[row].charAt(col);}
        public boolean correct(String[] typed){
            if(typed==null||typed.length!=size()*size())return false;
            for(int row=0;row<size();row++)
                for(int col=0;col<size();col++){
                    String value=typed[row*size()+col];
                    if(value==null||value.trim().isEmpty()||
                       Character.toUpperCase(value.trim().charAt(0))!=cell(row,col))
                        return false;
                }
            return true;
        }
    }

    public static int index(LocalDate date,int count){
        if(date==null||count<=0)throw new IllegalArgumentException("Date/count required");
        return Math.floorMod((int)date.toEpochDay(),count);
    }
    private static final Thought[] THOUGHTS={
        new Thought("A small act of kindness can brighten a whole day.","दया का एक छोटा काम पूरे दिन को बेहतर बना सकता है।"),
        new Thought("Ask questions today; understanding begins with curiosity.","आज प्रश्न पूछिए; समझ की शुरुआत जिज्ञासा से होती है।"),
        new Thought("Practice is progress, even when the result is not perfect.","अभ्यास ही प्रगति है, भले ही परिणाम पूरा न हो।"),
        new Thought("Listen carefully; every person can teach us something.","ध्यान से सुनिए; हर व्यक्ति हमें कुछ न कुछ सिखा सकता है।"),
        new Thought("Knowledge grows when we share what we learn.","ज्ञान बाँटने से और बढ़ता है।"),
        new Thought("Honesty builds trust one choice at a time.","ईमानदारी छोटे-छोटे सही फैसलों से विश्वास बनाती है।"),
        new Thought("Taking care of nature is taking care of our future.","प्रकृति की रक्षा करना अपने भविष्य की रक्षा करना है।"),
        new Thought("A good team values every member's contribution.","अच्छी टीम हर सदस्य के योगदान की कद्र करती है।"),
        new Thought("Courage can mean trying again after a mistake.","गलती के बाद दोबारा कोशिश करना भी साहस है।"),
        new Thought("Read a little every day and discover something new.","रोज़ थोड़ा पढ़िए और कुछ नया जानिए।"),
        new Thought("Your ideas matter; express them with respect.","आपके विचार महत्वपूर्ण हैं; उन्हें सम्मान से व्यक्त करें।"),
        new Thought("Clear thinking begins with checking the facts.","स्पष्ट सोच की शुरुआत तथ्यों की जाँच से होती है।"),
        new Thought("Help someone learn and you strengthen your own learning.","किसी को सिखाने से अपनी समझ भी मजबूत होती है।"),
        new Thought("Patience and steady effort make difficult tasks easier.","धैर्य और निरंतर प्रयास कठिन काम सरल बनाते हैं।"),
        new Thought("Celebrate effort, curiosity and kindness together.","मेहनत, जिज्ञासा और दयालुता—तीनों को महत्व दें।"),
        new Thought("Time spent learning is an investment in possibility.","सीखने में लगाया समय नई संभावनाओं में निवेश है।"),
        new Thought("Respect differences; they help us see the world widely.","भिन्नताओं का सम्मान करें; वे हमारा दृष्टिकोण बढ़ाती हैं।"),
        new Thought("One thoughtful question can lead to a new discovery.","एक अच्छा प्रश्न नई खोज की शुरुआत हो सकता है।"),
        new Thought("A responsible digital citizen checks before sharing.","जिम्मेदार डिजिटल नागरिक साझा करने से पहले जाँच करता है।"),
        new Thought("Consistency turns simple habits into useful skills.","नियमित अभ्यास छोटी आदतों को उपयोगी कौशल बनाता है।"),
        new Thought("Be fair, be curious, and keep learning.","निष्पक्ष रहें, जिज्ञासु रहें और सीखते रहें।")
    };

    private static final Speech[] SPEECHES={
        new Speech("Why Reading Matters",
            "Good morning respected teachers and my dear friends. Today I would like to talk about reading. Books introduce us to new ideas, people and places. Reading also strengthens our vocabulary and improves our ability to think clearly. We do not need to finish a big book in one day. Even ten peaceful minutes of reading can help us form a useful habit. Let us explore the school library, share a good story with a friend, and ask questions about what we read. Thank you, and have a wonderful day.",
            "पढ़ने का महत्व",
            "सुप्रभात आदरणीय शिक्षकगण और मेरे प्यारे साथियों। आज मैं पढ़ने के महत्व पर बोलना चाहता हूँ। पुस्तकें हमें नए विचारों, लोगों और स्थानों से परिचित कराती हैं। पढ़ने से शब्द-ज्ञान और सोचने की क्षमता भी बढ़ती है। हमें एक दिन में बहुत बड़ी पुस्तक समाप्त करने की जरूरत नहीं है। रोज़ दस मिनट ध्यान से पढ़ना भी अच्छी आदत बना सकता है। आइए पुस्तकालय का उपयोग करें, अच्छी कहानियाँ साझा करें और पढ़ी हुई बातों पर प्रश्न पूछें। धन्यवाद।"),
        new Speech("Save Water",
            "Good morning everyone. Water is essential for our homes, farms and ecosystems. Yet clean water is not unlimited. Small choices make a difference: turn off taps properly, report leaking pipes, and use only the water we need. We can also explain the importance of clean rivers and ponds to our families. Responsible water use is not only a school lesson; it is a daily responsibility. Let us start with one good habit today. Thank you.",
            "जल संरक्षण",
            "सुप्रभात सभी को। पानी हमारे घरों, खेती और पर्यावरण के लिए आवश्यक है। लेकिन स्वच्छ जल असीमित नहीं है। छोटे कदम बड़ा बदलाव ला सकते हैं। नल ठीक से बंद करें, पानी के रिसाव की जानकारी दें और जरूरत के अनुसार ही पानी लें। अपने परिवार को स्वच्छ नदियों और तालाबों का महत्व भी समझाएँ। जल संरक्षण केवल एक पाठ नहीं, बल्कि हमारी रोज़ की जिम्मेदारी है। आइए आज से एक अच्छी आदत अपनाएँ। धन्यवाद।"),
        new Speech("Science and Curiosity",
            "Respected teachers and dear friends, good morning. Science begins with a question: why does something happen? When we observe carefully, compare evidence and test ideas safely, we learn about the world. A wrong answer is not the end; it can help us find a better question. Whether you enjoy plants, space, mathematics or computers, curiosity is a powerful starting point. Let us be careful observers and thoughtful learners. Thank you.",
            "विज्ञान और जिज्ञासा",
            "आदरणीय शिक्षकों और प्रिय साथियों, सुप्रभात। विज्ञान की शुरुआत एक प्रश्न से होती है—ऐसा क्यों होता है? जब हम ध्यान से देखते हैं, प्रमाण की तुलना करते हैं और सुरक्षित तरीके से विचारों की जाँच करते हैं, तब हम दुनिया को समझते हैं। गलत उत्तर सीखने का अंत नहीं है; वह बेहतर प्रश्न तक पहुँचा सकता है। पौधे हों, अंतरिक्ष हो या गणित—जिज्ञासा हमें आगे बढ़ाती है। धन्यवाद।"),
        new Speech("Kindness at School",
            "Good morning. A school becomes stronger when students care for one another. Kindness can be as simple as including a classmate in a game, listening without interrupting, or helping someone understand a lesson. We should never judge people only by their marks, appearance or background. Everyone deserves respect and a chance to participate. Today, let us look for one small way to make our school a more welcoming place. Thank you.",
            "विद्यालय में दयालुता",
            "सुप्रभात। जब विद्यार्थी एक-दूसरे का ध्यान रखते हैं, तो विद्यालय और बेहतर बनता है। किसी साथी को खेल में शामिल करना, उसकी बात ध्यान से सुनना या पढ़ाई में सहायता करना दयालुता के उदाहरण हैं। हमें किसी को केवल अंकों, रूप या पृष्ठभूमि से नहीं आँकना चाहिए। हर विद्यार्थी सम्मान और अवसर का अधिकारी है। आइए आज विद्यालय को और अधिक सहयोगी बनाने का एक छोटा प्रयास करें। धन्यवाद।"),
        new Speech("Digital Responsibility",
            "Good morning respected teachers and friends. The internet offers excellent tools for learning, but we must use it thoughtfully. Before sharing a surprising headline, check who published it and when. Never share passwords or private information with strangers. If something online makes us uncomfortable, we can talk to a trusted adult. A responsible digital citizen is curious, careful and respectful. Let us use technology to learn and help others. Thank you.",
            "डिजिटल जिम्मेदारी",
            "आदरणीय शिक्षकगण और साथियों, सुप्रभात। इंटरनेट पढ़ाई में बहुत मदद करता है, लेकिन उसका उपयोग सोच-समझकर करना चाहिए। किसी हैरान करने वाली खबर को साझा करने से पहले उसके स्रोत और तारीख की जाँच करें। अपना पासवर्ड या निजी जानकारी किसी अजनबी को न दें। इंटरनेट पर कोई बात परेशान करे तो किसी भरोसेमंद बड़े से बात करें। जिम्मेदार डिजिटल नागरिक जिज्ञासु, सावधान और सम्मानजनक होता है। धन्यवाद।"),
        new Speech("Teamwork and Sportsmanship",
            "Good morning everyone. Sports and teamwork teach us to cooperate, follow rules and respect others. Winning feels exciting, but how we treat our teammates and opponents matters just as much. A good team encourages each member, learns from mistakes and celebrates fair play. These lessons also help us in classrooms and daily life. Let us support each other and remember that effort and respect are important parts of success. Thank you.",
            "टीमवर्क और खेल भावना",
            "सुप्रभात सभी को। खेल और टीमवर्क हमें सहयोग, नियमों का पालन और दूसरों का सम्मान करना सिखाते हैं। जीत खुशी देती है, लेकिन हम अपने साथियों और विरोधियों के साथ कैसा व्यवहार करते हैं, यह भी महत्वपूर्ण है। अच्छी टीम हर सदस्य का उत्साह बढ़ाती है, गलतियों से सीखती है और निष्पक्ष खेल को महत्व देती है। ये बातें कक्षा और जीवन में भी उपयोगी हैं। धन्यवाद।"),
        new Speech("Healthy Daily Habits",
            "Good morning. Small daily habits support our wellbeing and learning. Getting enough sleep, eating regular balanced meals, staying hydrated and making time for enjoyable movement can help us feel ready for school. Equally important are kindness to ourselves, rest and asking for help when we need it. Healthy habits are not about looking a certain way; they are about caring for our minds and bodies. Thank you.",
            "स्वस्थ दिनचर्या",
            "सुप्रभात। हमारी छोटी-छोटी दैनिक आदतें स्वास्थ्य और पढ़ाई में सहायता करती हैं। पर्याप्त नींद, नियमित संतुलित भोजन, पानी पीना और पसंद की शारीरिक गतिविधि के लिए समय निकालना उपयोगी है। आराम करना और जरूरत पर मदद माँगना भी उतना ही महत्वपूर्ण है। स्वस्थ आदतों का उद्देश्य किसी खास रूप में दिखना नहीं, बल्कि शरीर और मन की देखभाल करना है। धन्यवाद।"),
        new Speech("Respecting Our Environment",
            "Good morning everyone. The environment is our shared home. Trees give us shade, soil supports crops, and clean air and water sustain life. We can protect our surroundings by avoiding litter, reusing materials where safe, and caring for school plants. No single person can solve every environmental problem, but our collective choices matter. Let us leave our classroom and neighbourhood a little cleaner than we found them. Thank you.",
            "पर्यावरण का सम्मान",
            "सुप्रभात। पर्यावरण हमारा साझा घर है। पेड़ हमें छाया देते हैं, मिट्टी फसलों को सहारा देती है और स्वच्छ हवा व पानी जीवन के लिए जरूरी हैं। हम कूड़ा सही जगह डालकर, जरूरत के अनुसार वस्तुओं का पुनः उपयोग करके और पौधों की देखभाल करके मदद कर सकते हैं। कोई एक व्यक्ति हर समस्या नहीं सुलझा सकता, लेकिन हम सबकी छोटी कोशिशें महत्वपूर्ण हैं। धन्यवाद।"),
        new Speech("The Value of Time",
            "Good morning teachers and friends. Time is a resource we all share. Planning our day helps us balance learning, play, family and rest. We do not have to be busy every minute. A simple checklist and short breaks can make schoolwork more manageable. Being on time also shows respect for other people's time. Let us use our time wisely, without forgetting to enjoy learning. Thank you.",
            "समय का महत्व",
            "सुप्रभात शिक्षकगण और मित्रों। समय एक ऐसा संसाधन है जो हम सबके पास होता है। दिन की योजना बनाने से पढ़ाई, खेल, परिवार और आराम में संतुलन बना रहता है। हर मिनट व्यस्त रहना जरूरी नहीं है। छोटी कार्य-सूची और बीच-बीच में विश्राम से काम आसान लगता है। समय पर पहुँचना दूसरों के समय का सम्मान भी है। आइए समय का समझदारी से उपयोग करें। धन्यवाद।"),
        new Speech("Girls and Boys in STEM",
            "Good morning. Science, technology, engineering and mathematics belong to everyone. Talent and curiosity are not limited by gender or background. When we welcome different ideas, teams find more creative solutions. Let us encourage every classmate to ask questions, experiment safely and pursue subjects they enjoy. Our school grows stronger when every learner has the opportunity to explore. Thank you.",
            "विज्ञान में सभी के लिए अवसर",
            "सुप्रभात। विज्ञान, तकनीक, इंजीनियरिंग और गणित सभी के लिए हैं। प्रतिभा और जिज्ञासा किसी लिंग या पृष्ठभूमि तक सीमित नहीं हैं। जब हम अलग-अलग विचारों को महत्व देते हैं, तो बेहतर समाधान मिलते हैं। आइए हर साथी को प्रश्न पूछने, सुरक्षित प्रयोग करने और अपनी पसंद के विषय सीखने के लिए प्रोत्साहित करें। धन्यवाद।"),
        new Speech("Honesty and Integrity",
            "Good morning everyone. Honesty means being truthful and taking responsibility for our actions. At school, that includes doing our own work, giving credit for borrowed ideas and admitting mistakes. Telling the truth may sometimes feel difficult, but it helps others trust us. Integrity is not about never making a mistake; it is about choosing to do what is right afterward. Thank you.",
            "ईमानदारी और जिम्मेदारी",
            "सुप्रभात। ईमानदारी का अर्थ सच बोलना और अपने काम की जिम्मेदारी लेना है। विद्यालय में स्वयं काम करना, किसी के विचार का श्रेय देना और गलती स्वीकार करना इसके उदाहरण हैं। सच बोलना कभी-कभी कठिन लग सकता है, पर इससे विश्वास बढ़ता है। जिम्मेदार होने का अर्थ कभी गलती न करना नहीं, बल्कि गलती के बाद सही कदम उठाना है। धन्यवाद।"),
        new Speech("Our Constitution and Responsibilities",
            "Good morning respected teachers and friends. India's Constitution guides our democracy and protects important rights. Rights also remind us to respect the dignity and freedom of others. At school, we can practise fairness by listening to different opinions, avoiding discrimination and helping our community. Learning about civic responsibilities helps us become thoughtful citizens. Let us treat one another with respect. Thank you.",
            "संविधान और नागरिक जिम्मेदारी",
            "आदरणीय शिक्षकगण और साथियों, सुप्रभात। भारत का संविधान हमारे लोकतंत्र का मार्गदर्शन करता है और महत्वपूर्ण अधिकारों की रक्षा करता है। अधिकारों के साथ हमें दूसरों की गरिमा और स्वतंत्रता का सम्मान भी करना चाहिए। विद्यालय में हम सभी की बात सुनकर, भेदभाव से बचकर और समुदाय की मदद करके निष्पक्षता का अभ्यास कर सकते हैं। धन्यवाद।"),
        new Speech("Learning From Mistakes",
            "Good morning. Every learner makes mistakes. Sometimes a difficult problem needs more than one attempt, and that is a normal part of learning. Instead of hiding a mistake, we can ask what happened, seek guidance and try another method. Feedback from a teacher or friend can help us grow. Let us be patient with ourselves and supportive of others as we learn. Thank you.",
            "गलतियों से सीखना",
            "सुप्रभात। सीखने के दौरान सभी से गलतियाँ होती हैं। कठिन प्रश्न का हल कई प्रयासों के बाद मिल सकता है और यह सीखने का सामान्य हिस्सा है। गलती छिपाने के बजाय हमें समझना चाहिए कि क्या हुआ, सहायता माँगनी चाहिए और दूसरी विधि अपनानी चाहिए। शिक्षक और मित्रों की सलाह से प्रगति होती है। आइए धैर्य रखें और एक-दूसरे का सहयोग करें। धन्यवाद।"),
        new Speech("The Power of Questions",
            "Good morning friends. Many discoveries begin with a simple question. Why does the sky change colour? How do plants grow? What makes a bridge strong? Questions encourage us to observe, read and test our understanding. We should ask respectfully and listen carefully to the answer. A curious classroom is a place where everyone has something valuable to contribute. Thank you.",
            "प्रश्न पूछने की शक्ति",
            "सुप्रभात मित्रों। कई खोजों की शुरुआत एक सरल प्रश्न से होती है। आकाश का रंग क्यों बदलता है? पौधे कैसे बढ़ते हैं? पुल मजबूत कैसे बनता है? प्रश्न हमें देखने, पढ़ने और अपनी समझ की जाँच करने के लिए प्रेरित करते हैं। हमें सम्मान के साथ प्रश्न पूछने और उत्तर ध्यान से सुनने चाहिए। जिज्ञासु कक्षा में सभी का योगदान महत्वपूर्ण होता है। धन्यवाद।")
    };

    private static final Puzzle[] PUZZLES={
        new Puzzle("LEARN","LEARN","What we do at school.","What has many keys but cannot open a door?","A piano."),
        new Puzzle("ETARW","WATER","A drink and part of rivers.","What goes up but never comes down?","Your age."),
        new Puzzle("NAPLET","PLANET","Earth is one.","What has hands but cannot clap?","A clock."),
        new Puzzle("LIPENC","PENCIL","Used for writing.","Which month has 28 days?","Every month."),
        new Puzzle("RITBAGH","BRIGHT","Full of light.","What has a neck but no head?","A bottle."),
        new Puzzle("SITROHY","HISTORY","Study of the past.","What is full of holes but holds water?","A sponge."),
        new Puzzle("NREEGY","ENERGY","Needed to do work.","What can travel around the world while staying in a corner?","A stamp."),
        new Puzzle("TERUNAT","NATURE","The world around us.","What gets wetter as it dries?","A towel."),
        new Puzzle("NECICES","SCIENCE","Study of the natural world.","What can you catch but not throw?","A cold."),
        new Puzzle("TDUNEST","STUDENT","A learner.","What has one eye but cannot see?","A needle."),
        new Puzzle("LRIBARY","LIBRARY","Place to borrow books.","What has words but never speaks?","A book."),
        new Puzzle("OCTEFPR","PERFECT","Without mistakes.","What begins with T, ends with T and has T in it?","A teapot."),
        new Puzzle("PERASCTE","RESPECT","Showing consideration.","What comes down but never goes up?","Rain."),
        new Puzzle("IGVER","GIVER","Someone who shares.","What has a face and two hands but no arms or legs?","A clock.")
    };

    public static final String[] SIGNS={
        "Aries (मेष)","Taurus (वृषभ)","Gemini (मिथुन)","Cancer (कर्क)",
        "Leo (सिंह)","Virgo (कन्या)","Libra (तुला)","Scorpio (वृश्चिक)",
        "Sagittarius (धनु)","Capricorn (मकर)","Aquarius (कुंभ)","Pisces (मीन)"
    };
    private static final String[] FUN_REFLECTIONS={
        "Try a new book or hobby today. नई किताब या रुचि आज़माएँ।",
        "Make time to listen to a friend. किसी मित्र की बात ध्यान से सुनें।",
        "Ask a thoughtful question in class. कक्षा में अच्छा प्रश्न पूछें।",
        "Plan a small creative project. कोई छोटा रचनात्मक काम करें।",
        "Share one helpful idea with others. एक उपयोगी विचार साझा करें।",
        "Spend a little time outdoors safely. सुरक्षित रूप से बाहर कुछ समय बिताएँ।",
        "Practice a skill you enjoy. मनपसंद कौशल का अभ्यास करें।",
        "Write down something you are curious about. जिज्ञासा लिखें।",
        "Thank someone who helped you. मदद करने वाले को धन्यवाद दें।",
        "Take a comfortable break between tasks. काम के बीच आराम करें।",
        "Learn a new word and use it. नया शब्द सीखकर प्रयोग करें।",
        "Help keep your surroundings clean. आसपास सफाई में योगदान दें।"
    };

    private static final Crossword[] CROSSWORDS={
        new Crossword(new String[]{"STAR","TIRE","AREA","READ"},
            new String[]{"1. Shining object in the night sky",
                "2. Rubber covering around a wheel","3. Space or region","4. Understand written words"},
            new String[]{"1. A famous performer or a bright sky object",
                "2. A vehicle needs this on its wheels","3. A measured region",
                "4. Do this with a book"}),
        new Crossword(new String[]{"BALL","AREA","LEAD","LADY"},
            new String[]{"1. Round object used in games","2. Size of a surface",
                "3. Guide a team","4. A woman"},
            new String[]{"1. Something used in many sports","2. A part of a region",
                "3. Be at the front","4. Polite word for a woman"}),
        new Crossword(new String[]{"CARE","AREA","REAR","EARN"},
            new String[]{"1. Attention and concern","2. Space inside a boundary",
                "3. Back side","4. Receive money for work"},
            new String[]{"1. Look after someone","2. A district has one",
                "3. Opposite of front","4. Get something through effort"})
    };

    public static Thought thought(LocalDate day){return THOUGHTS[index(day,THOUGHTS.length)];}
    public static Speech speech(LocalDate day){return SPEECHES[index(day,SPEECHES.length)];}
    public static Puzzle puzzle(LocalDate day){return PUZZLES[index(day,PUZZLES.length)];}
    public static Crossword crossword(LocalDate day){return CROSSWORDS[index(day,CROSSWORDS.length)];}
    public static String reflection(LocalDate day,int sign){
        if(sign<0||sign>=SIGNS.length)throw new IllegalArgumentException("Unknown zodiac sign");
        return FUN_REFLECTIONS[Math.floorMod(index(day,FUN_REFLECTIONS.length)+sign,
            FUN_REFLECTIONS.length)];
    }
    public static boolean answer(String attempt,String solution){
        return attempt!=null&&solution!=null&&
            attempt.replaceAll("\\s+","").trim().equalsIgnoreCase(
                solution.replaceAll("\\s+","").trim());
    }
    public static String assemblyPlan(LocalDate day,boolean hindi){
        Thought t=thought(day);Speech s=speech(day);
        return hindi?
            "आज की प्रार्थना सभा\n\n1. सुप्रभात आदरणीय शिक्षकगण और प्रिय साथियों।\n"+
            "2. आज का विचार: "+t.hindi+"\n\n3. आज का लघु भाषण – "+s.hindiTitle+
            "\n"+s.hindi+"\n\n4. खेल और मुख्य समाचार पढ़ने से पहले उनके स्रोत और तारीख की जाँच करें।\n"+
            "5. आज का छोटा प्रश्न: "+puzzle(day).riddle+"\nधन्यवाद।":
            "TODAY'S SCHOOL ASSEMBLY\n\n1. Good morning respected teachers and friends.\n"+
            "2. Thought of the day: "+t.english+"\n\n3. Short speech – "+s.title+
            "\n"+s.english+"\n\n4. Read sports and news headlines only after checking"+
            " the published source and date.\n5. Riddle: "+puzzle(day).riddle+"\nThank you.";
    }
}
