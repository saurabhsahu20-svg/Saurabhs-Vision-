package com.saurabhsvision.eduwalls;

/** V6: Fifty unique LIVE designs, independent from the static poster gallery. */
public final class LiveSceneCatalog {
    private LiveSceneCatalog(){}
    public static final int COUNT=50;
    public static final String[] GROUPS={"MOVING MATHEMATICS","LIVING SCIENCE","SPACE MOTION","MAGICAL LEARNING","FOCUS & SUCCESS"};
    private static final String[][] TITLES={
        {"Pi Orbit","Infinite Numbers","Neon Geometry","Sine Universe","Algebra Nebula",
         "Trigonometry Waves","Prime Vortex","Graph Explorer","Moving Calculus","Maths Timewarp"},
        {"Quantum Atom","DNA Helix","Neon Laboratory","Molecular Dance","Magnetic Field",
         "Physics Pulse","Chemistry Universe","Cell Microscope","Periodic Sparks","Science Orbit"},
        {"Solar Explorer","Galaxy Portal","Planet Parade","Cosmic Comet","Nebula Learning",
         "Rocket Launch","Milky Way Academy","Star Constellations","Solar Eclipse","Astronomy Dreams"},
        {"Flying Library","Magical Storybook","Knowledge Fountain","Spellbinding Letters","Rainbow Learning",
         "School of Wonders","Illuminated Pages","Creative Universe","Floating Pencils","Book of Dreams"},
        {"Success Orbit","Focus Clock","Dream Ladder","Discipline Sparks","Journey of Learning",
         "Bright Ideas","Study Energy","Courage Stars","Goal Galaxy","Lead Tomorrow"}
    };
    private static final int[] ACCENTS={0xffffd156,0xff44e6ff,0xffff85df,0xffa3ffab,0xffefaf7c,0xffbfa9ff,0xff78e9f5,0xfffff49b,0xfff38b9d,0xff9fd5ff};
    public static int norm(int i){return Math.max(0,Math.min(COUNT-1,i));}
    public static String name(int i){i=norm(i);return TITLES[i/10][i%10];}
    public static String group(int i){return GROUPS[norm(i)/10];}
    public static int color(int i){return ACCENTS[norm(i)%10];}
}
