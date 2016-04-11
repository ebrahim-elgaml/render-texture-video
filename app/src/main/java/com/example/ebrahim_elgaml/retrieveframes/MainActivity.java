package com.example.ebrahim_elgaml.retrieveframes;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaExtractor;

import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import com.example.ebrahim_elgaml.retrieveframes.utils.FrameHolder;
import com.example.ebrahim_elgaml.retrieveframes.utils.IntegerComp;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;

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
    private int threadID = 0 ;
    private int cores = Runtime.getRuntime().availableProcessors() * 2;
    private ExecutorService myPool ;
    private Comparator<FrameHolder> comparator = new IntegerComp();
    private PriorityQueue<FrameHolder> myQueue ;
    private MediaExtractor mx = new MediaExtractor();
    private ArrayList<ByteBuffer> byteBuffers = new ArrayList<>();
    private File videoFile;
    private BitmapFactory.Options options;
    private PlayerThread mPThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        options = new BitmapFactory.Options();
        myTextView = (TextView)findViewById(R.id.textView);
        myQueue = new PriorityQueue<FrameHolder>(100, comparator);
        videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"test.mp4");
        mRenderer = new Renderer();
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mRenderer);
        mTextureView.setAlpha(0.5f);
        initRetriever(videoFile.getAbsolutePath(), 12.08, 20.08);
        Log.i(TAG, "No cores : "+ cores);

    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void getVideoData(String path){
        mx = new MediaExtractor();
        try {
            mx.setDataSource(path);
            mx.selectTrack(0);
            ByteBuffer bf = ByteBuffer.allocate( (39500));
            int sampleSize;
            ArrayList<Integer> a = new ArrayList<>();
            MediaFormat format = mx.getTrackFormat(0);
            Log.i(TAG, "Width  : " + format.getInteger(MediaFormat.KEY_WIDTH) + "Height : " + format.getInteger(MediaFormat.KEY_HEIGHT));
            while((sampleSize = mx.readSampleData(bf, 0)) >= 0){

                a.add(sampleSize);
                Log.i(TAG, "SAMPLE SIZE : " + sampleSize + " and " + (sampleSize % 4 == 0?"":"Not ") +"divisble by 4" + " - Sample time " + mx.getSampleTime());

                byteBuffers.add(bf);
                mx.advance();
                bf = ByteBuffer.allocate( (39500));
                //bf = ByteBuffer.allocate((int) (640*360*2));
            }
            int z = -1;
            int index = -1;
            for(int i = 0 ; i < a.size() ; i++){
                Integer s = a.get(i);
                if(s.intValue() > z){
                    z = s.intValue();
                    index = i;
                }
            }
            Log.i(TAG, "MAx : " + z + " With index : " + index);

        } catch (IOException e) {
            e.printStackTrace();
        }
        mx.release();
        mx = null;
        Log.i(TAG, "Buffers size : " + byteBuffers.size());
    }

    public void initRetriever(String path, double d, double f){
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        setVideoProbe(d, f);
        getVideoData(videoFile.getAbsolutePath());
//        mPThread = new PlayerThread();
//        mPThread.start();
        myTextView.setText("ARRAY LENGTH IS : " + byteBuffers.size());
        mRenderer.start();


    }
    public void setVideoProbe(double d, double f){
        durationInSeconds = d;
        frameRate = f;
        step = MICRO_SECODND/frameRate;
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
    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private ByteBuffer[] inputBuffers;
        private ByteBuffer[] outputBuffers;
        public boolean finished = false;

        public PlayerThread() {
            super("PlayThread");
        }
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            Log.i(TAG, "START PLAYER");
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(videoFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                MediaFormat format = extractor.getTrackFormat(0);
                String mime = format.getString(MediaFormat.KEY_MIME);
                extractor.selectTrack(0);
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, null, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (decoder == null) {
                Log.e("DecodeActivity", "Can't find video info!");
                return;
            }

            decoder.start();

            inputBuffers = decoder.getInputBuffers();
            outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();
            int index = 0  ;
            while (!isEOS) {
                Log.i(TAG, "START PLAYER IN LOOP " + index);
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000*5);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                            index ++;
                        }
                    }
                }
                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }

                        decoder.releaseOutputBuffer(outIndex, false);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }


            }

            decoder.stop();
            decoder.release();
            extractor.release();
            printTrace();
            finished = true;
        }
        public void printTrace(){
            for(int i = 0 ; i< outputBuffers.length; i++){
                Log.i(TAG, "SIZE : " + outputBuffers[i].remaining());
            }
            Log.i(TAG, "ARRAY LENGTH " + outputBuffers.length);
            outputBuffers[0].rewind();
            byte[] myByte = new byte[outputBuffers[0].remaining()];
            int b = 0;
           // outputBuffers[0].get(myByte);
            //ByteBuffer bb = outputBuffers[0].duplicate();
            outputBuffers[0].get(myByte, 10, 50);

//            while(outputBuffers[0].hasRemaining()){
//                myByte[b] = outputBuffers[0].get();
//                b++;
//                outputBuffers[0].get(myByte, 10, 50);
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//                outputBuffers[0].get();
//
//            }
            String s =Arrays.toString(myByte);
            Log.i(TAG, "TEST STRING : " + s);
            appendLog(s);
            int start = 0 ;
            int end = 200;
            int lines = 1;
            while(end < s.length()){
                Log.i(TAG, "LINE : " + lines + " >> " + s.substring(start, end));
                start = end;
                end += 200;
                lines++;
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
            //while(!mPThread.finished);
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
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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
            int i = 0 ;
            while (true && i < 1) {
                Rect dirty = null;
                if (partial) {

                    dirty = new Rect(0, mHeight * 3 / 8, mWidth, mHeight * 5 / 8);
                }
                Canvas canvas = surface.lockCanvas(dirty);
                if (canvas == null) {

                    break;
                }
                try {
                    Paint p=new Paint();
                    options.inSampleSize = mHeight>mWidth?mHeight:mWidth;
                    byteBuffers.get(0).rewind();
                    String s =Arrays.toString(byteBuffers.get(0).array());
                    appendLog(s);
                    int start = 0 ;
                    int end = 200;
                    int lines = 1;
                    while(end < s.length()){
                        Log.i(TAG, "LINE : " + lines + " >> " + s.substring(start, end));
                        start = end;
                        end += 200;
                        lines++;
                    }
                    Log.i(TAG, s.substring(start));
                    int[] px = new int[byteBuffers.get(55).remaining()];
                    int j = 0 ;
                    byteBuffers.get(55).rewind();

                    while(byteBuffers.get(55).hasRemaining()){
                        px[j] = byteBuffers.get(55).get();
                        j++;
                    }
                    byteBuffers.get(0).rewind();
                    Log.i(TAG, "WIDTH : "+mWidth +" H : "+mHeight);

                    // TAG MYRETRIEVERDEBUG
                    //Bitmap decoded ;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byteBuffers.get(0).rewind();
                    YuvImage y = new YuvImage(byteBuffers.get(0).array(), ImageFormat.NV21, 640, 360, null);
                    y.compressToJpeg(new Rect(0, 0, 640, 360), 50, out);
                    byte[] imageBytes = out.toByteArray();
                    byte[] rgb = new byte[imageBytes.length * 2];
                    //Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                   // toRGB565(imageBytes, 640, 368, rgb);
                    Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(byteBuffers.get(50).array()), null, options );
                    //decoded.copyPixelsFromBuffer(byteBuffers.get(0));
//                    decoded.setPixels(px, 0, mWidth, 0, 0, mWidth, mHeight );
//                    decoded.compress(Bitmap.CompressFormat.PNG, 90, bos);
                    decoded = Bitmap.createBitmap(createDummyColorsRGBA(640,360), 640,360, Bitmap.Config.ARGB_8888);
                    decoded = Bitmap.createBitmap(createDummyColorsRGBA(640,360), 640,360, Bitmap.Config.ARGB_8888);
                    //decoded.recycle();
                    //Log.i(TAG, "DECODED IMAGE LENGTH " + byteBuffers.get(1).array().length);
                    //decoded = Bitmap.createBitmap(640,360, Bitmap.Config.ARGB_8888);
                    //decoded.setPixels(ByteBuffer.wrap(rgb).asIntBuffer().array(), 0, mWidth, 0, 0, mWidth, mHeight );
                    Log.i(TAG, "DECODED IMAGE DATA" + ((decoded == null) ? "NIL" : "VALUE"));
                    Bitmap c = Bitmap.createScaledBitmap(decoded, mWidth, mHeight, false);
                    //p.setColor(Color.RED);
                    i++;
                    canvas.drawBitmap(c, 0, 0, p);
                    halt();
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
    private void toRGB565(byte[] yuvs, int width, int height, byte[] rgbs) {
        //the end of the luminance data
        final int lumEnd = width * height;
        //points to the next luminance value pair
        int lumPtr = 0;
        //points to the next chromiance value pair
        int chrPtr = lumEnd;
        //points to the next byte output pair of RGB565 value
        int outPtr = 0;
        //the end of the current luminance scanline
        int lineEnd = width;

        while (true) {

            //skip back to the start of the chromiance values when necessary
            if (lumPtr == lineEnd) {
                if (lumPtr == lumEnd) break; //we've reached the end
                //division here is a bit expensive, but's only done once per scanline
                chrPtr = lumEnd + ((lumPtr  >> 1) / width) * width;
                lineEnd += width;
            }

            //read the luminance and chromiance values
            final int Y1 = yuvs[lumPtr++] & 0xff;
            final int Y2 = yuvs[lumPtr++] & 0xff;
            final int Cr = (yuvs[chrPtr++] & 0xff) - 128;
            final int Cb = (yuvs[chrPtr++] & 0xff) - 128;
            int R, G, B;

            //generate first RGB components
            B = Y1 + ((454 * Cb) >> 8);
            if(B < 0) B = 0; else if(B > 255) B = 255;
            G = Y1 - ((88 * Cb + 183 * Cr) >> 8);
            if(G < 0) G = 0; else if(G > 255) G = 255;
            R = Y1 + ((359 * Cr) >> 8);
            if(R < 0) R = 0; else if(R > 255) R = 255;
            //NOTE: this assume little-endian encoding
            rgbs[outPtr++]  = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++]  = (byte) ((R & 0xf8) | (G >> 5));

            //generate second RGB components
            B = Y2 + ((454 * Cb) >> 8);
            if(B < 0) B = 0; else if(B > 255) B = 255;
            G = Y2 - ((88 * Cb + 183 * Cr) >> 8);
            if(G < 0) G = 0; else if(G > 255) G = 255;
            R = Y2 + ((359 * Cr) >> 8);
            if(R < 0) R = 0; else if(R > 255) R = 255;
            //NOTE: this assume little-endian encoding
            rgbs[outPtr++]  = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++]  = (byte) ((R & 0xf8) | (G >> 5));
        }
    }
    public static int[] createDummyColorsRGBA (int bitmapWidth, int bitmaoHeight){
        int[] colors = new int[bitmapWidth*bitmaoHeight];
        int a = 50;
        int r = 50;
        int g = 50;
        int b = 50;
        for(int i = 0 ; i < colors.length ; i++){
            Color c = new Color();
//            if(i % bitmapWidth == 0){
//                r = (r+50)%255;
//                g = (r+50)%255;
//                b = (r+50)%255;
//
//
//            }
            colors[i] = c.argb(a, r, g, b);
        }
        return colors;


    }
    public void appendLog(String text)
    {
        File logFile = new File("sdcard/log.txt");
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
