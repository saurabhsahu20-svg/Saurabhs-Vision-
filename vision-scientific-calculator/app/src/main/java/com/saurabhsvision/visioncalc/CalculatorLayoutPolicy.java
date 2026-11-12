package com.saurabhsvision.visioncalc;

/**
 * Pure, Android-independent math for fitting the calculator keypad into the
 * actual visible ScrollView viewport, including system-bar safe padding.
 */
public final class CalculatorLayoutPolicy {
    private CalculatorLayoutPolicy(){}

    /** Extra pixels to add to EACH of the numeric keypad rows.
     * Never shrinks buttons, never adds height on a short screen and never
     * exceeds the allowed touch-target cap.
     */
    public static int extraPerKeyRow(int viewportPx,int contentBottomPx,
            int bottomPaddingPx,int safeGapPx,int existingKeyPx,
            int maximumKeyPx,int rowCount){
        if(rowCount<=0 || viewportPx<=0 || maximumKeyPx<=existingKeyPx)return 0;
        long unused=(long)viewportPx-contentBottomPx-bottomPaddingPx-safeGapPx;
        if(unused<=0)return 0;
        int perRow=(int)Math.min(Integer.MAX_VALUE,unused/rowCount);
        return Math.max(0,Math.min(perRow,maximumKeyPx-existingKeyPx));
    }
}
