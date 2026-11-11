package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 100 3D-style JPG wallpapers are PRE-INSTALLED in APK assets by the build workflow.
 * No ZIP picker, no Internet, no runtime gallery permissions or import step needed.
 * Users can apply any included artwork as a static, animated live or real widget background.
 */
public final class Premium100Activity extends Activity {
    private static final int NAVY=0xff071b49,BLUE=0xff1746d2,GOLD=0xffffd54f;
    private static final int SAVE_JPG=9020;
    private final ArrayList<JSONObject> all=new ArrayList<>();
    private LinearLayout gallery;
    private TextView status;
    private String selectedSave=null;
    private int filter=-1;
    private static final String[] GROUPS={
        "MATHEMATICS","PHYSICS","CHEMISTRY","BIOLOGY","BOTANY",
        "SPACE","TECHNOLOGY","LEARNING","3D ABSTRACT","MOTIVATION"
    };
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        try{
            JSONArray items=new JSONArray(readAsset("premium100/catalog.json"));
            if(items.length()!=100)throw new IOException("Wallpaper pack incomplete: "+items.length());
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);
                if(!item.has("file")||!item.has("name")||!item.has("category"))
                    throw new IOException("Invalid wallpaper catalog item");
                all.add(item);
            }
        }catch(Exception ex){
            Toast.makeText(this,"The built-in artwork pack is missing. Please reinstall the complete APK.",Toast.LENGTH_LONG).show();
        }
        build();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int rgb,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(rgb);d.setCornerRadius(dp(radius));return d;
    }
    private TextView text(String content,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(content);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,1);
        return t;
    }
    private void add(LinearLayout parent,View child,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);
        parent.addView(child,lp);
    }
    private Button button(String label,int color,int ink){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(ink);b.setTypeface(null,1);b.setBackground(bg(color,13));
        b.setMinHeight(dp(50));return b;
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xffeff5ff);
        scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setPadding(dp(14),dp(14),dp(14),dp(28));
        root.setOrientation(LinearLayout.VERTICAL);scroll.addView(root);setContentView(scroll);
        LinearLayout head=new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18),dp(18),dp(18),dp(19));
        head.setBackground(bg(NAVY,22));
        add(head,text("🎓 SAURABH'S VISION",14,GOLD,true),0);
        add(head,text("100 PREMIUM 3D WALLPAPERS",23,Color.WHITE,true),9);
        add(head,text("All 100 are already inside your Android app. No download, ZIP or manual import.",13,0xffc5ddff,false),10);
        add(root,head,0);

        TextView how=text("Choose any wallpaper → Preview / Set Static / Set Animated Live / Use as Digital Widget Background / Save JPG.",14,0xff193c72,false);
        add(root,how,13);
        status=text("",14,BLUE,true);add(root,status,15);

        HorizontalScrollView scrollCats=new HorizontalScrollView(this);
        scrollCats.setHorizontalScrollBarEnabled(false);
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
        scrollCats.addView(filters);
        Button showAll=button("ALL 100",BLUE,Color.WHITE);
        filters.addView(showAll,spacing());
        showAll.setOnClickListener(v->{filter=-1;populate();});
        for(int i=0;i<GROUPS.length;i++){
            final int category=i;
            Button b=button(GROUPS[i]+" · 10",0xffdce8ff,NAVY);
            filters.addView(b,spacing());
            b.setOnClickListener(v->{filter=category;populate();});
        }
        add(root,scrollCats,15);
        gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);
        add(root,gallery,16);
        if(all.size()==100)populate();
        else{
            status.setText("Artwork was not included in this installation.");
            Button help=button("Close",GOLD,NAVY);add(root,help,12);
            help.setOnClickListener(v->finish());
        }
    }
    private LinearLayout.LayoutParams spacing(){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(47));
        lp.rightMargin=dp(7);return lp;
    }
    private String readAsset(String name)throws IOException{
        StringBuilder sb=new StringBuilder();
        try(InputStream in=getAssets().open(name)){
            byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)sb.append(new String(buf,0,n,"UTF-8"));
        }
        return sb.toString();
    }
    private Bitmap thumb(String filename){
        try(InputStream in=getAssets().open("premium100/"+filename)){
            BitmapFactory.Options o=new BitmapFactory.Options();
            o.inSampleSize=4;return BitmapFactory.decodeStream(in,null,o);
        }catch(IOException|OutOfMemoryError e){return null;}
    }
    private void populate(){
        gallery.removeAllViews();
        int start=filter<0?0:filter*10;
        int end=filter<0?all.size():Math.min(start+10,all.size());
        status.setText(filter<0?"Showing 100 built-in 3D wallpapers":GROUPS[filter]+" • 10 pre-installed wallpapers");
        for(int i=start;i<end;i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=i;j<Math.min(i+2,end);j++){
                JSONObject item=all.get(j);
                final String file=item.optString("file");
                final String title=item.optString("name");
                LinearLayout tile=new LinearLayout(this);
                tile.setPadding(dp(5),dp(5),dp(5),dp(10));
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setBackground(bg(Color.WHITE,17));
                ImageView picture=new ImageView(this);
                picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                picture.setBackground(bg(0xff081939,12));
                Bitmap mini=thumb(file);
                if(mini!=null)picture.setImageBitmap(mini);
                tile.addView(picture,new LinearLayout.LayoutParams(-1,dp(235)));
                TextView label=text(String.format(java.util.Locale.US,"%03d  %s",j+1,title),12,NAVY,true);
                label.setGravity(Gravity.CENTER);label.setMaxLines(2);
                tile.addView(label,new LinearLayout.LayoutParams(-1,dp(37)));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
                lp.setMargins(dp(4),dp(4),dp(4),dp(4));
                row.addView(tile,lp);
                tile.setClickable(true);tile.setFocusable(true);
                tile.setContentDescription("Wallpaper "+(j+1)+": "+title);
                tile.setOnClickListener(v->choose(file,title));
            }
            add(gallery,row,2);
        }
    }
    private void choose(String file,String title){
        new AlertDialog.Builder(this)
            .setTitle("3D  •  "+title)
            .setItems(new String[]{
                "🔍 Preview full picture",
                "🖼️ Set as Static Wallpaper",
                "✨ Set as Interactive Live Wallpaper",
                "⏰ Use as Digital Widget Background",
                "💾 Save HD JPG to my phone"
            },(dialog,choice)->{
                if(choice==0)preview(file,title);
                else if(choice==1)applyStatic(file);
                else if(choice==2)applyLive(file);
                else if(choice==3)applyWidget(file);
                else saveImage(file,title);
            }).show();
    }
    private void preview(String file,String title){
        Bitmap bmp=null;
        try(InputStream in=getAssets().open("premium100/"+file)){
            bmp=BitmapFactory.decodeStream(in);
        }catch(IOException e){Toast.makeText(this,"Cannot open picture",Toast.LENGTH_LONG).show();return;}
        if(bmp==null)return;
        ImageView image=new ImageView(this);
        image.setImageBitmap(bmp);image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        new AlertDialog.Builder(this).setTitle(title)
            .setView(image).setPositiveButton("Close",(d,w)->{}).show();
    }
    private void applyStatic(String file){
        Bitmap bmp=null;
        try(InputStream in=getAssets().open("premium100/"+file)){
            bmp=BitmapFactory.decodeStream(in);
            if(bmp==null)throw new IOException("Invalid built-in wallpaper");
            WallpaperManager.getInstance(this).setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM);
            Toast.makeText(this,"3D wallpaper applied to Home Screen.",Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Toast.makeText(this,"Could not set wallpaper. Check Android wallpaper permissions.",Toast.LENGTH_LONG).show();
        }finally{if(bmp!=null && !bmp.isRecycled())bmp.recycle();}
    }
    private File copyAsset(String file,String target)throws IOException{
        File temp=new File(getFilesDir(),target+".tmp");
        File dest=new File(getFilesDir(),target);
        try(InputStream in=getAssets().open("premium100/"+file);
            OutputStream out=new FileOutputStream(temp)){
            byte[] buf=new byte[16384];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
        }catch(IOException ex){temp.delete();throw ex;}
        if(dest.exists()&&!dest.delete()){temp.delete();throw new IOException("Cannot replace artwork");}
        if(!temp.renameTo(dest)){temp.delete();throw new IOException("Cannot finish image selection");}
        return dest;
    }
    private void applyLive(String file){
        try{
            copyAsset(file,"vision_artwork.jpg");
            SharedPreferences prefs=getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE);
            prefs.edit()
                .putBoolean(EduWallpaperService.V6_LIVE_ENABLED,false)
                .putBoolean(EduWallpaperService.CUSTOM_ART,true)
                .putInt(EduWallpaperService.COLLECTION,-1)
                .putInt(EduWallpaperService.POSTER,-1)
                .putInt("premium_hd_fx",0)
                .putLong(EduWallpaperService.CUSTOM_VERSION,System.currentTimeMillis())
                .apply();
            Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this,EduWallpaperService.class));
            try{startActivity(i);}
            catch(Exception e){
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            }
        }catch(Exception e){
            Toast.makeText(this,"Live wallpaper unavailable on this device.",Toast.LENGTH_LONG).show();
        }
    }
    private void applyWidget(String file){
        try{
            File selected=copyAsset(file,"vision_widget_premium100_source.jpg");
            PremiumWidgetArt.choose(this,selected);
            Toast.makeText(this,"3D widget background saved. Add Vision Edu Walls widget from your Home Screen.",Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Toast.makeText(this,"Could not set widget background.",Toast.LENGTH_LONG).show();
        }
    }
    private void saveImage(String file,String title){
        selectedSave=file;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/jpeg");
        i.putExtra(Intent.EXTRA_TITLE,file);
        try{startActivityForResult(i,SAVE_JPG);}
        catch(Exception e){Toast.makeText(this,"File picker is unavailable.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_JPG||result!=RESULT_OK||data==null||data.getData()==null||selectedSave==null)return;
        try(InputStream in=getAssets().open("premium100/"+selectedSave);
            OutputStream out=getContentResolver().openOutputStream(data.getData())){
            if(out==null)throw new IOException("Cannot write file");
            byte[] buffer=new byte[16384];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            Toast.makeText(this,"HD 3D wallpaper saved.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Saving failed.",Toast.LENGTH_LONG).show();}
        finally{selectedSave=null;}
    }
}
