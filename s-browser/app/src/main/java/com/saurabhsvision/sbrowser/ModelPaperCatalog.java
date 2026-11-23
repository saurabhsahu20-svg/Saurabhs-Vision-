package com.saurabhsvision.sbrowser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Strict owner-authored study catalog: 68 Class 10 and 72 Class 12 PDFs. */
public final class ModelPaperCatalog {
    private ModelPaperCatalog(){}
    // Legacy fields retained for code and older test compatibility.
    public static final String[] SUBJECTS={"English","Hindi","Mathematics","Sanskrit","Science","Social_Science"};
    public static final String[] TITLES={"English","हिंदी","गणित","संस्कृत","विज्ञान","सामाजिक विज्ञान"};
    public static final String[] SERIES={
        "नए 75-अंक सेट • चरणबद्ध हल","मूल 2025–26 मॉडल पेपर","पुराने 30-अंक अभ्यास सेट"
    };
    public static final String[] CLASS12_SUBJECTS={
        "Physics","Chemistry","Biology","Mathematics","English","Hindi",
        "Accountancy","Business_Studies","Economics","History","Geography","Political_Science"
    };
    public static final String[] CLASS12_TITLES={
        "भौतिक विज्ञान • 70","रसायन विज्ञान • 70","जीव विज्ञान • 70",
        "गणित • 80","अंग्रेज़ी • 80","हिंदी • 80",
        "लेखाशास्त्र • 80","व्यवसाय अध्ययन • 80","अर्थशास्त्र • 80",
        "इतिहास • 80","भूगोल • 70","राजनीति विज्ञान • 80"
    };
    public static final String[] CLASS12_SERIES={"अभ्यास सेट • 5 • चरणबद्ध हल","मूल 2025–26 मॉडल पेपर"};
    public static final String[] CLASS10_VISION_SERIES={
        "नए 75-अंक सेट • चरणबद्ध हल","मूल 2025–26 मॉडल पेपर",
        "पुराने 30-अंक अभ्यास सेट","💡 Vision Ka Saar • 75 अंक"
    };
    private static final Set<String> C10,C12,SAAR,ALL;
    static {
        Set<String> ten=new HashSet<>(),twelve=new HashSet<>(),saar=new HashSet<>();
        for(String subject:SUBJECTS){
            String base="Class_10/10_"+subject+"/";
            ten.add(base+"Original_2025-26.pdf");
            for(int i=1;i<=5;i++){
                String n=String.format(Locale.ROOT,"%02d",i);
                ten.add(base+"Full75_Set_"+n+".pdf");
                ten.add(base+"Set_"+n+".pdf");
            }
        }
        for(String subject:CLASS12_SUBJECTS){
            String base="Class_12/12_"+subject+"/";
            twelve.add(base+"Original_2025-26.pdf");
            for(int i=1;i<=5;i++)
                twelve.add(base+"Practice_Set_"+String.format(Locale.ROOT,"%02d",i)+".pdf");
        }
        saar.add("Class_10/10_Mathematics/Vision_Ka_Saar.pdf");
        saar.add("Class_10/10_Science/Vision_Ka_Saar.pdf");
        C10=Collections.unmodifiableSet(ten);
        C12=Collections.unmodifiableSet(twelve);
        SAAR=Collections.unmodifiableSet(saar);
        HashSet<String> full=new HashSet<>(ten);full.addAll(twelve);full.addAll(saar);
        ALL=Collections.unmodifiableSet(full);
    }
    public static Set<String> class10Paths(){return C10;}
    public static Set<String> class12Paths(){return C12;}
    public static Set<String> visionKaSaarPaths(){return SAAR;}
    public static Set<String> allPdfPaths(){return ALL;}
    public static boolean expectedPdf(String path){return path!=null&&ALL.contains(path);}
    public static boolean validImport(Set<String> selected){
        return selected!=null&&(selected.equals(C10)||selected.equals(C12)||
            selected.equals(SAAR)||selected.equals(ALL));
    }
    public static String path(int subjectIndex,int seriesIndex,int set){
        return path(10,subjectIndex,seriesIndex,set);
    }
    public static String path(int grade,int subjectIndex,int seriesIndex,int set){
        if(set<1||set>5)throw new IllegalArgumentException("Unknown paper set");
        String n=String.format(Locale.ROOT,"%02d",set);
        if(grade==10){
            if(subjectIndex<0||subjectIndex>=SUBJECTS.length||seriesIndex<0||seriesIndex>3)
                throw new IllegalArgumentException("Unknown Class 10 paper selection");
            String sub=SUBJECTS[subjectIndex],stem="Class_10/10_"+sub+"/";
            if(seriesIndex==3){
                if(!sub.equals("Mathematics")&&!sub.equals("Science"))
                    throw new IllegalArgumentException("Vision Ka Saar only Maths and Science");
                return stem+"Vision_Ka_Saar.pdf";
            }
            return stem+(seriesIndex==1?"Original_2025-26.pdf":
                seriesIndex==0?"Full75_Set_"+n+".pdf":"Set_"+n+".pdf");
        }
        if(grade==12){
            if(subjectIndex<0||subjectIndex>=CLASS12_SUBJECTS.length||seriesIndex<0||seriesIndex>1)
                throw new IllegalArgumentException("Unknown Class 12 paper selection");
            return "Class_12/12_"+CLASS12_SUBJECTS[subjectIndex]+"/"+
                (seriesIndex==1?"Original_2025-26.pdf":"Practice_Set_"+n+".pdf");
        }
        throw new IllegalArgumentException("Unsupported grade");
    }
    public static String friendlyName(int subjectIndex,int seriesIndex,int set){
        return friendlyName(10,subjectIndex,seriesIndex,set);
    }
    public static String friendlyName(int grade,int subjectIndex,int seriesIndex,int set){
        String path=path(grade,subjectIndex,seriesIndex,set);
        return "Saurabh_Vision_"+path.replace('/','_');
    }
}
