package com.saurabhsvision.eduwalls;

/** Original biology illustration subjects shared by 16 static posters and 16 distinct motion studies. */
public final class BiologyCatalog {
    private BiologyCatalog(){}
    public static final int COUNT=16;
    private static final String[] NAMES={
        "DNA DOUBLE HELIX","HUMAN HEART","HUMAN KIDNEY","NEPHRON",
        "LEAF ANATOMY","PLANT TISSUES","ANIMAL TISSUES","NEURON",
        "HUMAN LUNGS","HUMAN BRAIN","PLANT CELL","FLOWER ANATOMY",
        "CHLOROPLAST","STOMATA","ROOT & XYLEM","BLOOD CELLS"
    };
    private static final String[] FACTS={
        "DNA stores hereditary information.",
        "The heart pumps blood through the body.",
        "Kidneys filter blood and help balance water.",
        "Nephrons are functional units of the kidney.",
        "Leaves capture light for photosynthesis.",
        "Xylem transports water; phloem moves sugars.",
        "Tissues are groups of similar cells.",
        "Neurons carry electrical signals.",
        "Lungs exchange oxygen and carbon dioxide.",
        "The brain coordinates body functions.",
        "Plant cells have a cell wall and vacuole.",
        "Flowers support reproduction in flowering plants.",
        "Chloroplasts contain chlorophyll.",
        "Stomata regulate gas exchange.",
        "Roots absorb water; xylem transports it.",
        "Red blood cells carry oxygen."
    };
    private static final String[][] PARTS={
        {"BASE PAIRS","BACKBONE"},
        {"ATRIUM","VENTRICLE"},
        {"CORTEX","MEDULLA"},
        {"GLOMERULUS","LOOP OF HENLE"},
        {"VEINS","LEAF BLADE"},
        {"XYLEM","PHLOEM"},
        {"EPITHELIAL","MUSCLE"},
        {"DENDRITES","AXON"},
        {"BRONCHI","ALVEOLI"},
        {"CEREBRUM","CEREBELLUM"},
        {"CELL WALL","VACUOLE"},
        {"PETALS","STAMEN"},
        {"THYLAKOIDS","STROMA"},
        {"GUARD CELLS","PORE"},
        {"ROOT HAIRS","XYLEM"},
        {"RED BLOOD CELLS","WHITE BLOOD CELL"}
    };
    private static final int[] PALETTE={
        0xff80e5ff,0xffff728b,0xffffb4c6,0xff86efff,
        0xff80f5a0,0xff76eac8,0xffe7a1f8,0xffffc46f,
        0xff8ddcff,0xffe7a8ff,0xff9cfaab,0xffffe08a,
        0xff74e4aa,0xffb4ff85,0xffa5e28b,0xfff29d9f
    };
    public static int norm(int index){return Math.max(0,Math.min(COUNT-1,index));}
    public static String name(int index){return NAMES[norm(index)];}
    public static String fact(int index){return FACTS[norm(index)];}
    public static String part(int index,int side){return PARTS[norm(index)][side==0?0:1];}
    public static int accent(int index){return PALETTE[norm(index)];}
}
