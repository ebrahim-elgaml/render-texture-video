package com.example.ebrahim_elgaml.retrieveframes.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by ebrahim-elgaml on 4/4/16.
 */
public class FrameHolder {
    private Bitmap bitmap;
    private int priority;
    private int index;
    private  static int viewWidth ;
    private  static int viewHeight;

    public FrameHolder(Bitmap bitmap, int priority, int index) {
//        for(int i = 0 ; i < bitmap.getWidth() ; i++){
//            for(int j = 0 ; j < bitmap.getHeight() ; j++){
//                int color = bitmap.getPixel(i, j);
//                if(color == Color.BLACK){
//                    bitmap.setPixel(i, j, Color.TRANSPARENT);
//                }
//            }
//        }
        this.bitmap = bitmap;
        this.priority = priority;
        this.index = index;
    }
    public static FrameHolder createCopyFrame(FrameHolder h){
        Bitmap b = h.getBitmap();
        //return new FrameHolder(b.copy(b.getConfig(), b.isMutable()), h.getPriority(), h.getIndex());
        return new FrameHolder(Bitmap.createBitmap(b), h.getPriority(), h.getIndex());
    }

    public static int getViewWidth() {
        return viewWidth;
    }

    public static void setViewWidth(int viewWidth) {
        FrameHolder.viewWidth = viewWidth;
    }

    public static int getViewHeight() {
        return viewHeight;
    }

    public static void setViewHeight(int viewHeight) {
        FrameHolder.viewHeight = viewHeight;
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
    // this frame minus h <Bitmaps>
    public void makeDiff(FrameHolder h){
        Bitmap a = this.getBitmap();
        Bitmap b = h.getBitmap();
        //this.setBitmap(overlay(a, b));
        for(int i = 0 ; i < a.getWidth() ; i++){
            for(int j = 0 ; j < a.getHeight() ; j++){
                int pixelA = a.getPixel(i, j);
                int pixelB = b.getPixel(i, j);
                int newR = (int)(Color.red(pixelA) - Color.red(pixelB)) &0xFF;
                int newG = (int)(Color.green(pixelA) - Color.green(pixelB))&0xFF;
                int newB = (int)(Color.blue(pixelA) - Color.blue(pixelB))&0xFF;
                //int newAlpha = (int)((((pixelA>>24)& 0xFF)*0.8)+(((pixelB>>24)& 0xFF)*0.2));
                //Log.i("MYRETRIEVERDEBUG", "Color A : RED " + Color.red(pixelA) +" GREEN : " + Color.green(pixelA)  + " Blue : " + Color.blue(pixelA) );
                //Log.i("MYRETRIEVERDEBUG", "Color B : RED " + Color.red(pixelB) +" GREEN : " + Color.green(pixelB)  + " Blue : " + Color.blue(pixelB) );
                //if(pixelB == Color.BLACK) {
                    int color = Color.argb(255, newR, newG, newB);
                    a.setPixel(i, j, color);
//                }else{
//                    int color = Color.argb(Color.alpha(pixelA), Color.red(pixelB),  Color.green(pixelB), Color.blue(pixelB));
//                    a.setPixel(i, j, color);
//                }
            }
        }
        this.setIndex(h.getIndex());
        this.setPriority(h.getPriority());

    }
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new android.graphics.Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

}
