package com.saurabhsvision.caller;

/** Pure gesture decision logic for incoming real Telecom calls.
 *  Right = answer (+1); left = decline (-1); incomplete drag = cancel (0).
 */
public final class SlideRule {
    public static final int DECLINE=-1;
    public static final int NONE=0;
    public static final int ANSWER=1;
    private SlideRule(){}
    public static int decision(float offset,float limit){
        if(!Float.isFinite(offset)||!Float.isFinite(limit)||limit<=0f)
            return NONE;
        if(Math.abs(offset)<limit*.75f)return NONE;
        return offset>0f?ANSWER:DECLINE;
    }
}
