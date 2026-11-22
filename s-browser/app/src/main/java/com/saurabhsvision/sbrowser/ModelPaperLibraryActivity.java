package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Private, permission-minimal offline paper viewer for Grifly.
 * The owner-provided ZIP is imported once with Android's Documents picker.
 * Preview uses Android PdfRenderer, NEVER an iframe or DownloadManager.
 */
public final class ModelPaperLibraryActivity extends Activity {
    private static final int IMPORT_ZIP=3101;
    private static final int SAVE_PDF=3102;
    private static final int BLUE=Color.rgb(9,40,95);
    private static final int MINT=Color.rgb(18,137,110);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Spinner subject,series,set;
    private TextView status,pageInfo,summary;
    private Button preview,save,back,forward,plus,minus,importButton;
    private ImageView paperImage;
    private File pendingCopy,library;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private PdfRenderer.Page openPage;
    private Bitmap shownBitmap;
    private int pageIndex=0;
    private float zoom=1f;
    private boolean importing=false;

    private int px(int dp){return (int)(dp*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(px(radius));return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,1);
        t.setLineSpacing(px(3),1);return t;
    }
    private Button button(String label,int color){
        Button b=new Button(this);
        b.setText(label);b.setTextSize(14);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);b.setBackground(shape(color,12));
        b.setPadding(px(9),px(7),px(9),px(7));return b;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void pad(LinearLayout layout,View child,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=px(top);layout.addView(child,p);
    }
    private LinearLayout row(Button...buttons){
        LinearLayout r=new LinearLayout(this);r.setOrientation(0);
        for(Button b:buttons){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,px(48),1);
            p.rightMargin=px(5);r.addView(b,p);
        }
        return r;
    }
    private Spinner select(String[] options){
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);
        s.setAdapter(a);s.setBackground(shape(0xfff0f6ff,10));
        s.setPadding(px(11),px(4),px(8),px(4));
        return s;
    }
    private void message(String text){status.setText(text);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        library=new File(getFilesDir(),"model-papers-class10");
        ScrollView outer=new ScrollView(this);
        outer.setFillViewport(true);
        outer.setBackgroundColor(0xffeff4ff);
        LinearLayout body=column();
        body.setPadding(px(15),px(18),px(15),px(25));outer.addView(body);
        setContentView(outer);

        LinearLayout hero=column();hero.setPadding(px(17),px(16),px(17),px(18));
        hero.setBackground(shape(BLUE,19));
        TextView heading=text("📚 Model Paper Library",25,Color.WHITE,true);
        pad(hero,heading,0);
        pad(hero,text("CLASS 10 • SAURABH'S VISION • VISION FLASH 💡",12,0xffd2edff,true),8);
        pad(hero,text("6 विषय • 6 मूल पेपर • 30 नए 75-अंक सेट • 30 छोटे 30-अंक सेट",14,0xfff5faff,false),10);
        pad(body,hero,0);

        TextView notice=text("पहली बार 66 PDF वाली ZIP को Import करें। उसके बाद सभी पेपर Grifly में ऑफलाइन खुलेंगे। ZIP का चयन आप ही करेंगे; Grifly आपकी फाइल सर्वर पर नहीं भेजेगा।",14,0xff405473,false);
        notice.setPadding(px(13),px(14),px(13),px(14));
        notice.setBackground(shape(0xffffffff,15));pad(body,notice,14);

        importButton=button("📥 Import 66 PDF ZIP",0xff1555bc);
        importButton.setOnClickListener(v->chooseZip());
        pad(body,importButton,13);

        summary=text(" ",13,0xff305f94,true);pad(body,summary,10);
        pad(body,text("1. विषय चुनें",15,BLUE,true),17);
        subject=select(ModelPaperCatalog.TITLES);pad(body,subject,6);
        pad(body,text("2. पेपर चुनें",15,BLUE,true),15);
        series=select(ModelPaperCatalog.SERIES);pad(body,series,6);
        pad(body,text("3. सेट चुनें (मूल पेपर में केवल एक)",15,BLUE,true),15);
        set=select(new String[]{"सेट 01","सेट 02","सेट 03","सेट 04","सेट 05"});
        pad(body,set,6);

        preview=button("📖 Preview देखें",MINT);
        save=button("⬇ PDF Save करें",0xff2456a4);
        preview.setOnClickListener(v->openSelectedPdf());
        save.setOnClickListener(v->chooseSave());
        pad(body,row(preview,save),17);
        status=text("",14,0xff26486c,false);
        status.setPadding(px(13),px(12),px(13),px(12));
        status.setBackground(shape(0xffe1efff,12));pad(body,status,12);

        back=button("◀ पिछला",0xff45659a);
        forward=button("अगला ▶",0xff45659a);
        minus=button("Zoom −",0xff45659a);
        plus=button("Zoom +",0xff45659a);
        back.setOnClickListener(v->{if(renderer!=null){pageIndex=Math.max(0,pageIndex-1);renderPage();}});
        forward.setOnClickListener(v->{if(renderer!=null){pageIndex=Math.min(renderer.getPageCount()-1,pageIndex+1);renderPage();}});
        minus.setOnClickListener(v->{zoom=Math.max(.65f,zoom/1.2f);renderPage();});
        plus.setOnClickListener(v->{zoom=Math.min(2.2f,zoom*1.2f);renderPage();});
        pageInfo=text("PDF Preview यहाँ दिखेगी",14,BLUE,true);
        pageInfo.setGravity(Gravity.CENTER);pad(body,pageInfo,18);
        pad(body,row(back,forward),8);
        pad(body,row(minus,plus),7);
        paperImage=new ImageView(this);
        paperImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        paperImage.setAdjustViewBounds(true);
        paperImage.setBackground(shape(Color.WHITE,9));
        pad(body,paperImage,13);

        TextView foot=text("शिक्षक मार्गदर्शन: नए अभ्यास सेट स्वतंत्र रूप से बनाए गए हैं, आधिकारिक CGBSE प्रश्नपत्र या प्रमाणित उत्तर-कुंजी नहीं। गलत उत्तर दिखने पर शिक्षक से सत्यापन करें।",13,0xff60512b,false);
        foot.setPadding(px(13),px(14),px(13),px(14));
        foot.setBackground(shape(0xfffff3d5,12));pad(body,foot,20);

        android.widget.AdapterView.OnItemSelectedListener changed=new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                set.setEnabled(series.getSelectedItemPosition()!=1);
                closePdf();refreshAvailability();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        };
        subject.setOnItemSelectedListener(changed);
        series.setOnItemSelectedListener(changed);
        set.setOnItemSelectedListener(changed);
        refreshAvailability();
        importBundledPapersIfAvailable();
    }
    private File requestedPdf(){
        return new File(library,ModelPaperCatalog.path(subject.getSelectedItemPosition(),
            series.getSelectedItemPosition(),set.getSelectedItemPosition()+1));
    }
    private void refreshAvailability(){
        int count=0;
        for(String path:ModelPaperCatalog.allPdfPaths())
            if(new File(library,path).isFile())count++;
        summary.setText("ऑफलाइन उपलब्ध: "+count+" / 66 PDF");
        if(importing){message("PDF Import चल रहा है…");return;}
        if(count==0)message("पहले ऊपर Import 66 PDF ZIP दबाकर अपनी ZIP चुनें।");
        else if(!requestedPdf().isFile())message("इस पेपर की PDF नहीं मिली। ZIP दोबारा Import करें।");
        else message("✅ पेपर तैयार है। Preview दबाएँ, PDF डाउनलोड नहीं होगी।");
        preview.setEnabled(count>0 && requestedPdf().isFile());
        save.setEnabled(count>0 && requestedPdf().isFile());
    }
    private void chooseZip(){
        if(importing)return;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});
        try{startActivityForResult(intent,IMPORT_ZIP);}
        catch(Exception e){Toast.makeText(this,"ZIP picker उपलब्ध नहीं है",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==IMPORT_ZIP)importZip(uri);
        else if(request==SAVE_PDF){
            final File source=pendingCopy;
            if(source==null||!source.isFile())return;
            io.execute(()->{
                try(InputStream in=new FileInputStream(source);
                    OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                    if(out==null)throw new IllegalStateException("Output unavailable");
                    byte[] b=new byte[32768];int n;while((n=in.read(b))>=0)out.write(b,0,n);
                    runOnUiThread(()->message("✅ PDF सुरक्षित सेव हो गई।"));
                }catch(Exception e){runOnUiThread(()->message("⚠ PDF Save नहीं हो पाई: "+e.getMessage()));}
            });
        }
    }
    private void importZip(Uri uri){
        if(importing)return;
        importing=true;importButton.setEnabled(false);preview.setEnabled(false);save.setEnabled(false);
        message("सभी 66 PDF की जाँच और सुरक्षित Import कर रहे हैं…");
        closePdf();
        io.execute(()->{
            String result;
            File temp=new File(getFilesDir(),"model-paper-import-"+System.currentTimeMillis());
            try {
                if(!temp.mkdirs())throw new IllegalStateException("Temporary folder unavailable");
                Set<String> found=new HashSet<>();
                long total=0;
                try(InputStream input=getContentResolver().openInputStream(uri)){
                    if(input==null)throw new IllegalStateException("ZIP access denied");
                    try(ZipInputStream zip=new ZipInputStream(input)){
                        ZipEntry entry;
                        byte[] b=new byte[32768];
                        while((entry=zip.getNextEntry())!=null){
                            String path=entry.getName();
                            if(path.startsWith("/")||path.indexOf('\\')>=0||path.contains(".."))
                                throw new IllegalArgumentException("Unsafe ZIP entry");
                            if(!entry.isDirectory() && path.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")){
                                if(!ModelPaperCatalog.expectedPdf(path)||!found.add(path))
                                    throw new IllegalArgumentException("Wrong or duplicate PDF: "+path);
                                File dest=new File(temp,path),parent=dest.getParentFile();
                                if(!parent.isDirectory()&&!parent.mkdirs())
                                    throw new IllegalStateException("Cannot make PDF folder");
                                long count=0;
                                try(FileOutputStream out=new FileOutputStream(dest)){
                                    int n;
                                    while((n=zip.read(b))!=-1){
                                        count+=n;total+=n;
                                        if(count>12_000_000||total>70_000_000)
                                            throw new IllegalArgumentException("PDF pack too large");
                                        out.write(b,0,n);
                                    }
                                }
                                try(FileInputStream pdf=new FileInputStream(dest)){
                                    byte[] sig=new byte[5];
                                    if(pdf.read(sig)!=5||sig[0]!='%'||sig[1]!='P'||sig[2]!='D'||sig[3]!='F'||sig[4]!='-')
                                        throw new IllegalArgumentException("Invalid PDF: "+path);
                                }
                            }
                            zip.closeEntry();
                        }
                    }
                }
                if(!found.equals(ModelPaperCatalog.allPdfPaths()))
                    throw new IllegalArgumentException("पूरे 66 PDF नहीं मिले: "+found.size()+" / 66");
                File backup=new File(getFilesDir(),"model-paper-old-backup");
                deleteDir(backup);
                if(library.exists()&&!library.renameTo(backup))
                    throw new IllegalStateException("Old library is in use");
                if(!temp.renameTo(library)){
                    if(backup.exists())backup.renameTo(library);
                    throw new IllegalStateException("Could not activate imported PDFs");
                }
                deleteDir(backup);
                result="✅ सभी 66 PDF Grifly में ऑफलाइन Import हो गई हैं।";
            }catch(Exception e){
                result="⚠ Import नहीं हुआ: "+e.getMessage();
            }finally{deleteDir(temp);}
            final String report=result;
            runOnUiThread(()->{
                importing=false;importButton.setEnabled(true);
                refreshAvailability();message(report);
            });
        });
    }
    /**
     * Optional preloaded assets for fully offline distributions.
     * A normal GitHub source build contains no PDFs and uses the ZIP picker instead.
     * The owner can attach assets/model-paper-library/Class_10/... PDFs during packaging.
     */
    private void importBundledPapersIfAvailable(){
        if(importing || library.isDirectory() &&
           ModelPaperCatalog.allPdfPaths().stream().allMatch(p->new File(library,p).isFile()))return;
        String first=ModelPaperCatalog.path(0,0,1);
        try(InputStream probe=getAssets().open("model-paper-library/"+first)){
            if(probe.read()!='%')return;
        }catch(Exception ignored){return;}
        importing=true;importButton.setEnabled(false);preview.setEnabled(false);save.setEnabled(false);
        message("इस Grifly Test APK के 66 PDF अंदर से Import हो रहे हैं…");
        io.execute(()->{
            String report;
            File temp=new File(getFilesDir(),"model-papers-bundled-"+System.currentTimeMillis());
            try{
                if(!temp.mkdirs())throw new IllegalStateException("Temporary storage unavailable");
                byte[] buf=new byte[32768];long total=0;
                for(String path:ModelPaperCatalog.allPdfPaths()){
                    File target=new File(temp,path);
                    File parent=target.getParentFile();
                    if(!parent.isDirectory()&&!parent.mkdirs())
                        throw new IllegalStateException("Cannot create PDF storage");
                    long count=0;
                    try(InputStream src=getAssets().open("model-paper-library/"+path);
                        OutputStream dest=new FileOutputStream(target)){
                        int n;while((n=src.read(buf))!=-1){
                            count+=n;total+=n;
                            if(count>12_000_000 || total>70_000_000)
                                throw new IllegalArgumentException("Invalid embedded paper size");
                            dest.write(buf,0,n);
                        }
                    }
                    try(InputStream check=new FileInputStream(target)){
                        byte[] sig=new byte[5];
                        if(check.read(sig)!=5 || sig[0]!='%' || sig[1]!='P' ||
                            sig[2]!='D' || sig[3]!='F' || sig[4]!='-')
                            throw new IllegalArgumentException("Bundled asset is not PDF: "+path);
                    }
                }
                File backup=new File(getFilesDir(),"model-papers-previous");
                deleteDir(backup);
                if(library.exists()&&!library.renameTo(backup))
                    throw new IllegalStateException("Could not replace old paper library");
                if(!temp.renameTo(library)){
                    if(backup.exists())backup.renameTo(library);
                    throw new IllegalStateException("Could not activate PDF library");
                }
                deleteDir(backup);
                report="✅ सभी 66 PDF Grifly में पहले से शामिल हैं। Preview करें।";
            }catch(Exception e){report="⚠ Bundled PDF Import असफल: "+e.getMessage();}
            finally{deleteDir(temp);}
            final String message=report;
            runOnUiThread(()->{importing=false;importButton.setEnabled(true);
                refreshAvailability();message(message);});
        });
    }

    private static void deleteDir(File file){
        if(file==null||!file.exists())return;
        if(file.isDirectory()){
            File[] all=file.listFiles();
            if(all!=null)for(File f:all)deleteDir(f);
        }
        file.delete();
    }
    private void openSelectedPdf(){
        File pdf=requestedPdf();
        if(!pdf.isFile()){message("पहले 66-PDF ZIP Import करें।");return;}
        closePdf();
        try{
            descriptor=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);
            renderer=new PdfRenderer(descriptor);
            pageIndex=0;zoom=1;renderPage();
            message("✅ Offline Preview खुल गई। PDF सेव करने के लिए अलग Save बटन दबाएँ।");
        }catch(Exception e){
            closePdf();message("⚠ PDF Preview नहीं खुल सकी: "+e.getMessage());
        }
    }
    private void renderPage(){
        if(renderer==null)return;
        try{
            if(openPage!=null){openPage.close();openPage=null;}
            if(shownBitmap!=null){paperImage.setImageDrawable(null);shownBitmap.recycle();shownBitmap=null;}
            openPage=renderer.openPage(pageIndex);
            int screen=getResources().getDisplayMetrics().widthPixels-px(42);
            float base=(float)screen/Math.max(1,openPage.getWidth());
            float scale=Math.min(1.7f,base)*zoom;
            int width=Math.max(1,Math.min(1800,Math.round(openPage.getWidth()*scale)));
            int height=Math.max(1,Math.min(2400,Math.round(openPage.getHeight()*scale)));
            shownBitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            shownBitmap.eraseColor(Color.WHITE);
            openPage.render(shownBitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            paperImage.setImageBitmap(shownBitmap);
            pageInfo.setText("पेज "+(pageIndex+1)+" / "+renderer.getPageCount()+
                "   •   Zoom "+Math.round(zoom*100)+"%");
            back.setEnabled(pageIndex>0);forward.setEnabled(pageIndex<renderer.getPageCount()-1);
        }catch(Exception e){
            message("⚠ पेज दिखाने में त्रुटि: "+e.getMessage());
        }
    }
    private void chooseSave(){
        final File pdf=requestedPdf();
        if(!pdf.isFile()){message("PDF अभी उपलब्ध नहीं है।");return;}
        pendingCopy=pdf;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE,ModelPaperCatalog.friendlyName(
            subject.getSelectedItemPosition(),series.getSelectedItemPosition(),set.getSelectedItemPosition()+1));
        try{startActivityForResult(intent,SAVE_PDF);}
        catch(Exception e){message("इस फोन में PDF Save विकल्प नहीं खुल रहा।");}
    }
    private void closePdf(){
        try{if(openPage!=null)openPage.close();}catch(Exception ignored){}openPage=null;
        try{if(renderer!=null)renderer.close();}catch(Exception ignored){}renderer=null;
        try{if(descriptor!=null)descriptor.close();}catch(Exception ignored){}descriptor=null;
        if(paperImage!=null)paperImage.setImageDrawable(null);
        if(shownBitmap!=null)shownBitmap.recycle();shownBitmap=null;
        if(pageInfo!=null)pageInfo.setText("PDF Preview यहाँ दिखेगी");
    }
    @Override protected void onDestroy(){closePdf();io.shutdown();super.onDestroy();}
}
