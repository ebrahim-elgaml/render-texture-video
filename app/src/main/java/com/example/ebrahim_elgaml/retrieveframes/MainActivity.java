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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.ebrahim_elgaml.retrieveframes.utils.AnimatedGifEncoder;
import com.example.ebrahim_elgaml.retrieveframes.utils.FrameHolder;
import com.example.ebrahim_elgaml.retrieveframes.utils.FrameModifier;
import com.example.ebrahim_elgaml.retrieveframes.utils.IntegerComp;
import com.example.ebrahim_elgaml.retrieveframes.utils.MyView;

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
//    private TextureView mTextureView;
//    private Renderer mRenderer;
    private final int FRAMES_NUMBER_PER_THREAD = 80;
    private ArrayList<FrameRetrieverThread> retrieverThreads = new ArrayList<>();
    private FinalizeThread finalizeThread ;
    private int threadID = 0 ;
    private int cores = Runtime.getRuntime().availableProcessors() * 2;
    private ExecutorService myPool ;
    private final Comparator<FrameHolder> comparator = new IntegerComp();
    private PriorityQueue<FrameHolder> myQueue ;
    private File videoFile;
    private ArrayList<Bitmap> toSave = new ArrayList<>();
    private FrameModifier frameModifier ;
    private final int diffKeyFrames = 6;
    private MyView myView;
    public static ArrayList<FrameHolder> frameHolders = new ArrayList<>();
    private Button myButton;
    private EditText editText;
    private AnimatedGifEncoder encoder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myView = (MyView)findViewById(R.id.myView);
        encoder = new AnimatedGifEncoder();
        myTextView = (TextView)findViewById(R.id.textView);
        myButton = (Button)findViewById(R.id.button);
        editText = (EditText)findViewById(R.id.editText);
        editText.setText("15");
        myButton.setActivated(false);
        myPool = new ThreadPoolExecutor(
                        cores/2,
                        cores,
                        1,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>()
                );
        myQueue = new PriorityQueue<FrameHolder>(100, comparator);
        videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/testing/","test.mp4");
        frameModifier.setDiffKeyFrames(diffKeyFrames);
        initRetriever(videoFile.getAbsolutePath(), 12, 15);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //saveGIV();
                frameModifier.recreate();
                myView.setFrameModifier(frameModifier);
                myView.setCanStart(true);
                int l = 1000/Integer.parseInt(editText.getText().toString());
                myView.setFrameRate(l);
                myView.setPreviousTime(System.currentTimeMillis());
            }
        });

    }

    public void initRetriever(String path, double d, double f){
        progressDialog = ProgressDialog.show(MainActivity.this, "",
                "Loading ...", true);
        progressDialog.setCancelable(false);
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
                    myButton.setActivated(true);
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
            myView.setFrameModifier(frameModifier);
            myView.setCanStart(true);
            myView.setFrameRate(66);
            myView.setPreviousTime(System.currentTimeMillis());
            started = true;
            return true;
        }
    }
    public static void saveFrames(ArrayList<Bitmap> saveBitmapList, String path) throws IOException{
        Random r = new Random();
        int folder_id = r.nextInt(1000) + 1;

        String folder = Environment.getExternalStorageDirectory()+"/testing/" + path;
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
    public byte[] generateGIF() {
        frameModifier.recreate();
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        int i = 0 ;
        encoder.setRepeat(-1);
       // encoder.setDelay(66);
//        encoder.setFrameRate(15);
        encoder.setSize(320, 180);
        while(!frameModifier.getpQueue().isEmpty()){
    //            if(i % 7 == 0 ){
    //                bitmaps.add(frameModifier.remove().getBitmap());
    //                Log.i(TAG, "ADD BITMAP ");
    //            }else{
    //                frameModifier.remove().getBitmap();
    //            }
            bitmaps.add(frameModifier.remove().getBitmap());
            i++;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(bos);
        for (Bitmap bitmap : bitmaps) {
            encoder.addFrame(bitmap);
        }
        encoder.setDelay(66);
        Log.i(TAG ,"Bitmaps gif size : " + bitmaps.size());
        encoder.finish();
        return bos.toByteArray();
    }
    public void saveGIV(){
//        FileOutputStream outStream = null;
        String folder = Environment.getExternalStorageDirectory()+"";
        File saveFolder=new File(folder);
        File f = new File(saveFolder,("test"+".gif"));
        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(generateGIF());
            fo.flush();
            fo.close();

        } catch (IOException e) {
            Log.i(TAG, "EXCEPTION ");
            e.printStackTrace();
        }
//        try{
//            outStream = new FileOutputStream("/sdcard/test.gif");
//            outStream.write(generateGIF());
//            outStream.close();
//        }catch(Exception e){
//            Log.i(TAG, "EXCEPTION ");
//            e.printStackTrace();
//        }
    }


}
