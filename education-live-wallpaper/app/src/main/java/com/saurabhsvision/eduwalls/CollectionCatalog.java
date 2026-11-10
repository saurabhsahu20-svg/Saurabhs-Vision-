package com.saurabhsvision.eduwalls;

/** Fifty distinct education-art compositions, each usable as still or live wallpaper. */
public final class CollectionCatalog {
    private CollectionCatalog(){}
    public static final int COUNT=50;
    public static final String[] GROUPS={"MATHS","SCIENCE","SPACE","MAGICAL BOOKS","STUDY MOTIVATION"};
    private static final String[][] NAMES={
        {"Golden Maths","Rainbow Equations","Geometry Explorer","Algebra Spark","Maths Chalkboard",
         "Pi Galaxy","Number Adventure","Formula Dreams","Graph Universe","Maths Mastery"},
        {"Neon Atom","DNA Discovery","Chemistry Magic","Physics Power","Science Laboratory",
         "Molecule Universe","Microscope Dreams","Energy & Motion","Life Science","STEM Explorer"},
        {"Cosmic Classroom","Rocket Knowledge","Galaxy of Numbers","Planet Academy","Starry Algebra",
         "Orbiting Ideas","Astro Geometry","Space Laboratory","Saturn Science","Universe Explorer"},
        {"Magic Storybook","Book of Wonders","Reading Rainbow","Library of Dreams","Knowledge Tree",
         "Pencil Universe","School Adventure","Learning Castle","Flying Books","Creative Classroom"},
        {"Success Steps","Never Stop Learning","Focus & Grow","Practice Everyday","Dream Big",
         "Confidence Builder","Bright Future","Study Motivation","Curious Mind","Learn & Lead"}
    };
    private static final int[] COLORS={0xffffd54f,0xff84dcff,0xfff6a6ff,0xff99ffdb,0xffffab80,
        0xffa5a9ff,0xffc9f97d,0xfff5d58f,0xff8af4ff,0xffff89ca};
    public static String name(int index){int x=norm(index);return NAMES[x/10][x%10];}
    public static String category(int index){return GROUPS[norm(index)/10];}
    public static int accent(int index){return COLORS[norm(index)%10];}
    public static int norm(int index){return Math.max(0,Math.min(COUNT-1,index));}
    public static int posterBase(int index){
        int n=norm(index),g=n/10,v=n%10;
        int[][] bases={{0,1,2,6,0,7,1,6,2,0},{3,7,3,2,3,7,3,2,7,3},
            {7,0,7,7,0,7,6,3,7,7},{5,5,1,5,0,5,1,5,5,1},{4,4,6,4,5,4,4,5,4,4}};
        return bases[g][v];
    }
}
