package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Optional, on-device-only, encrypted text clipboard history.
 *
 * Default: disabled. S Board captures text only while the keyboard is visible
 * and after the user enables history. Stored text is AES-256-GCM encrypted with
 * AndroidKeyStore; entries are omitted after 24h, bounded and never uploaded.
 * Non-text clips, sensitive clips and password editors must not be added.
 */
final class ClipboardHistoryStore {
    static final long MAX_AGE_MS=24L*60L*60L*1000L;
    static final int MAX_ITEMS=20,MAX_TEXT=2000;
    private static final String PREFS="sboard_clipboard_history_private";
    private static final String KEY="history_blob";
    private static final String ACTIVE="history_enabled";
    private static final String ALIAS="sboard_clipboard_aes_gcm_v1";
    private final SharedPreferences prefs;

    static final class Entry {
        final String text;
        final long time;
        Entry(String t,long when){text=t;time=when;}
    }
    ClipboardHistoryStore(Context context){
        prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    boolean enabled(){return prefs.getBoolean(ACTIVE,false);}
    void setEnabled(boolean active){
        if(!active)prefs.edit().putBoolean(ACTIVE,false).remove(KEY).apply();
        else prefs.edit().putBoolean(ACTIVE,true).apply();
    }
    void clear(){prefs.edit().remove(KEY).apply();}

    private SecretKey encryptionKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        java.security.Key existing=ks.getKey(ALIAS,null);
        if(existing instanceof SecretKey)return (SecretKey)existing;
        KeyGenerator generator=KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build());
        return generator.generateKey();
    }
    private String encrypt(String plain) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,encryptionKey());
        byte[] iv=cipher.getIV();
        byte[] ciphertext=cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv,Base64.NO_WRAP)+":"+
            Base64.encodeToString(ciphertext,Base64.NO_WRAP);
    }
    private String decrypt(String encoded) throws Exception {
        String[] parts=encoded.split(":",2);
        if(parts.length!=2)throw new IllegalArgumentException("Invalid encrypted clip");
        byte[] iv=Base64.decode(parts[0],Base64.NO_WRAP);
        if(iv.length!=12)throw new IllegalArgumentException("Invalid GCM IV");
        byte[] ciphertext=Base64.decode(parts[1],Base64.NO_WRAP);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,encryptionKey(),new GCMParameterSpec(128,iv));
        return new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
    }
    ArrayList<Entry> entries(){
        ArrayList<Entry> out=new ArrayList<>();
        if(!enabled())return out;
        String encrypted=prefs.getString(KEY,null);
        if(encrypted==null||encrypted.isEmpty())return out;
        boolean pruned=false;
        long now=System.currentTimeMillis();
        try{
            JSONArray arr=new JSONArray(decrypt(encrypted));
            for(int i=0;i<arr.length();i++){
                JSONObject item=arr.optJSONObject(i);
                if(item==null){pruned=true;continue;}
                long time=item.optLong("time",0L);
                String text=item.optString("text","");
                if(time<=0||time>now||now-time>MAX_AGE_MS||
                   text.trim().isEmpty()||text.length()>MAX_TEXT||
                   out.size()>=MAX_ITEMS){pruned=true;continue;}
                out.add(new Entry(text,time));
            }
            if(pruned)save(out);
        }catch(Exception ignored){
            // Fail closed on key loss or corrupted ciphertext; never persist plaintext.
            clear();
        }
        return out;
    }
    boolean add(String text){
        if(!enabled()||text==null||text.trim().isEmpty()||text.length()>MAX_TEXT)
            return false;
        ArrayList<Entry> items=entries();
        for(int i=items.size()-1;i>=0;i--)
            if(items.get(i).text.equals(text))items.remove(i);
        items.add(0,new Entry(text,System.currentTimeMillis()));
        while(items.size()>MAX_ITEMS)items.remove(items.size()-1);
        return save(items);
    }
    boolean save(ArrayList<Entry> list){
        if(!enabled())return false;
        JSONArray arr=new JSONArray();
        try{
            for(Entry e:list){
                JSONObject value=new JSONObject();
                value.put("time",e.time);
                value.put("text",e.text);
                arr.put(value);
            }
            prefs.edit().putString(KEY,encrypt(arr.toString())).apply();
            return true;
        }catch(Exception ignored){
            // No insecure fallback if keystore is unavailable.
            return false;
        }
    }
}
