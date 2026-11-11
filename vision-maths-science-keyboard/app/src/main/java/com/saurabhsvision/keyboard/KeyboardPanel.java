package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.WeakHashMap;

/**
 * Adaptive, wrap-content IME container.
 *
 * The prior implementation capped the measured parent height WITHOUT shrinking
 * the child key rows. On phones where Android offered less height, this clipped
 * the last Space/Enter row. This one reflows ALL child rows to the actual space.
 * The final row gets priority, so the spacebar is never sacrificed to decoration.
 */
public final class KeyboardPanel extends LinearLayout {
    private KeyboardBackdrop painter;
    private final WeakHashMap<View,Integer> naturalRows=new WeakHashMap<>();
    private final WeakHashMap<View,Integer> naturalKeys=new WeakHashMap<>();
    private int portraitMaxDp=325,landscapeMaxDp=225;

    public KeyboardPanel(Context context,int theme){
        super(context);
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        setClipChildren(true);
        setClipToPadding(true);
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setTheme(theme);
    }
    public void setTheme(int theme){
        painter=new KeyboardBackdrop(getContext(),theme);
        invalidate();
    }
    public void setCompact(boolean compact){
        portraitMaxDp=compact?264:325;
        landscapeMaxDp=compact?189:225;
        requestLayout();
    }
    private int dp(float value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    @Override protected void onDraw(Canvas canvas){
        if(painter!=null)painter.drawArtwork(canvas,getWidth(),getHeight());
        super.onDraw(canvas);
    }
    private int baseHeight(View view,WeakHashMap<View,Integer> cache){
        Integer saved=cache.get(view);
        if(saved!=null)return saved;
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        int height=lp!=null?lp.height:0;
        if(height<=0)height=dp(42);
        cache.put(view,height);
        return height;
    }
    private int margins(View view){
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        if(lp instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams m=(ViewGroup.MarginLayoutParams)lp;
            return m.topMargin+m.bottomMargin;
        }
        return 0;
    }
    private void adjustKeysInside(View row,int rowHeight){
        if(!(row instanceof LinearLayout))return;
        LinearLayout inner=(LinearLayout)row;
        int available=Math.max(1,rowHeight-inner.getPaddingTop()-inner.getPaddingBottom());
        for(int j=0;j<inner.getChildCount();j++){
            View key=inner.getChildAt(j);
            if(key.getLayoutParams()==null)continue;
            int natural=baseHeight(key,naturalKeys);
            int fit=Math.max(1,available-margins(key));
            key.getLayoutParams().height=Math.min(natural,fit);
        }
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        boolean landscape=getResources().getConfiguration().orientation
            ==Configuration.ORIENTATION_LANDSCAPE;
        int cap=dp(landscape?landscapeMaxDp:portraitMaxDp);
        int mode=View.MeasureSpec.getMode(heightSpec);
        if(mode!=View.MeasureSpec.UNSPECIFIED){
            cap=Math.min(cap,View.MeasureSpec.getSize(heightSpec));
        }
        cap=Math.max(1,cap);
        int count=getChildCount();
        if(count>0){
            int totalMargins=getPaddingTop()+getPaddingBottom();
            int desired=0;
            int[] natural=new int[count];
            for(int i=0;i<count;i++){
                View child=getChildAt(i);
                natural[i]=baseHeight(child,naturalRows);
                totalMargins+=margins(child);
                desired+=natural[i];
            }
            int budget=Math.max(1,cap-totalMargins);
            int[] heights=new int[count];
            if(desired<=budget){
                System.arraycopy(natural,0,heights,0,count);
            }else if(count<=2){
                for(int i=0;i<count;i++)
                    heights[i]=Math.max(1,(int)((long)budget*natural[i]/Math.max(1,desired)));
            }else{
                // First child: top mode toolbar. Last child: mandatory spacebar.
                // Preserve bottom row before distributing space to other rows.
                int footer=Math.min(natural[count-1],
                    Math.min(Math.max(dp(36),(int)(budget*.23f)),budget/2));
                int toolbar=Math.min(natural[0],
                    Math.min(Math.max(dp(28),(int)(budget*.15f)),budget/4));
                footer=Math.max(1,footer);toolbar=Math.max(1,toolbar);
                int middle=Math.max(1,budget-footer-toolbar);
                int middleNatural=0;
                for(int i=1;i<count-1;i++)middleNatural+=natural[i];
                heights[0]=toolbar;
                heights[count-1]=footer;
                int assigned=0;
                for(int i=1;i<count-1;i++){
                    int slice=(int)((long)middle*natural[i]/Math.max(1,middleNatural));
                    heights[i]=Math.max(1,slice);
                    assigned+=heights[i];
                }
                // Assign rounding difference to the penultimate row, never the
                // bottom spacebar row, keeping the last row fully visible.
                heights[count-2]=Math.max(1,heights[count-2]+middle-assigned);
            }
            for(int i=0;i<count;i++){
                View child=getChildAt(i);
                ViewGroup.LayoutParams lp=child.getLayoutParams();
                if(lp!=null){
                    lp.height=heights[i];
                    adjustKeysInside(child,heights[i]);
                }
            }
        }
        // The Android IME parent may offer only a short viewport. Child rows
        // were fit BEFORE measurement, so the last Space/Enter row cannot clip.
        super.onMeasure(widthSpec,
            View.MeasureSpec.makeMeasureSpec(cap,View.MeasureSpec.AT_MOST));
    }
}
