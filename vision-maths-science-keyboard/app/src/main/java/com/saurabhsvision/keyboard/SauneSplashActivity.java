package com.saurabhsvision.keyboard;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;

/**
 * SAUNE BOARD branded native animated launch screen.
 *
 * Android launcher app icons are static on ordinary launchers: the sixth
 * final frame of the supplied Saune Board logo is the installed app icon.
 * This 60fps Canvas animation brings that SAME logo alive when the app opens,
 * without network access, video codecs or ongoing background work.
 */
public final class SauneSplashActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable enterApp=this::openApp;
    private boolean opened=false;
    private SauneLogoStage stage;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff041333);
        getWindow().setNavigationBarColor(0xff041333);
        stage=new SauneLogoStage();
        stage.setOnClickListener(v->openApp());
        setContentView(stage);
        handler.postDelayed(enterApp,2400);
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(enterApp);
        if(stage!=null)stage.stop();
        super.onDestroy();
    }
    private void openApp(){
        if(opened||isFinishing())return;
        opened=true;
        handler.removeCallbacks(enterApp);
        startActivity(new Intent(this,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        finish();
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    final class SauneLogoStage extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Bitmap logo;
        private ValueAnimator animation;
        private float phase=0;
        private int cyan=0xff62e8ff,gold=0xffffca56;
        private int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}

        SauneLogoStage(){
            super(SauneSplashActivity.this);
            logo=BitmapFactory.decodeResource(getResources(),R.drawable.saune_board_icon);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setContentDescription("Animated Saune Board Maths and Science keyboard logo");
            setClickable(true);
        }
        @Override protected void onAttachedToWindow(){
            super.onAttachedToWindow();
            animation=ValueAnimator.ofFloat(0,1);
            animation.setDuration(2200);
            animation.setRepeatCount(ValueAnimator.INFINITE);
            animation.addUpdateListener(v->{phase=(float)v.getAnimatedValue();invalidate();});
            animation.start();
        }
        void stop(){if(animation!=null){animation.cancel();animation=null;}}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
        private void circle(Canvas c,float x,float y,float radius,int color,int alpha){
            paint.reset();paint.setAntiAlias(true);paint.setColor(color);paint.setAlpha(alpha);
            c.drawCircle(x,y,radius,paint);
        }
        private void label(Canvas c,String str,float cx,float y,float size,int color,boolean bold){
            paint.reset();paint.setAntiAlias(true);
            paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
            paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(color);c.drawText(str,cx,y,paint);
        }
        @Override protected void onDraw(Canvas canvas){
            int w=getWidth(),h=getHeight();
            if(w<10||h<10)return;
            paint.reset();paint.setAntiAlias(true);
            paint.setShader(new LinearGradient(0,0,w,h,
                new int[]{0xff020921,0xff071c42,0xff020a21},
                null,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
            float x=w*.5f,y=h*.405f,diameter=Math.min(w*.76f,h*.40f);
            float t=phase*(float)(Math.PI*2);
            float pulse=1f+(float)Math.sin(t)*.026f;
            float extent=diameter*.71f;

            // Soft, breathing cyan halo behind the submitted logo artwork.
            paint.setShader(new RadialGradient(x,y,extent,
                new int[]{0x7f3c9cff,0x233fbdff,0x00082033},
                new float[]{0f,.53f,1f},Shader.TileMode.CLAMP));
            canvas.drawCircle(x,y,extent,paint);paint.setShader(null);

            // The icon's EXACT final-frame imagery, not a redraw from Google.
            if(logo!=null&&!logo.isRecycled()){
                float s=diameter*pulse;
                Rect src=new Rect(0,0,logo.getWidth(),logo.getHeight());
                RectF dst=new RectF(x-s/2,y-s/2,x+s/2,y+s/2);
                paint.reset();paint.setAntiAlias(true);paint.setFilterBitmap(true);
                canvas.drawBitmap(logo,src,dst,paint);
            }

            // Subtle travelling blue and gold orbital light strips.
            canvas.save();
            canvas.rotate(phase*110f,x,y);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(dp(1),diameter*.006f));
            for(int k=0;k<2;k++){
                paint.setColor(k==0?cyan:gold);
                paint.setAlpha(130+k*35);
                RectF arc=new RectF(x-diameter*.53f,y-diameter*.28f,
                    x+diameter*.53f,y+diameter*.28f);
                canvas.drawArc(arc,phase*360+k*180,80,false,paint);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();

            for(int i=0;i<19;i++){
                float a=i*2.399963f+phase*.63f;
                float rr=diameter*(.57f+.3f*(i%4)/4f);
                float px=x+(float)Math.cos(a)*rr;
                float py=y+(float)Math.sin(a)*rr*.73f;
                circle(canvas,px,py,Math.max(dp(1),diameter*(.004f+.001f*(i%3))),
                    i%3==0?gold:cyan,90+(int)(70*(.5+.5*Math.sin(t+i))));
            }

            float baseline=y+diameter*.65f;
            label(canvas,"SAUNE BOARD",x,baseline,
                Math.min(w*.087f,dp(34)),Color.WHITE,true);
            label(canvas,"MATHS  •  SCIENCE  •  INDIAN LANGUAGES",x,
                baseline+dp(32),Math.min(w*.031f,dp(12)),0xffa8d8ff,false);
            label(canvas,"by SAURABH'S VISION",x,h-dp(75),
                Math.min(w*.037f,dp(14)),gold,true);

            // Subtle loading indicator; tap anywhere to skip.
            float bottom=h-dp(41),spacing=dp(15);
            for(int i=0;i<3;i++){
                float strength=.5f+.5f*(float)Math.sin(t*1.3f+i*2.1f);
                circle(canvas,x+(i-1)*spacing,bottom,dp(3.3f),cyan,(int)(70+185*strength));
            }
        }
        private float dp(float v){return getResources().getDisplayMetrics().density*v;}
    }
}
