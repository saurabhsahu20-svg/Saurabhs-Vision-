package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.WeakHashMap;

/**
 * Measured S Board keyboard. The previous LinearLayout inherited measurement
 * could let fixed-height children exceed the visible IME viewport. This class
 * measures AND lays out every row explicitly. All three alphabet rows come
 * first, then the separate wide Space / Enter row as the FOURTH key row.
 * It requests enough physical IME height to fit all four rows, with a bottom
 * navigation safety inset, instead of hiding/reordering the last letter row.
 */
public final class KeyboardPanel extends LinearLayout {
    private KeyboardBackdrop painter;
    private final WeakHashMap<View,Integer> naturalRows=new WeakHashMap<>();
    private final WeakHashMap<View,Integer> naturalKeys=new WeakHashMap<>();
    private int portraitMaxDp=365,landscapeMaxDp=340;
    private int[] heights=new int[0];

    public KeyboardPanel(Context context,int theme){
        super(context);
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        setClipToPadding(true);
        setClipChildren(true);
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setTheme(theme);
    }
    public void setTheme(int theme){
        painter=new KeyboardBackdrop(getContext(),theme);
        invalidate();
    }
    public void setCompact(boolean compact){
        // Compact no longer means deleting/clipping the fourth row.
        // Expand the IME surface itself; the OS can still constrain it.
        portraitMaxDp=compact?330:365;
        landscapeMaxDp=compact?305:340;
        requestLayout();
    }
    private int dp(float v){
        return (int)(v*getResources().getDisplayMetrics().density+.5f);
    }
    @Override protected void onDraw(Canvas canvas){
        if(painter!=null)painter.drawArtwork(canvas,getWidth(),getHeight());
        super.onDraw(canvas);
    }
    private int original(View v,WeakHashMap<View,Integer> map){
        Integer old=map.get(v);
        if(old!=null)return old;
        ViewGroup.LayoutParams lp=v.getLayoutParams();
        int h=lp==null||lp.height<=0?dp(38):lp.height;
        map.put(v,h);
        return h;
    }
    private int topMargin(View view){
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        return lp instanceof ViewGroup.MarginLayoutParams?((ViewGroup.MarginLayoutParams)lp).topMargin:0;
    }
    private int bottomMargin(View view){
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        return lp instanceof ViewGroup.MarginLayoutParams?((ViewGroup.MarginLayoutParams)lp).bottomMargin:0;
    }
    private int leftMargin(View view){
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        return lp instanceof ViewGroup.MarginLayoutParams?((ViewGroup.MarginLayoutParams)lp).leftMargin:0;
    }
    private int rightMargin(View view){
        ViewGroup.LayoutParams lp=view.getLayoutParams();
        return lp instanceof ViewGroup.MarginLayoutParams?((ViewGroup.MarginLayoutParams)lp).rightMargin:0;
    }
    private void resizeKeyChildren(View row,int availableHeight){
        if(!(row instanceof LinearLayout))return;
        LinearLayout inner=(LinearLayout)row;
        int rowBudget=Math.max(1,availableHeight-inner.getPaddingTop()-inner.getPaddingBottom());
        for(int i=0;i<inner.getChildCount();i++){
            View child=inner.getChildAt(i);
            ViewGroup.LayoutParams lp=child.getLayoutParams();
            if(lp==null)continue;
            int natural=original(child,naturalKeys);
            // LinearLayout treats the child's explicit height as exact even
            // if its parent was measured smaller. Fit children too.
            lp.height=Math.max(1,Math.min(natural,rowBudget-topMargin(child)-bottomMargin(child)));
        }
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        boolean landscape=getResources().getConfiguration().orientation
            ==Configuration.ORIENTATION_LANDSCAPE;
        int cap=dp(landscape?landscapeMaxDp:portraitMaxDp);
        int specMode=View.MeasureSpec.getMode(heightSpec);
        if(specMode!=View.MeasureSpec.UNSPECIFIED)
            cap=Math.min(cap,View.MeasureSpec.getSize(heightSpec));
        cap=Math.max(1,cap);

        int width=View.MeasureSpec.getSize(widthSpec);
        if(View.MeasureSpec.getMode(widthSpec)==View.MeasureSpec.UNSPECIFIED)
            width=getResources().getDisplayMetrics().widthPixels;
        width=Math.max(1,width);

        int n=getChildCount();
        int padH=getPaddingTop()+getPaddingBottom();
        int[] desired=new int[n];
        int margins=0,actionIndex=n-1;
        for(int i=0;i<n;i++){
            View child=getChildAt(i);
            desired[i]=original(child,naturalRows);
            margins+=topMargin(child)+bottomMargin(child);
            if("sboard_space_action".equals(child.getTag()))actionIndex=i;
        }
        int budget=Math.max(n,cap-padH-margins);
        heights=KeyboardLayoutPolicy.fit(desired,budget,dp(36),actionIndex);
        int availableWidth=Math.max(1,width-getPaddingLeft()-getPaddingRight());
        for(int i=0;i<n;i++){
            View child=getChildAt(i);
            resizeKeyChildren(child,heights[i]);
            int childWidth=Math.max(1,availableWidth-leftMargin(child)-rightMargin(child));
            child.measure(View.MeasureSpec.makeMeasureSpec(childWidth,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.max(1,heights[i]),View.MeasureSpec.EXACTLY));
        }
        int usedHeight=padH+margins+KeyboardLayoutPolicy.total(heights);
        // Never report a height smaller than the sum of the rows we laid out.
        // Android can now reserve the correct IME surface for Space + Enter.
        setMeasuredDimension(width,Math.min(cap,Math.max(1,usedHeight)));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int y=getPaddingTop();
        int availableWidth=Math.max(1,r-l-getPaddingLeft()-getPaddingRight());
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            y+=topMargin(child);
            int x=getPaddingLeft()+leftMargin(child);
            int w=Math.max(1,availableWidth-leftMargin(child)-rightMargin(child));
            int height=i<heights.length?heights[i]:Math.max(1,child.getMeasuredHeight());
            child.layout(x,y,x+w,y+height);
            y+=height+bottomMargin(child);
        }
    }
}
