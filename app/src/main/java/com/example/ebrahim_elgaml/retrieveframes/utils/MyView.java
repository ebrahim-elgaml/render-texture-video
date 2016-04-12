package com.example.ebrahim_elgaml.retrieveframes.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ebrahim-elgaml on 4/12/16.
 */
public class MyView extends View {
    private FrameModifier frameModifier;
    private boolean canStart = false;
    private long previousTime;
    private long frameRate;
    private Bitmap previousBitmap;
    public MyView(Context context) {
        super(context);
    }


    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }
    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        long current = System.currentTimeMillis();
        if(canStart && !frameModifier.isEmpty() && current - previousTime >= frameRate) {
            previousTime = current;
            previousBitmap = frameModifier.remove().getBitmap();
            canvas.drawBitmap(previousBitmap, 0, 0, new Paint());
        }else{
            if(previousBitmap != null){
                canvas.drawBitmap(previousBitmap, 0, 0, new Paint());
            }
        }
        invalidate();

    }


    public FrameModifier getFrameModifier() {
        return frameModifier;
    }

    public void setFrameModifier(FrameModifier frameModifier) {
        this.frameModifier = frameModifier;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public void setCanStart(boolean canStart) {
        this.canStart = canStart;
    }

    public long getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(long frameRate) {
        this.frameRate = frameRate;
    }
    public long getPreviousTime() {
        return previousTime;
    }

    public void setPreviousTime(long previousTime) {
        this.previousTime = previousTime;
    }

    public Bitmap getPreviousBitmap() {
        return previousBitmap;
    }

    public void setPreviousBitmap(Bitmap previousBitmap) {
        this.previousBitmap = previousBitmap;
    }

}
