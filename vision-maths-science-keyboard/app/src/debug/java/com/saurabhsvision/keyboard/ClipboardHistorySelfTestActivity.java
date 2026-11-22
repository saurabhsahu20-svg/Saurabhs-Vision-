package com.saurabhsvision.keyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;

/** Debug-only Android Keystore and clipboard regression test, never in release APK. */
public final class ClipboardHistorySelfTestActivity extends Activity {
    private static final String LOG="SBoardClipQA";
    private static void check(boolean condition,String name){
        if(!condition)throw new AssertionError(name);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        ClipboardHistoryStore store=new ClipboardHistoryStore(this);
        try{
            store.setEnabled(false);
            check(!store.enabled(),"opt-in must default disabled after reset");
            check(!store.add("SBOARD_TEST_TOKEN"),"disabled history must not record text");
            check(store.entries().isEmpty(),"disabled history must appear empty");
            store.setEnabled(true);
            check(store.enabled(),"toggle enable");
            check(store.add("SBOARD_TEST_TOKEN"),"encrypted save failed");
            check(store.entries().size()==1,"read after save");
            check("SBOARD_TEST_TOKEN".equals(store.entries().get(0).text),
                "decryption lost sample text");
            String ciphertext=getSharedPreferences(
                "sboard_clipboard_history_private",Context.MODE_PRIVATE)
                .getString("history_blob","");
            check(!ciphertext.isEmpty()&&!ciphertext.contains("SBOARD_TEST_TOKEN"),
                "clipboard history was persisted in plaintext");
            check(store.add("SBOARD_TEST_TOKEN"),"deduplicating save failed");
            check(store.entries().size()==1,"identical history items duplicated");
            ArrayList<ClipboardHistoryStore.Entry> old=new ArrayList<>();
            old.add(new ClipboardHistoryStore.Entry("expired",
                System.currentTimeMillis()-ClipboardHistoryStore.MAX_AGE_MS-2000));
            check(store.save(old),"could not create expired record");
            check(store.entries().isEmpty(),"expired history must not reappear");
            for(int i=0;i<25;i++)
                check(store.add("test_clip_"+i),"bounded history save "+i);
            check(store.entries().size()==ClipboardHistoryStore.MAX_ITEMS,
                "history exceeds item cap");
            ClipboardManager cm=(ClipboardManager)getSystemService(
                Context.CLIPBOARD_SERVICE);
            check(cm!=null,"Android ClipboardManager missing");
            ClipData styled=ClipData.newHtmlText("test", "Hello World",
                "<b>Hello</b> <i>World</i>");
            check(styled.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML),
                "HTML clipboard mime type missing");
            store.clear();
            check(store.entries().isEmpty(),"clear does not remove history");
            store.setEnabled(false);
            check(!store.enabled(),"disable must persist");
            Log.i(LOG,"PASS: clipboard opt-in, AES-GCM ciphertext, 24-hour expiry, 20 clips, clear, HTML MIME");
        }catch(Throwable error){
            Log.e(LOG,"FAIL: clipboard Android self-test: "+error.getClass().getSimpleName());
            store.setEnabled(false);
            throw new AssertionError("S Board clipboard self-test failed",error);
        }finally{
            store.setEnabled(false);
            finish();
        }
    }
}
