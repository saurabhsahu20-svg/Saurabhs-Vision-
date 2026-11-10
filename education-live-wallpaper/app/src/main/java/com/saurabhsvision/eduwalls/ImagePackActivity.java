package com.saurabhsvision.eduwalls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Import the separately distributed original HD wallpaper ZIP as a single pack.
 * Only local files; no Internet access, no cloud uploads, no permissions.
 */
public final class ImagePackActivity extends Activity {
    private static final int PICK_ZIP=6381;
    private static final int NAVY=0xff071b49,BLUE=0xff1746d2,GOLD=0xffffd54f;
    private static final int MAX_ITEMS=60,MAX_ENTRY_BYTES=6000000,MAX_TOTAL_BYTES=140000000;
    private File directory;
    private LinearLayout gallery;
    private TextView status;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        directory=new File(getFilesDir(),"vision_hd_collection");
        getWindow().setStatusBarColor(NAVY);
        buildUI();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int col){GradientDrawable d=new GradientDrawable();d.setColor(col);d.setCornerRadius(dp(14));return d;}
    private Button button(String label,int color,int foreground){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);
        b.setTextColor(foreground);b.setBackground(bg(color));b.setMinHeight(dp(52));return b;
    }
    private void buildUI(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(0xfff1f5ff);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(24));
        sc.addView(root);setContentView(sc);
        TextView title=new TextView(this);
        title.setText("✨ PREMIUM 3D HD ART STUDIO");
        title.setTextSize(24);title.setTextColor(NAVY);
        title.setTypeface(null,1);root.addView(title);
        TextView note=new TextView(this);note.setText("Import the Premium 3D HD wallpaper ZIP in one tap (up to 60 pictures). Choose any artwork as a Static wallpaper, interactive Live wallpaper, or Digital Widget background. Your art stays on this phone.");
        note.setTextColor(0xff48628a);note.setTextSize(14);note.setPadding(0,dp(9),0,dp(13));root.addView(note);
        Button choose=button("📦 Import Premium 3D HD Wallpaper ZIP",GOLD,NAVY);
        root.addView(choose);choose.setOnClickListener(v->pickZip());
        Button fx=button("🌟 LIVE FX: Stardust / Comets / Orbits / Minimal",BLUE,Color.WHITE);
        LinearLayout.LayoutParams fxp=new LinearLayout.LayoutParams(-1,-2);fxp.topMargin=dp(10);
        root.addView(fx,fxp);fx.setOnClickListener(v->selectEffect());
        Button reset=button("↩ Restore Original Digital Widget Backgrounds",0xffdceaff,NAVY);
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.topMargin=dp(10);
        root.addView(reset,rlp);
        reset.setOnClickListener(v->{
            PremiumWidgetArt.clear(this);
            Toast.makeText(this,"Original educational widget backgrounds restored.",Toast.LENGTH_LONG).show();
        });
        status=new TextView(this);status.setTextColor(BLUE);status.setTextSize(14);
        status.setPadding(0,dp(16),0,dp(10));root.addView(status);
        gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);
        root.addView(gallery);
        refresh();
    }
    private void selectEffect(){
        final String[] items={"🌠 Floating Stardust","☄️ Shooting Comets","🪐 Neon Orbits","🌿 Minimal Glow / Battery Friendly"};
        int current=getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE).getInt("premium_hd_fx",0);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Live 3D Wallpaper Animation")
            .setSingleChoiceItems(items,Math.max(0,Math.min(3,current)),(dlg,which)->{
                getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE).edit()
                    .putInt("premium_hd_fx",which).apply();
                Toast.makeText(this,"Effect selected: "+items[which],Toast.LENGTH_SHORT).show();
                dlg.dismiss();
            })
            .setNegativeButton("Cancel",null)
            .show();
    }
    private void pickZip(){
        try {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});
            startActivityForResult(i,PICK_ZIP);
        }catch(Exception e){Toast.makeText(this,"File picker not available.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int rc,int res,Intent data){
        super.onActivityResult(rc,res,data);
        if(rc!=PICK_ZIP||res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        status.setText("Importing HD wallpapers from ZIP…");
        new Thread(()->{
            String result;
            try{
                int count=importZip(uri);
                result=count+" HD wallpaper images imported!";
            }catch(Exception e){result="Import unsuccessful. Make sure you selected a ZIP containing JPG or PNG wallpapers.";}
            final String message=result;
            runOnUiThread(()->{status.setText(message);refresh();});
        },"vision-art-import").start();
    }
    private int importZip(Uri uri)throws IOException {
        if(!directory.exists()&&!directory.mkdirs())throw new IOException("Storage error");
        ArrayList<File> pending=new ArrayList<>();
        int count=0,total=0;
        try(InputStream input=getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException("Cannot read ZIP");
            try(ZipInputStream zip=new ZipInputStream(input)){
                ZipEntry entry;
                byte[] chunk=new byte[8192];
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName().toLowerCase(Locale.ROOT);
                    if(entry.isDirectory() || !(name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".png"))){
                        zip.closeEntry();continue;
                    }
                    if(count>=MAX_ITEMS)break;
                    ByteArrayOutputStream output=new ByteArrayOutputStream(250000);
                    int n,bytes=0;
                    while((n=zip.read(chunk))!=-1){
                        bytes+=n;total+=n;
                        if(bytes>MAX_ENTRY_BYTES||total>MAX_TOTAL_BYTES)throw new IOException("Wallpaper archive too large");
                        output.write(chunk,0,n);
                    }
                    byte[] payload=output.toByteArray();
                    BitmapFactory.Options opts=new BitmapFactory.Options();
                    opts.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(payload,0,payload.length,opts);
                    if(opts.outWidth<80||opts.outHeight<160)throw new IOException("Invalid picture");
                    opts.inJustDecodeBounds=false;
                    opts.inSampleSize=1;
                    while(opts.outWidth/opts.inSampleSize>1080||opts.outHeight/opts.inSampleSize>1920)opts.inSampleSize*=2;
                    Bitmap bitmap=BitmapFactory.decodeByteArray(payload,0,payload.length,opts);
                    if(bitmap==null)throw new IOException("Cannot decode image");
                    File dest=new File(directory,String.format(Locale.US,"hd_%03d_temp.jpg",count+1));
                    try(FileOutputStream out=new FileOutputStream(dest)){
                        if(!bitmap.compress(Bitmap.CompressFormat.JPEG,89,out))throw new IOException("Cannot save art");
                    }finally{bitmap.recycle();}
                    pending.add(dest);
                    count++;
                    zip.closeEntry();
                }
            }
        }catch(IOException ex){
            for(File f:pending)f.delete();
            throw ex;
        }
        if(count==0){for(File f:pending)f.delete();throw new IOException("No pictures found");}
        // Replace collection only when the entire archive has been decoded safely.
        File[] existing=directory.listFiles((f,n)->n.matches("hd_[0-9]{3}\\.jpg"));
        if(existing!=null)for(File f:existing)f.delete();
        for(int i=0;i<pending.size();i++){
            File target=new File(directory,String.format(Locale.US,"hd_%03d.jpg",i+1));
            if(!pending.get(i).renameTo(target))throw new IOException("Cannot finish import");
        }
        return count;
    }
    private ArrayList<File> images(){
        ArrayList<File> files=new ArrayList<>();
        File[] all=directory.listFiles((f,n)->n.matches("hd_[0-9]{3}\\.jpg"));
        if(all!=null){Collections.addAll(files,all);Collections.sort(files,(a,b)->a.getName().compareTo(b.getName()));}
        return files;
    }
    private void refresh(){
        if(gallery==null)return;
        gallery.removeAllViews();
        ArrayList<File> photos=images();
        status.setText(photos.size()+" HD wallpapers currently in this app.");
        if(photos.isEmpty()){
            TextView t=new TextView(this);
            t.setText("First download the HD Wallpaper ZIP from your ChatGPT collection, then tap Import ZIP above.");
            t.setTextSize(15);t.setTextColor(0xff52658c);
            t.setPadding(0,dp(20),0,0);gallery.addView(t);return;
        }
        for(int n=0;n<photos.size();n+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int k=n;k<Math.min(n+2,photos.size());k++){
                final File image=photos.get(k);
                final int index=k;
                LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
                tile.setPadding(dp(4),dp(4),dp(4),dp(9));tile.setBackground(bg(Color.WHITE));
                ImageView view=new ImageView(this);view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=4;
                Bitmap thumb=BitmapFactory.decodeFile(image.getAbsolutePath(),o);
                if(thumb!=null)view.setImageBitmap(thumb);
                tile.addView(view,new LinearLayout.LayoutParams(-1,dp(190)));
                TextView text=new TextView(this);
                text.setText(String.format(Locale.US,"HD Wallpaper %02d",index+1));
                text.setTextColor(NAVY);text.setTextSize(13);text.setGravity(Gravity.CENTER);
                tile.addView(text);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
                lp.setMargins(dp(3),dp(4),dp(3),dp(4));row.addView(tile,lp);
                tile.setOnClickListener(v->chooseImage(image,index));
            }
            gallery.addView(row);
        }
    }
    private void chooseImage(File file,int index){
        new android.app.AlertDialog.Builder(this)
            .setTitle("Premium HD Wallpaper "+String.format(Locale.US,"%02d",index+1))
            .setItems(new String[]{
                "✨ Animated LIVE Wallpaper",
                "🖼️ Static HD Wallpaper",
                "⏰ Digital Widget Background (real phone details)"
            },(dlg,which)->{
                if(which==0)applyLive(file);
                else if(which==1)applyStatic(file);
                else applyWidgetBackground(file);
            })
            .setNegativeButton("Cancel",null)
            .show();
    }
    private void applyWidgetBackground(File image){
        try{
            PremiumWidgetArt.choose(this,image);
            Toast.makeText(this,"HD Digital Widget art applied! Time, weather, date and battery remain live.",Toast.LENGTH_LONG).show();
        }catch(IOException e){
            Toast.makeText(this,"Couldn't apply this picture to widgets.",Toast.LENGTH_LONG).show();
        }
    }
    private void applyStatic(File image){
        Bitmap bmp=null;
        try{
            BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=1;
            bmp=BitmapFactory.decodeFile(image.getAbsolutePath(),o);
            if(bmp==null)throw new IOException("Invalid image");
            WallpaperManager.getInstance(this).setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM);
            Toast.makeText(this,"Static HD wallpaper applied.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Cannot apply wallpaper on this device.",Toast.LENGTH_LONG).show();}
        finally{if(bmp!=null)bmp.recycle();}
    }
    private void applyLive(File image){
        File target=new File(getFilesDir(),"vision_artwork.jpg"),tmp=new File(getFilesDir(),"vision_artwork_tmp.jpg");
        try(FileInputStream in=new FileInputStream(image); FileOutputStream out=new FileOutputStream(tmp)){
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
        }catch(IOException e){Toast.makeText(this,"Couldn't load image.",Toast.LENGTH_LONG).show();return;}
        if(target.exists()&&!target.delete()){tmp.delete();Toast.makeText(this,"Could not replace old image.",Toast.LENGTH_LONG).show();return;}
        if(!tmp.renameTo(target)){Toast.makeText(this,"Could not select HD art.",Toast.LENGTH_LONG).show();return;}
        getSharedPreferences(EduWallpaperService.PREFS,MODE_PRIVATE).edit()
            .putBoolean(EduWallpaperService.V6_LIVE_ENABLED,false)
            .putBoolean(EduWallpaperService.CUSTOM_ART,true)
            .putInt(EduWallpaperService.POSTER,-1)
            .putInt(EduWallpaperService.COLLECTION,-1)
            .putLong(EduWallpaperService.CUSTOM_VERSION,System.currentTimeMillis()).apply();
        Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,EduWallpaperService.class));
        try{startActivity(i);}
        catch(Exception e){
            try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(Exception ignored){Toast.makeText(this,"Open Android wallpaper picker.",Toast.LENGTH_LONG).show();}
        }
    }
}
