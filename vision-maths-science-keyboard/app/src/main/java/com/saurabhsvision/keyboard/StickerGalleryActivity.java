package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Discoverable sticker gallery that works from S Board Settings without needing
 * to open hidden keyboard toolbar tabs. Original artwork is rendered locally.
 * Gallery shares user-selected PNG via Android's standard receiving-app chooser.
 */
public final class StickerGalleryActivity extends Activity {
    private static final int INK=0xffedf4ff;
    private static final int BG=0xff101726;
    private static final int CARD=0xff1e2940;
    private static final int ACTIVE=0xff536df0;
    private int pack=9; // Open Love first: all earlier nine packs remain accessible.
    private LinearLayout page;
    private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
    private TextView text(String content,int sp,boolean bold){
        TextView v=new TextView(this);
        v.setText(content);v.setTextSize(sp);v.setTextColor(INK);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private GradientDrawable bg(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        return d;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        render();
    }
    private void render(){
        page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        setContentView(page);
        LinearLayout top=new LinearLayout(this);
        top.setPadding(dp(12),dp(8),dp(12),dp(5));
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=text("‹",33,true);back.setGravity(Gravity.CENTER);
        back.setContentDescription("Return to S Board settings");
        top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(49)));
        back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView headline=text("🧩 S Board Stickers",22,true);
        titles.addView(headline);
        TextView note=text("126 original stickers • 15 packs • Offline",12,false);
        note.setTextColor(0xff9cb7d6);
        titles.addView(note);
        top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        page.addView(top);
        TextView hint=text("Select a category, then tap a picture to SHARE its PNG.",13,false);
        hint.setPadding(dp(17),dp(7),dp(12),dp(10));
        hint.setTextColor(0xffbad6f0);
        page.addView(hint);
        HorizontalScrollView categories=new HorizontalScrollView(this);
        categories.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=new LinearLayout(this);
        tabs.setPadding(dp(8),dp(5),dp(8),dp(9));
        for(int i=0;i<StickerCatalog.packCount();i++){
            final int selected=i;
            String title=(i>=9?"✦ ":"")+StickerCatalog.PACKS[i];
            TextView button=text(title,13,true);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(13),0,dp(13),0);
            button.setContentDescription("Show "+StickerCatalog.PACKS[i]+" stickers");
            button.setBackground(bg(i==pack?ACTIVE:CARD,16));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));
            lp.setMargins(dp(3),0,dp(3),0);
            tabs.addView(button,lp);
            button.setOnClickListener(v->{pack=selected;render();});
        }
        categories.addView(tabs);page.addView(categories);
        categories.post(()->{
            if(categories.getParent()!=null){
                int target=Math.max(0,pack-1);
                int width=dp(109);
                categories.scrollTo(target*width,0);
            }
        });

        ScrollView viewport=new ScrollView(this);
        viewport.setFillViewport(true);
        page.addView(viewport,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout contents=new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        contents.setPadding(dp(10),dp(4),dp(10),dp(14));
        viewport.addView(contents);
        String tag=StickerCatalog.PACKS[pack];
        TextView section=text(tag+"  ·  "+StickerCatalog.itemsInPack(pack)+" images",18,true);
        section.setPadding(dp(7),dp(9),0,dp(9));
        contents.addView(section,new LinearLayout.LayoutParams(-1,dp(47)));
        int total=StickerCatalog.itemsInPack(pack);
        for(int r=0;r<(total+2)/3;r++){
            LinearLayout row=new LinearLayout(this);
            for(int c=0;c<3;c++){
                int position=r*3+c;
                if(position>=total){
                    row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(136),1f));
                    continue;
                }
                final int index=StickerCatalog.id(pack,position);
                LinearLayout card=new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                GradientDrawable shape=bg(0xfff9fbff,13);
                card.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(0x554a7ae5),shape,null));
                ImageView preview=new ImageView(this);
                preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                preview.setImageBitmap(StickerCatalog.preview(index,240));
                card.addView(preview,new LinearLayout.LayoutParams(-1,dp(104)));
                TextView caption=text(StickerCatalog.text(index),11,false);
                caption.setTextColor(0xff24334b);
                caption.setGravity(Gravity.CENTER);
                caption.setSingleLine(true);
                caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
                card.addView(caption,new LinearLayout.LayoutParams(-1,dp(22)));
                card.setContentDescription("Share PNG: "+StickerCatalog.text(index));
                LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(0,dp(130),1f);
                cardLp.setMargins(dp(4),dp(4),dp(4),dp(4));
                row.addView(card,cardLp);
                card.setOnClickListener(v->share(index));
            }
            contents.addView(row);
        }
        TextView foot=text("Original S Board art · other apps may not accept images directly from a keyboard.",11,false);
        foot.setPadding(dp(13),dp(11),dp(10),dp(17));
        foot.setTextColor(0xffbed1eb);
        contents.addView(foot);
    }
    private void share(int index){
        try{
            Uri png=StickerProvider.uriFor(index);
            Intent send=new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM,png);
            send.setClipData(ClipData.newUri(getContentResolver(),
                "Original S Board sticker",png));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send,"Share S Board sticker"));
        }catch(RuntimeException error){
            Toast.makeText(this,"No app available to share this picture.",
                Toast.LENGTH_SHORT).show();
        }
    }
}
