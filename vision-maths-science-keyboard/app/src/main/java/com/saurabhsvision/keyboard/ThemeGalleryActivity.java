package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Original offline visual themes catalog.
 * Previews are rendered locally with Canvas, never copied from another app.
 * Existing 0..10 theme selection indices are preserved for upgrades.
 */
public final class ThemeGalleryActivity extends Activity {
    private static final int PHOTO_REQUEST=5421;
    private static final String PREFS="vision_keyboard_settings";
    private static final String[] FILTERS={
        "All","Classic","Sports","Festive","Floral","Celestial",
        "Scenic","Minimal","Vivid","Spotlights","Feather",
        "Carbon","Colorful","Originals","Nickel","Abstract","Landscapes","Custom"
    };
    private static final int DARK=0xff101319,SOFT=0xff20242d,ACCENT=0xff7299ff;
    private SharedPreferences prefs;
    private String filter="All";
    private LinearLayout screen,listing;
    private HorizontalScrollView chipScroll;
    private EditText typingTest;

    private int dp(float size){
        return (int)(size*getResources().getDisplayMetrics().density+.5f);
    }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);
        d.setCornerRadius(dp(radius));return d;
    }
    private TextView label(String value,int size,boolean bold,int color){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        build();
    }
    private void build(){
        screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(DARK);
        setContentView(screen);
        LinearLayout heading=new LinearLayout(this);
        heading.setPadding(dp(16),dp(12),dp(16),dp(8));
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=label("‹",32,true,Color.WHITE);
        back.setGravity(Gravity.CENTER);
        heading.addView(back,new LinearLayout.LayoutParams(dp(40),dp(50)));
        back.setContentDescription("Back to S Board settings");
        back.setOnClickListener(v->finish());
        TextView title=label("S Board Themes",23,true,Color.WHITE);
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(50),1f));
        Button create=new Button(this);
        create.setText("＋ Create");create.setTextSize(13);create.setAllCaps(false);
        create.setTextColor(Color.WHITE);create.setBackground(shape(0xff30466f,17));
        heading.addView(create,new LinearLayout.LayoutParams(dp(112),dp(44)));
        create.setOnClickListener(v->choosePhoto());
        screen.addView(heading);

        chipScroll=new HorizontalScrollView(this);chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);
        chips.setPadding(dp(12),dp(5),dp(12),dp(10));
        chips.setGravity(Gravity.CENTER_VERTICAL);
        for(String category:FILTERS){
            TextView chip=label(category,13,true,Color.WHITE);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(13),0,dp(13),0);
            chip.setBackground(shape(category.equals(filter)?0xff3268e7:0xff292d35,24));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(39));
            lp.setMargins(dp(3),0,dp(3),0);
            chips.addView(chip,lp);
            chip.setOnClickListener(v->{filter=category;build();});
            if(category.equals(filter)){
                final TextView selectedChip=chip;
                chipScroll.post(()->chipScroll.scrollTo(
                    Math.max(0,selectedChip.getLeft()-dp(75)),0));
            }
        }
        chipScroll.addView(chips);
        screen.addView(chipScroll,new LinearLayout.LayoutParams(-1,dp(56)));

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        listing=new LinearLayout(this);listing.setOrientation(LinearLayout.VERTICAL);
        listing.setPadding(dp(12),0,dp(12),dp(16));
        scroll.addView(listing);
        showThemes();

        LinearLayout tryPanel=new LinearLayout(this);
        tryPanel.setOrientation(LinearLayout.HORIZONTAL);
        tryPanel.setPadding(dp(13),dp(8),dp(13),dp(8));
        tryPanel.setGravity(Gravity.CENTER_VERTICAL);
        tryPanel.setBackgroundColor(0xff252b35);
        typingTest=new EditText(this);
        typingTest.setSingleLine(true);
        typingTest.setHint("Try out your new theme");
        typingTest.setHintTextColor(0xffb0b6c7);
        typingTest.setTextColor(Color.WHITE);
        typingTest.setTextSize(15);
        typingTest.setPadding(dp(8),0,dp(8),0);
        tryPanel.addView(typingTest,new LinearLayout.LayoutParams(0,dp(45),1f));
        TextView tryButton=label("⌨ Try",15,true,Color.WHITE);
        tryButton.setGravity(Gravity.CENTER);
        tryButton.setBackground(shape(0xff356be5,12));
        tryPanel.addView(tryButton,new LinearLayout.LayoutParams(dp(82),dp(43)));
        tryButton.setOnClickListener(v->openTypingTest());
        screen.addView(tryPanel,new LinearLayout.LayoutParams(-1,dp(62)));
    }
    private static String category(int t){
        if(t<=10)return "Classic";
        if(t<=12)return "Celestial";
        if(t<=14)return "Floral";
        if(t<=16)return "Scenic";
        if(t<=19)return "Vivid";
        if(t<=21)return "Minimal";
        if(t<=23)return "Spotlights";
        if(t<=25)return "Carbon";
        if(t<=27)return "Feather";
        if(t<=29)return "Festive";
        if(t<=31)return "Sports";
        if(t<=33)return "Colorful";
        if(t==34)return "Custom";
        if(t<=39)return "Originals";
        if(t<=42)return "Carbon";
        if(t<=44)return "Nickel";
        if(t<=46)return "Abstract";
        return "Landscapes";
    }
    private void showThemes(){
        int cards=0;
        for(String section:FILTERS){
            if("All".equals(section))continue;
            if(!"All".equals(filter)&&!filter.equals(section))continue;
            int count=0;
            for(int i=0;i<ThemePalette.count();i++)
                if(category(i).equals(section))count++;
            if(count==0)continue;
            TextView name=label(section+"  ·  "+count+" themes",18,true,Color.WHITE);
            LinearLayout.LayoutParams hd=new LinearLayout.LayoutParams(-1,dp(44));
            hd.topMargin=dp(8);
            listing.addView(name,hd);
            LinearLayout row=null;
            for(int i=0;i<ThemePalette.count();i++){
                if(!category(i).equals(section))continue;
                if(cards%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
                    listing.addView(row,new LinearLayout.LayoutParams(-1,dp(181)));}
                addThemeCard(row,i);
                cards++;
            }
            if(cards%2==1){
                View spacer=new View(this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,1,1f);
                row.addView(spacer,lp);
                cards++;
            }
        }
    }
    private void addThemeCard(LinearLayout row,int theme){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(SOFT,13));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(173),1f);
        lp.setMargins(dp(3),dp(3),dp(3),dp(3));
        row.addView(card,lp);
        ThemePreview preview=new ThemePreview(this,theme);
        card.addView(preview,new LinearLayout.LayoutParams(-1,0,1f));
        TextView caption=label(ThemePalette.NAMES[theme]+
            (prefs.getInt("theme",4)==theme?"  ✓":""),12,true,Color.WHITE);
        caption.setPadding(dp(8),0,dp(5),0);
        caption.setSingleLine(true);
        caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(caption,new LinearLayout.LayoutParams(-1,dp(35)));
        card.setContentDescription("Apply "+ThemePalette.NAMES[theme]+" keyboard theme");
        card.setOnClickListener(v->{
            if(theme==ThemePalette.PHOTO_THEME){
                if(prefs.getString(PhotoThemeArt.PHOTO_URI,null)==null){choosePhoto();return;}
            }
            prefs.edit().putInt("theme",theme).apply();
            Toast.makeText(this,ThemePalette.NAMES[theme]+" applied. Open S Board to see it.",
                Toast.LENGTH_SHORT).show();
            build();
        });
    }
    private void openTypingTest(){
        typingTest.requestFocus();
        InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(im!=null)typingTest.postDelayed(
            ()->im.showSoftInput(typingTest,InputMethodManager.SHOW_IMPLICIT),160);
    }
    private void choosePhoto(){
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.setType("image/*");
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(pick,PHOTO_REQUEST);}
        catch(RuntimeException e){Toast.makeText(this,"Photo picker not available.",
            Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PHOTO_REQUEST||result!=RESULT_OK||data==null||
            data.getData()==null)return;
        Uri uri=data.getData();
        try{
            PhotoThemeArt.saveChosenUri(this,uri);
            Toast.makeText(this,"Photo theme applied. S Board uses a dark overlay for readable keys.",
                Toast.LENGTH_LONG).show();
            build();
        }catch(Exception ex){Toast.makeText(this,
            "Could not use the selected photo. Please choose another.",
            Toast.LENGTH_LONG).show();}
    }

    /** Small, original keyboard visual rendered dynamically from saved palette colors. */
    private static final class ThemePreview extends View {
        private final int theme;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap photo;
        private final RectF rect=new RectF();
        ThemePreview(Context c,int theme){
            super(c);this.theme=theme;
            photo=theme==ThemePalette.PHOTO_THEME?PhotoThemeArt.load(c,320):null;
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            if(photo!=null){
                p.setFilterBitmap(true);p.setColor(Color.WHITE);
                canvas.drawBitmap(photo,new Rect(0,0,photo.getWidth(),photo.getHeight()),
                    new Rect(0,0,(int)w,(int)h),p);
                p.setColor(0x98000818);canvas.drawRect(0,0,w,h,p);
            }else{
                p.setShader(new LinearGradient(0,0,w,h,
                    ThemePalette.BASE[theme],ThemePalette.MID[theme],
                    Shader.TileMode.CLAMP));
                canvas.drawRect(0,0,w,h,p);p.setShader(null);
                ThemeArtwork.drawOverlay(canvas,theme,w,h);
            }
            float left=w*.045f, right=w*.955f;
            paintText(canvas,"the      |        I        |       thanks",
                w*.12f,h*.16f,h*.085f,ThemePalette.TEXT[theme]);
            String[] rows={"qwertyuiop","asdfghjkl","⇧zxcvbnm⌫"};
            float keyHeight=h*.19f,gap=h*.035f;
            for(int row=0;row<rows.length;row++){
                int n=rows[row].length();
                float y=h*(.28f+row*.226f);
                float cw=(right-left)/n;
                for(int i=0;i<n;i++){
                    float x=left+i*cw;
                    rect.set(x+1,y,x+cw-1,y+keyHeight);
                    p.setShader(new LinearGradient(0,y,0,y+keyHeight,
                        ThemePalette.KEY_TOP[theme],ThemePalette.KEY_BOTTOM[theme],
                        Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(rect,Math.min(5,cw*.12f),Math.min(5,cw*.12f),p);
                    p.setShader(null);
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.6f);
                    p.setColor(ThemePalette.STROKE[theme]);canvas.drawRoundRect(rect,3,3,p);
                    p.setStyle(Paint.Style.FILL);
                    paintText(canvas,rows[row].substring(i,i+1),
                        x+cw*.32f,y+keyHeight*.70f,h*.12f,ThemePalette.TEXT[theme]);
                }
            }
            rect.set(left,h*.755f,left+w*.18f,h*.95f);
            p.setColor(ThemePalette.FUNCTION_TOP[theme]);
            canvas.drawRoundRect(rect,4,4,p);
            paintText(canvas,"123",left+w*.03f,h*.886f,h*.11f,ThemePalette.TEXT[theme]);
            rect.set(left+w*.20f,h*.755f,right-w*.14f,h*.95f);
            p.setColor(ThemePalette.SPACE_TOP[theme]);
            canvas.drawRoundRect(rect,4,4,p);
            paintText(canvas,"space",w*.44f,h*.88f,h*.1f,ThemePalette.TEXT[theme]);
            rect.set(right-w*.12f,h*.755f,right,h*.95f);
            p.setColor(ThemePalette.ENTER_TOP[theme]);canvas.drawRoundRect(rect,4,4,p);
        }
        private void paintText(Canvas c,String str,float x,float baseline,
            float size,int color){
            p.reset();p.setAntiAlias(true);p.setColor(color);p.setTextSize(size);
            p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
            c.drawText(str,x,baseline,p);
        }
    }
}
