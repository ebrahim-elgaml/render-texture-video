package com.example.ebrahim_elgaml.retrieveframes;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import com.example.ebrahim_elgaml.retrieveframes.utils.FrameHolder;
import com.example.ebrahim_elgaml.retrieveframes.utils.FrameModifier;
import com.example.ebrahim_elgaml.retrieveframes.utils.IntegerComp;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MYRETRIEVERDEBUG";
    private MediaMetadataRetriever retriever;
    private double durationInSeconds;
    private final long MICRO_SECODND = 1000000;
    private double frameRate;
    private double step;
    private TextView myTextView;
    private ProgressDialog progressDialog;
    private TextureView mTextureView;
    private Renderer mRenderer;
    private final int FRAMES_NUMBER_PER_THREAD = 80;
    private ArrayList<FrameRetrieverThread> retrieverThreads = new ArrayList<>();
    private FinalizeThread finalizeThread ;
    private int threadID = 0 ;
    private int cores = Runtime.getRuntime().availableProcessors() * 2;
    private ExecutorService myPool ;
    private final Comparator<FrameHolder> comparator = new IntegerComp();
    private PriorityQueue<FrameHolder> myQueue ;
    private GCThread gcThread;
    private final int frameWidth = 320;
    private final int frameHeight = 180;
    private File videoFile;
    private ArrayList<Bitmap> toSave = new ArrayList<>();
    private FrameModifier frameModifier ;
    private final int diffKeyFrames = 6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myTextView = (TextView)findViewById(R.id.textView);
        myPool = new ThreadPoolExecutor(
                        cores/2,
                        cores,
                        1,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>()
                );
        myQueue = new PriorityQueue<FrameHolder>(100, comparator);
        videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/testing/","test.mp4");
        mRenderer = new Renderer();
        mTextureView = (TextureView) findViewById(R.id.textureView);
        frameModifier.setDiffKeyFrames(diffKeyFrames);
        mTextureView.setSurfaceTextureListener(mRenderer);
        mTextureView.setBackgroundColor(Color.WHITE);
        initRetriever(videoFile.getAbsolutePath(), 12, 15);

    }

    public void initRetriever(String path, double d, double f){
        progressDialog = ProgressDialog.show(MainActivity.this, "",
                "Loading ...", true);
        progressDialog.setCancelable(false);
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        setVideoProbe(d, f);
//        mRenderer.start();
        setRetrieveThreads();
    }
    public void setVideoProbe(double d, double f){
        durationInSeconds = d;
        frameRate = f;
        step = MICRO_SECODND/frameRate;
    }
    public void setRetrieveThreads(){

        for(long start = 0 ; start < durationInSeconds * MICRO_SECODND ; start += step * (FRAMES_NUMBER_PER_THREAD + 1)){
            FrameRetrieverThread th = new FrameRetrieverThread(start, threadID);
            retrieverThreads.add(th);
        }
        finalizeThread = new FinalizeThread();
        finalizeThread.start();
        runThreads();
//        gcThread = new GCThread();
//        gcThread.start();
    }
    public void runThreads(){
       // Log.i(TAG, "Number of threads : " + retrieverThreads.size());
        for(FrameRetrieverThread f : retrieverThreads){

            myPool.execute(f);
        }
        myPool.shutdown();
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

        }
        public void run(){
            int index = 0;
            for(long seconds = start ; seconds < end && seconds < (durationInSeconds * MICRO_SECODND)  ; seconds += step){
                Bitmap b = retriever.getFrameAtTime(seconds, MediaMetadataRetriever.OPTION_CLOSEST);
                if(b!=null) {
                    myQueue.add(new FrameHolder(b, ID, index));
                    index ++;
                }

            }

            finished = true;
        }
    }
    public class FinalizeThread extends Thread{
        public boolean started = false;
        public FinalizeThread(){
            super("Finalize Thread");
        }
        public void run(){
            while(!checkRetrieverThreads());
           // Log.i(TAG, "After Stall");
            myTextView.post(new Runnable() {
                @Override
                public void run() {
                    myTextView.setText("ARRAY LENGTH IS : " + frameModifier.size());
                    progressDialog.dismiss();
              //      Log.i(TAG, "DISMISS");
                }
            });
            myTextView.post(new Runnable() {
                @Override
                public void run() {
                    mRenderer.start();
              //      Log.i(TAG, "START");
                }
            });
        }
        public boolean checkRetrieverThreads(){
            for(FrameRetrieverThread f : retrieverThreads){
                if(!f.finished){
                    return false;
                }
            }
            frameModifier = new FrameModifier(myQueue);
            started = true;
            return true;
        }
    }

    public class GCThread extends Thread {
        public GCThread(){
            super("Garbage collector thred");
        }
        public void run(){
            while(!finalizeThread.started){
                if(myQueue.size() % 3 == 0){
                    System.gc();
                }
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
            paint.setColor(Color.TRANSPARENT);
            paint.setStyle(Paint.Style.FILL);

            //boolean partial = false;
            ArrayList<Bitmap> frames = new ArrayList<>();
            while (true && !frameModifier.isEmpty()) {
                Rect dirty = null;
                Canvas canvas = surface.lockCanvas(dirty);
                if (canvas == null) {

                    break;
                }
                try {
                    //Paint p=new Paint();
                    FrameHolder h = frameModifier.remove();
                    frames.add(h.getBitmap());
                    canvas.drawBitmap(h.getBitmap(), 0, 0, null);
                } finally {
                    try {
                        surface.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException iae) {

                        break;
                    }
                }
                try {
                    sleep(67);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            surface.release();
            try {
                saveFrames(frames);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void halt() {
            synchronized (mLock) {
                mDone = true;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
            Log.i(TAG, " Texture available ");
            mWidth = width;
            mHeight = height;
            FrameHolder.setViewWidth(mWidth);
            FrameHolder.setViewHeight(mHeight);
            synchronized (mLock) {
                mSurfaceTexture = st;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
            Log.i(TAG, " Texture available ");
            mWidth = width;
            mHeight = height;
            FrameHolder.setViewWidth(mWidth);
            FrameHolder.setViewHeight(mHeight);
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
    public void saveFrames(ArrayList<Bitmap> saveBitmapList) throws IOException{
        Random r = new Random();
        int folder_id = r.nextInt(1000) + 1;

        String folder = Environment.getExternalStorageDirectory()+"/testing/frames2/";
        File saveFolder=new File(folder);
//        if(!saveFolder.exists()){
//            saveFolder.mkdirs();
//        }

        int i=1;
        for (Bitmap b : saveBitmapList){
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

            File f = new File(saveFolder,(i+".jpg"));

            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

            fo.flush();
            fo.close();

            i++;
        }


    }
    // path ends with .txt
    public void appendLog(String text, String path)
    {
        File logFile = new File("sdcard/"+ path);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
