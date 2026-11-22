package com.saurabhsvision.keyboard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import android.graphics.Bitmap;

/**
 * Read-only app-created sticker images. Only predefined integer sticker IDs
 * can be opened; there is no generic filesystem/clipboard exposure.
 */
public final class StickerProvider extends ContentProvider {
    static final String AUTHORITY="com.saurabhsvision.keyboard.stickers";
    static Uri uriFor(int item){return Uri.parse("content://"+AUTHORITY+"/s/"+StickerCatalog.clamp(item));}
    private int validId(Uri uri){
        if(uri==null||uri.getPathSegments().size()!=2||
            !"s".equals(uri.getPathSegments().get(0)))
            throw new IllegalArgumentException("Invalid sticker URI");
        String part=uri.getLastPathSegment();
        if(part==null||!part.matches("[0-9]{1,2}"))
            throw new IllegalArgumentException("Invalid sticker ID");
        int id=Integer.parseInt(part);
        if(id<0||id>=StickerCatalog.count())
            throw new IllegalArgumentException("Unknown sticker ID");
        return id;
    }
    private File itemFile(int id) throws FileNotFoundException {
        if(getContext()==null)throw new FileNotFoundException();
        File cache=new File(getContext().getCacheDir(),"sboard_sticker_png");
        if(!cache.exists()&&!cache.mkdirs())throw new FileNotFoundException("Sticker cache unavailable");
        File output=new File(cache,"sboard_"+id+".png");
        if(!output.exists()||output.length()==0){
            Bitmap bitmap=StickerCatalog.draw(id,320);
            try(FileOutputStream stream=new FileOutputStream(output)){
                if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))
                    throw new FileNotFoundException("Could not render sticker image");
            }catch(java.io.IOException ex){
                throw new FileNotFoundException("Sticker render failed");
            }finally{bitmap.recycle();}
        }
        return output;
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){validId(uri);return "image/png";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)
        throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only sticker");
        return ParcelFileDescriptor.open(itemFile(validId(uri)),
            ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] columns,String selection,
        String[] selectionArgs,String sortOrder){
        int id=validId(uri);
        String[] requested=columns==null?
            new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:columns;
        MatrixCursor matrix=new MatrixCursor(requested);
        Object[] row=new Object[requested.length];
        for(int i=0;i<requested.length;i++){
            if(OpenableColumns.DISPLAY_NAME.equals(requested[i]))
                row[i]="SBoard_"+StickerCatalog.text(id).replaceAll("[^a-zA-Z0-9]","_")+".png";
            if(OpenableColumns.SIZE.equals(requested[i])){
                try{row[i]=itemFile(id).length();}
                catch(FileNotFoundException ignored){row[i]=0;}
            }
        }
        matrix.addRow(row);return matrix;
    }
    @Override public Uri insert(Uri uri,ContentValues content){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues content,String selection,String[] args){return 0;}
    @Override public int delete(Uri uri,String selection,String[] args){return 0;}
}
