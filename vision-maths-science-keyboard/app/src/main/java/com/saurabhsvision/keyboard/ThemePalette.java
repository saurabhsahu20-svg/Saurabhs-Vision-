package com.saurabhsvision.keyboard;

/**
 * Single source of truth for S Board's offline keyboard themes.
 * Stable indices 0–4 preserve every existing user's previously saved theme.
 * 5–10 add six premium themes without touching the keyboard's layout or keys.
 * Colors are standard opaque ARGB integers; no external services or assets.
 */
public final class ThemePalette {
    private ThemePalette(){}

    public static final String[] NAMES={
        "Cosmic Blue", "Golden Mathematics", "Science Lab Green",
        "Purple Quantum", "Pure White", "Maroon Velvet",
        "Metallic Silver", "Emerald Neon", "Sapphire Blue",
        "Rose Gold", "Midnight Black"
    };
    public static final String[] SYMBOLS={
        "π","∑","⚛","Ω","☀","♥","◆","✦","♢","✧","∞"
    };
    public static final int[] ACCENTS={
        0xff65e9ff,0xffffd55c,0xff9affd4,0xffeea8ff,0xff2563eb,
        0xffff8ea2,0xff466b89,0xff46efaa,0xff76c5ff,0xffffb5a0,0xff69f3fa
    };
    public static final int[] ON_ACCENT={
        0xff041b33,0xff25152f,0xff032d1d,0xff21103d,0xffffffff,
        0xff310e20,0xffffffff,0xff052a1f,0xff051a3a,0xff371d28,0xff041922
    };
    public static final int[] BASE={
        0xff061531,0xff1a0b31,0xff05283b,0xff1c103a,0xfff5f8ff,
        0xff28091a,0xffedf1f6,0xff031e19,0xff03142f,0xff2c1724,0xff050a12
    };
    public static final int[] MID={
        0xff0f356c,0xff37225c,0xff135568,0xff46306f,0xffe8effa,
        0xff65233b,0xffcfd7e2,0xff105a48,0xff0b4282,0xff684153,0xff142238
    };
    public static final int[] HIGHLIGHT={
        0xff70e7ff,0xffffcf65,0xff9df7cb,0xffd5b8ff,0xff4a78d3,
        0xffff99aa,0xff8097b4,0xff77ffd2,0xff8fd8ff,0xffffbca6,0xff72edf4
    };

    // Keycap colors for themes 5–10; old designs 0–4 are deliberately
    // handled by the original styling path in VisionKeyboardService.
    public static final int[] KEY_TOP={
        0xff2d6397,0xff2d6397,0xff2d6397,0xff2d6397,0xffffffff,
        0xff7a304a,0xffffffff,0xff216c57,0xff2364a7,0xff98657a,0xff253449
    };
    public static final int[] KEY_BOTTOM={
        0xff071d40,0xff071d40,0xff071d40,0xff071d40,0xfff7faff,
        0xff250b1d,0xffd7e0ec,0xff092c23,0xff071d42,0xff2a1523,0xff070f20
    };
    public static final int[] FUNCTION_TOP={
        0xff1c4a80,0xff1c4a80,0xff1c4a80,0xff1c4a80,0xffe6eefb,
        0xff873951,0xffeaf0f7,0xff155542,0xff1e548b,0xff805369,0xff213249
    };
    public static final int[] FUNCTION_BOTTOM={
        0xff0c2c51,0xff0c2c51,0xff0c2c51,0xff0c2c51,0xffdbe7f8,
        0xff351127,0xffc6d4e4,0xff073729,0xff092449,0xff382031,0xff0d1628
    };
    public static final int[] SPACE_TOP={
        0xff2865a4,0xff2865a4,0xff2865a4,0xff2865a4,0xffe6eefb,
        0xff93405d,0xffedf4fb,0xff1e7860,0xff2677bb,0xffa76c7c,0xff304b61
    };
    public static final int[] SPACE_BOTTOM={
        0xff082753,0xff082753,0xff082753,0xff082753,0xffdbe7f8,
        0xff350b21,0xffc4d4e5,0xff083526,0xff082951,0xff34192b,0xff0b1528
    };
    public static final int[] ENTER_TOP={
        0xff68ffff,0xff68ffff,0xff68ffff,0xff68ffff,0xff3c82f6,
        0xffffaac4,0xff6489aa,0xff6affc4,0xff8cdeff,0xffffc8b1,0xff79faff
    };
    public static final int[] ENTER_BOTTOM={
        0xff0bbce9,0xff0bbce9,0xff0bbce9,0xff0bbce9,0xff1d55cf,
        0xffda507b,0xff345570,0xff0fc37e,0xff1694dd,0xffcf7083,0xff19b7c6
    };
    public static final int[] STROKE={
        0xff3cdbff,0xffffd55c,0xff73eac0,0xffd9aeff,0xffcbd6e6,
        0xffff9fb9,0xff8da4bc,0xff58d9ac,0xff67c8ff,0xffffa9ab,0xff6bf7ff
    };
    public static final int[] TEXT={
        0xffffffff,0xffffffff,0xffffffff,0xffffffff,0xff162338,
        0xffffeff4,0xff20334b,0xffebfff6,0xfff0f7ff,0xfffff2ee,0xfff2ffff
    };
    public static int count(){return NAMES.length;}
    public static int norm(int index){return Math.floorMod(index,NAMES.length);}
    public static boolean isLight(int index){int n=norm(index);return n==4||n==6;}
}
