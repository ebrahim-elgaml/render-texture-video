package com.example.ebrahim_elgaml.retrieveframes;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MYRETRIEVERDEBUG";
    private MediaMetadataRetriever retriever;
    private ArrayList<Bitmap> frames = new ArrayList<Bitmap>();
    private double durationInSeconds;
    private final long MICRO_SECODND = 1000000;
    private double frameRate;
    private double step;
    private TextView myTextView;
    private ProgressDialog progressDialog;
    private TextureView mTextureView;
    private Renderer mRenderer;
    private final int FRAMES_NUMBER_PER_THREAD = 100;
    private ArrayList<FrameRetrieverThread> retrieverThreads = new ArrayList<>();
    private FinalizeThread finalizeThread ;
    private int threadID = 0 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVideoProbe(12.08, 20.08);
        File videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"test.mp4");
        initRetriever(videoFile.getAbsolutePath(), 12.08, 30.08);
        myTextView = (TextView)findViewById(R.id.textView);
        mRenderer = new Renderer();
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mRenderer);

    }
    public void initRetriever(String path, double d, double f){
        progressDialog = ProgressDialog.show(MainActivity.this, "",
                "Loading ...", true);
        progressDialog.setCancelable(true);
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        setVideoProbe(d, f);
        setRetrieveThreads();
    }
    public void setVideoProbe(double d, double f){
        durationInSeconds = d;
        frameRate = f;
        step = MICRO_SECODND/frameRate;
    }
    public void setRetrieveThreads(){
//
//        for(long start = 0 ; start < durationInSeconds * MICRO_SECODND ; start += step * (FRAMES_NUMBER_PER_THREAD + 1)){
//
//            FrameRetrieverThread th = new FrameRetrieverThread(start, threadID);
//            retrieverThreads.add(th);
//        }
        FrameRetrieverThread th = new FrameRetrieverThread(0, threadID);
        retrieverThreads.add(th);
        runThreads();
        finalizeThread = new FinalizeThread();
        finalizeThread.start();
    }
    public void runThreads(){
        Log.i(TAG, "Number of threads : "+ retrieverThreads.size());
        for(FrameRetrieverThread f : retrieverThreads){
            f.start();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRenderer.halt();
    }
    public class FrameRetrieverThread extends Thread{
        private long start; // in micro seconds
        private double end; // in micro seconds
        private int ID;
        private boolean finished = false;

        public FrameRetrieverThread( long start, int ID) {
            super("My Thread");
            this.start = start;
            this.ID = ID;
            this.end = start + (step * FRAMES_NUMBER_PER_THREAD);
            threadID ++;
            Log.i(TAG, " start :" + start + " end " + end);

        }
        public void run(){
            for(long seconds = 0 ; seconds < durationInSeconds * MICRO_SECODND; seconds += step){
                frames.add(retriever.getFrameAtTime(seconds, MediaMetadataRetriever.OPTION_CLOSEST));
            }
            finished = true;
        }
    }
    public class FinalizeThread extends Thread{
        public FinalizeThread(){
            super("Finalize Thread");
           // Log.i(TAG, "Threads count : " +  retrieverThreads.size());
        }
        public void run(){
           // Log.i(TAG, "Before Stall");
            while(!checkRetrieverThreads());
          //  Log.i(TAG, "After Stall");
            myTextView.post(new Runnable() {
                @Override
                public void run() {
                    if (checkRetrieverThreads()) {
                        myTextView.setText("ARRAY LENGTH IS : " + frames.size());
                        progressDialog.dismiss();
                    }
                }
            });
        }
        public boolean checkRetrieverThreads(){
            for(FrameRetrieverThread f : retrieverThreads){
                if(!f.finished){
                    return false;
                }
            }
            return true;
        }
    }
    private  class Renderer extends Thread implements TextureView.SurfaceTextureListener {
        private Object mLock = new Object();        // guards mSurfaceTexture, mDone
        private SurfaceTexture mSurfaceTexture;
        private boolean mDone;

        private int mWidth;     // from SurfaceTexture
        private int mHeight;

        public Renderer() {
            super("TextureViewCanvas Renderer");
        }

        @Override
        public void run() {
            while (true) {
                SurfaceTexture surfaceTexture = null;

                synchronized (mLock) {
                    while (!mDone && (surfaceTexture = mSurfaceTexture) == null) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException ie) {
                            throw new RuntimeException(ie);     // not expected
                        }
                    }
                    if (mDone) {
                        break;
                    }
                }

                // Render frames until we're told to stop or the SurfaceTexture is destroyed.
                doAnimation();
            }

        }
        private void doAnimation() {
            final int BLOCK_WIDTH = 80;
            final int BLOCK_SPEED = 2;
            int clearColor = 0;
            int xpos = -BLOCK_WIDTH / 2;
            int xdir = BLOCK_SPEED;

            // Create a Surface for the SurfaceTexture.
            Surface surface = null;
            synchronized (mLock) {
                SurfaceTexture surfaceTexture = mSurfaceTexture;
                if (surfaceTexture == null) {

                    return;
                }
                surface = new Surface(surfaceTexture);
            }

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);

            boolean partial = false;
            while (true) {
                Rect dirty = null;
                if (partial) {
                    // Set a dirty rect to confirm that the feature is working.  It's
                    // possible for lockCanvas() to expand the dirty rect if for some
                    // reason the system doesn't have access to the previous buffer.
                    dirty = new Rect(0, mHeight * 3 / 8, mWidth, mHeight * 5 / 8);
                }
                Canvas canvas = surface.lockCanvas(dirty);
                if (canvas == null) {

                    break;
                }
                try {
                    // just curious
                    if (canvas.getWidth() != mWidth || canvas.getHeight() != mHeight) {

                    }
                    Paint p=new Paint();
                    Bitmap b = frames.get(50);
                    p.setColor(Color.RED);
                    canvas.drawBitmap(b, 0, 0, p);
                } finally {
                    try {
                        surface.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException iae) {

                        break;
                    }
                }

                // Advance state
                clearColor += 4;
                if (clearColor > 255) {
                    clearColor = 0;
                    partial = !partial;
                }
                xpos += xdir;
                if (xpos <= -BLOCK_WIDTH / 2 || xpos >= mWidth - BLOCK_WIDTH / 2) {

                    xdir = -xdir;
                }
            }

            surface.release();
        }

        public void halt() {
            synchronized (mLock) {
                mDone = true;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {

            mWidth = width;
            mHeight = height;
            synchronized (mLock) {
                mSurfaceTexture = st;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {

            mWidth = width;
            mHeight = height;
        }

        @Override   // will be called on UI thread
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            synchronized (mLock) {
                mSurfaceTexture = null;
            }
            return true;
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureUpdated(SurfaceTexture st) {

        }
    }

}
