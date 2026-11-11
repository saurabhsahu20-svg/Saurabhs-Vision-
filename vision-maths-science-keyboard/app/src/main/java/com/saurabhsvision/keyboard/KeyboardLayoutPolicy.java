package com.saurabhsvision.keyboard;

/** Pure Java vertical sizing policy; can be regression-tested without Android. */
public final class KeyboardLayoutPolicy {
    private KeyboardLayoutPolicy(){}

    /**
     * Every view receives some height, but the final action row (Space / Enter)
     * has first claim on the budget. All other rows shrink proportionally.
     */
    public static int[] fit(int[] natural,int budget,int minimumFooter) {
        if(natural==null||natural.length==0)return new int[0];
        int n=natural.length;
        int[] result=new int[n];
        budget=Math.max(n,budget);
        if(n==1){result[0]=budget;return result;}
        int desired=0;
        for(int value:natural)desired+=Math.max(1,value);
        if(desired<=budget){
            for(int i=0;i<n;i++)result[i]=Math.max(1,natural[i]);
            return result;
        }
        // Make enough space for all preceding rows, but reserve the
        // Space/Enter action row before laying out typing/emoji rows.
        int footer=Math.min(Math.max(1,natural[n-1]),
            Math.min(Math.max(minimumFooter,budget/4),budget-n+1));
        footer=Math.max(1,footer);
        result[n-1]=footer;
        int otherBudget=budget-footer;
        int otherNatural=0;
        for(int i=0;i<n-1;i++)otherNatural+=Math.max(1,natural[i]);
        int assigned=0;
        for(int i=0;i<n-1;i++){
            int amount=Math.max(1,(int)((long)otherBudget*Math.max(1,natural[i]) /
                                         Math.max(1,otherNatural)));
            result[i]=amount;
            assigned+=amount;
        }
        int difference=otherBudget-assigned;
        // Keep the last action row constant. Correct any rounding mismatch
        // on taller non-footer rows, while preserving at least 1 pixel each.
        while(difference<0){
            int biggest=0;
            for(int i=1;i<n-1;i++)if(result[i]>result[biggest])biggest=i;
            if(result[biggest]<=1)break;
            result[biggest]--;difference++;
        }
        for(int i=0;difference>0;i=(i+1)%(n-1)){
            result[i]++;difference--;
        }
        return result;
    }
    /**
     * Reserve Space/Enter even when its row is lifted one level above the last
     * alphabet row. Legacy layouts continue using the last row by default.
     */
    public static int[] fit(int[] natural,int budget,int minimumActionHeight,int actionIndex){
        if(natural==null||natural.length==0)return new int[0];
        int n=natural.length;
        if(actionIndex<0||actionIndex>=n||actionIndex==n-1)
            return fit(natural,budget,minimumActionHeight);
        int[] reordered=new int[n];
        int at=0;
        for(int i=0;i<n;i++)if(i!=actionIndex)reordered[at++]=natural[i];
        reordered[n-1]=natural[actionIndex];
        int[] fitted=fit(reordered,budget,minimumActionHeight);
        int[] result=new int[n];
        at=0;
        for(int i=0;i<n;i++)if(i!=actionIndex)result[i]=fitted[at++];
        result[actionIndex]=fitted[n-1];
        return result;
    }
    /**
     * Keep the entire IME above the visible Android navigation / gesture bar
     * in landscape. Fixed dp heights (305dp) are not safe on low-height phones:
     * Android can clip the final row without exposing the real viewport size
     * via the IME's UNSPECIFIED measure spec. Use actual screen-height pixels.
     *
     * An IME occupies up to 80% of the physical landscape screen's shorter
     * side. The real system navigation inset is handled by Android's
     * non-edge-to-edge compatibility IME window (target SDK 34), so the last
     * action row is no longer drawn underneath the system navigation buttons.
     * Other row sizes shrink proportionally only when needed.
     */
    public static int landscapeHeightCap(int shortScreenSidePx,int maxConfiguredPx){
        if(shortScreenSidePx<=0)return Math.max(1,maxConfiguredPx);
        int safe=(int)(shortScreenSidePx*0.80f);
        return Math.max(1,Math.min(Math.max(1,maxConfiguredPx),Math.max(1,safe)));
    }
    public static int total(int[] rows){
        int sum=0;for(int height:rows)sum+=height;return sum;
    }
}
