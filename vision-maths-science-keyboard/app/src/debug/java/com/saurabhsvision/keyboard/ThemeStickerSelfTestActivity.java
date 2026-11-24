package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.io.InputStream;
import java.util.Arrays;

/** Debug-only original theme/sticker test, no user data or network. */
public final class ThemeStickerSelfTestActivity extends Activity {
    private static void check(boolean pass,String label){
        if(!pass)throw new AssertionError(label);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        try{
            int total=ThemePalette.count();
            check(total>=51,"Expected gallery theme count");
            check(ThemePalette.NAMES.length==ThemePalette.BASE.length,"Theme BASE length");
            check(total==ThemePalette.MID.length,"Theme MID length");
            check(total==ThemePalette.ACCENTS.length,"Theme ACCENTS length");
            check(total==ThemePalette.KEY_TOP.length,"Theme KEY_TOP length");
            check(total==ThemePalette.KEY_BOTTOM.length,"Theme KEY_BOTTOM length");
            check(total==ThemePalette.SPACE_TOP.length,"Theme SPACE_TOP length");
            check(total==ThemePalette.SPACE_BOTTOM.length,"Theme SPACE_BOTTOM length");
            check(total==ThemePalette.FUNCTION_TOP.length,"Theme FUNCTION_TOP length");
            check(total==ThemePalette.FUNCTION_BOTTOM.length,"Theme FUNCTION_BOTTOM length");
            check(total==ThemePalette.STROKE.length,"Theme STROKE length");
            check(total==ThemePalette.TEXT.length,"Theme TEXT length");
            check(ThemePalette.PHOTO_THEME==34,"Photo index moved");
            check(ThemePalette.norm(-1)==total-1,"Theme index normalization");
            check(StickerCatalog.count()==126,"54 preserved + 72 new sticker designs");
            check("Great job!".equals(StickerCatalog.text(0)),"Original first sticker changed");
            check("Game on".equals(StickerCatalog.text(53)),"Original final sticker changed");
            check(StickerCatalog.id(9,0)==54,"Love sticker start index moved");
            check(StickerCatalog.id(14,11)==125,"Last mood sticker index");
            check(StickerCatalog.packFor(54)==9,"New pack mapping");
            check("Love".equals(StickerCatalog.PACKS[9]),"Love category missing");
            check("Other".equals(StickerCatalog.PACKS[14]),"Other category missing");
            check(StickerCatalog.itemsInPack(9)==12,"Mood pack must offer 12 stickers");
            check(StickerCatalog.packCount()==15,"Nine preserved + six new original packs");
            for(int index=0;index<StickerCatalog.count();index++){
                Bitmap bmp=StickerCatalog.draw(index,120);
                check(bmp!=null&&bmp.getWidth()==120,"Sticker bitmap "+index);
                bmp.recycle();
            }
            Bitmap art=StickerCatalog.draw(54,320);
            check(art.getPixel(0,0)==android.graphics.Color.TRANSPARENT,
                "New Moodlings must have a transparent background");
            art.recycle();
            Uri sample=StickerProvider.uriFor(125);
            check("image/png".equals(getContentResolver().getType(sample)),"Sticker MIME");
            try(InputStream input=getContentResolver().openInputStream(sample)){
                Bitmap png=BitmapFactory.decodeStream(input);
                check(png!=null&&png.getWidth()==320,"Provider PNG payload");
                check(png.getPixel(0,0)==android.graphics.Color.TRANSPARENT,
                    "PNG transparency lost");
                png.recycle();
            }
            check(getSharedPreferences("vision_keyboard_settings",Context.MODE_PRIVATE)
                .getInt("theme",4)>=0,"Theme preference");
            Log.i("SBoardThemeQA","PASS: 51 themes, original 54 IDs unchanged, 126 PNG stickers, 6 mood packs, transparency, provider");
        }catch(Throwable failure){
            Log.e("SBoardThemeQA","FAIL: "+failure.getClass().getSimpleName());
            throw new AssertionError("Original theme/sticker regression failed",failure);
        }finally{finish();}
    }
}
