package com.saurabhsvision.keyboard;

/** Counts the UTF-16 code units in one code point directly before the cursor.
 * Kept free of Android dependencies for a fast regression test.
 */
public final class UnicodeDeletion {
    private UnicodeDeletion(){}
    public static int backspaceUnits(CharSequence beforeCursor){
        if(beforeCursor!=null&&beforeCursor.length()>=2){
            int end=beforeCursor.length();
            if(Character.isHighSurrogate(beforeCursor.charAt(end-2))&&
               Character.isLowSurrogate(beforeCursor.charAt(end-1)))return 2;
        }
        return 1;
    }
}
