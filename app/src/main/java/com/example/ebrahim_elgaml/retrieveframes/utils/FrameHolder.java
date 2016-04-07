package com.example.ebrahim_elgaml.retrieveframes.utils;

import android.graphics.Bitmap;

/**
 * Created by ebrahim-elgaml on 4/4/16.
 */
public class FrameHolder {
    private Bitmap bitmap;
    private int priority;
    private int index;

    public FrameHolder(Bitmap bitmap, int priority, int index) {
        this.bitmap = bitmap;
        this.priority = priority;
        this.index = index;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }





    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
