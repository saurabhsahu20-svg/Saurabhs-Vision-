package com.saurabhsvision.visioncalc;

/**
 * Low-glare calculator colour palettes. Every pastel uses dark text on
 * light keys. Theme indices and colour values are pure Java so CI can
 * exercise persistence/bounds without an Android emulator.
 */
public final class ComfortTheme {
    public static final String[] NAMES={
        "Soft Sky Blue", "Gentle Mint Green", "Calm Lavender",
        "Warm Cream", "Original Neon Dark"
    };
    private static final int[][] COLORS={
        // background, panel, card, accent, detail, ink, muted, soft,
        // error, digit, operator, clear, equals, displayTop, displayBottom
        {0xffE9F4FA,0xffF4FAFF,0xffE1F0F9,0xff326D98,0xff557D9B,
         0xff17384F,0xff5B7488,0xffD7E9F4,0xffA83646,
         0xffF8FCFE,0xffC7E3F2,0xffDFE7F6,0xff8BBFDF,
         0xffF7FCFF,0xffDBEEF8},
        {0xffEDF5EE,0xffF8FCF8,0xffE1F0E6,0xff32785D,0xff5D8570,
         0xff1E3E33,0xff62776D,0xffD9EADD,0xff9D3C4A,
         0xffFCFDFC,0xffCDE6D4,0xffE0E6F0,0xff92D1AF,
         0xffF8FEF9,0xffE0F1E5},
        {0xffF2F0F9,0xffFBFAFE,0xffE8E5F5,0xff675B94,0xff8176A6,
         0xff322E4F,0xff747188,0xffE4DFF3,0xffA8404C,
         0xffFEFDFF,0xffDCD7EE,0xffEFE1ED,0xffB6A8DF,
         0xffFCFBFF,0xffEAE5F8},
        {0xffF8F3E9,0xffFFFDF8,0xffF1EAD9,0xff977245,0xffA58963,
         0xff493D2F,0xff7E7060,0xffF0E5D0,0xffA13E42,
         0xffFFFDF9,0xffF0E0C4,0xffF3E1D5,0xffE4C69C,
         0xffFFFEF9,0xffF4EDDC},
        {0xff06162D,0xff0D2545,0xff14335A,0xff23DBF0,0xffFFD16B,
         0xffF7FBFF,0xffA8C7DD,0xff173B65,0xffFF9B9B,
         0xff234A74,0xff2A5A84,0xff844051,0xff10BBD3,
         0xff12375C,0xff081E3B}
    };
    public final int index;
    public final int background,panel,card,accent,detail,ink,muted,soft,error;
    public final int digit,operator,clear,equals,displayTop,displayBottom;
    private ComfortTheme(int n){
        index=n;
        int[] c=COLORS[n];
        background=c[0];panel=c[1];card=c[2];accent=c[3];detail=c[4];
        ink=c[5];muted=c[6];soft=c[7];error=c[8];
        digit=c[9];operator=c[10];clear=c[11];equals=c[12];
        displayTop=c[13];displayBottom=c[14];
    }
    public boolean dark(){return index==4;}
    public static int clamp(int value){
        return value>=0 && value<NAMES.length?value:0;
    }
    public static ComfortTheme choose(int index){
        return new ComfortTheme(clamp(index));
    }
    private static double channel(int v){
        double s=v/255.0;
        return s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);
    }
    public static double contrast(int fg,int bg){
        double a=.2126*channel((fg>>16)&255)+
            .7152*channel((fg>>8)&255)+.0722*channel(fg&255);
        double b=.2126*channel((bg>>16)&255)+
            .7152*channel((bg>>8)&255)+.0722*channel(bg&255);
        return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    }
}
