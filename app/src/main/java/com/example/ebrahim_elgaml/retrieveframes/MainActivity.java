package com.example.ebrahim_elgaml.retrieveframes;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MYRETRIEVERDEBUG";
    private MediaMetadataRetriever retriever;
    private String path = "http://res.cloudinary.com/ebrahim-elgaml/video/upload/v1458828046/a2mwuu7vbvfdsbfhg7qg.mp4";
    private ArrayList<Bitmap> frames = new ArrayList<Bitmap>();
    private double durationInMicroSeconds;
    private final long MICRO_SECODND = 1000000;
    private double frameRate;
    private double step;
    private int framesCount = 0;
    private MyThread th;
    private TextView myTextView;
    private ImageView firstImage;
    private ImageView secondImage;
    private ProgressDialog progressDialog;
    private TextureView mTextureView;
    private Renderer mRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // setVideoProbe(12.08, 20.08);
      //  FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();

        File videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"test.mp4");
        Uri videoFileUri = Uri.parse(videoFile.toString());
        myTextView = (TextView)findViewById(R.id.textView);
        firstImage = (ImageView) findViewById(R.id.imageView);
        secondImage = (ImageView) findViewById(R.id.imageView2);
        progressDialog = new ProgressDialog(this);
        progressDialog = ProgressDialog.show(MainActivity.this, "",
                "Loading ...", true);
        progressDialog.setCancelable(true);
        retriever = new MediaMetadataRetriever();
        th = new MyThread();
        th.setUrl(videoFile.getAbsolutePath());
        th.setArrayList(frames);
        th.setThreadTextView(myTextView);
        th.start();
        mRenderer = new Renderer();
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mRenderer);
    }
    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        // Don't do this -- halt the thread in onPause() and wait for it to finish.
        mRenderer.halt();
    }
    public class MyThread extends Thread{
        private String url ;
        private TextView threadTextView;
        private ArrayList<Bitmap> threadList;
        public MyThread(){
            super("My Thread");
        }
        public void setUrl(String s){
            url = s;
        }
        public void setArrayList(ArrayList<Bitmap> s){
            threadList = s;
        }
        public void setThreadTextView(TextView t){
            threadTextView = t;
        }
        public void run(){

            setVideoProbe(12.08, 20.08);
            getVideoFrames(url);
            finalizeThread();
        }
        public void finalizeThread(){
        //    Log.i(TAG, "FINISH THREAD");
            threadTextView.post(new Runnable() {
                @Override
                public void run() {
                    threadTextView.setText("ARRAY LENGTH IS : " + threadList.size());
                    progressDialog.dismiss();
                }
            });
            firstImage.post(new Runnable() {
                @Override
                public void run() {
                    firstImage.setImageBitmap(threadList.get(0));
                }
            });
            secondImage.post(new Runnable() {
                @Override
                public void run() {
                    secondImage.setImageBitmap(threadList.get(199));
                }
            });
            myTextView.post(new Runnable() {
                @Override
                public void run() {
                    mRenderer.start();
                }
            });

        }
        public void setVideoProbe(double d, double f){
            durationInMicroSeconds = d;
            frameRate = f;
            step = MICRO_SECODND/frameRate;
           // Log.i(TAG, "step : " + step);

        }
        public void getVideoFrames(String s){
            retriever.setDataSource(s);
          //  Log.i(TAG, "LOOP LIMIT : " + durationInMicroSeconds * MICRO_SECODND );

            for(long seconds = 0 ; seconds < durationInMicroSeconds * MICRO_SECODND ; seconds += step){
                frames.add(retriever.getFrameAtTime(seconds, MediaMetadataRetriever.OPTION_CLOSEST));
                framesCount ++;
                //Log.i(TAG, "SECODS : " + seconds);
               // Log.i(TAG, "FRAMES COUNT IN LOOP : " + frames.size());
            }

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

                // Latch the SurfaceTexture when it becomes available.  We have to wait for
                // the TextureView to create it.
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
                Log.d(TAG, "Got surfaceTexture=" + surfaceTexture);

                // Render frames until we're told to stop or the SurfaceTexture is destroyed.
                doAnimation();
            }

            Log.d(TAG, "Renderer thread exiting");
        }

        /**
         * Draws updates as fast as the system will allow.
         * <p>
         * In 4.4, with the synchronous buffer queue queue, the frame rate will be limited.
         * In previous (and future) releases, with the async queue, many of the frames we
         * render may be dropped.
         * <p>
         * The correct thing to do here is use Choreographer to schedule frame updates off
         * of vsync, but that's not nearly as much fun.
         */
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
                    Log.d(TAG, "ST null on entry");
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
                    Log.d(TAG, "lockCanvas() failed");
                    break;
                }
                try {
                    // just curious
                    if (canvas.getWidth() != mWidth || canvas.getHeight() != mHeight) {
                        Log.d(TAG, "WEIRD: width/height mismatch");
                    }

                    // Draw the entire window.  If the dirty rect is set we should actually
                    // just be drawing into the area covered by it -- the system lets us draw
                    // whatever we want, then overwrites the areas outside the dirty rect with
                    // the previous contents.  So we've got a lot of overdraw here.
//                    int j = 0;
//                    ByteBuffer bf = MainActivity.byteBuffers.get(frameIndex);
//                    pixels = new int[bf.array().length];
//                    bf.position(0);
//                    while (bf.hasRemaining()) {
//                        pixels[j] = bf.get();
//                        j++;
//                    }
//
//                    b = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
//                    b.setPixels(pixels, 0, mWidth, 0, 0, mWidth, mHeight);
//                    //canvas.drawBitmap(MainActivity.b, 0, 0, new Paint());

                    // canvas.drawRGB(clearColor, clearColor, clearColor);
                    Paint p=new Paint();
//                    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.images);
                    Bitmap b = frames.get(50);
                    p.setColor(Color.RED);
                    canvas.drawBitmap(b, 0, 0, p);
//                    frameIndex++;
                    //canvas.drawRect(xpos, mHeight / 4, xpos + BLOCK_WIDTH, mHeight * 3 / 4, paint);
                } finally {
                    // Publish the frame.  If we overrun the consumer, frames will be dropped,
                    // so on a sufficiently fast device the animation will run at faster than
                    // the display refresh rate.
                    //
                    // If the SurfaceTexture has been destroyed, this will throw an exception.
                    try {
                        surface.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException iae) {
                        Log.d(TAG, "unlockCanvasAndPost failed: " + iae.getMessage());
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
                    Log.d(TAG, "change direction");
                    xdir = -xdir;
                }
            }

            surface.release();
        }

        /**
         * Tells the thread to stop running.
         */
        public void halt() {
            synchronized (mLock) {
                mDone = true;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable(" + width + "x" + height + ")");
            mWidth = width;
            mHeight = height;
            synchronized (mLock) {
                mSurfaceTexture = st;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged(" + width + "x" + height + ")");
            mWidth = width;
            mHeight = height;
        }

        @Override   // will be called on UI thread
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            Log.d(TAG, "onSurfaceTextureDestroyed");

            synchronized (mLock) {
                mSurfaceTexture = null;
            }
            return true;
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureUpdated(SurfaceTexture st) {
            //Log.d(TAG, "onSurfaceTextureUpdated");
        }
    }

}
