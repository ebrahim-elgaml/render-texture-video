package com.example.ebrahim_elgaml.retrieveframes;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
       // getVideoFrames(videoFile.getAbsolutePath());

//        Log.i(TAG, "Width : " + p.getWidth() + "Height : " + p.getHeight());
  //      Log.i(TAG, "FrAMES CoUNT  : " + frames.size());
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

}
