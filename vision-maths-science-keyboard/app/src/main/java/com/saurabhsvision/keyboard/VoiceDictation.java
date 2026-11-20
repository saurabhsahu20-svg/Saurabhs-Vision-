package com.saurabhsvision.keyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/**
 * Opt-in speech to text for the current editor only.
 * Audio is processed by the Android device's installed recognition provider,
 * which may use network/cloud processing. No app-owned audio recording, file,
 * transcript log, telemetry, or background/continuous listening is created.
 */
final class VoiceDictation implements RecognitionListener {
    interface Callback {
        void onVoiceState(boolean active, String message);
        void onVoiceText(String text);
    }
    private final Context context;
    private final Callback callback;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean active=false;
    private final Runnable timeout=()->{
        if(active)finishWithError("Listening timed out. Tap microphone and try again.");
    };
    VoiceDictation(Context context,Callback callback){
        this.context=context; this.callback=callback;
    }
    boolean isActive(){return active;}

    boolean start(String languageTag){
        if(active)return false;
        if(!SpeechRecognizer.isRecognitionAvailable(context)){
            callback.onVoiceState(false,"Install or enable an Android speech recognition service.");
            return false;
        }
        try{
            recognizer=SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(this);
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
            // A speech provider may still use its server: this is not offline-only.
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);
            active=true;
            callback.onVoiceState(true,"Listening… Speak clearly.");
            handler.postDelayed(timeout,30000L);
            recognizer.startListening(intent);
            return true;
        }catch(RuntimeException ex){
            finishWithError("Voice recognition could not start.");
            return false;
        }
    }
    /** End the utterance and let the provider return the final text. */
    void stop(){
        if(active&&recognizer!=null){
            callback.onVoiceState(true,"Converting speech to text…");
            try{recognizer.stopListening();}
            catch(RuntimeException e){finishWithError("Voice typing stopped.");}
        }
    }
    void cancel(){
        handler.removeCallbacks(timeout);
        SpeechRecognizer old=recognizer;
        recognizer=null;
        active=false;
        if(old!=null){
            try{old.cancel();}catch(RuntimeException ignored){}
            old.destroy();
        }
        callback.onVoiceState(false,"");
    }
    private void finishWithError(String message){
        cancel();
        callback.onVoiceState(false,message);
    }
    @Override public void onReadyForSpeech(Bundle b){callback.onVoiceState(true,"Listening…");}
    @Override public void onBeginningOfSpeech(){callback.onVoiceState(true,"Listening…");}
    @Override public void onRmsChanged(float rms){}
    @Override public void onBufferReceived(byte[] buffer){}
    @Override public void onEndOfSpeech(){callback.onVoiceState(true,"Converting speech to text…");}
    @Override public void onError(int error){
        String message;
        switch(error){
            case SpeechRecognizer.ERROR_NO_MATCH:
                message="No clear words heard. Please speak again.";break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message="No speech detected. Tap mic and speak.";break;
            case SpeechRecognizer.ERROR_AUDIO:
                message="Microphone error. Check audio access.";break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message="Microphone permission is required.";break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message="Speech service needs a working connection.";break;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                message="This language is not available in your speech service.";break;
            default:
                message="Speech recognition unavailable. Try again.";break;
        }
        finishWithError(message);
    }
    @Override public void onResults(Bundle results){
        ArrayList<String> phrases=results==null?null:
            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String transcript="";
        if(phrases!=null){
            for(String candidate:phrases){
                if(candidate!=null&&!candidate.trim().isEmpty()){
                    transcript=candidate.trim();break;
                }
            }
        }
        cancel();
        if(!transcript.isEmpty())callback.onVoiceText(transcript);
        else callback.onVoiceState(false,"No words recognized. Tap mic and retry.");
    }
    @Override public void onPartialResults(Bundle b){}
    @Override public void onEvent(int eventType, Bundle params){}
}