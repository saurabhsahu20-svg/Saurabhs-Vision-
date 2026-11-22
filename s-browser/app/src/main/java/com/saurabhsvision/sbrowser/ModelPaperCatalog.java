package com.saurabhsvision.sbrowser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Strict catalog for the 66 Class 10 paper PDF files supplied by the owner. */
public final class ModelPaperCatalog {
    private ModelPaperCatalog() {}
    public static final String[] SUBJECTS={
        "English","Hindi","Mathematics","Sanskrit","Science","Social_Science"
    };
    public static final String[] TITLES={
        "English","हिंदी","गणित","संस्कृत","विज्ञान","सामाजिक विज्ञान"
    };
    public static final String[] SERIES={
        "नए 75-अंक सेट • चरणबद्ध हल",
        "अपलोड किया हुआ मूल 2025–26 मॉडल पेपर",
        "30-अंक के पुराने छोटे अभ्यास सेट"
    };
    private static final Set<String> ALL;
    static {
        Set<String> expected=new HashSet<>();
        for(String subject:SUBJECTS){
            String base="Class_10/10_"+subject+"/";
            expected.add(base+"Original_2025-26.pdf");
            for(int set=1;set<=5;set++){
                String n=String.format(java.util.Locale.ROOT,"%02d",set);
                expected.add(base+"Full75_Set_"+n+".pdf");
                expected.add(base+"Set_"+n+".pdf");
            }
        }
        ALL=Collections.unmodifiableSet(expected);
    }
    public static boolean expectedPdf(String path){
        return path!=null && ALL.contains(path);
    }
    public static Set<String> allPdfPaths(){
        return ALL;
    }
    public static String path(int subjectIndex,int seriesIndex,int set){
        if(subjectIndex<0||subjectIndex>=SUBJECTS.length||seriesIndex<0||seriesIndex>2||set<1||set>5)
            throw new IllegalArgumentException("Unknown paper selection");
        String stem="Class_10/10_"+SUBJECTS[subjectIndex]+"/";
        String n=String.format(java.util.Locale.ROOT,"%02d",set);
        return stem+(seriesIndex==1?"Original_2025-26.pdf":
                    seriesIndex==0?"Full75_Set_"+n+".pdf":"Set_"+n+".pdf");
    }
    public static String friendlyName(int subjectIndex,int seriesIndex,int set){
        String kind=seriesIndex==1?"Original_2025-26":seriesIndex==0?"75_Marks_Set_":"30_Marks_Set_";
        if(seriesIndex!=1)kind+=String.format(java.util.Locale.ROOT,"%02d",set);
        return "Saurabh_Vision_Class10_"+SUBJECTS[subjectIndex]+"_"+kind+".pdf";
    }
}
