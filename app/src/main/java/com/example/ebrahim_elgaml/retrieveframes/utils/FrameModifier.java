package com.example.ebrahim_elgaml.retrieveframes.utils;

import android.util.Log;

import com.example.ebrahim_elgaml.retrieveframes.MainActivity;

import java.util.PriorityQueue;

/**
 * Created by ebrahim-elgaml on 4/12/16.
 */
public class FrameModifier {
    private static int diffKeyFrames; // No. of frames between two keyFrames
    private PriorityQueue<FrameHolder> pQueue;

    public FrameModifier(PriorityQueue<FrameHolder> q){
        FrameHolder previous = q.remove();
        int index = 1;
        pQueue = new PriorityQueue<FrameHolder>(100, new IntegerComp());
        pQueue.add(previous);
        MainActivity.frameHolders.add(previous);
        int myDiff = ++diffKeyFrames ;
        while(!q.isEmpty()){
            Log.i("MYRETRIEVERDEBUG", "KEYFRAME : " + index);
            if(index % myDiff == 0){
                previous = q.remove();
            }else{
                previous = FrameHolder.createCopyFrame(previous);
                FrameHolder temp = q.remove();
                previous.makeDiff(temp);
            }
            pQueue.add(previous);
            MainActivity.frameHolders.add(previous);
            index++;
//            if(index == 8){
//                break;
//            }
        }
    }

    public static int getDiffKeyFrames() {
        return diffKeyFrames;
    }

    public static void setDiffKeyFrames(int diffKeyFrames) {
        FrameModifier.diffKeyFrames = diffKeyFrames;
    }

    public PriorityQueue<FrameHolder> getpQueue() {
        return pQueue;
    }

    public void setpQueue(PriorityQueue<FrameHolder> pQueue) {
        this.pQueue = pQueue;
    }
    public boolean isEmpty(){
        return pQueue.isEmpty();
    }
    public  FrameHolder remove(){
        return pQueue.remove();
    }
    public int size(){
        return pQueue.size();
    }
    public void recreate(){
        pQueue.clear();
        for(FrameHolder f : MainActivity.frameHolders){
            pQueue.add(f);
        }
    }
}
