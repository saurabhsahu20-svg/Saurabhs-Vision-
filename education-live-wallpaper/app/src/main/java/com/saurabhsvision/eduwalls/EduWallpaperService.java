package com.saurabhsvision.eduwalls;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public final class EduWallpaperService extends WallpaperService {
    public static final String PREFS = "vision_edu_wallpaper";
    public static final String THEME = "theme";
    public static final String SPEED = "speed";
    public static final String INTERACTIVE = "interactive";
    public static final String CUSTOM_ART = "custom_art";
    public static final String CUSTOM_VERSION = "custom_version";
    @Override public Engine onCreateEngine() { return new EduEngine(); }

    private final class EduEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final EducationPainter painter = new EducationPainter(EduWallpaperService.this);
        private final SharedPreferences prefs = getSharedPreferences(PREFS,MODE_PRIVATE);
        private volatile boolean visible = false;
        private float tapX=-1000f,tapY=-1000f;
        private long tapAt=0L;
        private float downX, downY, pageOffset = 0f;
        private long lastTapAt = 0L;
        private float lastTapX=-1000f,lastTapY=-1000f;
        private final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                if(!visible)return;
                drawFrame();
                int speed=prefs.getInt(SPEED,1);
                int delay = speed==0?100:(speed==2?35:52);
                if(powerManager!=null && powerManager.isPowerSaveMode()) delay=Math.max(150,delay);
                handler.postDelayed(this,delay);
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
            if(!prefs.getBoolean(INTERACTIVE,true)) {super.onTouchEvent(event);return;}
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                downX=event.getX();downY=event.getY();
            } else if(action==MotionEvent.ACTION_UP) {
                float x=event.getX(), y=event.getY();
                float dx=x-downX,dy=y-downY;
                final long now=SystemClock.uptimeMillis();
                float threshold=getResources().getDisplayMetrics().density*72f;
                if(Math.abs(dx)>threshold && Math.abs(dx)>Math.abs(dy)*1.6f) {
                    // Horizontal gesture changes the wallpaper design (when the launcher forwards swipes).
                    int old=prefs.getInt(THEME,0);
                    int next=(old+(dx<0?1:3))%4;
                    prefs.edit().putInt(THEME,next).apply();
                    tapX=x;tapY=y;tapAt=now;
                } else if(Math.abs(dx)<threshold*.70f && Math.abs(dy)<threshold*.70f) {
                    // Double-tap changes theme; single-tap creates a subject-specific burst.
                    float distance=(float)Math.hypot(x-lastTapX,y-lastTapY);
                    if(now-lastTapAt<340 && distance<threshold) {
                        int next=(prefs.getInt(THEME,0)+1)%4;
                        prefs.edit().putInt(THEME,next).apply();
                        lastTapAt=0;
                    } else {
                        lastTapAt=now;lastTapX=x;lastTapY=y;
                    }
                    tapX=x;tapY=y;tapAt=now;
                }
                if(visible)drawFrame();
            }
            super.onTouchEvent(event);
        }
        @Override public void onOffsetsChanged(float xOffset,float yOffset,float xOffsetStep,float yOffsetStep,int xPixelOffset,int yPixelOffset) {
            pageOffset=(0.5f-xOffset)*2f;
            if(visible)drawFrame();
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
                    prefs.getInt(SPEED,1),tapX,tapY,tapAt,prefs.getBoolean(INTERACTIVE,true),pageOffset,prefs.getBoolean(CUSTOM_ART,false));
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
