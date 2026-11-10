package com.saurabhsvision.eduwalls;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public final class EduWallpaperService extends WallpaperService {
    public static final String PREFS = "vision_edu_wallpaper";
    public static final String THEME = "theme";
    public static final String SPEED = "speed";
    @Override public Engine onCreateEngine() { return new EduEngine(); }

    private final class EduEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final EducationPainter painter = new EducationPainter();
        private final SharedPreferences prefs = getSharedPreferences(PREFS,MODE_PRIVATE);
        private volatile boolean visible = false;
        private float tapX=-1000f,tapY=-1000f;
        private long tapAt=0L;
        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                if(!visible)return;
                drawFrame();
                handler.postDelayed(this,prefs.getInt(SPEED,1)==0?84:42);
            }
        };
        @Override public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(true);
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        @Override public void onDestroy() {
            visible=false;
            handler.removeCallbacks(ticker);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }
        @Override public void onVisibilityChanged(boolean isVisible) {
            visible=isVisible;
            handler.removeCallbacks(ticker);
            if(visible)handler.post(ticker);
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height) {
            super.onSurfaceChanged(holder,format,width,height);
            if(visible)drawFrame();
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible=false;handler.removeCallbacks(ticker);
            super.onSurfaceDestroyed(holder);
        }
        @Override public void onTouchEvent(MotionEvent event) {
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
                tapX=event.getX();tapY=event.getY();tapAt=SystemClock.uptimeMillis();
            }
            super.onTouchEvent(event);
        }
        @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {
            if(visible) {handler.removeCallbacks(ticker);handler.post(ticker);}
        }
        private void drawFrame() {
            SurfaceHolder holder=getSurfaceHolder();
            Canvas canvas=null;
            try {
                canvas=holder.lockCanvas();
                if(canvas!=null) painter.draw(canvas,canvas.getWidth(),canvas.getHeight(),
                    SystemClock.uptimeMillis()/1000f,prefs.getInt(THEME,0),
                    prefs.getInt(SPEED,1),tapX,tapY,tapAt);
            } catch(IllegalArgumentException | IllegalStateException ignored) {
                // A temporary detached surface must not crash the wallpaper.
            } finally {
                if(canvas!=null) {
                    try {holder.unlockCanvasAndPost(canvas);}catch(IllegalArgumentException | IllegalStateException ignored){}
                }
            }
        }
    }
}
