package com.example.ebrahim_elgaml.retrieveframes.utils;

import java.util.Comparator;

/**
 * Created by ebrahim-elgaml on 4/4/16.
 */
public class IntegerComp implements Comparator<FrameHolder> {

    @Override
    public int compare(FrameHolder lhs, FrameHolder rhs) {
        int diff = lhs.getPriority() - rhs.getPriority();
        if(diff == 0){
            diff = lhs.getIndex() - rhs.getIndex();
        }
        return  diff ;
    }
}
